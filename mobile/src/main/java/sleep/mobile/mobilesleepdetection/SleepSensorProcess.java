/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * SleepSensorProcess.java
 */

package sleep.mobile.mobilesleepdetection;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/*
 * This class is intended to be created and recreated everytime the user
 * wishes to use Sleep Mode or not. The primary objective of this object
 * is to countdown the time for the user to sleep using a number of sensors
 * such as audio, proximity, camera, and touch. This class also works with WearableController
 * to record the data collected by the Android Watch optionally. The watch's
 * data will be handled in the WearableDataAnalysis class, but this class
 * will maintain the lifecycle of these objects.
 *
 */

public class SleepSensorProcess implements Runnable, SensorEventListener {

    private static final int SENSOR_SENSITIVITY = 4;
    public static final long ASKING_FREQUENCY = 10000;                           // controls the frequency that the device will use audio in milliseconds

    public boolean countingDown = false;
    public long timeOfCountDown;

    public boolean destroySleepProcess = false;                                  // when this boolean becomes true, this object will be replaced and garbage-collected

    public TextToSpeech tts=null;
    public TextToSpeechRunner ttsr = new TextToSpeechRunner();                   // Runnable object for text-to-speech


    private SpeechRecognitionRunner srr = new SpeechRecognitionRunner();         // This Runnable objct will begin the speech recognition
    private SpeechRecognitionStopper srs = new SpeechRecognitionStopper();       // This Runnable object will stop the speech recognition

    private SpeechRecognizer mSpeechRecognizer;
    private Intent mSpeechRecognizerIntent;
    private MainActivity a;

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private boolean proximityNear;

    private long timeOut = 0;

    public int sleepingStatus = 0;              // an integer that keeps track of whether the user is sleeping : 0 - not sleeping      1 - currently checking         2 - sleeping
    public int speechRecognitionStatus = 0;     // an integer that keeps track of the speech sensor :            0 - not listening     1 - listening and awaiting     2 - finished listening


