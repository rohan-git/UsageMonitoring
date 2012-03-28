package com.usagemonitoring;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import android.R.integer;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Toast;

/**
 * @author Rohan
 * 
 *         Listens to change in <b>CONTENT_URI </b>, <br>
 *         It will reflect changes in most settings <br>
 *         except the Networks:
 *         <ul>
 *         <li>Network(Edge/3g/4g)</li>
 *         <li>Bluetooth</li>
 *         <li>GPS</li>
 *         </ul>
 *         Those seem to have been deprecated here and <br>
 *         have a different process for listening.
 * 
 **/
public class PhoneSettingsRecorder {

	private static final String TAG = "PH_STNG";

	// represents seconds.
	// private static final int LOWER_LIMIT = 5; // 5 seconds.
	private static final int UPPER_LIMIT = 31556926; // seconds in a year
	//
	HashMap<String, String> initialSettings = new HashMap<String, String>();
	HashMap<String, String> changedSettings = new HashMap<String, String>();
	HashMap<String, Long> startTimes = new HashMap<String, Long>();
	HashMap<String, Long> endTimes = new HashMap<String, Long>();
	HashMap<String, Long> runningTimes = new HashMap<String, Long>();

	private PhoneSettingsDB phoneSettingsDB;
	boolean train = true;

	Context context;
	private ContentObserver co;

	public PhoneSettingsRecorder(Context context) {

		this.context = context;
		phoneSettingsDB = new PhoneSettingsDB(context);

		initializeStartTimes(0L);
		initializeEndTimes(0L);
		initializeRunningTimes(0L);

	}

	private void initializeStartTimes(long times) {
		startTimes.put(System.ACCELEROMETER_ROTATION, times);
		startTimes.put(System.AIRPLANE_MODE_ON, times);
		startTimes.put(System.DIM_SCREEN, times);
		startTimes.put(System.HAPTIC_FEEDBACK_ENABLED, times);
		startTimes.put(System.MODE_RINGER, times);
		startTimes.put(System.RINGTONE, times);
		startTimes.put(System.SCREEN_BRIGHTNESS, times);
		startTimes.put(System.SCREEN_BRIGHTNESS_MODE, times);
		startTimes.put(System.SCREEN_OFF_TIMEOUT, times);
		startTimes.put(System.SOUND_EFFECTS_ENABLED, times);
		startTimes.put(System.USB_MASS_STORAGE_ENABLED, times);
		startTimes.put(System.VIBRATE_ON, times);
		startTimes.put(System.VOLUME_ALARM, times);
		startTimes.put(System.VOLUME_BLUETOOTH_SCO, times);
		startTimes.put(System.VOLUME_MUSIC, times);
		startTimes.put(System.VOLUME_NOTIFICATION, times);
		startTimes.put(System.VOLUME_RING, times);
		startTimes.put(System.VOLUME_SYSTEM, times);
		startTimes.put(System.VOLUME_VOICE, times);

	}

	private void initializeEndTimes(long times) {

		endTimes.put(System.ACCELEROMETER_ROTATION, times);
		endTimes.put(System.AIRPLANE_MODE_ON, times);
		endTimes.put(System.DIM_SCREEN, times);
		endTimes.put(System.HAPTIC_FEEDBACK_ENABLED, times);
		endTimes.put(System.MODE_RINGER, times);
		endTimes.put(System.RINGTONE, times);
		endTimes.put(System.SCREEN_BRIGHTNESS, times);
		endTimes.put(System.SCREEN_BRIGHTNESS_MODE, times);
		endTimes.put(System.SCREEN_OFF_TIMEOUT, times);
		endTimes.put(System.SOUND_EFFECTS_ENABLED, times);
		endTimes.put(System.USB_MASS_STORAGE_ENABLED, times);
		endTimes.put(System.VIBRATE_ON, times);
		endTimes.put(System.VOLUME_ALARM, times);
		endTimes.put(System.VOLUME_BLUETOOTH_SCO, times);
		endTimes.put(System.VOLUME_MUSIC, times);
		endTimes.put(System.VOLUME_NOTIFICATION, times);
		endTimes.put(System.VOLUME_RING, times);
		endTimes.put(System.VOLUME_SYSTEM, times);
		endTimes.put(System.VOLUME_VOICE, times);
	}

