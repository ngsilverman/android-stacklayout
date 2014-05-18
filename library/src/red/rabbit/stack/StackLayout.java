package red.rabbit.stack;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.OverScroller;

public class StackLayout extends FrameLayout {

    /*
     * TODO  Factor in the screen density
     * TODO  Reduce if scroll threshold overlaps sweep threshold
     *       (ex. if view is very small)
     */
    private static final int SCROLL_THRESHOLD = 30;

    /**
     * This value is relative to the width of the screen. If the top child
     * travels further than that proportion of the screen it should be swept
     * away.
     */
    private static final float SWEEP_THRESHOLD = .4f;

    /**
     * Duration of the spring back to the default position when the top child
     * is released.
     */
    private static final int SPRING_DURATION = 200;

    /** 
     * If set to true swept views will be added back to the bottom of the
     * stack, otherwise they are removed entirely.
     */
    private boolean mInfinite = false;

    /** Used to detect scrolls and flings. */
    private GestureDetector mGestureDetector;

    /** Used to handle flings and spring backs. */
    private OverScroller mScroller;

    /** Whether or not the user is currently touching the screen */
    private boolean mIsTouching;

    /**
     * Whether or not the top child is currently hovering in a position that
     * can trigger a sweep.
     */
    private boolean mIsHovering;

    /** Whether or not the top child is locked in a horizontal scroll. */
    private boolean mIsScrollingHorizontally;

    /** Whether or not the top child is locked in a vertical scroll. */
    private boolean mIsScrollingVertically;

    /** Left position of the top child, relative to this view. */
    private int mChildLeft;

    /** Top position of the top child, relative to this view. */
    private int mChildTop;

    /** Bottom position of this view, relative to its parent. */
    private int mBottom;

    /** Left position of this view, relative to its parent. */
    private int mLeft;

    /** Right position of this view, relative to its parent. */
    private int mRight;

    /** Top position of this view, relative to its parent. */
    private int mTop;

    /** If set, swipe and hover events will be sent to this listener. */
    private StackListener mListener;

    public StackLayout(Context context) {
        super(context);
        init(context, null);
    }

    public StackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mGestureDetector = new GestureDetector(context, new GestureListener());
        mScroller = new OverScroller(context);

        if (attrs != null) {
            final TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.StackLayout,
                    0, 0);

