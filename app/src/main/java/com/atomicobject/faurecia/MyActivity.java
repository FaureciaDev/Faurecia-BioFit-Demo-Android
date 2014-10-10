package com.atomicobject.faurecia;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MyActivity extends Activity {

    private TextView m_StatusText;
    private Button m_OnButton;
    private Button m_OffButton;
    private Button m_DiscoverButton;

    private ListView m_DiscoveredDevicesListView;
    private BluetoothDevicesAdapter m_DiscoveredDevicesAdapter;

    private BluetoothAdapter m_BluetoothAdapter;
    private BioFitService m_BioFitService;

    private boolean m_DiscoveringDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my);

        m_StatusText = (TextView) findViewById(R.id.statusLabel);
        m_OnButton = (Button)findViewById(R.id.bluetoothOnButton);
        m_OffButton = (Button)findViewById(R.id.bluetoothOffButton);
        m_DiscoverButton = (Button)findViewById(R.id.bluetoothDiscoverButton);

        m_DiscoveredDevicesListView = (ListView)findViewById(R.id.scanResultList);
        m_DiscoveredDevicesAdapter = new BluetoothDevicesAdapter(MyActivity.this);
        m_DiscoveredDevicesListView.setAdapter(m_DiscoveredDevicesAdapter);

        m_BluetoothAdapter = null;
        m_BioFitService = null;
        m_DiscoveringDevices = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        BluetoothManager m_BluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        m_BluetoothAdapter = (m_BluetoothManager == null) ? null : m_BluetoothManager.getAdapter();

        Intent gattServiceIntent = new Intent(this, BioFitService.class);
        bindService(gattServiceIntent, m_ServiceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection m_ServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            m_BioFitService = ((BioFitService.LocalBinder) service).getService();
            if (m_BioFitService.initialize()) {
                configureWithBluetooth();
            }
            else {
                configureWithoutBluetooth();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            m_BioFitService = null;
        }
    };

    private void configureWithBluetooth() {
        m_OnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetooth();
            }
        });

        m_OffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableBluetooth();
            }
        });

        m_DiscoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverDevices();
            }
        });

        m_DiscoveredDevicesListView.setOnItemClickListener(m_DeviceClickListener);
    }

    private void configureWithoutBluetooth() {
        m_OnButton.setEnabled(false);
        m_OffButton.setEnabled(false);
        m_DiscoverButton.setEnabled(false);
        m_StatusText.setText("Status: not supported");

        longToast("Your device does not support Bluetooth");
    }

    @Override
    protected void onResume() {
        super.onResume();

        final IntentFilter intentFilter = new IntentFilter(BioFitService.ACTION_DEVICE_DISCOVERED);

        LocalBroadcastManager.getInstance(this).registerReceiver(m_ServiceReceiver, intentFilter);
    }

    private final BroadcastReceiver m_ServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            // These are all of the intent actions the BioFit service will send.
            // You won't normally act on all of them--see DeviceInfoActivity for
            // responding to the BioFit notifiable group data.

            if (BioFitService.ACTION_DEVICE_DISCOVERED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BioFitService.DEVICE);
                m_DiscoveredDevicesAdapter.add(device);
                m_DiscoveredDevicesAdapter.notifyDataSetChanged();
            }
            else if (BioFitService.ACTION_GATT_CONNECTED.equals(action)) {
            }
            else if (BioFitService.ACTION_GATT_DISCONNECTED.equals(action)) {
            }
            else if (BioFitService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            }
            else if (BioFitService.ACTION_DATA_AVAILABLE.equals(action)) {
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        stopDiscoveringDevices();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_ServiceReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(m_ServiceConnection);
        m_BioFitService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void enableBluetooth() {
        if (m_BluetoothAdapter == null) {
            longToast("Bluetooth is unavailable");
        }
        else if (!m_BluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, 1);
        }
        else {
            shortToast("Bluetooth is already on");
        }
    }

    private void disableBluetooth() {
        if (m_BluetoothAdapter == null) {
            longToast("Bluetooth is unavailable");
        }
        else if (m_BluetoothAdapter.isEnabled()) {
            stopDiscoveringDevices();
            m_BluetoothAdapter.disable();
            m_StatusText.setText("Status: Disconnected");

            longToast("Bluetooth turned off");
        }
        else {
            shortToast("Bluetooth is already off");
        }
    }

    public void discoverDevices() {
        if (m_DiscoveringDevices) {
            stopDiscoveringDevices();
        }
        else {
            m_DiscoveredDevicesAdapter.clear();
            m_DiscoverButton.setText(getApplicationContext().getResources().getString(R.string.bluetoothDiscoverDoneText));
            m_DiscoveringDevices = true;
            m_BioFitService.startDiscovery();
        }
    }

    private void stopDiscoveringDevices() {
        if (m_DiscoveringDevices) {
            m_DiscoverButton.setText(getApplicationContext().getResources().getString(R.string.bluetoothDiscoverText));
            m_DiscoveringDevices = false;
            m_BioFitService.stopDiscovery();
        }
    }

    private AdapterView.OnItemClickListener m_DeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            stopDiscoveringDevices();

            BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);

            Intent intent = new Intent(MyActivity.this, DeviceInfoActivity.class);
            intent.putExtra(BioFitService.DEVICE, device);
            MyActivity.this.startActivity(intent);
        }
    };

    static class BluetoothDevicesAdapter extends ArrayAdapter<BluetoothDevice> {
        public BluetoothDevicesAdapter(Context context) {
            super(context, 0, new ArrayList<BluetoothDevice>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice device = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_device, parent, false);
            }

            TextView deviceNameLabel = (TextView) convertView.findViewById(R.id.itemDeviceName);
            TextView deviceAddressLabel = (TextView) convertView.findViewById(R.id.itemDeviceAddress);

            deviceNameLabel.setText(device.getName());
            deviceAddressLabel.setText(device.getAddress());

            return convertView;
        }
    }

    private void shortToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void longToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