	private void initializeRunningTimes(long times) {

		runningTimes.put(System.ACCELEROMETER_ROTATION, times);
		runningTimes.put(System.AIRPLANE_MODE_ON, times);
		runningTimes.put(System.DIM_SCREEN, times);
		runningTimes.put(System.HAPTIC_FEEDBACK_ENABLED, times);
		runningTimes.put(System.MODE_RINGER, times);
		runningTimes.put(System.RINGTONE, times);
		runningTimes.put(System.SCREEN_BRIGHTNESS, times);
		runningTimes.put(System.SCREEN_BRIGHTNESS_MODE, times);
		runningTimes.put(System.SCREEN_OFF_TIMEOUT, times);
		runningTimes.put(System.SOUND_EFFECTS_ENABLED, times);
		runningTimes.put(System.USB_MASS_STORAGE_ENABLED, times);
		runningTimes.put(System.VIBRATE_ON, times);
		runningTimes.put(System.VOLUME_ALARM, times);
		runningTimes.put(System.VOLUME_BLUETOOTH_SCO, times);
		runningTimes.put(System.VOLUME_MUSIC, times);
		runningTimes.put(System.VOLUME_NOTIFICATION, times);
		runningTimes.put(System.VOLUME_RING, times);
		runningTimes.put(System.VOLUME_SYSTEM, times);
		runningTimes.put(System.VOLUME_VOICE, times);

	}

	public void setTrainOrTest(boolean train) {
		this.train = train;
	}

	void createReceivers() {

		initializeStartTimes(java.lang.System.currentTimeMillis());
		getConfigInformation(initialSettings);
		getConfigInformation(changedSettings);
		writeInitialInformation();
		// compareAndexchangeInformation();

		final Handler h1 = new Handler();
		co = new ContentObserver(h1) {

			@Override
			public void onChange(boolean selfChange) {
				// TODO Auto-generated method stub
				super.onChange(selfChange);

				getConfigInformation(changedSettings);
				compareAndexchangeInformation();

			}
		};

		context.getContentResolver().registerContentObserver(
				Settings.System.CONTENT_URI, true, co);

	}

	void destroy() {

		context.getContentResolver().unregisterContentObserver(co);
		co = null;
		phoneSettingsDB.close();
	}

	// Called by the receiver upon receiving broadcast of settings change
	private void getConfigInformation(HashMap<String, String> changedSettings) {

		Log.e(TAG, ">> ph information");

		changedSettings.put(System.ACCELEROMETER_ROTATION, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.ACCELEROMETER_ROTATION));

		changedSettings
				.put(System.AIRPLANE_MODE_ON, Settings.System.getString(
						context.getContentResolver(),
						Settings.System.AIRPLANE_MODE_ON));

		// initialSettings.put(System.BLUETOOTH_ON, Settings.System.getString(
		// context.getContentResolver(), Settings.System.BLUETOOTH_ON));

		changedSettings.put(System.DIM_SCREEN, Settings.System.getString(
				context.getContentResolver(), Settings.System.DIM_SCREEN));

