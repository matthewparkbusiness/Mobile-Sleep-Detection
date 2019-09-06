/* Matthew Park
 * 2/16/17
 * Synopsys Science Fair Project -
 * Energy Conservation Through Timely Sleep Detection With Mobile Devices
 *
 * DataAnalyzer.java
 */

package sleep.mobile.mobilesleepdetection;

/**
 * Created by Jamba Juice on 3/12/2017.
 *
 * This class analyzes the sensor data retreived from both the mobile device and
 * the wearable device to determine whether or not the user is sleeping. Based on
 * the accuracy and priority of the data, this class will determine a certain value
 * that indicates the liklihood of the user sleeping. This uses the standard deviation
 * of all the data sets and assumes that all the data collected will be in the form of
 * a normal bell distribution.
 */
public class DataAnalyzer {

    // when the composite probability exceeds this threshold, the user will be considered to be asleep
    public static final double THRESHOLD_PROBABILITY_FOR_SLEEP = 0.9;

    // it will take 30 seconds for the heart rate to have a defined standard deviation and normal distribution
    public static final long HEART_RATE_CALIBRATION_TIME_MS = 300000;
    // this time defines how often the acceleromter will reset
    public static final long ACCELEROMETER_DURATION = 60000;

    public static final double HEART_RATE_SENSOR_WEIGHTED_PROBABILITY = 0.45;
    public static final double FACE_DETECTION_WEIGHTED_PROBABILITY = 0.45;
    public static final double ACCELEROMETER_WEIGHTED_PROBABILITY = 0.1;

    public static final double EYE_UPPER_BOUND_PROBABILITY = 1;
    public static final double EYE_LOWER_BOUND_PROBABILITY = 0.12;

    public static double compositeProbability;

    public int lastHeartRateData;
    public double lastAccelerometerData;
    public float lastLeftEyeBlinkData;
    public float lastRightEyeBlinkData;

    public int heartRateSampleAmount;
    public double averageHeartRate;
    public int totalHeartRate;
    public double heartRateStandardDeviation;

    public int accelerometerSampleAmount;
    public int averageAcceleromenter;
    public int accelerometerStandardDeviation;

    public int collectiveStatus;

    public double heartRateProbability;
    public double accelerometerProbability;
    public double biometricProbability;

    public long accelerometerTimer;


    /*
    Heart rate probability is calculated through an assumed normal distribution from the sample
    set collected during the calibration time. The calibration time is the time in millis that
    the user will definitely be awake for and therefore be able to find the baseline for when
    their heart rate is awake. When their heart rate reaches a very high standard deviation away
    from the mean, the probability of the user sleeping increases.
     */
    public void addHeartRateData(int hr){
        if(System.currentTimeMillis() > HEART_RATE_CALIBRATION_TIME_MS){
            heartRateSampleAmount ++;
            totalHeartRate += hr;
            averageHeartRate = totalHeartRate/heartRateSampleAmount;
            heartRateStandardDeviation = Math.sqrt((hr*hr - averageHeartRate*averageHeartRate + heartRateStandardDeviation * (heartRateSampleAmount - 1))/heartRateSampleAmount);
        }
        else{
            // calculate normal distribution probability

            // z-score is calculated through the amount of standard deviations away from the mean
            double zScore = (hr - averageHeartRate)/heartRateStandardDeviation;
            heartRateProbability = (0.5 - integrateHeartRateDistribution(zScore))*2;

            recalculateCompositeProbability();
        }

        lastHeartRateData = hr;

    }
    /*
    Integrating the normal distribution curve will provide the relative probability
    of a specific z-score to occur.
     */
    public double integrateHeartRateDistribution(double zScore){

        double rramShift = 0.0001;
        double rram = 0;

        // trapezoidal integration - integrating rightwards to calculate the probability under the normal distribution curve
        for(double i=zScore;i<averageHeartRate;i+=rramShift)
            rram = 0.5*(normalDistribution(i) + normalDistribution(i+0.0001))*0.0001;
        return rram;
    }

    /*
    A method that returns the function of the normal distribution curve with a passed z-score.
     */
    public double normalDistribution(double x){
        return 1/(heartRateStandardDeviation*Math.sqrt(6.28)) * Math.pow(2.71, -(x-averageHeartRate)*(x-averageHeartRate)/(2*heartRateStandardDeviation*heartRateStandardDeviation));
    }

    /*
    This method must use both the left eye and right eye probability that is acquired through
    the biometric tracking. They are averaged out and compared to the lower and upper
    threshold probability.
     */
    public void addEyeBlinkProbabilityData(float leftEye, float rightEye){
        if(leftEye == -1 || rightEye == -1){
            biometricProbability = 1;
            recalculateCompositeProbability();
            return;

        }

        lastLeftEyeBlinkData = leftEye;
        lastRightEyeBlinkData = rightEye;

        float average = (lastLeftEyeBlinkData + lastRightEyeBlinkData)/2;
        biometricProbability = Math.abs((average - EYE_UPPER_BOUND_PROBABILITY)/(EYE_UPPER_BOUND_PROBABILITY - EYE_LOWER_BOUND_PROBABILITY));
        recalculateCompositeProbability();
    }


    /*
    This method assesses the maximum acceleration that the user has moved and uses that to calculate
    the composite probability of the user. However, the accelerometer data must reset, since
    after a period of time the user may actually be asleep and not moving anymore.
     */
    public void addAccelerometerData(double acc){
        if(System.currentTimeMillis() - accelerometerTimer > ACCELEROMETER_DURATION){
            accelerometerTimer = System.currentTimeMillis();
            accelerometerProbability = 0;
        }
        accelerometerProbability = Math.max(accelerometerProbability, acc/10);
        lastAccelerometerData = acc;
    }

    /*
    This method recalculates the composite probability following the weighted attribute variables
    and the sensor probabilities collected
     */
    public void recalculateCompositeProbability(){

        compositeProbability = HEART_RATE_SENSOR_WEIGHTED_PROBABILITY*heartRateProbability +
                ACCELEROMETER_WEIGHTED_PROBABILITY*accelerometerProbability +
                FACE_DETECTION_WEIGHTED_PROBABILITY*biometricProbability;
    }

}
