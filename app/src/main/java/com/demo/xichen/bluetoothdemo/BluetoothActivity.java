package com.demo.xichen.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.xichen.bluetoothdemo.connect.AcceptThread;
import com.demo.xichen.bluetoothdemo.connect.ConnectThread;
import com.demo.xichen.bluetoothdemo.connect.Constant;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xi.Chen on 2015/12/3.
 */
public class BluetoothActivity extends Activity implements View.OnClickListener{
    private ListView lvBinded;
    private ListView lvScan;
    private List<BluetoothDevice> mBondedDeviceList = new ArrayList<>();
    private List<BluetoothDevice> mDeviceList = new ArrayList<>();

    private DeviceAdapter mBonderAdapter;
    private DeviceAdapter mAdapter;
    private BlueToothController mController = new BlueToothController();
    private TextView tvState;
    private TextView tvReceive;
    private String receiveStr="";
    private Switch swBTOpen;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    CheckBox cb16show;
    private Handler mUIHandler = new MyHandler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        init();
        if(getBTstate()) {
            getBounderDevice();
        }
        registerBluetoothReceiver();
    }

    private void getBounderDevice() {
        //?????????????????????
        mBondedDeviceList = mController.getBondedDeviceList();
        mBonderAdapter.refresh(mBondedDeviceList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if( mAcceptThread != null) {
            mAcceptThread.cancel();
        }
        if( mConnectThread != null) {
            mConnectThread.cancel();
        }

        unregisterReceiver(mReceiver);
    }

    private void init()
    {
        tvState=(TextView)findViewById(R.id.tvState);

        lvBinded = (ListView) findViewById(R.id.listView);
        mBonderAdapter = new DeviceAdapter(mBondedDeviceList, this);
        lvBinded.setAdapter(mBonderAdapter);
        lvBinded.setOnItemClickListener(BindedDeviceClick);

        lvScan = (ListView) findViewById(R.id.listView2);
        mAdapter = new DeviceAdapter(mDeviceList, this);
        lvScan.setAdapter(mAdapter);
        lvScan.setOnItemClickListener(ScanDeviceClick);

        swBTOpen=(Switch)findViewById(R.id.swBtOpen);
        swBTOpen.setOnCheckedChangeListener(BT_Switch);

        tvReceive =(TextView)findViewById(R.id.tvReceive);
        cb16show=(CheckBox)findViewById(R.id.cb16show);
    }

    CompoundButton.OnCheckedChangeListener BT_Switch =new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if (b)
            {
                mController.turnOnBlueTooth(BluetoothActivity.this, 1);
            }
            else {
                mController.turnOffBlueTooth();
            }
        }
    };
    AdapterView.OnItemClickListener BindedDeviceClick=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = mBondedDeviceList.get(position);
            log(device.getName());
            if( mConnectThread != null) {
                mConnectThread.cancel();
            }
            mConnectThread = new ConnectThread(device, mController.getAdapter(), mUIHandler);
            mConnectThread.start();//????????????thread
            log("thread start~~~~~~");
        }
    };
    AdapterView.OnItemClickListener ScanDeviceClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = mDeviceList.get(position);
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                device.createBond();
            }
        }
    };
    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        //????????????
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //????????????
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //????????????
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //????????????????????????
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //????????????
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        registerReceiver(mReceiver, filter);
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if( BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action) ) {
                setProgressBarIndeterminateVisibility(true);
                //?????????????????????
                mDeviceList.clear();
                mAdapter.notifyDataSetChanged();
            }
            else if( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                tvState.setVisibility(View.INVISIBLE);
                tvState.setText("????????????");
                setProgressBarIndeterminateVisibility(false);
            }
            else if( BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("chenxi","add");
                //???????????????????????????
                mDeviceList.add(device);
                mAdapter.notifyDataSetChanged();
            }
            else if( BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,0);
                if( scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    setProgressBarIndeterminateVisibility(true);
                }
                else {
                    setProgressBarIndeterminateVisibility(false);
                }
            }
            else if( BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action) ) {
                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if( remoteDevice == null ) {
                    //showToast("no device");
                    Log.i("chenxi","no device");
                    return;
                }
                int status = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0);
                if( status == BluetoothDevice.BOND_BONDED) {
                    //showToast("Bonded " + remoteDevice.getName());
                    Log.i("chenxi", "Bonded " + remoteDevice.getName());
                }
                else if( status == BluetoothDevice.BOND_BONDING){
                    //showToast("Bonding " + remoteDevice.getName());
                    Log.i("chenxi", "Bonding " + remoteDevice.getName());
                }
                else if(status == BluetoothDevice.BOND_NONE){
                    //showToast("Not bond " + remoteDevice.getName());
                    Log.i("chenxi", "Not bond " + remoteDevice.getName());
                }
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btScan:
                Log.i("chenxi", "btScan you click");
                tvState.setVisibility(View.VISIBLE);
                tvState.setText("????????????...");
                //????????????
                mAdapter.refresh(mDeviceList);
                mController.findDevice();
                //mListView.setOnItemClickListener(bindDeviceClick);
                break;
            case R.id.btBinded:

                Log.i("chenxi","btBinded you click");
                //?????????????????????
                mBondedDeviceList = mController.getBondedDeviceList();
                mBonderAdapter.refresh(mBondedDeviceList);
                //mListView.setOnItemClickListener(bindedDeviceClick);
                break;
            case R.id.btSend:
                EditText sendStr=(EditText)findViewById(R.id.etSend);
                CheckBox cb16Send=(CheckBox)findViewById(R.id.cb16Send);
                if (cb16Send.isChecked())
                {
                    log("16send is check.+");
                    sendByte(sendStr.getText().toString());
                }else {
                    sendStr(sendStr.getText().toString());
                }
                break;
            case R.id.btClear:
                receiveStr="";
                tvReceive.setText("");
                break;
        }
    }
    private void sendStr(String word) {
        if( mAcceptThread != null) {
            try {
                mAcceptThread.sendData(word.getBytes("utf-8"));
                log("mAcceptThread sendStr:"+word);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        else if( mConnectThread != null) {
            try {
                mConnectThread.sendData(word.getBytes("utf-8"));
                log("mConnectThread sendStr:"+word);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else
        {
            Toast.makeText(BluetoothActivity.this,"??????????????????????????????",Toast.LENGTH_LONG).show();
        }
    }
    private void sendByte(String str) {
        if( mAcceptThread != null)
        {
            String[] dateStr = str.split(" ");
            for(int i=0;i<dateStr.length;i++)
            {
                mConnectThread.sendData(Integer.parseInt( dateStr[i],16 ));
            }
        }
        else if( mConnectThread != null)
        {
            String[] dateStr = str.split(" ");
            for(int i=0;i<dateStr.length;i++)
            {
                mConnectThread.sendData(Integer.parseInt( dateStr[i],16 ));
            }
        }
        else
        {
            Toast.makeText(BluetoothActivity.this,"??????????????????????????????",Toast.LENGTH_LONG).show();
        }
    }
    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constant.MSG_START_LISTENING:
                    //setProgressBarIndeterminateVisibility(true);
                    break;
                case Constant.MSG_FINISH_LISTENING:
                    //setProgressBarIndeterminateVisibility(false);
                    break;
                case Constant.MSG_GOT_DATA://????????????
                    if(cb16show.isChecked()) {//16????????????
                        receiveStr = receiveStr + " " + Integer.toHexString((int) msg.obj);
                        tvReceive.setText(receiveStr);
                    }else {
                        int date=(int) msg.obj;//?????????ASCII???
                        receiveStr = receiveStr + " " +(char)date;//???ASCII?????????????????????
                        tvReceive.setText(receiveStr);
                    }
                    break;
                case Constant.MSG_ERROR:
                    //showToast("error: "+String.valueOf(msg.obj));
                    break;
                case Constant.MSG_CONNECTED_TO_SERVER:
                    //showToast("Connected to Server");
                    break;
                case Constant.MSG_GOT_A_CLINET:
                    //showToast("Got a Client");
                    break;
            }
        }
    }
    private boolean getBTstate() {
        if (mController.getBlueToothStatus()) {//ToggleButton ??????????????????
            swBTOpen.setChecked(true);
            return true;
        } else {
            swBTOpen.setChecked(false);
            return false;
        }
    }
    private void log(String str){
        Log.i("chenxi",str+"  @BluetoothActivity");
    }
}
