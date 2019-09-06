/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * GUIController.java
 */

package sleep.mobile.mobilesleepdetection;

/*
Import necessary libraries for GUI goals; Canvas, Color, Bitmap
 */


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Matthew Park on 2/16/2017.
 *
 * Primarily focuses on any GUI display that the user may interact with
 * visually. This class extends SurfaceView and uses a separate loop-thread
 * that draws on the surface's Canvas. I decided to draw my own
 * UI components rather than use Android's preset, since I can customly
 * design the look and feel of each component and use images easier.
 *
 *
 */

public class GUIManager extends SurfaceView implements Runnable {   // runs loop controlling canvas

    // width and height of the canvas
    public static int width;
    public static  int height;

    // guiThread runs "this" class when the SurfaceHolder is prepared
    Thread guiThread;
    SurfaceHolder holder;
    boolean threadAlreadyStarted;

    // colors used for GUI
    Paint white;
    Paint blue;

    //Variables used for GUI positioning and images
    int switchX;
    int switchWidth;
    int switchHeight;
    Bitmap switchBitmap;
    boolean switchDragging;
    Bitmap bluetooth;
    Bitmap bluetoothShadow;
    Bitmap moon;

    private SensorControllerCreater sensorControllerCreater = new SensorControllerCreater();
    private SleepSensorProcess sleepSensorProcess;
    private boolean sleepSensorProcessRunning;

    public GUIManager(Context c) {
        super(c);

        // initialize Paint objects
        white = new Paint();
        white.setColor(Color.WHITE);
        white.setStyle(Paint.Style.FILL);
        white.setStrokeWidth(20);
        white.setTextSize(100);

        blue = new Paint();
        blue.setColor(Color.CYAN);
        blue.setStyle(Paint.Style.FILL);
        blue.setTextSize(40);

        // load all images and get dimensions
        switchBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.switchimage);
        bluetooth = BitmapFactory.decodeResource(getResources(), R.drawable.bluetooth);
        bluetoothShadow = BitmapFactory.decodeResource(getResources(), R.drawable.bluetoothshadow);

        moon = BitmapFactory.decodeResource(getResources(), R.drawable.moon);
        moon = Bitmap.createScaledBitmap(moon, 200, 200, false);

        switchWidth = switchBitmap.getWidth();
        switchHeight = switchBitmap.getHeight();

        guiThread = new Thread(this);

