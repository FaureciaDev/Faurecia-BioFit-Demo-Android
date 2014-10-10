package com.atomicobject.faurecia;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.UUID;

public class DeviceInfoActivity extends Activity {
    public static final String KEEP_ON_DRIVING = "Keep on driving!";
    public static final String OFFER_MASSAGE = "Would you like a massage?";

    private BluetoothDevice m_Device;
    private BioFitService m_BioFitService;

    private TextView m_HeartRateLabel;
    private TextView m_SuggestionLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_info);

        Intent intent = getIntent();
        m_Device = intent.getParcelableExtra(BioFitService.DEVICE);

        TextView deviceLabel = (TextView) findViewById(R.id.deviceLabel);
        m_HeartRateLabel = (TextView) findViewById(R.id.heartRateLabel);
        m_SuggestionLabel = (TextView) findViewById(R.id.suggestionLabel);

        deviceLabel.setText(m_Device.getName());
        m_HeartRateLabel.setText("? beats/min");
        m_SuggestionLabel.setText(KEEP_ON_DRIVING);

        m_BioFitService = null;
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent gattServiceIntent = new Intent(this, BioFitService.class);
        bindService(gattServiceIntent, m_ServiceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection m_ServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            m_BioFitService = ((BioFitService.LocalBinder) service).getService();

            m_BioFitService.connect(m_Device);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            m_BioFitService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        final IntentFilter intentFilter = new IntentFilter(BioFitService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BioFitService.ACTION_GATT_SERVICES_DISCOVERED);

        LocalBroadcastManager.getInstance(this).registerReceiver(m_ServiceReceiver, intentFilter);
    }

    private final BroadcastReceiver m_ServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BioFitService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                m_BioFitService.subscribeToCharacteristics(7, 8, 15, 26);
            }
            else if (BioFitService.ACTION_DATA_AVAILABLE.equals(action)) {
                UUID uuid = (UUID) intent.getSerializableExtra(BioFitService.CHARACTERISTIC);

                if (BioFitService.NOTIFIABLE_GROUP_VALUES_UUID.equals(uuid)) {
                    int time = intent.getIntExtra(BioFitService.TIME, 0);

                    // These are the values BioFit is notifying us of -- it will be the values
                    // we've subscribed to, except for a brief period during startup where it
                    // may be empty. The decoding of the characteristics isn't automatic yet.
                    // If you change the subscriptions to types other than uint8, you will
                    // have to change BioFitService.broadcastUpdate().

                    int[] value = intent.getIntArrayExtra(BioFitService.VALUE);

                    if (value != null && value.length == 4) {
                        m_HeartRateLabel.setText(value[0] + " beats/min @ time " + time);
                        if (value[2] > 60) {
                            m_SuggestionLabel.setText(OFFER_MASSAGE);
                        }
                        else {
                            m_SuggestionLabel.setText(KEEP_ON_DRIVING);
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(m_ServiceReceiver);
    }

    @Override
    protected void onStop() {
        super.onPause();

        if (m_BioFitService != null) {
            m_BioFitService.disconnect();
        }
        unbindService(m_ServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_info, menu);
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
