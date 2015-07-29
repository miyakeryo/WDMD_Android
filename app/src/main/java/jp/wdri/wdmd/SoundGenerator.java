package jp.wdri.wdmd;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

/**
 * Created by r on 15/07/28.
 * SoundGenerator
 */
public class SoundGenerator {

    private static int sampleRate = 44100;
    private static int bufferSize = 0;

    private static int getBufferSize(){
        if(bufferSize==0){
            bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        }
        return bufferSize;
    }

    public static byte[] getSound(double frequency, double length) {
        byte[] buffer = new byte[(int)Math.ceil(getBufferSize() * length)];
        for(int i=0; i<buffer.length; i++) {
            double wave = i / (sampleRate / frequency) * (Math.PI * 2);
            wave = Math.sin(wave);
            buffer[i] = wave > 0.0 ? Byte.MAX_VALUE : Byte.MIN_VALUE;
        }

        return buffer;
    }

    public static AudioTrack createAudioTrack() {
        return new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_DEFAULT,
                getBufferSize(),
                AudioTrack.MODE_STREAM);
    }

    public static double getFrequency(int noteNo){
        return 440.0 * Math.pow(2.0, (noteNo - 69) / 12.0);
    }
}