		changedSettings.put(System.HAPTIC_FEEDBACK_ENABLED, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.HAPTIC_FEEDBACK_ENABLED));

		changedSettings.put(System.MODE_RINGER, Settings.System.getString(
				context.getContentResolver(), Settings.System.MODE_RINGER));

		changedSettings.put(System.RINGTONE, Settings.System.getString(
				context.getContentResolver(), Settings.System.RINGTONE));

		// screen
		changedSettings.put(System.SCREEN_BRIGHTNESS, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS));

		changedSettings.put(System.SCREEN_BRIGHTNESS_MODE, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.SCREEN_BRIGHTNESS_MODE));

		changedSettings.put(System.SCREEN_OFF_TIMEOUT, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.SCREEN_OFF_TIMEOUT));

		changedSettings.put(System.SOUND_EFFECTS_ENABLED, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.SOUND_EFFECTS_ENABLED));

		// usb
		changedSettings.put(System.USB_MASS_STORAGE_ENABLED, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.USB_MASS_STORAGE_ENABLED));

		// Volume and vibrate
		changedSettings.put(System.VIBRATE_ON, Settings.System.getString(
				context.getContentResolver(), Settings.System.VIBRATE_ON));

		changedSettings.put(System.VOLUME_ALARM, Settings.System.getString(
				context.getContentResolver(), Settings.System.VOLUME_ALARM));

		changedSettings.put(System.VOLUME_BLUETOOTH_SCO, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.VOLUME_BLUETOOTH_SCO));

		changedSettings.put(System.VOLUME_MUSIC, Settings.System.getString(
				context.getContentResolver(), Settings.System.VOLUME_MUSIC));

		changedSettings.put(System.VOLUME_NOTIFICATION, Settings.System
				.getString(context.getContentResolver(),
						Settings.System.VOLUME_NOTIFICATION));

		changedSettings.put(System.VOLUME_RING, Settings.System.getString(
				context.getContentResolver(), Settings.System.VOLUME_RING));

		changedSettings.put(System.VOLUME_SYSTEM, Settings.System.getString(
				context.getContentResolver(), Settings.System.VOLUME_SYSTEM));

		changedSettings.put(System.VOLUME_VOICE, Settings.System.getString(
				context.getContentResolver(), Settings.System.VOLUME_VOICE));

		Log.e(TAG, "<< got ph information");

	}

	private void writeInitialInformation() {

		for (String key : initialSettings.keySet()) {
			Log.e(TAG,
					"initially ik: " + key + " || iv: "
							+ initialSettings.get(key));

			endTimes.put(key, java.lang.System.currentTimeMillis());
			runningTimes.put(key,
					(endTimes.get(key) - startTimes.get(key)) / 1000);
			startTimes.put(key, endTimes.get(key));
			if (
			// runningTimes.get(key)
			// > LOWER_LIMIT &&
			runningTimes.get(key) < UPPER_LIMIT) {

				SQLiteDatabase db = phoneSettingsDB.getWritableDatabase();
				phoneSettingsDB.insertStuff(db, train, key,
						initialSettings.get(key), runningTimes.get(key));

			}
		}

	}

	  
	private void compareAndexchangeInformation() {

		for (String key : initialSettings.keySet()) {
			if (!(initialSettings.get(key).equals(changedSettings.get(key)))) {

				Log.e(TAG, "ik: " + key + " || iv: " + initialSettings.get(key));
				Log.e(TAG, "ck: " + key + " || cv: " + changedSettings.get(key));

				endTimes.put(key, java.lang.System.currentTimeMillis());
				runningTimes.put(key,
						(endTimes.get(key) - startTimes.get(key)) / 1000);
				startTimes.put(key, endTimes.get(key));
				if (
				// runningTimes.get(key)
				// > LOWER_LIMIT &&
				runningTimes.get(key) < UPPER_LIMIT) {

					SQLiteDatabase db = phoneSettingsDB.getWritableDatabase();
					phoneSettingsDB.insertStuff(db, train, key,
							initialSettings.get(key), runningTimes.get(key));

				}
			}

		}

		for (String key : changedSettings.keySet())
			initialSettings.put(key, changedSettings.get(key));

	}

	public void readSettingsInformation() {
		SQLiteDatabase db = phoneSettingsDB.getReadableDatabase();
		phoneSettingsDB.readStuff(db, train);
	}

	public void writeAndReadSettingsInformation() {

		writeInitialInformation();
		// readSettingsInformation();
		readPercentageSettingsTimes();
	}

	public ArrayList<MultipleValues> writeAndReadPercSettingsInformation() {

		writeInitialInformation();
		return readPercentageSettingsTimes();
	}

	public HashMap<String, HashMap<String, String>> readAverageSettingsTimes() {
		SQLiteDatabase db = phoneSettingsDB.getReadableDatabase();
		return phoneSettingsDB.getAverageTimes(db, train);
	}

	public ArrayList<MultipleValues> readPercentageSettingsTimes() {
		SQLiteDatabase db = phoneSettingsDB.getReadableDatabase();
		return phoneSettingsDB.getPercentageTimes(db, train);
	}

	public void clearDB() {
		SQLiteDatabase db = phoneSettingsDB.getWritableDatabase();
		phoneSettingsDB.clearStuff(db, train);
	}

	static class PhoneSettingsDB extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "PhoneSettingsDB";
		private static final String SETTINGS_TRAINING_TABLE_NAME = "settingsTrain";
		private static final String SETTINGS_TESTING_TABLE_NAME = "settingsTest";
		private static final String SETTINGS_TYPE_KEY_COLUMN = "SETTINGS_TYPE_KEY";
		private static final String SETTINGS_TYPE_VALUE_COLUMN = "SETTINGS_TYPE_VALUE";
		private static final String SETTINGS_DURATION_COLUMN = "SETTINGS_DURATION";
		private static final String SETTINGS_TRAINING_TABLE_CREATE = "CREATE TABLE "
				+ SETTINGS_TRAINING_TABLE_NAME
				+ " ("
				+ SETTINGS_TYPE_KEY_COLUMN
				+ " TEXT, "
				+ SETTINGS_TYPE_VALUE_COLUMN
				+ " TEXT, "
				+ SETTINGS_DURATION_COLUMN + " TEXT);";
		private static final String SETTINGS_TESTING_TABLE_CREATE = "CREATE TABLE "
				+ SETTINGS_TESTING_TABLE_NAME
				+ " ("
				+ SETTINGS_TYPE_KEY_COLUMN
				+ " TEXT, "
				+ SETTINGS_TYPE_VALUE_COLUMN
				+ " TEXT, "
				+ SETTINGS_DURATION_COLUMN + " TEXT);";

		private static final String TAG = "PH_DB";

		PhoneSettingsDB(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(SETTINGS_TRAINING_TABLE_CREATE);
			db.execSQL(SETTINGS_TESTING_TABLE_CREATE);

		}

		public void insertStuff(SQLiteDatabase db, boolean train,
				String connectionTypeKey, String connectionTypeValue,
				long connectionDuration) {

			Log.i(TAG, "<<begin insert");

			String myTable = "";
			if (train == true) {
				myTable = SETTINGS_TRAINING_TABLE_NAME;
			} else {
				myTable = SETTINGS_TESTING_TABLE_NAME;
			}
			ContentValues cv = new ContentValues();
			cv.put(SETTINGS_TYPE_KEY_COLUMN, connectionTypeKey);
			cv.put(SETTINGS_TYPE_VALUE_COLUMN, connectionTypeValue);
			cv.put(SETTINGS_DURATION_COLUMN, connectionDuration);

			db.insert(myTable, null, cv);

			Log.i(TAG, "<<INSERT SUCCess");
			db.close();

		}

		public void readStuff(SQLiteDatabase db, boolean train) {

			Log.e(TAG, "<<begin read");

			String myTable = "";
			if (train == true) {
				myTable = SETTINGS_TRAINING_TABLE_NAME;
			} else {
				myTable = SETTINGS_TESTING_TABLE_NAME;
			}
			Cursor output = db.query(myTable, new String[] {
					SETTINGS_TYPE_KEY_COLUMN, SETTINGS_TYPE_VALUE_COLUMN,
					SETTINGS_DURATION_COLUMN }, null, null, null, null, null);

			// Log.e(TAG, "<<dsad");
			while (output.moveToNext()) {
				Log.e(TAG,
						"Settings Key: "
								+ output.getString(output
										.getColumnIndex(SETTINGS_TYPE_KEY_COLUMN))
								+ " : "
								+ "Settings Value: "
								+ output.getString(output
										.getColumnIndex(SETTINGS_TYPE_VALUE_COLUMN))
								+ " : "
								+ "Settings Duration: "
								+ output.getString(output
										.getColumnIndex(SETTINGS_DURATION_COLUMN)));
			}

			output.close();
			Log.e(TAG, ">>READ SUCCess");
			db.close();
		}

		public HashMap<String, HashMap<String, String>> getAverageTimes(
				SQLiteDatabase db, boolean train) {

			Log.e(TAG, "<<mobile time");

			String myTable = "";
			if (train == true) {
				myTable = SETTINGS_TRAINING_TABLE_NAME;
			} else {
				myTable = SETTINGS_TESTING_TABLE_NAME;
			}
			Cursor output = db.rawQuery("Select " + SETTINGS_TYPE_KEY_COLUMN
					+ "," + SETTINGS_TYPE_VALUE_COLUMN + ", avg("
					+ SETTINGS_DURATION_COLUMN + ") from " + myTable + ""
					+ " group by " + SETTINGS_TYPE_KEY_COLUMN + ","
					+ SETTINGS_TYPE_VALUE_COLUMN + " order by "
					+ "1 asc, 3 desc"
			// + " avg("
			// + SETTINGS_DURATION_COLUMN + ") desc"
					, null);
			int columnIndex = 0;
			HashMap<String, HashMap<String, String>> temp = new HashMap<String, HashMap<String, String>>();
			HashMap<String, String> inner = new HashMap<String, String>();

			while (output.moveToNext()) {

				String key = output.getString(output
						.getColumnIndexOrThrow(SETTINGS_TYPE_KEY_COLUMN));
				String value = output.getString(output
						.getColumnIndexOrThrow(SETTINGS_TYPE_VALUE_COLUMN));
				String duration = output.getString(output
						.getColumnIndexOrThrow("avg("
								+ SETTINGS_DURATION_COLUMN + ")"));
				inner.put(value, duration);
				temp.put(key, inner);
				Log.e(TAG, "Setting Type: " + key + " :: Setting Val: " + value
						+ " :: Avg Setting Dur: " + duration);
				columnIndex++;

			}

			output.close();
			db.close();
			Log.e(TAG, ">>mobile time");

			return temp;

		}

		/**
		 * 
		 * select b.SETTINGS_TYPE_KEY,
		 * (b.SETTINGS_DURATION*100/(a.SETTINGS_DURATION)) as perc from (select
		 * SETTINGS_TYPE_KEY, SETTINGS_TYPE_VALUE, SUM(SETTINGS_DURATION) as
		 * value from settings group by SETTINGS_TYPE_KEY, SETTINGS_TYPE_VALUE )
		 * as b inner join (select SETTINGS_TYPE_KEY, sum(SETTINGS_DURATION) as
		 * value from settings group by SETTINGS_TYPE_KEY) as a on
		 * b.SETTINGS_TYPE_KEY = a.SETTINGS_TYPE_KEY
		 *  Query didn't work on android sqlite, using
		 * loops
		 * */

		public ArrayList<MultipleValues> getPercentageTimes(
				SQLiteDatabase db, boolean train) {

			Log.e(TAG, "<<mobile time p");
			String myTable = "";
			if (train == true) {
				myTable = SETTINGS_TRAINING_TABLE_NAME;
			} else {
				myTable = SETTINGS_TESTING_TABLE_NAME;
			}
			Cursor output = db.rawQuery("Select " + SETTINGS_TYPE_KEY_COLUMN
					+ "," + SETTINGS_TYPE_VALUE_COLUMN + ", sum("
					+ SETTINGS_DURATION_COLUMN + ") from " + myTable
					+ " as perc" + " group by " + SETTINGS_TYPE_KEY_COLUMN
					+ "," + SETTINGS_TYPE_VALUE_COLUMN + " order by "
					+ "1 asc, 3 desc", null);

			ArrayList<String> keys = new ArrayList<String>();
			ArrayList<String> values = new ArrayList<String>();
			ArrayList<String> percentages = new ArrayList<String>();
			ArrayList<Float> durations = new ArrayList<Float>();
			ArrayList<Float> durationSums = new ArrayList<Float>();

			while (output.moveToNext()) {

				String key = output.getString(output
						.getColumnIndexOrThrow(SETTINGS_TYPE_KEY_COLUMN));
				String value = output.getString(output
						.getColumnIndexOrThrow(SETTINGS_TYPE_VALUE_COLUMN));
				String duration = output.getString(output
						.getColumnIndexOrThrow("sum("
								+ SETTINGS_DURATION_COLUMN + ")"));

				keys.add(key);
				values.add(value);
				durations.add(Float.parseFloat(duration));

//				Log.e(TAG, "Setting Type: " + key + " :: Setting Val: " + value
//						+ " :: Got AvgP. Setting Dur: " + duration);
			}

			float percDuration = 0;
			float sumDuration = 0;
			LinkedHashSet<String> uniqueKey; // = new HashSet<String>();
			ArrayList<Integer> frequencyList = new ArrayList<Integer>();
			
			
			for (int i = 0; i < keys.size(); i++) {
				durationSums.add(0F);
			}

			// Log.e(TAG, "" + durationSums.size());
			int partitionIndex = 0;

			uniqueKey = new LinkedHashSet<String>(keys);
			Log.e(TAG, "uniq setings " + uniqueKey.size());

//			for (String k : keys) {
//				Log.e(TAG, "keys are: " + k);
//			}
			
			for (String s : uniqueKey) {
				frequencyList.add(Collections.frequency(keys, s));
//				Log.e(TAG,
//						"added to frqList: " + Collections.frequency(keys, s));
			}
			//for (Integer f : frequencyList)
				//Log.e(TAG, "f: " + f);

			for (int i = 0; i < frequencyList.size(); i++) {
				for (int j = 0; j < frequencyList.get(i); j++) {
					sumDuration += durations.get(j + partitionIndex);
				}
				//Log.e(TAG, "~dur is" + sumDuration);
				for (int j = 0; j < frequencyList.get(i); j++) {
					durationSums.set(j + partitionIndex, sumDuration);
				}
				partitionIndex += frequencyList.get(i);
				//Log.e(TAG, "~partitioned at" + partitionIndex);
				sumDuration = 0;
			}

			// for (int k = 0; k < keys.size() - 1; k++) {
			//
			// // if (!uniqueKey.contains(k)) {
			// // uniqueKey.add(k);
			//
			// // sumDuration += durations.get(k);
			//
			// // durationSums.set(k, durationSums.get(k) +
			// // durations.get(k));
			// // }
			// // else{
			// //
			// // }
			// if (!keys.get(k + 1).equalsIgnoreCase(keys.get(k))) {
			// Log.e(TAG, "\n here\n");
			// for (int l = partitionIndex; l <= k; l++) {
			// sumDuration += durations.get(l);
			// }
			//
			// Log.e(TAG, "sumDur "+sumDuration);
			// for (int l = partitionIndex; l <= k; l++) {
			// durationSums.set(l, sumDuration);
			// }
			// // sumDuration = 0;
			// partitionIndex = k;
			// Log.e(TAG, "partitioned at "+partitionIndex);
			//
			// // for(int l = k+1;l<keys.size();l++){
			// // sumDuration+=durations.get(l);
			// // }
			// // for(int l = k+1; l<keys.size();l++){
			// // durationSums.set(l, sumDuration);
			// // }
			//
			// }
			// }
			// }
			for (int k = 0; k < keys.size(); k++) {

				float tempDurationSum = durationSums.get(k);
				if (tempDurationSum == 0)
					percDuration = 100;
				else
					percDuration = durations.get(k) * 100 / tempDurationSum;

				percentages.add(percDuration + "");

				Log.e(TAG, "P Setting Type: " + keys.get(k)
						+ " :: Setting Val: " + values.get(k) + "\n"
						+ " :: Setting Duration: " + durations.get(k)
						+ " :: Sum Duration " + durationSums.get(k)
						+ " :: Perc. Setting Dur: " + percentages.get(k) + "%%");
				// sumDuration = 0;
				// percDuration = 100;

			}
			
			Log.e(TAG, "Sizes are" + keys.size() 
					+ " :: Val: " + values.size() + "\n"
					+ " :: Duration: " + durations.size()
					+ " :: Perc.: " + percentages.size()  + "%%");

//			HashMap<String, String> inner = new HashMap<String, String>();
//			HashMap<String, HashMap<String, String>> returnOuter = new HashMap<String, HashMap<String, String>>();
//
//			for (int i = 0; i < keys.size(); i++) {
//				for (int j = 0; j < values.size(); j++) {
//					inner.put(j + "", percentages.get(j));
//				}
//				returnOuter.put(i+"", inner);
//			}
			
			ArrayList<MultipleValues> returnItems = new ArrayList<MultipleValues>();
			
			MultipleValues tempVal = new MultipleValues();
			
			for(int k = 0; k < keys.size();k++){
				
				tempVal.key = keys.get(k);
				tempVal.value= values.get(k);
				tempVal.percentage= percentages.get(k);
				
				returnItems.add(tempVal);
				tempVal = new MultipleValues();
			}
			Log.e(TAG, "R Sizes are " + returnItems.size());
			//b1.
			
		
//			HashMap<String, List<String>>  keyValues = new HashMap<String, List<String>>();
//			HashMap<String, List<String>>  keyPercentages = new HashMap<String, List<String>>();
//			//HashMap<List<String>, List<String>> valuePercentage = new  HashMap<List<String>, List<String>>();
//		
//			partitionIndex = 0;
//			ArrayList<String> returnVals = new ArrayList<String>();
//			ArrayList<String> returnPercs = new ArrayList<String>();
			
//			for (int i = 0; i < keys.size(); i++) {
//				returnVals.add("");
//				returnPercs.add("");
//			}
			
//			Log.e(TAG, "flist size: "+frequencyList.size());
//			
//			for (int i = 0; i < frequencyList.size(); i++) {
// 				for (int j = 0; j < frequencyList.get(i); j++) {
// 					
//					returnVals.add( values.get(j+partitionIndex));
//					returnPercs.add( percentages.get(j+partitionIndex));
//					  
//	 				Log.e(TAG, "Addv "+values.get(j+partitionIndex));
//	 				Log.e(TAG, "Addp "+percentages.get(j+partitionIndex));
//				}
// 				
// 				keyValues.put(keys.get(i), returnVals);
// 				keyPercentages.put(keys.get(i), returnPercs); 
// 				 
// 				returnVals = new ArrayList<String>();
// 				returnPercs = new ArrayList<String>(); 
// 				
//				partitionIndex += frequencyList.get(i);
// 
//			}
			
			 
//			for (int k = 0; k < keys.size(); k++) {
//				
//				valuePercentage.put(returnVals, returnPercs);				
//				keyValues.put(keys.get(k), valuePercentage);
//			}
//			
//			for (int k = 0; k < keys.size(); k++) {
//				valuePercentage.put(values.get(k), percentages.get(k));
//			}
			
			 
			output.close();
			db.close();
			Log.e(TAG, ">>mobile time p");

			return returnItems;

		}

		public void clearStuff(SQLiteDatabase db, boolean train) {

			String myTable = "";
			if (train == true) {
				myTable = SETTINGS_TRAINING_TABLE_NAME;
			} else {
				myTable = SETTINGS_TESTING_TABLE_NAME;
			}

			db.delete(myTable, null, new String[] {});
			db.close();
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

}
