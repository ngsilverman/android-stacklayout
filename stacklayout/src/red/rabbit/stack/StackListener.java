package red.rabbit.stack;

public interface StackListener {

    public void onHoverBottom();
    public void onHoverLeft();
    public void onHoverRight();
    public void onHoverTop();

    public void onSweepBottom();
    public void onSweepLeft();
    public void onSweepRight();
    public void onSweepTop();

    public void onCancel();
}
