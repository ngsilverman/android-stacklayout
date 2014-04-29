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

    // TODO  Factor in the screen density
    private static final int SCROLL_THRESHOLD = 30;

    // TODO  Factor in the screen density
    private static final int SWEEP_THRESHOLD = 200;

    /**
     * Duration of the spring back to the default position when the top child
     * is released.
     */
    private static final int SPRINGBACK_DURATION = 200;

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

    /** Whether or not the top child is locked in a horizontal scroll */
    private boolean mIsScrollingHorizontally;

    /** Whether or not the top child is locked in a vertical scroll */
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            mIsTouching = false;

            if (!mScroller.computeScrollOffset()) {
                springBack();
                layoutTopChild();
            }
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

        /*
         * The fling is insufficient to swipe away the top child so spring back
         * to the default position immediately instead.
         */
        if (!isSweptAway(mScroller.getFinalX(), mScroller.getFinalY())) {
            springBack();
        }
    }

    /**
     * @param left  Left position of the top child, relative to its parent.
     * @param top  Top position of the top child, relative to its parent.
     * @return  Whether or not the top child being positioned at these
     *          coordinates should trigger a sweep-away.
     */
    private boolean isSweptAway(int left, int top) {
        return Math.abs(left) >= getWidth() - SWEEP_THRESHOLD || Math.abs(top) >= getHeight() - SWEEP_THRESHOLD;
    }

    /**
     * Initiates a spring back the of top child to the default position. Be
     * sure to call {@link #layoutTopChild} following this method to start the
     * spring.
     */
    private void springBack() {
        mScroller.forceFinished(true);
        mScroller.startScroll(mChildLeft, mChildTop, -mChildLeft, -mChildTop, SPRINGBACK_DURATION);
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
                /*
                 * TODO  If swept away but still visible animate to the nearest
                 *       edge.
                 */

                removeView(topChild);

                if (mInfinite) {
                    addView(topChild, 0);
                }

                mScroller.forceFinished(true);

                mChildLeft = 0;
                mChildTop = 0;
            }
        }
    }
}
