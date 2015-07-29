package jp.wdri.wdmd;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by r on 15/07/28.
 */
public class DeviceListDialog extends DialogFragment implements AdapterView.OnItemClickListener {

    private View view;
    private TextView mScanningText;
    List<BluetoothDevice> mDeviceList;
    private DeviceAdapter mDeviceAdapter;
    private static final long SCAN_PERIOD = 10000; //10 seconds
    private Handler mHandler = new Handler();
    private Thread mUiThread = null;
    BluetoothLeScanner mScanner;
    ScanSettings mSettings;
    private List<ScanFilter> mScanFilters = new ArrayList<>();
    private boolean mScanning;
    private String mDeviceAddress = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        lp.width = metrics.widthPixels;
        lp.height = (int) (metrics.heightPixels * 0.8);
        dialog.getWindow().setAttributes(lp);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.DialogLight);
        dialog.setTitle(R.string.select_devices);
        dialog.setCanceledOnTouchOutside(false);
        setCancelable(false);
        return dialog;
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.device_list_f, container, false);

        mScanningText = (TextView) view.findViewById(R.id.scanning);

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            close();
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            close();
        }

        mDeviceList = new ArrayList<>();
        mDeviceAdapter = new DeviceAdapter(getActivity(), mDeviceList);

        ListView newDevicesListView = (ListView) view.findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mDeviceAdapter);
        newDevicesListView.setOnItemClickListener(this);

        assert adapter != null;
        mScanner = adapter.getBluetoothLeScanner();
        mSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        view.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mScanning) scanLeDevice(true);
                else close();
            }
        });

        scanLeDevice(true);
        return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        stopScan();
        Intent result = new Intent();
        int resultCode = Activity.RESULT_CANCELED;
        if(mDeviceAddress!=null) {
            result.putExtra(BluetoothDevice.EXTRA_DEVICE, mDeviceAddress);
            resultCode = Activity.RESULT_OK;
        }
        if (getTargetFragment() != null) {
            getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, result);
        } else {
            PendingIntent pi = getActivity().createPendingResult(getTargetRequestCode(), result, PendingIntent.FLAG_ONE_SHOT);
            try {
                pi.send(resultCode);
            } catch (PendingIntent.CanceledException ignored) {
            }
        }
        super.onDismiss(dialog);
    }


    public void close(){
        getDialog().dismiss();
    }

    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = (Button)view.findViewById(R.id.btn_cancel);
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mScanner.stopScan(mScanCallback);
                    cancelButton.setText(R.string.scan);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mScanner.startScan(mScanFilters, mSettings, mScanCallback);
            cancelButton.setText(R.string.cancel);
        } else {
            stopScan();
            cancelButton.setText(R.string.scan);
        }
    }

    private void stopScan(){
        if(mScanner!=null) {
            mScanning = false;
            mScanner.stopScan(mScanCallback);
        }
    }

    private void runOnUiThread(Runnable action) {
        if(mUiThread==null){
            mUiThread = getActivity().getMainLooper().getThread();
        }
        if (Thread.currentThread() != mUiThread) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    private ScanCallback mScanCallback =
        new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                final BluetoothDevice device = result.getDevice();
                final int rssi = result.getRssi();
                DeviceListDialog.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addDevice(device, rssi);
                    }
                });
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;

        for (BluetoothDevice listDev : mDeviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        mDeviceAdapter.putRSSIValue(device.getAddress(), rssi);
        if (!deviceFound) {
            mDeviceList.add(device);
            mScanningText.setVisibility(View.GONE);
            mDeviceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = mDeviceList.get(position);
        mDeviceAddress = device.getAddress();
        stopScan();
        close();
    }
}
