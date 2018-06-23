package com.dauntlessconcepts.dc_frag_ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.dauntlessconcepts.dcblelib.log.BleLog;

import java.util.ArrayList;
import java.util.Random;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentOne.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentOne#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentOne extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = "DC_FragOne";

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    //BLE
    private ListView deviceListView;
    private Button scanButton;
    ArrayList<BluetoothDevice> listItems = new ArrayList<BluetoothDevice>();
    ArrayAdapter<BluetoothDevice> stringAdapter;

    public FragmentOne() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentOne.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentOne newInstance(String param1, String param2) {
        Log.d("DC", "FragOne: newInstance");
        FragmentOne fragment = new FragmentOne();
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
        Log.d("DC", "FragOne: onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("DC", "FragOne: onCreateView");

        View myFragmentView = inflater.inflate(R.layout.fragment_one, container, false);

        //BLE
        deviceListView = (ListView) myFragmentView.findViewById(R.id.device_list);
        scanButton = (Button) myFragmentView.findViewById(R.id.btn_Scan);


        scanButton.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).scanDevicesPeriod();
            }
        });

        deviceListView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice item = (BluetoothDevice) deviceListView.getItemAtPosition(position);
                BleLog.i(TAG,"Selected: " + item.getName());
                ((MainActivity)getActivity()).connectDevice(item);
            }
        });

        //Set custom listview item
        stringAdapter=new ArrayAdapter<BluetoothDevice>(getActivity().getApplicationContext(),
                R.layout.custom_list, R.id.mac, listItems);
        deviceListView.setAdapter(stringAdapter);

        // Inflate the layout for this fragment
        return myFragmentView;
    }

    public ListView getDeviceListView(){
        return deviceListView;
    }

    public ArrayList<BluetoothDevice> getListItems(){
        return listItems;
    }

    public ArrayAdapter<BluetoothDevice> getStringAdapter(){
        return stringAdapter;
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
        Log.d("DC", "FragOne: onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("DC", "FragOne: onDetach");
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
}