        // get SurfaceHolder to trigger guiThread and get Canvas
        holder = this.getHolder();
        holder.addCallback(

                new SurfaceHolder.Callback() {

                    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}

                    public void surfaceCreated(SurfaceHolder arg0) {
                        // once the surface is created, start the guiThread
                        // threadAlreadyStarted is there just to make sure there are not more than one guiThreads running at once
                        if (!threadAlreadyStarted) {
                            guiThread.start();
                            threadAlreadyStarted = true;
                        }
                    }

                    public void surfaceDestroyed(SurfaceHolder arg0) {}

                });
    }


    /*
    guiThread centrally takes care of the graphic interface of the program.
    At first, this method will display a bouncing bluetooth animation for
    telling the user that the program is initializing the bluetooth. Then,
    the plan is to display a switch on the screen that the user can use to
    turn on and off the power switch whenever they choose. During sleep mode
    this method will then display a clock and a soothing background.
     */
    public void run() {
        Canvas c = holder.lockCanvas();
        width = c.getWidth();
        height = c.getHeight();

        holder.unlockCanvasAndPost(c);

        // coordinates used for GUI - bouncing bluetooth symbol animation using acceleration and velocity
        int bluetoothBoundaryY = 400;
        double bluetoothVY = -20;
        double bluetoothY = 400;

        // while the bluetooth is loading, display a bluetooth symbol bouncing
        while(!BluetoothManager.READY){
            c = holder.lockCanvas();
            if (c == null) break;

            c.drawColor(Color.BLACK);
            c.drawBitmap(bluetoothShadow, width/2 - bluetoothShadow.getWidth()/2, (int)bluetooth.getHeight() + 20 + bluetoothBoundaryY - bluetoothShadow.getHeight()/2, white);

            bluetoothVY = bluetoothVY + 1;
            bluetoothY = (int)(bluetoothY + bluetoothVY);
            c.drawBitmap(bluetooth, width/2 - bluetooth.getWidth()/2, (int)bluetoothY, white);

            // reupdate velocities and positions
            if(bluetoothY > bluetoothBoundaryY){
                bluetoothY = bluetoothBoundaryY;
                bluetoothVY = -20;
            }

            holder.unlockCanvasAndPost(c);
            try {
                Thread.currentThread().sleep(10); // aiming for smooth animations
            } catch (InterruptedException ie) {}

        }
        // since the bluetooth is set, sleepMode is set to 1
        MainActivity.sleepMode = 1;
        MainActivity.bluetoothController.communicateWithSwitchDevice(false);


        while (true) {
            c = holder.lockCanvas();
            if (c == null) break;
            // background is always black
            c.drawColor(Color.BLACK);

            if(MainActivity.sleepMode == 1){
                // drawing the cyan switch with on and off
                if(switchDragging) c.drawRect(switchX, 100, (int)(switchX + switchWidth/2), 100 + switchBitmap.getHeight(), blue);
                else if(BluetoothManager.SWITCH_ON) c.drawRect(width/2 - switchBitmap.getWidth()/2, 100, width/2, 100 + switchBitmap.getHeight(), blue);
                else c.drawRect(width/2, 100, switchBitmap.getWidth()/2 + width/2, 100 + switchBitmap.getHeight(), blue);

                // the text for on and off are on a separate bitmap
                c.drawBitmap(switchBitmap, width/2 - switchBitmap.getWidth()/2, 100, white);

            }
            else if(MainActivity.sleepMode == 2) {
                c.drawBitmap(moon, width/2 - 100, 50, white);
                if (sleepSensorProcess.countingDown) {
                    c.drawText((5 - (int) ((System.currentTimeMillis() - sleepSensorProcess.timeOfCountDown) / 1000)) + "",450, 150, white);
                }
                c.drawText("Heart rate (bpm) :"+ MainActivity.dataAnalyzer.lastHeartRateData, 50, 850, blue);
                c.drawText("Left eye probability : " + MainActivity.dataAnalyzer.lastLeftEyeBlinkData, 50, 900, blue);
                c.drawText("Right eye probability : " + MainActivity.dataAnalyzer.lastRightEyeBlinkData, 50, 950, blue);
                c.drawText("Accelerometer data : " + MainActivity.dataAnalyzer.lastAccelerometerData, 50, 1000, blue);

                c.drawText("Composite probability : " + DataAnalyzer.compositeProbability, 50, 1050, blue);

            }
            holder.unlockCanvasAndPost(c);
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException ie) {}

        }
    }

    /*
    Touch event is used for the toggle switch and the buttons that
    jump to both settings and wearable connectivity. Touch-screen
    events are also used during sleep detection where if the user touches
    the screen, the timer would reset.

     */
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                if(y > 100 && y < 100 + switchHeight){
                    switchX = (int)x - switchWidth/4;
                    switchDragging = true;
                }

                break;
            case MotionEvent.ACTION_UP:
                if(x < width/2){
                    // -- if the user switches the toggle on
                    BluetoothManager.communicateWithSwitchDevice(false);
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    MainActivity.mainActivity.runOnUiThread(sensorControllerCreater);
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.mainActivity);
                    builder.setMessage("Turn on Sleep Mode?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
                }
                else{
                    // -- if the user switches the toggle off
                    BluetoothManager.communicateWithSwitchDevice(true);
                }
                switchDragging = false;
        }

        return true;
    }



    /*
    SensorControllerCreater is a Runnable object meant for the GUIController
    to recreate everytime the user wishes to use SleepMode. This is necesary
    because the SensorControllerProcess contains its own lifecycle and will
    be much easier to recreate the object rather than try to reset the object.

     */

    public class SensorControllerCreater implements Runnable{

        public void run(){


            sleepSensorProcess = new SleepSensorProcess();
            sleepSensorProcess.init(MainActivity.mainActivity);

            MainActivity.sleepMode = 2;
            MainActivity.mainActivity.initCameraController();
        }


    }


}