package com.andrewdutcher.geotunes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by andrew on 1/31/15.
 */
public class TouchOverlayView extends View {
    private boolean mEnabled = false;
    private TouchCallback mCallbacks = null;
    private Integer currentTouch = null;

    public TouchOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCaptureEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //super.onTouchEvent(event);
        if (mCallbacks != null) {
            if (currentTouch == null) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    currentTouch = event.getPointerId(0);
                    mCallbacks.onTouchDown(event.getX(0), event.getY(0));
                }
            } else {
                int pointerIndex = event.findPointerIndex(currentTouch);
                if (pointerIndex != -1) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            mCallbacks.onTouchMove(event.getX(pointerIndex), event.getY(pointerIndex));
                            break;
                        case MotionEvent.ACTION_UP:
                            mCallbacks.onTouchUp(event.getX(pointerIndex), event.getY(pointerIndex));
                            currentTouch = null;
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            mCallbacks.onTouchCancel();
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return mEnabled;

    }

    public void registerCallback(TouchCallback callbacks) {
        this.mCallbacks = callbacks;
    }

    public void unregisterCallback() {
        this.mCallbacks = null;
        this.currentTouch = null;
    }

    /*@Override
    public void onDraw(Canvas canvas) {
        Paint dap = new Paint();
        dap.setARGB(255,255,0,0);
        canvas.drawRect(0,0,300,300,dap);
    }*/

    public static abstract class TouchCallback {
        public abstract void onTouchDown(double x, double y);
        public abstract void onTouchMove(double x, double y);
        public abstract void onTouchUp(double x, double y);
        public abstract void onTouchCancel();
    }
}
