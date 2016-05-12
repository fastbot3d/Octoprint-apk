package android.app.printerapp.util.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by root on 16-3-17.
 */
public class DisableFrameLayout extends FrameLayout {

    public DisableFrameLayout(Context context) {
        super(context);
    }

    public DisableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DisableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private boolean touch_disabled=false;

    public void disable_touch(boolean b) {
        touch_disabled =b;
    }


    public boolean onInterceptTouchEvent (MotionEvent ev){
        return touch_disabled;
    }

}
