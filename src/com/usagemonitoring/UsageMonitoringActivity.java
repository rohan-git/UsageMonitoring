package com.usagemonitoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class UsageMonitoringActivity extends Activity {

	private static final String TAG = "MAIN_AC";

	/** Called when the activity is first created. */

	Intent theIntent;
	Button startLearningButton, stopLearningButton, startMonitoringButton,
			stopMonitoringButton, clearTraining, clearTesting;

	// Intent wifiEndIntent;

	// Context context = getApplicationContext();

	int a;
	WifiMobileRecorder wifiMobileRecorder;
	ContactsRecorder contactsRecorder;
	RunningAppsRecorder runningAppsRecorder;
	GPSLocationRecorder gpsLocationRecorder;
	PhoneSettingsRecorder phoneSettingsRecorder;

	ArrayList<MultipleValues> phSettingsTrainingData;
	HashMap<String, Float> runningAppsTrainingData;
	ArrayList<LocationsReturns> locationsTrainingData;

	// DecisionEngine dMaker;
	int start, stop;
	Timer timer = new Timer();

	// Timer timer2 = new Timer();

	public UsageMonitoringActivity() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Context context = getApplicationContext();
		// dMaker = new DecisionEngine(context);
		wifiMobileRecorder = new WifiMobileRecorder(context);
		contactsRecorder = new ContactsRecorder(context);
		runningAppsRecorder = new RunningAppsRecorder(context);
		gpsLocationRecorder = new GPSLocationRecorder(context);
		gpsLocationRecorder.setMinDist(0);
		gpsLocationRecorder.setMinTime(1000);
		phoneSettingsRecorder = new PhoneSettingsRecorder(context);
		// wifiMobileRecorder.createReceiver();

		locationsTrainingData = new ArrayList<LocationsReturns>();
		phSettingsTrainingData = new ArrayList<MultipleValues>();
		runningAppsTrainingData = new HashMap<String, Float>();

		startLearningButton = (Button) findViewById(R.id.startlearningbutton);
		stopLearningButton = (Button) findViewById(R.id.stoplearningbutton);
		startMonitoringButton = (Button) findViewById(R.id.startmonitoringbutton);
		stopMonitoringButton = (Button) findViewById(R.id.stopmonitoringbutton);
		clearTraining = (Button) findViewById(R.id.cleartraining);
		clearTesting = (Button) findViewById(R.id.cleartesting);

		startLearningButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				phoneSettingsRecorder.setTrainOrTest(true);
				phoneSettingsRecorder.createReceivers();

				runningAppsRecorder.createRecorder();
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						runningAppsRecorder.pollForTasks();
					}
				}, 0, 10000);

				gpsLocationRecorder.createListener();

			}
		});
		stopLearningButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				phSettingsTrainingData = phoneSettingsRecorder
						.writeAndReadPercSettingsInformation();
				phoneSettingsRecorder.destroy();

				runningAppsTrainingData = runningAppsRecorder
						.readRunningAppsPercentageInformation();
				runningAppsRecorder.destroy();

				timer.cancel();

				locationsTrainingData = gpsLocationRecorder
						.readAverageGpsInformation();
				gpsLocationRecorder.destroy();
			}
		});

		startMonitoringButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				theIntent.putExtra("locationsTrainingData",
						locationsTrainingData);
				theIntent.putParcelableArrayListExtra("phSettingsTrainingData",
						phSettingsTrainingData);
				theIntent.putExtra("runningAppsTrainingData",
						runningAppsTrainingData);

				startService(theIntent);

			}
		});
		stopMonitoringButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				// dMaker.gatherInformation();

				stopService(theIntent);

				// dMaker.gatherLocationInfo();
			}
		});

		clearTraining.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

//				phoneSettingsRecorder.clearDB();
//				runningAppsRecorder.clearDB();
//				gpsLocationRecorder.clearDB();

			}
		});
		clearTesting.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

			}
		});

		Date d1 = new Date();
		start = d1.getSeconds();
		// Log.e(TAG, start+"");
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		a = 10;

		// wifiMobileRecorder.createReceiver();
		theIntent = new Intent(this, BackgroundService.class);
		// wifiEndIntent = new Intent(this, WifiData.class);
		theIntent.putExtra("a", a);
		//

		// networkSettingChanged.printi();
		Log.e("MENU", "Constructed!");
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		super.onTouchEvent(event);

		// phoneSettingsRecorder.clearDB();
		// gpsLocationRecorder.clearDB();

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		// wifiMobileRecorder.destroy();
		// phoneSettingsRecorder.destroy();

		Date d2 = new Date();
		stop = d2.getSeconds();
		Log.e(TAG, (stop - start) + "");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.new_:
			new_();
			return true;
		case R.id.help:
			showHelp();
			return true;
		default:
			return super.onOptionsItemSelected(item);

		}
	}

	private void new_() {
		// TODO Auto-generated method stub
		Toast.makeText(this, "New", Toast.LENGTH_SHORT).show();
		// startService(this.wifiStartIntent);
	}

	private void showHelp() {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Help", Toast.LENGTH_SHORT).show();

		// stopService(this.wifiStartIntent);

	}
}