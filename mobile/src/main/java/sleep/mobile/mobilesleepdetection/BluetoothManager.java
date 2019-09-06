/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * BluetoothController.java
 */

package sleep.mobile.mobilesleepdetection;

import java.io.*;
import java.util.Set;
import java.util.UUID;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

/* This class maintains the bluetooth connection with the power-switch device.
 * It is responsible for keeping track of whether the device is on or off and being
 * able to switch device on when neccesary. The UUID and bluetooth name are preset
 * but in reality they would be changed depending on the device being used. The device
 * needs to be on while the phone is searching for bluetooth connections.
 *
 */

public class BluetoothManager {

    public static boolean SWITCH_ON;
    public static boolean READY = false;

    public static BluetoothCommunicator bComm;

    /* Preset for my power-switch
    *  In reality, this would change depending on the device's id
    *  */
    private  String UUIDNUM = "00001101-0000-1000-8000-00805F9B34FB";
    private  String bluetoothName = "RNBT-8748";

    private BroadcastReceiver discoveryDevicesReceiver = null;
    private BroadcastReceiver discoveryFinishedReceiver = null;
    private BluetoothDevice bd;
    private BluetoothAdapter bluetoothAdapter;
    private String result;
    private Context context;

    private ServerConnectionThread serverConnectionThread;

    public BluetoothManager(){
        bComm = new BluetoothCommunicator();
    }

    public String getResult(){
        return result;
    }

    public  void setBluetoothName(String name){
        bluetoothName = name;
    }

    public  void connectToBluetooth(Context _context){
        context = _context;
        try{
            /*bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (discoveryDevicesReceiver == null) {
                discoveryDevicesReceiver = new BroadcastReceiver() {
                    
                    // when a new device is found, the onReceive method is called with the Intent
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            
                            // searches for the device with the predefined UUID and bluetooth name
                            if(device.getName().equals(bluetoothName)){
                                Toast.makeText(context, "Found Device", Toast.LENGTH_LONG).show();
                                
                                // searching is no longer needed when the device is already found
                                bluetoothAdapter.cancelDiscovery();
                                bd = device;
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {}

                                connectToDevice();
                            }
                        }
                    }
                };
            }
            if (discoveryFinishedReceiver==null) {
                discoveryFinishedReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        context.unregisterReceiver(discoveryFinishedReceiver);
                    }
                };
            }
            
            IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            IntentFilter finishedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            context.registerReceiver(discoveryDevicesReceiver, foundFilter);
            context.registerReceiver(discoveryFinishedReceiver, finishedFilter);
            bluetoothAdapter.startDiscovery();*/

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice result = null;

            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
            if (devices != null) {
                for (BluetoothDevice device : devices) {
                    if (bluetoothName.equals(device.getName())) {
                        bd = device;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {}

                        connectToDevice();
                        break;
                    }
                }
            }

        }
        catch(Exception e){}

    }

    
    public void connectToDevice(){
        Toast.makeText(context, "Connected", Toast.LENGTH_LONG).show();
        BluetoothDevice deviceSelected = bd;
        serverConnectionThread = new ServerConnectionThread(deviceSelected, bluetoothAdapter);
        serverConnectionThread.start();
    }

    public void writeToBluetooth(String send){
        serverConnectionThread.ioThread.write(send);
    }

    public class ServerConnectionThread extends Thread {
        public IOThread ioThread;

        public BluetoothSocket bluetoothSocket;
        private BluetoothAdapter bluetoothAdapter;

        public ServerConnectionThread(BluetoothDevice device, BluetoothAdapter btAdapter) {
            bluetoothAdapter = btAdapter;
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(UUIDNUM));
            } catch (IOException e) {
                Log.d("ServerConnectionThread", e.getLocalizedMessage());
            }
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                bluetoothSocket.connect();

                ioThread = new IOThread(bluetoothSocket);
                ioThread.start();
                READY = true;
            } catch (IOException connectException) {
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    Log.d("ServerConnectionThread", closeException.getLocalizedMessage());
                }
                return;
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
                if (ioThread!=null) ioThread.cancel();
            } catch (IOException e) {
                Log.d("ServerConnectionThread", e.getLocalizedMessage());
            }
        }
    }


    public class IOThread extends Thread {
        final BluetoothSocket bluetoothSocket;
        final InputStream inputStream;
        final OutputStream outputStream;

        PrintWriter pw = null;

        public IOThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
            pw = new PrintWriter(outputStream);
        }

        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            while(true){
                try {
                    result = br.readLine();
                } catch (IOException e) {}
            }
        }

        public void write(String str) {
            pw.println(str);
            pw.flush();
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.d("IOThread", e.getLocalizedMessage());
            }
        }
    }

    public class BluetoothCommunicator implements Runnable{
        String message = "o13h";
        public void run(){
            writeToBluetooth(message);
        }
    }

    public static void communicateWithSwitchDevice(boolean turnOff){
        if(turnOff && SWITCH_ON){
            SWITCH_ON = false;
            bComm.message = "o13l";
            MainActivity.mainActivity.runOnUiThread(bComm);
        }
        else if(!SWITCH_ON){
            SWITCH_ON = true;
            bComm.message = "o13h";
            MainActivity.mainActivity.runOnUiThread(bComm);
        }
    }

}