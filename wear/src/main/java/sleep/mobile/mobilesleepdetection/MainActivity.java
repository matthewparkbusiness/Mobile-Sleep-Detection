/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * MainActivity.java
 */

package sleep.mobile.mobilesleepdetection;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/* This class extends Activity and is where the main UI thread begins for the
 * wearable device. Due to convenience, I decided to make this class be the
 * MessageListener and control all the sensors rather than for the mobile
 * device program that delegates responsabilities to different clases. This
 * class mainly uses the standard heart rate and accelerometer that come
 * standard with most wearable devices on the google play market.
 *
 */

public class MainActivity extends Activity implements MessageApi.MessageListener, SensorEventListener {

    public static int heartRate = 0;
    private boolean sendingSensorData = false;

    private GoogleApiClient client;
    private String nodeId;
    private List<Node> nodesList;

    private static MessageSender messageSender ;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // WearGUIController is a SurfaceView that controls any screen output
        WearGUIController guiController = new WearGUIController(this);
        setContentView(guiController);


        // register sensors to the system service
        mSensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        messageSender =  new MessageSender();
        initializePhoneConnection(this);
    }

    /* When the user exits the app, ther is no point for the application
     * to continue running and use up battery. Therefore, the activity can finish.
     *
     */
    public void onPause(){
        super.onPause();
        mSensorManager.unregisterListener(this);
        finish();
    }

     /*
      * This method is called when the mobile device sends data.
      * For my program, the watch initiates interaction by sending data to the phone.
      *
      */
     public void onMessageReceived(MessageEvent e) {
         if(e.getPath().equals("/sensor")){
             String data = new String(e.getData());
             if(data.equals("begin recording")){
                 sendingSensorData = true;
             }
         }
    }


    private void initializePhoneConnection(Context context) {
        new Thread(new Runnable(){

            public void run(){
                client = new GoogleApiClient.Builder(MainActivity.this).addApi(Wearable.API).build();
                client.connect();
                while(!client.isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }

                Wearable.MessageApi.addListener(client, MainActivity.this);
                NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(client).await();
                nodesList = result.getNodes();

                sendMessage("register watch");
            }

        }).start();

    }

    /*
     * This class sends data to the phone.
     * This Runnable object is necesary because the sendMessage method can only be run
     * by the UI thread.
     *
     */
    public class MessageSender implements Runnable{
        String message = "This message is sent to the phone.";

        public void run(){
            for(int i=0;i<nodesList.size();i++){
                Wearable.MessageApi.sendMessage(client, nodesList.get(i).getId(), "/sensorsent", new String(message).getBytes()).await();
            }
        }
    }

    public static void sendMessage(String message){
        messageSender.message = message;
        new Thread(messageSender).start();
    }


    long lastTimeHeartRate;
    long lastTimeAccerometer;

    /* This method receives all the sensor data from heart rate and accelerometer.
     * It will send this information to the phone to be processed.
     *
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_HEART_RATE){
            heartRate = (int)event.values[0];
            if(sendingSensorData){
                if(System.currentTimeMillis() -lastTimeHeartRate> 5000){
                    sendMessage("HR"+heartRate);
                    lastTimeHeartRate = System.currentTimeMillis();
                }
            }
        }
        else if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            float ax=event.values[0];
            float ay=event.values[1];
            float az=event.values[2];

            double totalAcceleration = Math.sqrt(ax*ax + ay*ay + az*az);
            if(sendingSensorData){
                if(System.currentTimeMillis() -lastTimeAccerometer> 5000){
                    sendMessage("AC"+totalAcceleration);
                    lastTimeAccerometer = System.currentTimeMillis();
                }
            }
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


}
