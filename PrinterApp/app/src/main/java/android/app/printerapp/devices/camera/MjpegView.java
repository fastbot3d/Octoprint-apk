package android.app.printerapp.devices.camera;

import android.app.printerapp.Log;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;


/**
 * External class
 * @author alberto-baeza
 *
 */

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "MjpegView";

    public final static int POSITION_UPPER_LEFT  = 9;
    public final static int POSITION_UPPER_RIGHT = 3;
    public final static int POSITION_LOWER_LEFT  = 12;
    public final static int POSITION_LOWER_RIGHT = 6;

    public final static int SIZE_STANDARD   = 1; 
    public final static int SIZE_BEST_FIT   = 4;
    public final static int SIZE_FULLSCREEN = 8;

    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;    
    private boolean showFps = false;
    private boolean mRun = false;
    private boolean surfaceDone = false;
    private boolean mStandby = false;
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayBackgroundColor;
    private int ovlPos;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;

    private boolean mIOIsRight = false;

    public class MjpegViewThread extends Thread {
        private SurfaceHolder mSurfaceHolder;
        private int frameCounter = 0;
        private long start;
        private Bitmap ovl;

        public MjpegViewThread(SurfaceHolder surfaceHolder, Context context) {
            mSurfaceHolder = surfaceHolder;
        }

        private Rect destRect(int bmw, int bmh) {
            int tempx;
            int tempy;
            if (displayMode == MjpegView.SIZE_STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_BEST_FIT) {
                float bmasp = (float) bmw / (float) bmh;
                bmw = dispWidth;
                bmh = (int) (dispWidth / bmasp);
                if (bmh > dispHeight) {
                    bmh = dispHeight;
                    bmw = (int) (dispHeight * bmasp);
                }
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);

                //Log.d("lkj camera ", "destRect w=" + bmw + ", h=" + bmh);

                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegView.SIZE_FULLSCREEN){
                return new Rect(0, 0, dispWidth, dispHeight);
            }
            return null;
        }

        public void setSurfaceSize(int width, int height) {
            synchronized(mSurfaceHolder) {
                dispWidth = width;
                dispHeight = height;
                Log.d("lkj-CAMERA", "setSurfaceSize dispWidth=" + dispWidth + ",dispHeight=" + dispHeight);
            }
        }

        private Bitmap makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            int bwidth  = b.width()+2;
            int bheight = b.height()+2;
            Bitmap bm = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            p.setColor(overlayBackgroundColor);
            c.drawRect(0, 0, bwidth, bheight, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left + 1, (bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);

            return bm;           
        }

        public void run() {
            start = System.currentTimeMillis();
            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
            Bitmap bm = null;
            int width;
            int height;
            Rect destRect;
            Canvas c = null;
            Paint p = new Paint();
            String fps;
            boolean has_exp = false;

            Log.d("lkj-CAMERA", "run 0 ");

            while (true) {
               // Log.d("lkj-CAMERA", "surfaceDone=" + surfaceDone + ",mStandby=" + mStandby + ",mIOIsRight=" + mIOIsRight);
                if(surfaceDone && !mStandby && mIOIsRight) {
                    has_exp = false;
                    try {
                     //   Log.d("lkj-CAMERA", "run 2");
                        if (mIn != null){
                            bm = mIn.readMjpegFrame2();
                        }
                        if (bm==null){
                            Log.d("lkj-CAMERA", "bm==null");
                            continue;
                        }
                        destRect = destRect(bm.getWidth(),bm.getHeight());
                        //c.drawColor(Color.BLACK);

                        c = mSurfaceHolder.lockCanvas();

                        synchronized (mSurfaceHolder) {
                            try {
                               // Log.d("lkj-CAMERA", "run ok.....");
                                c.drawBitmap(bm, null, destRect, p);

                               if((bm!= null) && bm.isRecycled()){
                                    bm.recycle();
                                    bm = null;
                                }
                            } catch (Exception e) {
                                has_exp = true;
                                Log.e("lkj - CAMERA", "thread catch IOException hit in run" + e.toString());
                            }
                        }
                    } catch (IOException e) {
                        mIOIsRight = false;
                        Log.d("lkj-CAMERA", "run IOException....." + e.toString());
                        has_exp = true;
                    } finally {
                        if (c != null) {
                            try {
                                mSurfaceHolder.unlockCanvasAndPost(c);
                            } catch (Exception e){
                                Log.d(TAG, "catch mSurfaceHolder Exception " + e.toString());
                            }
                        }
                        if(mIOIsRight == false){
                            try{
                                if(mIn != null) {
                                    mIn.close();  //lkj why block ???
                                    mIn = null;
                                }
                            } catch (Exception e){
                                Log.d(TAG, "catch IOException " + e.toString());
                            }                            ;
                        }
                    }
                } else {
                    try {
                        //join(100);
                     //   Log.d("lkj-CAMERA", "run sleep");
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

          //  mRun = false;
          //  Log.d(TAG, "mjpeg thread exit");
        }
    }

    private void init(Context context) {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        if (thread == null) {
            thread = new MjpegViewThread(holder, context);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }
        setFocusable(true);
        overlayPaint = new Paint();
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(12);
        overlayPaint.setTypeface(Typeface.DEFAULT);
        overlayTextColor = Color.WHITE;
        overlayBackgroundColor = Color.BLACK;
        ovlPos = MjpegView.POSITION_LOWER_RIGHT;
        displayMode = MjpegView.SIZE_FULLSCREEN;
        dispWidth = getWidth();
        dispHeight = getHeight();
    }
    
    

    public void startPlayback() {
        Log.i("CAMERA", "startPlayback 1");
    }

    public void stopPlayback() {
        Log.i("CAMERA", "Stopped Playback!!");
    }

    public MjpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
        Log.d("lkj-CAMERA", "surfaceChanged");
        if (thread!=null)
            thread.setSurfaceSize(w, h);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceDone = false;
        Log.d("lkj-CAMERA", "surfaceDestroyed");
    }

    public MjpegView(Context context) {
        super(context);
        init(context);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("lkj-CAMERA", "surfaceCreated");
        surfaceDone = true;
    }

    public void showFps(boolean b) { 
        showFps = b; 
    }

    public void setSource(MjpegInputStream source) {
        try{
            if(mIn != null) {
                mIn.close();  //lkj why block ???
            }
        } catch (Exception e){
            Log.d(TAG, "setSource catch IOException " + e.toString());
        }

        mIn = source;
    }


    public void setDisplayMode(int s) { 
        displayMode = s; 
    }



    /**
     * NEW METHOD
     * 
     * Restarts playback if goes down
     * @param context
     */
    public void restartPlayback(Context context){
        Log.i("CAMERA", "Restarting playback!!");
    }
    
    public boolean getThreadStatus(){
    	return thread.isAlive();
    }
    public void setStandby(boolean b){
        mStandby = b;
    }
    public boolean getStandby(){
        return mStandby;
    }

    public boolean getIOIsRight() {
        return mIOIsRight;
    }
    public void setIOIsRight(boolean b) {
        mIOIsRight = b;
    }

    public boolean isRunning(){
    	return mRun;
    }

}
