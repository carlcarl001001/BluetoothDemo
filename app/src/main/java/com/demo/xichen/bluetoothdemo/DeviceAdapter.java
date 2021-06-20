package com.demo.xichen.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Rex on 2015/5/27.
 */
public class DeviceAdapter extends BaseAdapter {

    private List<BluetoothDevice> mData;
    private Context mContext;

    public DeviceAdapter(List<BluetoothDevice> data, Context context) {
        mData = data;
        mContext = context.getApplicationContext();
    }


    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View itemView = view;
        //复用View，优化性能
        if( itemView == null) {
            itemView = LayoutInflater.from(mContext).inflate(R.layout.list_style,viewGroup,false);
        }

        TextView line1 = (TextView) itemView.findViewById(R.id.tvText1);
        TextView line2 = (TextView) itemView.findViewById(R.id.tvText2);
        //line1.setTextColor(0x000000);
        //line2.setTextColor(0x000000);
        //获取对应的蓝牙设备
        BluetoothDevice device = (BluetoothDevice) getItem(i);

        //显示名称
        line1.setText(device.getName());
        //显示地址
        line2.setText(device.getAddress());

        return itemView;
    }

    public void refresh(List<BluetoothDevice> data) {
        mData = data;
        notifyDataSetChanged();
    }
}
