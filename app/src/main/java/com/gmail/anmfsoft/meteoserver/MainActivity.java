package com.gmail.anmfsoft.meteoserver;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity implements OnSeekBarChangeListener{

    PendingIntent pendingIntent;
	AlarmManager manager;
	SeekBar sb;
	TextView tv1,tv2;

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    String device;


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data){
        Log.d(TAG,"onActivityResult"+" codigo="+requestCode);
        switch (requestCode) {
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "File successfully saved.");
                    //mBitmapToSave = null;
                    // Just start the camera again for another photo.
                    //startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                    //        REQUEST_CODE_CAPTURE_IMAGE);
                }
                break;
        }
        Log.d(TAG,"fin de onActivityResult"+" codigo="+requestCode);
    }


    @Override
    protected void onPause() {
        Log.d(TAG,"onPause");
        super.onPause();
        Log.d(TAG,"fin onPause");
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.LinearLayout1, new PlaceholderFragment()).commit();
		}

        /*
		Intent alarmIntent = new Intent(this, AlarmReceiver.class);
	    pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        */

	    //SeekBar seekBar = (SeekBar)layout.findViewById(R.id.see);
	    sb=(SeekBar)findViewById(R.id.seekBar1);
	    sb.setOnSeekBarChangeListener(this);
	    sb.setProgress(5);
	    sb.setMax(10);
	    tv2=(TextView)findViewById(R.id.textView2);

        this.obtenerPreferences();

        Log.d(TAG,"fin onCreate");
	}
	
	public void startAlarm(View view) {
        this.cancelAlarm(view);
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);

        Bundle extras = alarmIntent.getExtras();
        extras.putString("device", device);

        Log.d(TAG,"startAlarm");
	    manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
	    int interval = 10000;
        int ii=this.sb.getProgress();
        if(ii==0)
            interval=15000;
        else
            interval=60000*ii;
	    Log.d("MeteoServer",interval+"");
	    manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
        Log.d(TAG,"fin startAlarm");
	}


	
	public void cancelAlarm(View view) {
        Log.d(TAG,"cancelAlarm");
		Intent alarmIntent = new Intent(this, AlarmReceiver.class);
		pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
		manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
	    if (manager != null) {
	        manager.cancel(pendingIntent);
	        //Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
	        Log.d("MeteoServer","Alarma eliminada");
	    }else
	    	Log.d("MeteoServer","Alarma no existe");


        Log.d(TAG,"fin cancelAlarm");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG,"onCreateOptionsMenu");
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
        Log.d(TAG,"fin onCreateOptionsMenu");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG,"onOptionsItemSelected");
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
        Intent intent;
		if (id == R.id.action_settings) {
            Log.d(TAG, "config option");
            intent = new Intent(this, PrefsActivity.class);
            startActivity(intent);

            return true;
		}
        Log.d(TAG,"fin onOptionsItemSelected");
		return super.onOptionsItemSelected(item);
	}
	public static class PlaceholderFragment extends Fragment {
		public PlaceholderFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
            Log.d(TAG,"onCreateView de PlaceholderFragment");
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
            Log.d(TAG,"fin onCreateView de PlaceholderFragment");
			return rootView;
		}
	}
	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        Log.d(TAG,"onProgressChanged");
		final Handler handler = new Handler();
		final int iaux=arg1;
		handler.post(new Runnable()
        {
        	public void run()
        	{
            	Log.d("MeteoServer",iaux+"");
                if(iaux==0)
                    tv2.setText("15 seg.");
                else
                    tv2.setText((iaux)+" min.");
            }
        });
        Log.d(TAG,"fin onProgressChanged");
	}
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {    }



    private void obtenerPreferences() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        device = prefs.getString("device", "Meteo");
    }

}
