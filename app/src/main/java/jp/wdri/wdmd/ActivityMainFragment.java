package jp.wdri.wdmd;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;


/**
 * A placeholder fragment containing a simple view.
 */
public class ActivityMainFragment extends Fragment implements OnTouchListener {

    private Map<Short, AudioTrack> mAudioTrackMap = new HashMap<>();
    private boolean mConnecting = false;
    private Button mScanBtn;

    public static final int DEVICE_LIST_ID = 2;

    public ActivityMainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.main_f, container, false);

        view.findViewById(R.id.btnC).setOnTouchListener(this);
        view.findViewById(R.id.btnCs).setOnTouchListener(this);
        view.findViewById(R.id.btnD).setOnTouchListener(this);
        view.findViewById(R.id.btnDs).setOnTouchListener(this);
        view.findViewById(R.id.btnE).setOnTouchListener(this);
        view.findViewById(R.id.btnF).setOnTouchListener(this);
        view.findViewById(R.id.btnFs).setOnTouchListener(this);
        view.findViewById(R.id.btnG).setOnTouchListener(this);
        view.findViewById(R.id.btnGs).setOnTouchListener(this);
        view.findViewById(R.id.btnA).setOnTouchListener(this);
        view.findViewById(R.id.btnAs).setOnTouchListener(this);
        view.findViewById(R.id.btnB).setOnTouchListener(this);

        mScanBtn = (Button)view.findViewById(R.id.scan_btn);
        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnecting){
                    MIDIService.disconnect(getActivity());
                } else {
                    DeviceListDialog dialog = new DeviceListDialog();
                    dialog.setTargetFragment(ActivityMainFragment.this, DEVICE_LIST_ID);
                    dialog.show(getFragmentManager(), "device");
                }
            }
        });

        if (BluetoothAdapter.getDefaultAdapter() != null) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().equals(MIDIService.INTENT_ACTION)){
                        onReceiveMIDI(intent.getIntExtra(MIDIService.EXTRA_ACTION, 0),
                                      intent.getByteArrayExtra(MIDIService.EXTRA_DATA));
                    }
                }
            }, new IntentFilter(MIDIService.INTENT_ACTION));
        }else{
            toast("Bluetooth is not available");
        }

        return view;
    }

    void onReceiveMIDI(int action, byte[] data){
        switch (action){
            case MIDIService.ACTION_GATT_CONNECTED:
                toast("Connected");
                mScanBtn.setText(R.string.disconnect);
                break;
            case MIDIService.ACTION_GATT_DISCONNECTED:
                toast("Disconnected");
                audioStopAll();
                mScanBtn.setText(R.string.scan);
                break;
            case MIDIService.ACTION_GATT_SERVICES_DISCOVERED:
                // ok
                break;
            case MIDIService.ACTION_NOT_SUPPORT_MIDI:
                toast("Device dose not support MIDI");
                break;
            case MIDIService.ACTION_MIDI_ENABLED:
                // ok
                break;
            case MIDIService.ACTION_DATA_AVAILABLE:
                for(byte dat : data){
                    receiveMidiDat((short) (dat & 0x00FF));
                }
                break;
            default:
                break;
        }
    }


    /*
    sample

    0xbf	0b10111111	Header Byte
    0xff	0b11111111	Timestamp Byte
    0x90	0b10010000	MIDI Status Note On
    0x46	0b01000110	MIDI Note No
    0x3f	0b00111111	MIDI Velocity
    0xbf	0b10111111	Header Byte
    0xff	0b11111111	Timestamp Byte
    0x90	0b10010000	MIDI Status Note On
    0x46	0b01000110	MIDI Note No
    0x00	0b00111111	MIDI Velocity 0
    */
    static final short BIT_7 = 0b10000000;
    private short midi_status = 0;
    private short midi_note_no = 0;

    void receiveMidiDat(short dat){
        if((dat & BIT_7) == BIT_7){
            // HeaderとTimestampは無視する
            midi_status = dat;
            midi_note_no = 0;
        }else if(midi_note_no==0){
            switch(midi_status){
                case 0x80: // Note Off
                    midi_note_no = dat;
                    audioStop(midi_note_no);
                    break;
                case 0x90: // Note On
                    midi_note_no = dat;
                    break;
                default:
                    break;
            }
        }else{
            switch(midi_status){
                case 0x90: // Note On
                    if(dat==0){ // velocity
                        audioStop(midi_note_no);
                    }else{
                        audioStart(midi_note_no);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DEVICE_LIST_ID) {
            String device = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
            if(device!=null){
                MIDIService.connect(getActivity(), device);
            }else{
                MIDIService.disconnect(getActivity());
            }
        }
    }

    void toast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    void audioStart(final short noteNo){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    double freq = SoundGenerator.getFrequency(noteNo);
                    byte[] sound = SoundGenerator.getSound(freq, 10.0);
                    AudioTrack audioTrack = SoundGenerator.createAudioTrack();
                    mAudioTrackMap.put(noteNo, audioTrack);
                    audioTrack.play();
                    audioTrack.write(sound, 0, sound.length);
                    if(audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop();
                        audioTrack.release();
                    }
                }
            }).start();

    }

    void audioStop(final short noteNo){
        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioTrack audioTrack = mAudioTrackMap.get(noteNo);
                if(audioTrack!=null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop();
                    audioTrack.release();
                }
            }
        }).start();
    }

    void audioStopAll(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(Map.Entry<Short, AudioTrack> entry : mAudioTrackMap.entrySet()) {
                    AudioTrack audioTrack = entry.getValue();
                    if(audioTrack!=null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack.stop();
                        audioTrack.release();
                    }
                }
            }
        }).start();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                audioStopAll();
                return false;
            default:
                return false;
        }
        short noteNo = 0;
        switch (v.getId()){
            case R.id.btnC:
                noteNo = 60;
                break;
            case R.id.btnCs:
                noteNo = 61;
                break;
            case R.id.btnD:
                noteNo = 62;
                break;
            case R.id.btnDs:
                noteNo = 63;
                break;
            case R.id.btnE:
                noteNo = 64;
                break;
            case R.id.btnF:
                noteNo = 65;
                break;
            case R.id.btnFs:
                noteNo = 66;
                break;
            case R.id.btnG:
                noteNo = 67;
                break;
            case R.id.btnGs:
                noteNo = 68;
                break;
            case R.id.btnA:
                noteNo = 69;
                break;
            case R.id.btnAs:
                noteNo = 70;
                break;
            case R.id.btnB:
                noteNo = 71;
                break;
            default:
                break;
        }
        if(noteNo!=0) {
            audioStart(noteNo);
        }
        return true;
    }

}