    public void init(MainActivity a){
        this.a = a;
        sensorManager = (SensorManager) a.getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        tts = new TextToSpeech(a, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    tts.setLanguage(Locale.ENGLISH);
                    new Thread(SleepSensorProcess.this).start();
                }
            }
        });
    }


    public void run(){
        long previousAskingTime = System.currentTimeMillis();
        while(true){
            if(System.currentTimeMillis() - previousAskingTime > ASKING_FREQUENCY){
                beginSensors();
                previousAskingTime = System.currentTimeMillis();
            }
            /*
            If the probability of the user sleeping exceeds the threshold probability for the user sleeping,
            my mobile software will ask the user if he is sleeping. This part is critical because if the software
            asks to frequently, the convenience of this product will go down.

             */
            else if(DataAnalyzer.compositeProbability > DataAnalyzer.THRESHOLD_PROBABILITY_FOR_SLEEP){
                beginSensors();
                previousAskingTime = System.currentTimeMillis();
            }

            if(destroySleepProcess) break;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
        }
    }

    public class TextToSpeechRunner implements Runnable{
        public String message = "Are you sleeping?";

        public void run(){
            tts.speak(message, TextToSpeech.QUEUE_ADD, null);
            Toast.makeText(a, message, Toast.LENGTH_LONG).show();
        }
    }

    /*
    This inner class simply allows a speech recognizer to be created, since this is
    all executed outside of the main UI thread. These Runnable classes are created
    in order for certain necesary pieces of code to be executed on the UI thread.


    The SpeechRecognitionRunner class is responsable for reinitializing the speech
    recognizer object everytime the speech cycle ends and repeats.
    */
    public class SpeechRecognitionRunner implements Runnable{

        public void run(){
            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(a);
            mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, a.getPackageName());

            SpeechRecognitionListener listener = new SpeechRecognitionListener();
            mSpeechRecognizer.setRecognitionListener(listener);
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
    }

    public class SpeechRecognitionStopper implements Runnable{
        public void run(){
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
        }
    }


    // Waits for text-to-speech to finish before listening
    // This is necessary for the speech recognition to not input tts output.
    public void waitForTTSToFinish(){
        while(!tts.isSpeaking() && !destroySleepProcess){
            try {Thread.sleep(10);} catch (InterruptedException e) {}
        }
        while(tts.isSpeaking() && !destroySleepProcess){
            try {Thread.sleep(10);} catch (InterruptedException e) {}
        }
    }

    /*
    This method awaits sensor information from the voice recognition intent or the
    proximity sensor information. This method overall holds up the SleepSensor thread
    until there is confirmation the user is awake or it has timed out.
     */
    public void activateSensors(){
        waitForTTSToFinish();
        speechRecognitionStatus = 1;
        sleepingStatus = 1;

        a.runOnUiThread(srr);
        timeOut = System.currentTimeMillis();
        while(speechRecognitionStatus == 1 && sleepingStatus != 0 && !destroySleepProcess){
            if(proximityNear){
                a.runOnUiThread(srs);
                sleepingStatus = 0;
                break;
            }
            if(System.currentTimeMillis() - timeOut > 8000) {
                a.runOnUiThread(srs);
                sleepingStatus = 1;
                break;
            }
            try {Thread.sleep(10);} catch (InterruptedException e) {}
        }

        speechRecognitionStatus = 0;
    }

    public void beginSensors(){
        if(destroySleepProcess) return;
        if(sleepingStatus == 0){
            ttsr.message = "Are you sleeping?";
            a.runOnUiThread(ttsr);
            activateSensors();

            if(sleepingStatus == 1){
                // exit the app at this point
                final AudioManager mode = (AudioManager) MainActivity.mainActivity.getSystemService(Context.AUDIO_SERVICE);
                mode.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                mode.setMode(AudioManager.MODE_IN_CALL);

                System.exit(0);
            }
            else{

                ttsr.message = "Okay.";
                a.runOnUiThread(ttsr);
            }
            return;
        }
    }

    // these four lines exit SleepMode entirely, the camera must be removed
    // and recreated on the next entry
    public void exitSleepMode(){
        destroySleepProcess = true;                                 // break out of the sleep loop
        MainActivity.cameraManager.destroy();                    // release the camera from the controller object
        MainActivity.sleepMode = 1;                                 // sleepMode which holds the state of the app
        MainActivity.mainActivity.runOnUiThread(srs);
        MainActivity.mainActivity.destroyCameraController();        // removes the camera area from the screen
    }

    public void beginCountdown(){
        timeOfCountDown = System.currentTimeMillis();
        countingDown = true;
    }

    public void cancelCountDown(){
        countingDown = false;
    }

    /* This method receives all the sensor data from heart rate and accelerometer.
     * It will send this information to the phone to be processed.
     *
     */
     public void onSensorChanged(SensorEvent event) {

         if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY)
             proximityNear = true;
         else
             proximityNear = false;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private class SpeechRecognitionListener implements RecognitionListener
    {
        public void onBeginningOfSpeech() {}
        public void onBufferReceived(byte[] buffer) {}
        public void onEndOfSpeech() {}
        public void onRmsChanged(float rmsdB) {}
        public void onEvent(int eventType, Bundle params) {}

        public void onPartialResults(Bundle partialResults) {
            speechRecognitionStatus = 2;
        }

        public void onError(int error) {
            speechRecognitionStatus = 2;
        }

        public void onReadyForSpeech(Bundle params) {}

        public void onResults(Bundle results) {
             ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if(matches.size()>10){
                for(int i=0;i<10;i++){
                    if(matches.get(i).toLowerCase().equals("no")) sleepingStatus = 0;
                }
            }
            else{
                for(int i=0;i<matches.size();i++){
                    if(matches.get(i).toLowerCase().equals("no")) sleepingStatus = 0;
                }
            }
            speechRecognitionStatus = 2;

        }

    }

}