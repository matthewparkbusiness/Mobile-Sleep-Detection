/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * WearGUIController.java
 */

package sleep.mobile.mobilesleepdetection;

/**
 * Created by Jamba Juice on 3/11/2017.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/* This class simply controls the screen display for the android watch.
 * For now, it will only display a this program's logo for confirmation that
 * the bluetooth connection has been estabilished.
 *
 */

public class WearGUIController extends SurfaceView implements Runnable {

    // declare background thread that draw on Canvas
    Thread thread;
    SurfaceHolder holder;
    boolean threadAlreadyStarted;

    int width;
    int height;

    // declare colors used in this class
    Paint red;
    Paint black;
    Paint white;

    public WearGUIController(Context c){
        super(c);
        this.setKeepScreenOn(true);
        white = new Paint();
        red = new Paint();
        red.setColor(Color.RED);

        white = new Paint();
        white.setColor(Color.WHITE);
        white.setTextSize(20);

        black = new Paint();
        black.setColor(Color.BLACK);

        thread = new Thread(this);

        holder = this.getHolder();
        holder.addCallback(new SurfaceHolder.Callback(){

                    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}

                    public void surfaceCreated(SurfaceHolder arg0) {
                        if(!threadAlreadyStarted){
                            thread.start();
                            threadAlreadyStarted = true;
                        }
                    }

                    public void surfaceDestroyed(SurfaceHolder arg0) {}
                });
    }


    public void run(){

        Canvas c = holder.lockCanvas();
        width = c.getWidth();
        height = c.getHeight();

        holder.unlockCanvasAndPost(c);

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.moon);
        bm = Bitmap.createScaledBitmap(bm, 200, 200, false);
        while(true){
            c = holder.lockCanvas();
            if(c == null) break;
            c.drawColor(Color.BLACK);

            // draw the moon logo only
            c.drawBitmap(bm, width/2 - 100, height/2 - 100, white);

            holder.unlockCanvasAndPost(c);
            try{Thread.currentThread().sleep(15);} catch(InterruptedException ie){ }

        }
    }



}
