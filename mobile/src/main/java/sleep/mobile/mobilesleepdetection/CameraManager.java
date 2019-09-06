/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * CameraController.java
 */

package sleep.mobile.mobilesleepdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;

import android.util.SparseArray;
import android.util.Log;

import java.io.IOException;

import android.hardware.Camera;

import android.view.TextureView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;


/* This class is in charge of getting pictures from the camera at a certain
 * frequency and detecting the face of the user. Using the Vision API from
 * google play, this program finds the probability fo the left or right
 * eye being open to detect sleep. This probability data is sent to the
 * DataAnalyzer class to be processed along with the other sensor data.
 *
 *
 */


public class CameraManager extends TextureView implements TextureView.SurfaceTextureListener {

    public static final int MAX_FACES = 2;
    private Camera mCamera;
    private long previewCallbackTimeout = 0;

    // These objects are from the Vision API from google instead of OpenCV
    private FaceDetector detector;
    private EyeDetector eyeDetector = new EyeDetector();

    private Paint green;

    public CameraManager(Context c){
        super(c);

        green = new Paint();
        green.setColor(Color.GREEN);
        green.setStyle(Paint.Style.FILL);
        green.setStrokeWidth(20);

        setSurfaceTextureListener(this);
        detector = new FaceDetector.Builder(MainActivity.mainActivity).setLandmarkType(FaceDetector.ALL_LANDMARKS).setClassificationType(FaceDetector.ALL_CLASSIFICATIONS).build();
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        // the front facing camera is needed, so the id is 1
        mCamera = Camera.open(1);
        mCamera.setDisplayOrientation(90);

        Camera.PreviewCallback previewCallback = new Camera.PreviewCallback()
        {
            public void onPreviewFrame(byte[] data, Camera camera)
            {
                try {
                    if(System.currentTimeMillis() - previewCallbackTimeout > 7000){
                        Bitmap preview = CameraManager.this.getBitmap();

                        eyeDetector.preview = preview;
                        // The face detection takes a relatively long time and may hold up the UI Thread
                        new Thread(eyeDetector).start();
                        previewCallbackTimeout = System.currentTimeMillis();
                    }
                } catch(Exception e) {
                     Log.e(MainActivity.LOG_TAG, e.getMessage());
                }
            }

        };
        mCamera.setPreviewCallback(previewCallback);

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            Log.e(MainActivity.LOG_TAG, ioe.getMessage());
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }




    public void destroy(){
        mCamera.stopPreview();
        mCamera.release();
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    public class EyeDetector implements Runnable{

        Bitmap preview;
        public void run(){
            // The FaceDetector class only can handle Bitmaps of the format RGB_565
            preview= preview.copy(Bitmap.Config.RGB_565, false);
            Frame frame = new Frame.Builder().setBitmap(preview).build();
            SparseArray<Face> faces = detector.detect(frame);
            if(faces.size() == 0) return;
            MainActivity.dataAnalyzer.addEyeBlinkProbabilityData(faces.get(0).getIsLeftEyeOpenProbability(), faces.get(0).getIsRightEyeOpenProbability());


        }

    }


}