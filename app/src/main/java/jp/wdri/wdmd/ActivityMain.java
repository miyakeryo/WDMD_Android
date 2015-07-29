package jp.wdri.wdmd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by r on 15/07/28.
 * ActivityMain
 */
public class ActivityMain extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, MIDIService.class));
    }

}
