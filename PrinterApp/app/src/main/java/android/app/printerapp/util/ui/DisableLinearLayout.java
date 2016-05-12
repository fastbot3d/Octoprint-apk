package android.app.printerapp.util.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by root on 16-3-22.
 */
public class DisableLinearLayout  extends LinearLayout {
    public DisableLinearLayout(Context context) {
        super(context);
    }

    public DisableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DisableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private boolean touch_disabled=false;

    public void disable_touch(boolean b) {
        touch_disabled =b;
    }

    public DisableLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean onInterceptTouchEvent (MotionEvent ev){
        return touch_disabled;
    }

}