            try {
                mInfinite = a.getBoolean(R.styleable.StackLayout_infinite, false);
            } finally {
                a.recycle();
            }
        }
    }

    /** Register callbacks to be invoked when swipes and hovers occur. */
    public void setListener(StackListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (!mScroller.computeScrollOffset()) {
                triggerSpring(mChildLeft, mChildTop);
                layoutTopChild();
            }

            mIsTouching = false;
        }

        return true;
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            mIsTouching = true;

            mScroller.forceFinished(true);

            layoutTopChild();

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            /*
             * If locked in a directional scroll, reset the idle position and
             * increment the other. Otherwise increment both.
             */
            mChildLeft += mIsScrollingVertically ? -mChildLeft : distanceX;
            mChildTop += mIsScrollingHorizontally ? -mChildTop : distanceY;

            layoutTopChild();

            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            fling((int) -velocityX, (int) -velocityY);

            layoutTopChild();

            return true;
        }
    }

    /**
     * Initiates a fling of the top child. Be sure to call
     * {@link #layoutTopChild} following this method to start the fling.
     */
    private void fling(int velocityX, int velocityY) {
        mScroller.forceFinished(true);
        mScroller.fling(
                mChildLeft, mChildTop,
                !mIsScrollingVertically ? velocityX : 0, !mIsScrollingHorizontally ? velocityY : 0,
                -getWidth(), getWidth(), -getHeight(), getHeight(),
                0, 0);

        triggerSpring(mScroller.getFinalX(), mScroller.getFinalY());
    }

    /**
     * @param left  Left position of the top child, relative to its parent.
     * @param top  Top position of the top child, relative to its parent.
     * @return  Given the position of the top child, will either spring it back
     *          to its default position or sweep it away.
     */
    private void triggerSpring(int left, int top) {
        if (isOverSweepThreshold(left, top)) {
            springForward();
        } else {
            springBack();
        }
    }

    /**
     * @param left  Left position of the top child, relative to its parent.
     * @param top  Top position of the top child, relative to its parent.
     * @return  Whether or not the top child being positioned at these
     *          coordinates should trigger a sweep-away.
     */
    private boolean isOverSweepThreshold(int left, int top) {
        return Math.abs(left) >= getWidth() * SWEEP_THRESHOLD || Math.abs(top) >= getHeight() * SWEEP_THRESHOLD;
    }

    /**
     * @param left  Left position of the top child, relative to its parent.
     * @param top  Top position of the top child, relative to its parent.
     * @return  Whether or not the top child being positioned at these
     *          coordinates is completely swept-away.
     */
    private boolean isSweptAway(int left, int top) {
        return Math.abs(left) >= getWidth() || Math.abs(top) >= getHeight();
    }

    /**
     * Initiates a spring to the final swept-away position the of top child. Be
     * sure to call {@link #layoutTopChild} following this method to start the
     * spring.
     */
    private void springForward() {
        mScroller.forceFinished(true);
        mScroller.startScroll(
                mChildLeft,
                mChildTop,
                mChildLeft > 0 ? getWidth() - mChildLeft : - getWidth() - mChildLeft,
                mChildTop > 0 ? getHeight() - mChildTop : - getHeight() - mChildTop,
                SPRING_DURATION);
    }

    /**
     * Initiates a spring back the of top child to the default position. Be
     * sure to call {@link #layoutTopChild} following this method to start the
     * spring.
     */
    private void springBack() {
        mScroller.forceFinished(true);
        mScroller.startScroll(mChildLeft, mChildTop, -mChildLeft, -mChildTop, SPRING_DURATION);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;

        layoutTopChild();

        // Layout all the children except the top one
        for (int i = 0; i < getChildCount() - 1; i++) {
            getChildAt(i).layout(mLeft, mTop, mRight, mBottom);
        }
    }

    /**
     * Positions the top child appropriately and takes care of ongoing flings
     * and spring-backs.
     */
    private void layoutTopChild() {
        final View topChild = getChildAt(getChildCount() - 1);
        if (topChild != null) {
            // A fling or spring operation is active.
            if (mScroller.computeScrollOffset()) {
                /*
                 * If locked in a directional scroll, reset the idle position
                 * and increment the other. Otherwise increment both.
                 */
                mChildLeft = mIsScrollingVertically ? 0 : mScroller.getCurrX();
                mChildTop = mIsScrollingHorizontally ? 0 : mScroller.getCurrY();

                post(new Runnable() {
                    @Override
                    public void run() {
                        layoutTopChild();
                    }
                });
            }

            if (!mIsScrollingHorizontally && !mIsScrollingVertically) {
                if (Math.abs(mChildLeft) > SCROLL_THRESHOLD) {
                    mIsScrollingHorizontally = true;
                } else if (Math.abs(mChildTop) > SCROLL_THRESHOLD) {
                    mIsScrollingVertically = true;
                }
            } else if (Math.abs(mChildLeft) <= SCROLL_THRESHOLD && Math.abs(mChildTop) <= SCROLL_THRESHOLD) {
                mIsScrollingHorizontally = false;
                mIsScrollingVertically = false;
            }

            if (mIsScrollingHorizontally) {
                topChild.layout(mLeft - mChildLeft, mTop, mRight - mChildLeft, mBottom);
            } else if (mIsScrollingVertically) {
                topChild.layout(mLeft, mTop - mChildTop, mRight, mBottom - mChildTop);
            } else {
                topChild.layout(mLeft, mTop, mRight, mBottom);
            }

            if (!mIsTouching && isSweptAway(mChildLeft, mChildTop)) {
                onSweptAway();

                removeView(topChild);

                if (mInfinite) {
                    addView(topChild, 0);
                }

                mScroller.forceFinished(true);

                mChildLeft = 0;
                mChildTop = 0;
            } else {
                if (isOverSweepThreshold(mChildLeft, mChildTop)) {
                    onHover();
                } else {
                    onNotHover();
                }
            }
        }
    }

    private void onNotHover() {
        if (mIsHovering) {
            mIsHovering = false;
            if (mListener != null) mListener.onCancel();
        }
    }

    private void onHover() {
        if (!mIsHovering) {
            mIsHovering = true;

            if (mListener != null) {
                if (mIsScrollingHorizontally) {
                    if (mChildLeft > 0) mListener.onHoverLeft();
                    else mListener.onHoverRight();
                } else if (mChildTop > 0) mListener.onHoverTop();
                else mListener.onHoverBottom();
            }
        }
    }

    private void onSweptAway() {
        mIsHovering = false;

        if (mListener != null) {
            if (mIsScrollingHorizontally) {
                if (mChildLeft > 0) mListener.onSweepLeft();
                else mListener.onSweepRight();
            } else if (mChildTop > 0) mListener.onSweepTop();
            else mListener.onSweepBottom();
        }
    }
}
