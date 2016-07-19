package com.anddle.anddleled;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DeviceItemAdapter extends ArrayAdapter<BluetoothDevice> {

    private final LayoutInflater mInflater;
    private int mResource;

    public DeviceItemAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = LayoutInflater.from(context);
        mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(mResource, parent, false);
        }

        TextView name = (TextView) convertView.findViewById(R.id.device_name);
        TextView info = (TextView) convertView.findViewById(R.id.device_info);
        BluetoothDevice device = getItem(position);
        name.setText(device.getName());
        info.setText(device.getAddress());

        return convertView;
    }
}
