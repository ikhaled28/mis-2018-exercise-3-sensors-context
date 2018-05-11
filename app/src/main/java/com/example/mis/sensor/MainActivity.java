package com.example.mis.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.SeekBar;

import com.example.mis.sensor.views.CustomView;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener{

    //https://developer.android.com/guide/topics/sensors/sensors_motion

    private double xAxis, yAxis, zAxis,magnitude;
    private boolean mInitialized;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private final float NOISE = (float) 2.0;

    ArrayList <AccelerometerViewData> xyzData = new ArrayList<AccelerometerViewData>();
    CustomView accelerometerView;

    private MediaPlayer mMusic;

    //example variables
    private double[] freqCounts;
    private double[] magnituedFFT;
    private int magnitudeCounter = 0;

    private boolean isJogging = false;
    private boolean isCycling = false;


    SeekBar sampleRateChanger;
    private int sampleRate = 1000;

    SeekBar windowSizeChanger;
    private int wsize = 32;

    private float locationSpeed = 0.0f;

    private boolean isMusicPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //initiate and fill example array with random values

        accelerometerView = (CustomView) findViewById(R.id.xyzView);
        magnituedFFT = new double[wsize];

        // https://stackoverflow.com/questions/40740933/setting-timer-with-seek-bar
        sampleRateChanger = (SeekBar) findViewById(R.id.seekBarSampleData);
        sampleRateChanger.setMax(20000);
        sampleRateChanger.setProgress(1000);


        windowSizeChanger = (SeekBar) findViewById(R.id.seekBarWindowSize);
        sampleRateChanger.setMax(2048);
        sampleRateChanger.setProgress(wsize);

        mInitialized = false;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer, sampleRate);


        sampleRateChanger.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                int minutes = seekBar.getProgress() / 60;
//                int seconds = seekBar.getProgress() - minutes * 60;
                sampleRate = seekBar.getProgress();
                if(sampleRate<1000){
                    sampleRate=1000;
                }
                updateSampleSize(sampleRate);
            }
        });

        windowSizeChanger.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (seekBar.getProgress() <=32){
                    wsize = 32;
                } else if(seekBar.getProgress() >= 64 && seekBar.getProgress() < 128){
                    wsize = 64;
                }else if(seekBar.getProgress() >= 128 && seekBar.getProgress() < 256){
                    wsize = 128;
                }else if(seekBar.getProgress() >= 256 && seekBar.getProgress() < 512){
                    wsize = 256;
                }else if(seekBar.getProgress() >= 512 && seekBar.getProgress() < 1024){
                    wsize = 512;
                }else if(seekBar.getProgress() >= 1024 && seekBar.getProgress() < 1536){
                    wsize = 1024;
                }else{
                    wsize = 2048;
                }
                magnitudeCounter = 0;
            }
        });


    }


    void updateSampleSize(int sampleRate){

        mSensorManager.unregisterListener(this);
        mSensorManager.registerListener(this, mAccelerometer, sampleRate);
        Log.d("Sample Rate", sampleRate + "");
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
    }



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(sensorEvent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public void starMusic(){
        mMusic = MediaPlayer.create(MainActivity.this,R.raw.music);
        mMusic.start();
        isMusicPlaying = true;
    }

    public void stopMusic(){
        mMusic.stop();
        isMusicPlaying = false;
    }

    private void getAccelerometer(SensorEvent sensorEvent) {

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        long timestamp = System.currentTimeMillis();
        if (!mInitialized) {
            xAxis = x;
            yAxis = y;
            zAxis = z;
            magnitude = (double) sqrt(xAxis*xAxis + yAxis*yAxis + zAxis*zAxis);
            mInitialized = true;

        } else {
            double deltaX = Math.abs(xAxis - x);
            double deltaY = Math.abs(yAxis - y);
            double deltaZ = Math.abs(zAxis - z);
//            if (deltaX < NOISE) deltaX = (float)0.0;
//            if (deltaY < NOISE) deltaY = (float)0.0;
//            if (deltaZ < NOISE) deltaZ = (float)0.0;
            xAxis = x;
            yAxis = y;
            zAxis = z;
            magnitude = (double) sqrt(xAxis*xAxis + yAxis*yAxis + zAxis*zAxis);
        }

//        System.out.println("X" + xAxis);
//        System.out.println("Y" + yAxis);
//        System.out.println("Z" + zAxis);
//        System.out.println("M" + magnitude);

        xyzData.add(new AccelerometerViewData((float) xAxis, (float) yAxis, (float) zAxis));
        if (xyzData.size() > 100){
            xyzData.remove(0);
        }
        accelerometerView.SetAccelerometerData(xyzData);


        if(magnituedFFT.length == wsize) {
            new FFTAsynctask(wsize).execute(magnituedFFT);
            magnitudeCounter = 0;
        }else{
            magnitudeCounter++;
            magnituedFFT[magnitudeCounter] = magnitude;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location == null){
            locationSpeed = 0.0f;
        }else {
            locationSpeed = location.getSpeed();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */

    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; /* window size must be power of 2 */

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
            if(!isMusicPlaying){
                playMusicFFT();
            }
        }
    }


    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }

    void playMusicFFT(){
        double peakValue = 0;

        for (int i = 0; i< wsize; i++){
            if(freqCounts[i] > peakValue){
                peakValue = freqCounts[i];
            }
        }

        if ( peakValue >= 0 && peakValue <=10){
            if (locationSpeed >= 1.0 && locationSpeed <= 20.0){
                if(!isMusicPlaying) {
                    starMusic();
                    isJogging = true;
                    isCycling = false;
                }
            }

        }else if ( peakValue >= 0 && peakValue <=10){
            if (locationSpeed >= 1.0 && locationSpeed <= 20.0){
                if (!isMusicPlaying) {
                    starMusic();
                    isCycling = true;
                    isJogging = false;
                }
            }
        }else {
            //No Music Playing
            if (isMusicPlaying) {
                stopMusic();
                isCycling = false;
                isJogging = false;
            }
        }
    }
}
