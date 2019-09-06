
/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * MainActivity.java
 */
package sleep.mobile.mobilesleepdetection;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/* This Activity class is where the UIThread begins and initializes all the
 * classes needed for this program.
 *
 */

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "sleepDetection252";

    public static MainActivity mainActivity;

    public static int sleepMode = 0;

    // contains static references of all the controllers
    public static BluetoothManager bluetoothController;
    public static WearableManager wearableManager;
    public static GUIManager guiManager;
    public static DataAnalyzer dataAnalyzer;
    public static CameraManager cameraManager;

    private RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this;

        // the app will by default set the volume of the tts to 8
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(am.STREAM_MUSIC, 8, 0);

        // this app should take up the full screen of the phone
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        guiManager = new GUIManager(this);
        CameraManager cc = new CameraManager(this);

        // the surface view and the camera controller will be organized in a relative layout to predefine coordinates
        relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
        relativeLayout.setLayoutParams(rlp);

        relativeLayout.addView(guiManager, rlp);

        setContentView(relativeLayout, rlp);

        bluetoothController = new BluetoothManager();
        wearableManager = new WearableManager();
        dataAnalyzer = new DataAnalyzer();
    }

    /* Any action concerning views or UI must be dealt with by the UIThread.
     * This method needs to display a new CameraController view when the user
     * enters the SleepMode for facial recognition
     *
     */
    public void initCameraController(){
        runOnUiThread(new Thread(new Runnable(){

            public void run(){
                cameraManager = new CameraManager(MainActivity.this);
                RelativeLayout.LayoutParams paramsCamera = new RelativeLayout.LayoutParams(300  , 400);
                paramsCamera.leftMargin = GUIManager.width/2 - 150;
                paramsCamera.topMargin = 350;
                relativeLayout.addView(cameraManager, paramsCamera);
            }

        }));

    }

    public void destroyCameraController(){
        runOnUiThread(new Thread(new Runnable(){
            public void run(){
                relativeLayout.removeView(cameraManager);
            }
        }));
    }


    /*
    Meant to speed up gradle build
    Since I am using multiple libraries such as google play vision and wearble, the amount
    of methods would often overflow and multiple dex is neccesary.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    @Override
    protected void onStart(){
        super.onStart();
        bluetoothController.connectToBluetooth(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }





}
