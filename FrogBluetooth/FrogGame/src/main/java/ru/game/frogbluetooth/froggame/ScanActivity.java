package ru.game.frogbluetooth.froggame;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

/**
 * Created by Evgenij on 21.03.14.
 *
 */
public class ScanActivity extends ActionBarActivity
{
    //-----------------------------
    //Constants
    //-----------------------------

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    private boolean isSaning = false;

    //-----------------------------
    //Variables
    //-----------------------------

    //-----------------------------
    //Ctors
    //-----------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        ListView newDevicesListView = (ListView)findViewById(R.id.Scan_List_Other);
        ListView pairedListView = (ListView)findViewById(R.id.Scan_List_Paired);
        Button mScan = (Button)findViewById(R.id.Scan_ScanBtn);

        mScan.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(isSaning)
                {
                    ((Button) v).setText(R.string.scan_btn_text);
                }
                else
                {
                    ((Button) v).setText(R.string.scan_btn_stop_text);
                }

                doScan();
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_info);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_info);

        // Find and set up the ListView for paired devices
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if(pairedDevices != null && pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else
        {
            String noDevices = getResources().getText(R.string.no_devices).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }

        String noDevices = getResources().getText(R.string.no_devices).toString();
        mNewDevicesArrayAdapter.add(noDevices);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    //-----------------------------
    //Methods
    //-----------------------------

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if(mBtAdapter != null && mBtAdapter.isDiscovering())
        {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    private void doScan()
    {
        if(isSaning)
        {
            setProgressBarIndeterminateVisibility(false);
            mBtAdapter.cancelDiscovery();
            isSaning = false;
            return;
        }

        setProgressBarIndeterminateVisibility(true);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering())
        {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
        isSaning = true;
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    //-----------------------------
    //Getters/Setters
    //-----------------------------

    //-----------------------------
    //Inner Classes
    //-----------------------------

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                if(mNewDevicesArrayAdapter.getCount() == 1 && mNewDevicesArrayAdapter.getItem(0).equals(getResources().getString(R.string.no_devices)))
                    mNewDevicesArrayAdapter.clear();

                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    //Повторки не добавляем в список
                    if(!isContains(device.getAddress()))
                    {
                        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }
                // When discovery is finished, change the Activity title
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                setProgressBarIndeterminateVisibility(false);
                //setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0)
                {
                    String noDevices = getResources().getText(R.string.no_devices).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }

        private boolean isContains(String data)
        {
            for(int i = 0; i < mNewDevicesArrayAdapter.getCount(); i++)
            {
                String containData = mNewDevicesArrayAdapter.getItem(i);
                String number = containData.split("\n")[1];
                if(number.contains(data))
                    return true;
            }

            return false;
        }
    };
}
