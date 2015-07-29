package jp.wdri.wdmd;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import java.util.UUID;

/**
 * Created by r on 15/07/28.
 * MIDIService
 */
public class MIDIService extends Service {

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    public final static String INTENT_ACTION = "jp.wdri.wdmd.MIDIService";
    public final static String INTENT_CALL   = "jp.wdri.wdmd.RxMIDIService";

    public final static int ACTION_GATT_CONNECTED           = 10;
    public final static int ACTION_GATT_DISCONNECTED        = 20;
    public final static int ACTION_GATT_SERVICES_DISCOVERED = 30;
    public final static int ACTION_NOT_SUPPORT_MIDI         = 40;
    public final static int ACTION_MIDI_ENABLED             = 50;
    public final static int ACTION_DATA_AVAILABLE           = 60;

    public final static int ACTION_CALL_CONNECT             = 100;
    public final static int ACTION_CALL_DISCONNECT          = 110;

    public final static String EXTRA_ACTION  = "EXTRA_ACTION";
    public final static String EXTRA_DATA    = "EXTRA_DATA";
    public final static String EXTRA_ADDRESS = "EXTRA_ADDRESS";

    public static final UUID BLE_UUID_MIDI_SERVICE           = UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");
    public static final UUID BLE_UUID_MIDI_IO_CHARACTERISTIC = UUID.fromString("7772E5DB-3868-4112-A1A9-F2669D106BF3");

    public static final UUID BLE_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState){
                case BluetoothProfile.STATE_CONNECTED:
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    mBluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    close();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                registerMIDI();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final int action) {
        broadcastUpdate(action, null);
    }

    private void broadcastUpdate(final int action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra(EXTRA_ACTION, action);
        if(characteristic!=null) {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void registerMIDI()
    {
        BluetoothGattService midiService = mBluetoothGatt.getService(BLE_UUID_MIDI_SERVICE);
        if (midiService == null) {
            broadcastUpdate(ACTION_NOT_SUPPORT_MIDI);
            return;
        }
        BluetoothGattCharacteristic midiChar = midiService.getCharacteristic(BLE_UUID_MIDI_IO_CHARACTERISTIC);
        if (midiChar == null) {
            broadcastUpdate(ACTION_NOT_SUPPORT_MIDI);
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(midiChar, true);

        BluetoothGattDescriptor descriptor = midiChar.getDescriptor(BLE_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

        broadcastUpdate(ACTION_MIDI_ENABLED);
    }

    /* Connection */

    public boolean connect(final String address) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || address == null) {
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = adapter.getRemoteDevice(address);
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    public void close() {
        if (mBluetoothGatt != null) {
            mBluetoothDeviceAddress = null;
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    static public void connect(final Context context, final String address) {
        final Intent intent = new Intent(INTENT_CALL);
        intent.putExtra(EXTRA_ACTION, ACTION_CALL_CONNECT);
        intent.putExtra(EXTRA_ADDRESS, address);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    static public void disconnect(final Context context) {
        final Intent intent = new Intent(INTENT_CALL);
        intent.putExtra(EXTRA_ACTION, ACTION_CALL_DISCONNECT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(MIDIService.INTENT_CALL)) {
                    switch (intent.getIntExtra(EXTRA_ACTION,0)){
                        case ACTION_CALL_CONNECT:
                            connect(intent.getStringExtra(EXTRA_ADDRESS));
                            break;
                        case ACTION_CALL_DISCONNECT:
                            disconnect();
                            break;
                        default:
                            break;
                    }
                }
            }
        }, new IntentFilter(MIDIService.INTENT_CALL));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}

