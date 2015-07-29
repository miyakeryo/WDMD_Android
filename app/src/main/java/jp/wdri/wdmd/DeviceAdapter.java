package jp.wdri.wdmd;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by r on 15/07/28.
 */
public class DeviceAdapter extends BaseAdapter {
    Context context;
    List<BluetoothDevice> devices;
    LayoutInflater inflater;
    HashMap<String, Integer> rssiValues = new HashMap<>();

    public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewGroup vg;

        if (convertView != null) {
            vg = (ViewGroup) convertView;
        } else {
            vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
        }

        BluetoothDevice device = devices.get(position);
        final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
        final TextView tvname = ((TextView) vg.findViewById(R.id.name));
        final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
        final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

        tvrssi.setVisibility(View.VISIBLE);
        byte rssival = (byte) rssiValues.get(device.getAddress()).intValue();
        if (rssival != 0) {
            tvrssi.setText("Rssi = " + String.valueOf(rssival));
        }

        tvname.setText(device.getName());
        tvadd.setText(device.getAddress());
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            tvname.setTextColor(Color.BLACK);
            tvadd.setTextColor(Color.BLACK);
            tvpaired.setTextColor(Color.GRAY);
            tvpaired.setVisibility(View.VISIBLE);
            tvpaired.setText(R.string.paired);
            tvrssi.setVisibility(View.VISIBLE);
            tvrssi.setTextColor(Color.BLACK);

        } else {
            tvname.setTextColor(Color.BLACK);
            tvadd.setTextColor(Color.BLACK);
            tvpaired.setVisibility(View.GONE);
            tvrssi.setVisibility(View.VISIBLE);
            tvrssi.setTextColor(Color.BLACK);
        }
        return vg;
    }

    public void putRSSIValue(String address, Integer value){
        rssiValues.put(address, value);
    }
}
