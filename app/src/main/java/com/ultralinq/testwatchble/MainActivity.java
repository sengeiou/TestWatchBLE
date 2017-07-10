package com.ultralinq.testwatchble;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import android.util.Log;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.idevicesinc.sweetblue.BleManagerConfig.ScanFilter;

import java.util.UUID;

public class MainActivity extends Activity {

    private TextView mTextView;
    private TextView mValueView;
    private Button clickButton;

    private BleManager m_bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mValueView = (TextView) stub.findViewById(R.id.valueView);
                clickButton = (Button) findViewById(R.id.btnStart);

                mTextView.setText("");

                clickButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mTextView.getText() == "") {
                            mTextView.setText("Searching...");
                            clickButton.setText("Stop");
                            startScan();

                        } else {
                            m_bleManager.stopScan();
                            m_bleManager.disconnectAll();
                            m_bleManager.undiscoverAll();
                            mTextView.setText("");
                            clickButton.setText("Start");
                        }
                    }
                });
            }
        });

        BluetoothEnabler.start(this);

        m_bleManager = BleManager.get(this);

        //startScan();

    }


    private void startScan() {

        UUID nonin_uuid = UUID.fromString("46A970E0-0D5F-11E2-8B5E-0002A5D5C51B");  // Nonin
        UUID fora_uuid = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
        UUID scale_uuid = UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb");

        if (m_bleManager.isScanningReady()) {

        }

        m_bleManager.startScan(scanFilter(nonin_uuid), new BleManager.DiscoveryListener() {

            @Override public void onEvent(DiscoveryEvent event)
            {
                m_bleManager.stopScan();

                if( event.was(LifeCycle.DISCOVERED) )
                {
                    Log.i("UltraLinq", event.device().getName_debug() + " Found!");
                    //UUID [] uuidArray = event.device().getAdvertisedServices();
                    //event.device().undiscover();


                    event.device().connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent event)
                        {
                            if( event.didEnter(BleDeviceState.INITIALIZED) )
                            {
                                Log.i("TestWatchBLE", event.device().getName_debug() + " just initialized!");
                                UUID notify_uuid = UUID.fromString("0AAD7EA0-0D60-11E2-8E3C-0002A5D5C51B");
                                //UUID notify_uuid = UUID.fromString("0000ffb2-0000-1000-8000-00805f9b34fb");
                                event.device().enableNotify(notify_uuid, new BleDevice.ReadWriteListener() {

                                    @Override public void onEvent(ReadWriteEvent result)
                                    {
                                        if( result.wasSuccess() )
                                        {
                                            Log.i("TestWatchBLE", "Notify Success");
                                        }

                                        if (result.isNotification()) {

                                            //Log.i("TestWatchBLE", "Scale notify");

                                            Log.i("TestWatchBLE", "Battery level is " + result.data()[2] + "%");
                                            Log.i("TestWatchBLE", " SpO2 = " + result.data()[7] + "%" + " HR = " + result.data()[9] + " bpm");
                                            mValueView.setText("    SpO2 = " + result.data()[7] + "%" +
                                                    "\n    HR = " + result.data()[9] + " bpm");

                                        }
                                    }

                                });


                            }

                            if (event.didEnter(BleDeviceState.DISCONNECTED)) {
                                Log.i("TestWatchBLE", "Disconnected from: " + event.device().getName_debug());
                                mValueView.setText("SpO2");
                                //mTextView.setText("Disconnected!");
                                event.device().undiscover();
                                //startScan();
                            }

                            if (event.didEnter(BleDeviceState.CONNECTED)) {
                                Log.i("TestWatchBLE", event.device().getName_debug() + " just connected!");
                                mTextView.setText("Connected!");
                            }

                            if (event.didEnter(BleDeviceState.UNDISCOVERED)) {
                                Log.i("TestWatchBLE", event.device().getName_debug() + " UnDiscovered!");
                                mTextView.setText("");
                                clickButton.setText("Start");
                                //startScan();
                            }
                        }

                    });

                }


            }
        });
    }

    private ScanFilter scanFilter(final UUID uuid) {
        ScanFilter filter = new ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {

                //UUID myUUID = UUID.fromString("46A970E0-0D5F-11E2-8B5E-0002A5D5C51B");
                return Please.acknowledgeIf(e.advertisedServices().contains(uuid));
                        //.thenStopScan();
            }
        };

        return filter;
    }



    @Override protected void onResume()
    {
        super.onResume();

        m_bleManager.onResume();
        startScan();
    }

    @Override protected void onPause()
    {
        super.onPause();

        m_bleManager.onPause();
    }
}
