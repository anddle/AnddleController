package com.anddle.anddleled;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

public class ControlActivity extends AppCompatActivity {

    private static final String TAG = "ControlActivity";
    private final int RESULT_CODE_BTDEVICE = 0;

    private ConnectionManager mConnectionManager;
    private MenuItem mConnectionMenuItem;
    private SeekBar mSeekBar;

    private final static int MSG_SENT_DATA = 0;
    private final static int MSG_RECEIVE_DATA = 1;
    private final static int MSG_UPDATE_UI = 2;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SENT_DATA: {

                }
                break;

                case MSG_RECEIVE_DATA: {


                }
                break;

                case MSG_UPDATE_UI: {
                    updateUI();
                }
                break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!BTAdapter.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);
            finish();
            return;
        }

        int hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
            finish();
            return;
        }

        mSeekBar = (SeekBar) findViewById(R.id.light_bar);
        mSeekBar.setMax(255);
        mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);

        mConnectionManager = new ConnectionManager(mConnectionListener);
        mConnectionManager.startListen();

        if(BTAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(i);
        }
    }

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {

                byte level = (byte)progress;
                byte [] data = {level};
                Log.d(TAG, "send data:"+data);
                mConnectionManager.sendData(data);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {

                byte level = (byte)seekBar.getProgress();
                byte [] data = {level};
                Log.d(TAG, "send data:"+data);
                mConnectionManager.sendData(data);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_UPDATE_UI);
        mHandler.removeMessages(MSG_SENT_DATA);
        mHandler.removeMessages(MSG_RECEIVE_DATA);

        if(mConnectionManager != null) {
            mConnectionManager.disconnect();
            mConnectionManager.stopListen();
        }
    }

    private ConnectionManager.ConnectionListener mConnectionListener = new ConnectionManager.ConnectionListener() {

        @Override
        public void onConnectStateChange(int oldState, int State) {

            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        @Override
        public void onListenStateChange(int oldState, int State) {

            mHandler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
        }

        @Override
        public void onSendData(boolean suc, byte[] data) {

            mHandler.obtainMessage(MSG_SENT_DATA, suc?1:0, 0, data).sendToTarget();
        }

        @Override
        public void onReadData(byte[] data) {

            mHandler.obtainMessage(MSG_RECEIVE_DATA,  data).sendToTarget();

        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.main_menu, menu);
        mConnectionMenuItem = menu.findItem(R.id.connect_menu);
        updateUI();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.connect_menu: {
                if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
                    mConnectionManager.disconnect();

                }
                else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
                    mConnectionManager.disconnect();

                }
                else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
                    Intent i = new Intent(ControlActivity.this, DeviceListActivity.class);
                    startActivityForResult(i, RESULT_CODE_BTDEVICE);
                }

            }
            return true;

            case R.id.about_menu: {
                Intent i = new Intent(this, AboutActivity.class);
                startActivity(i);
            }
            return true;

            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult, requestCode="+requestCode+" resultCode="+resultCode );
        if(requestCode == RESULT_CODE_BTDEVICE && resultCode == RESULT_OK) {
            String deviceAddr = data.getStringExtra("DEVICE_ADDR");
            mConnectionManager.connect(deviceAddr);
        }
    }

    private void updateUI()
    {
        if(mConnectionManager == null) {
            return;
        }

        if(mConnectionMenuItem == null) {

            mSeekBar.setEnabled(false);

            return;
        }

        Log.d(TAG, "current BT ConnectState="+mConnectionManager.getState(mConnectionManager.getCurrentConnectState())
                +" ListenState="+mConnectionManager.getState(mConnectionManager.getCurrentListenState()));

        if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTED) {
            mConnectionMenuItem.setTitle(R.string.disconnect);

            mSeekBar.setEnabled(true);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_CONNECTING) {
            mConnectionMenuItem.setTitle(R.string.cancel);

            mSeekBar.setEnabled(false);
        }
        else if(mConnectionManager.getCurrentConnectState() == ConnectionManager.CONNECT_STATE_IDLE) {
            mConnectionMenuItem.setTitle(R.string.connect);

            mSeekBar.setEnabled(false);
        }
    }
}
