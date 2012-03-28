package com.usagemonitoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class BackgroundService extends Service {

	private static final String TAG = "SRVC";
	private static final long APPS_POLL_FREQ = 10000L;
	// Handler h1;
	// Runnable r1;

	// WifiMobileRecorder wifiMobileRecorder;

	StringBuilder runningAppwarnings = new StringBuilder();

	DecisionEngine dMaker;
	HashMap<String, Float> runningAppsTrainingData = new HashMap<String, Float>();
	MultipleValues[] phSettingsTrainingData; // = new MultipleValues[];
	LocationsReturns[] locationsTrainingData; // = new LocationsReturns[]();

	List<MultipleValues> phSettingsTrainingDataList = new ArrayList<MultipleValues>();
	ArrayList<LocationsReturns> locationsTrainingDataList = new ArrayList<LocationsReturns>();

	Timer timer = new Timer();
	Handler handler = new Handler();

	Runnable r1 = new Runnable() {

		@Override
		public void run() {
 
//			runningAppwarnings.append(dMaker.gatherRunningAppsInfo());
//			 dMaker.printRunningAppsWarnings();
//			Toast.makeText(getBaseContext(), runningAppwarnings,
//					Toast.LENGTH_SHORT).show();
//			runningAppwarnings = new StringBuilder();
			doStuff();
			handler.postDelayed(r1, APPS_POLL_FREQ);

		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		dMaker = new DecisionEngine(getApplicationContext());
		 
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onStart(Intent intent, int startId) {

		super.onStart(intent, startId);
		Bundle b = intent.getExtras();
		int temp = b.getInt("a");

		Log.e(TAG, "started service , a = " + temp);

		runningAppsTrainingData = (HashMap<String, Float>) intent
				.getSerializableExtra("runningAppsTrainingData");
		phSettingsTrainingDataList = intent
				.getParcelableArrayListExtra("phSettingsTrainingData");
		locationsTrainingDataList = intent
				.getParcelableArrayListExtra("locationsTrainingData");

		dMaker.initStuff();
		dMaker.addPhSettingsTrainingData(phSettingsTrainingDataList);
		dMaker.addRunningAppsTrainingData(runningAppsTrainingData);
		dMaker.addGPSTrainingData(locationsTrainingDataList);

		handler.post(r1);
 
	}

	// ~~~ Can't create handler inside thread that has not called Looper.prepare()
	void doStuff() {
 		 
		// Log.e(TAG, "test");
  		
		dMaker.gatherSettingsInformation();
		StringBuilder sb = dMaker.gatherRunningAppsInfo();
		
		dMaker.gatherLocationInfo();
		
//		Toast.makeText(getApplicationContext(), sb.toString(), Toast.LENGTH_SHORT).show();
		 
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub

		super.onDestroy();
		 

		handler.removeCallbacks(r1);
		// timer.cancel();

		dMaker.destroy();

		// contactsRecorder.destroy();
		// h1.removeCallbacks(r1);

		// try {
		// h1.wait(1000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		Log.e(TAG, "destroyed service ");

	}

	// protected void onHandleIntent(Intent arg0) {
	// // TODO Auto-generated method stub
	//
	// Log.e("WIFIDATa", "Handling intnet");
	//
	// }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	class BackgroundHelper extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			// doStuff();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			Toast.makeText(getApplicationContext(), "DONE", Toast.LENGTH_SHORT)
					.show();

		}

	}

}
