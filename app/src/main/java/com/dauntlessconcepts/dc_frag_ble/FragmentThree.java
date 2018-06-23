package com.dauntlessconcepts.dc_frag_ble;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

import ca.uol.aig.fftpack.RealDoubleFFT;

import static java.lang.Math.sqrt;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentThree.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentThree#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentThree extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ProgressBar barBass, barMid, barTreb;
    int frequency = 44100;
    int blockSize = 1024;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    boolean CANCELLED_FLAG = false;
    boolean started = true;
    RecordAudio recordTask;
    private double mFreq;
    private SeekBar LEDbrightness;
    private SeekBar Sensitivity;
    private CheckBox checkMusic, checkLow, checkMid, checkHigh;

    AudioRecord audioRecord;
    private RealDoubleFFT transformer;
    boolean enOutput = false;

    private OnFragmentInteractionListener mListener;

    public FragmentThree() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentThree.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentThree newInstance(String param1, String param2) {
        Log.d("DC", "FragThree: newInstance");
        FragmentThree fragment = new FragmentThree();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Log.d("DC", "FragThree: onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("DC", "FragThree: onCreateView");

        View myFragmentView = inflater.inflate(R.layout.fragment_three, container, false);

        barBass = (ProgressBar) myFragmentView.findViewById(R.id.bar1);
        barMid = (ProgressBar) myFragmentView.findViewById(R.id.bar2);
        barTreb = (ProgressBar) myFragmentView.findViewById(R.id.bar3);
        LEDbrightness =(SeekBar) myFragmentView.findViewById(R.id.brightness);
        Sensitivity =(SeekBar) myFragmentView.findViewById(R.id.sensitivity);
        checkMusic = (CheckBox) myFragmentView.findViewById(R.id.chk_Music);
        checkLow = (CheckBox) myFragmentView.findViewById(R.id.chk_Low);
        checkMid = (CheckBox) myFragmentView.findViewById(R.id.chk_Mid);
        checkHigh = (CheckBox) myFragmentView.findViewById(R.id.chk_High);

        final Handler handler = new Handler();
        Timer timer = new Timer();
        transformer = new RealDoubleFFT(blockSize);

        recordTask = new RecordAudio();
        recordTask.execute();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        //System.out.println("FFT");
                        enOutput = true;
                    }
                });
            }
        };
        timer.schedule(task, 0, 50); //it executes this every 1000ms

        // Inflate the layout for this fragment
        return myFragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        Log.d("DC", "FragThree: onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("DC", "FragThree: onDetach");
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private class RecordAudio extends AsyncTask<Void, double[], Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {

            int bufferSize = AudioRecord.getMinBufferSize(frequency,
                    channelConfiguration, audioEncoding);
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, frequency,
                    channelConfiguration, audioEncoding, bufferSize);
            int bufferReadResult;
            short[] buffer = new short[blockSize];
            double[] toTransform = new double[blockSize];
            Log.d("SETTT", "Init");
            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                Log.d("Recording failed", e.toString());

            }
            while (started) {
                if (isCancelled() || (CANCELLED_FLAG == true)) {

                    started = false;
                    //publishProgress(cancelledResult);
                    Log.d("doInBackground", "Cancelling the RecordTask");
                    break;
                } else {
                    bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                    }

                    //Hanning Window
                    toTransform = HanningWindow(toTransform,0,blockSize);

                    //FFT
                    transformer.ft(toTransform);

                    //Evaluate FFT results
                    publishProgress(toTransform);

                }

            }
            return true;
        }
        @Override
        protected void onProgressUpdate(double[]...progress) {

            //Sampling rate - 8000 Hz
            //# of samples - 128

            //Nyquist frequency - 4000 Hz

            //Frequency resolution - (4000/256/2) - 31.25 Hz

            int mPeakPos = 0;
            double mMaxFFTSample = 150.0;
            double sumLow = 0.0;
            double sumMid = 0.0;
            double sumHigh = 0.0;
            double sumTotal = 0.0;
            double currentVal = 0.0;
            double dcVal = 0.0;
            int valLow = 0;
            int valMid = 0;
            int valHigh = 0;

            int binLowFirst = 1*2;        //43 Hz - 80 Hz
            int binLowSecond = 6*2;       //258 Hz - 128 Hz - 11 bins
            int binMidFirst = 9*2;        //388 Hz - 194 Hz
            int binMidSecond = 47*2;      //2024 Hz - 1012 Hz - 77 bins
            int binHighFirst = 56*2;      //2411 Hz - 1200 Hz
            int binHighSecond = 93*2;     //4005 Hz - 2000 Hz - 75 bins


            //for (int i = 0; i < progress[0].length; i++) {
            //    int x = i;
            //    int downy = (int) (150 - (progress[0][i] * 10));
            //    int upy = 150;
            //    //Log.i("SETTT", "X: " + i + " downy: " + downy + " upy: " + upy);

            //    if(downy < mMaxFFTSample)
            //    {
            //        mMaxFFTSample = downy;
            //        //mMag = mMaxFFTSample;
            //        mPeakPos = i;
            //    }
            //}

            //mFreq = (((1.0 * frequency) / (1.0 * blockSize)) * mPeakPos)/2;

            if(enOutput){
                if(progress[0].length>=blockSize) {

                    dcVal = progress[0][0]*progress[0][0];

                    for (int i = 1; i<(blockSize/2); i++){
                        currentVal = progress[0][i]*progress[0][i];
                        sumTotal += currentVal;

                        if(i >= binLowFirst && i <= binLowSecond){
                            sumLow += currentVal;
                        }

                        if(i >= binMidFirst && i <= binMidSecond){
                            sumMid += currentVal;
                        }

                        if(i >= binHighFirst && i <= binHighSecond){
                            sumHigh += currentVal;
                        }
                    }

                    Log.d("DC", "Total: " + sumTotal + " Low: " + sumLow + " Mid: " + sumMid + " High: " + sumHigh);

                    sumLow /= (binLowSecond-binLowFirst+1);
                    sumMid /= (binMidSecond-binMidFirst+1);
                    sumHigh /= (binHighSecond-binHighFirst+1);

                    //Log.d("DC", "Low: " + sumLow + " Mid: " + sumMid + " High: " + sumHigh);

                    //sumTotal /= ((binLowSecond-binLowFirst+1)+(binMidSecond-binMidFirst+1)+(binHighSecond-binHighFirst+1));
                    sumTotal = sumLow+sumMid+sumHigh;

                    Log.d("DC1", "Total: " + sumTotal + " Low: " + sumLow + " Mid: " + sumMid + " High: " + sumHigh);

                    if(sumTotal < ((float) Sensitivity.getProgress()/50)){
                        valLow = 0;
                        valMid = 0;
                        valHigh = 0;
                    }else{
                        valLow = (int) Math.round(sumLow/sumTotal*255*LEDbrightness.getProgress()/100);
                        valMid = (int) Math.round(sumMid/sumTotal*255*LEDbrightness.getProgress()/100);
                        valHigh = (int) Math.round(sumHigh/sumTotal*255*LEDbrightness.getProgress()/100);
                    }

                    //txtBass.setText("L: " + sumLow);
                    //txtMid.setText("M: " + sumMid);
                    //txtHigh.setText("H: " + sumHigh);


                    //Log.d("SETTT", "Low: " + valLow);
                    //Log.d("SETTT", "Mid: " + valMid);
                    //Log.d("SETTT", "High: " + valHigh);

                    //Log.d("SETTT", "L: " + sumLow);
                    //Log.d("SETTT", "M: " + sumMid);
                    //Log.d("SETTT", "H: " + sumHigh);
                    //Log.d("SETTT", "T: " + sumTotal);
                    //Log.d("SETTT", " ");

                    if(!checkLow.isChecked()){
                        valLow = 0;
                    }

                    if(!checkMid.isChecked()){
                        valMid = 0;
                    }

                    if(!checkHigh.isChecked()){
                        valHigh = 0;
                    }

                    barBass.setProgress(valLow);
                    barMid.setProgress(valMid);
                    barTreb.setProgress(valHigh);

                    String message = "RGB," + valLow + "," + valMid + "," + valHigh + "?";

                    if(((MainActivity)getActivity()) != null){
                        if(((MainActivity)getActivity()).getDeviceConnected() && checkMusic.isChecked()){
                            ((MainActivity)getActivity()).writeDataToCharacteristic(message);
                        }
                    }

                }
                //Log.d("SETTT", "FREQ: " + mFreq + " MAG: " + mMaxFFTSample);
                enOutput = false;
            }
        }

        public short[] HanningWindow(short[] signal_in, int pos, int size)
        {
            for (int i = pos; i < pos + size; i++)
            {
                int j = i - pos; // j = index into Hann window function
                signal_in[i] = (short) (signal_in[i] * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * j / size)));
            }
            return signal_in;
        }

        public double[] HanningWindow(double[] signal_in, int pos, int size)
        {
            for (int i = pos; i < pos + size; i++)
            {
                int j = i - pos; // j = index into Hann window function
                signal_in[i] = (double) (signal_in[i] * 0.5 * (1.0 - Math.cos(2.0 * Math.PI * j / size)));
            }
            return signal_in;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            try{
                audioRecord.stop();
            }
            catch(IllegalStateException e){
                Log.e("Stop failed", e.toString());

            }
        }
    }
}
