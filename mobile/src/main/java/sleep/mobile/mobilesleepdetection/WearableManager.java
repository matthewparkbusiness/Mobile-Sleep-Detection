/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * WearableController.java
 */

package sleep.mobile.mobilesleepdetection;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * Created by Jamba Juice on 3/11/2017.
 */

/* This class is responsible for maintaining the connection with the Android Wear
 * if the user has one. This class uses the Message API from google play and is tied up
 * with the wear project. Other Controller classes use this object to send data to the
 * wearable device.
 *
 */
public class WearableManager implements MessageApi.MessageListener{

    public static boolean usingAndroidWatch = true;
    public static boolean recordingData = false;

    private GoogleApiClient client;
    private String nodeId;
    private List<Node> nodesList;


    private static MessageSender messageSender ;

    public WearableManager(){
        messageSender = new MessageSender();
        client = new GoogleApiClient.Builder(MainActivity.mainActivity).addApi(Wearable.API).build();
        retrieveDeviceNode();
    }

    /* For the communication of this class, the mobile device will wait for the android wear to
     * send "register watch" and then receives information about heart rate and accelerometer.
     *
     */
    public void onMessageReceived(MessageEvent e) {
        if(e.getPath().equals("/sensorsent")){
            String data = new String(e.getData());
            Log.d("testting", data);
            if(data.equals("register watch")){
                usingAndroidWatch = true;
                beginRecordingData();
            }
            else if(data.startsWith("HR")){
                MainActivity.dataAnalyzer.addHeartRateData(Integer.parseInt(data.substring(2)));
            }
            else if(data.startsWith("AC")){
                MainActivity.dataAnalyzer.addAccelerometerData(Double.parseDouble(data.substring(2)));
            }
        }
    }

    public void beginRecordingData(){
        recordingData = true;
        sendMessage("begin recording");
    }


    // This method ultimately establishes the connection between the android wear.
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // create connection
                client.connect();
                while(!client.isConnected()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {}
                }

                // get list of devices that the phone has connection to
                Wearable.MessageApi.addListener(client, WearableManager.this);
                NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(client).await();
                nodesList = result.getNodes();
            }
        }).start();
    }


    // this method sends a string to the mobile device through the MessageSender class
    public static void sendMessage(String message){
        messageSender.message = message;
        new Thread(messageSender).start();
    }

    public class MessageSender implements Runnable{
        String message = "This message is sent to the phone.";

        public void run(){
            for(int i=0;i<nodesList.size();i++){
                Wearable.MessageApi.sendMessage(client, nodesList.get(i).getId(), "/sensor", new String(message).getBytes()).await();
            }
        }
    }

}
