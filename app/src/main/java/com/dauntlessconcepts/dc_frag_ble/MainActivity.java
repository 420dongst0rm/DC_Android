package com.dauntlessconcepts.dc_frag_ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.os.Looper;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ListView;
import android.widget.TextView;

import com.dauntlessconcepts.dcblelib.LiteBleGattCallback;
import com.dauntlessconcepts.dcblelib.LiteBluetooth;
import com.dauntlessconcepts.dcblelib.conn.BleCharactCallback;
import com.dauntlessconcepts.dcblelib.conn.LiteBleConnector;
import com.dauntlessconcepts.dcblelib.exception.BleException;
import com.dauntlessconcepts.dcblelib.exception.hanlder.DefaultBleExceptionHandler;
import com.dauntlessconcepts.dcblelib.log.BleLog;
import com.dauntlessconcepts.dcblelib.scan.PeriodScanCallback;
import com.dauntlessconcepts.dcblelib.utils.BluetoothUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
    implements FragmentOne.OnFragmentInteractionListener, FragmentTwo.OnFragmentInteractionListener,
        FragmentThree.OnFragmentInteractionListener, FragmentFour.OnFragmentInteractionListener{

    private static final String TAG = "DC";

    //BLE
    public String UUID_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public String UUID_CHAR_READ = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    public String UUID_CHAR_WRITE = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    private static int TIME_OUT_SCAN = 10000;
    public boolean deviceConnected = false;
    private static LiteBluetooth liteBluetooth;
    private DefaultBleExceptionHandler bleExceptionHandler;
    private static String MAC = "00:00:00:AA:AA:AA";
    private Activity activity;

    private FragmentOne fragmentOne;
    private FragmentTwo fragmentTwo;
    private FragmentThree fragmentThree;
    private FragmentFour fragmentFour;

    private ListView mainDeviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;            //BLE

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        fragmentOne = new FragmentOne();
        fragmentTwo = new FragmentTwo();
        fragmentThree = new FragmentThree();
        fragmentFour = new FragmentFour();

        // Add Fragments to adapter one by one
        adapter.addFragment(fragmentOne, "");           //Discover
        adapter.addFragment(fragmentTwo, "");           //Color
        adapter.addFragment(fragmentThree, "");         //Music
        adapter.addFragment(fragmentFour, "");          //Settings
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        //Set Icons
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_power_settings_new_white_48dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_color_lens_white_48dp);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_mic_white_48dp);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_settings_white_48dp);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        //Set toolbar icon
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_launcher);

        //BLE
        if (liteBluetooth == null) {
            liteBluetooth = new LiteBluetooth(activity);
        }
        liteBluetooth.enableBluetoothIfDisabled(activity, 1);
        bleExceptionHandler = new DefaultBleExceptionHandler(this);
        BleLog.i(TAG,"lB: " + liteBluetooth);
    }

    @Override
    public void onFragmentInteraction(Uri uri){
        //you can leave it empty
    }

    // Adapter for the viewpager using FragmentPagerAdapter
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    //BLE
    public void scanDevicesPeriod() {
        liteBluetooth.startLeScan(new PeriodScanCallback(TIME_OUT_SCAN) {
            @Override
            public void onScanTimeout() {
                BleLog.i(TAG,TIME_OUT_SCAN + " Millis Scan Timeout! ");
            }

            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                BleLog.i(TAG, "device: " + device.getName() + "  ");
                addDevice(device,rssi);

            }
        });
    }

    //BLE
    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;

        for (BluetoothDevice listDev : fragmentOne.getListItems()) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        if (!deviceFound) {
            fragmentOne.getListItems().add(device);
            fragmentOne.getStringAdapter().notifyDataSetChanged();
            BleLog.i(TAG, "device: " + device.getName() + "  mac: " + device.getAddress()
                    + "  rssi: " + rssi);
        }
    }

    //BLE
    public void connectDevice(final BluetoothDevice device) {
        liteBluetooth.connect(device, false, new LiteBleGattCallback() {

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
                BleLog.i(TAG,"Connected: " + device.getName());
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                BluetoothUtil.printServices(gatt);
                BleLog.i(TAG,"Services discovered.");

                if(Looper.myLooper()==null){
                    Looper.prepare();
                }

                deviceConnected = true;
            }

            @Override
            public void onConnectFailure(BleException exception) {
                bleExceptionHandler.handleException(exception);
            }
        });
    }

    public void writeDataToCharacteristic(String data) {
        LiteBleConnector connector = liteBluetooth.newBleConnector();
        connector.withUUIDString(UUID_SERVICE, UUID_CHAR_WRITE, null)
                .writeCharacteristic(data.getBytes(), new BleCharactCallback() {
                    @Override
                    public void onSuccess(BluetoothGattCharacteristic characteristic) {
                        BleLog.i(TAG, "Write Success, DATA: " + Arrays.toString(characteristic.getValue()));
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        BleLog.i(TAG, "Write failure: " + exception);
                        bleExceptionHandler.handleException(exception);
                    }
                });
    }

    public boolean getDeviceConnected(){
        return deviceConnected;
    }
}
