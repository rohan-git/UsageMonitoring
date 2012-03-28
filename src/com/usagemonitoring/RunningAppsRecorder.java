package com.usagemonitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.R.bool;
import android.R.integer;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class RunningAppsRecorder {

	private static final String TAG = "RUN_APPS";
	private static final int MAX_RUNNING = 1000;
	private static final int IMPORTANCE_INCREMENTER = 1;

	private ActivityManager am;
	private ActivityManager.RunningServiceInfo serviceInfo;
	private ActivityManager.RecentTaskInfo recentInfo;

	private List<RunningAppProcessInfo> runningInfoList;
	private List<RunningServiceInfo> runningServiceInfoList;
	private List<RecentTaskInfo> recentTaskInfos;

	RunningAppsDB runningDatabase;

	Context context;

	boolean train = true;

	public RunningAppsRecorder(Context context) {

		this.context = context;
		runningDatabase = new RunningAppsDB(context);
	}

	void destroy() {
		runningDatabase.close();

	}

	void createRecorder() {
		am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);

		serviceInfo = new RunningServiceInfo();
		recentInfo = new RecentTaskInfo();

		runningInfoList = am.getRunningAppProcesses();
		runningServiceInfoList = am.getRunningServices(MAX_RUNNING);
		recentTaskInfos = am.getRecentTasks(MAX_RUNNING,
				ActivityManager.RECENT_WITH_EXCLUDED);
	}

	public void setTrain(boolean train) {
		this.train = train;
	}

	void display() {

		// Log.e(TAG, "INFO");
		// Log.e(TAG, "Running apps");
		// Log.e(TAG, runningInfo.processName + "klkl");

		// for (RunningAppProcessInfo rpi : runningInfoList) {
		// Log.e(TAG, rpi.processName + "\n");
		//
		// }

		pollForTasks();

		// Log.e(TAG, "Running Services");

		// meminfo.
		// runninginfo.
		// serviceinfo.

	}

	void pollForTasks() {

		ComponentName cName;
		String packageName;

		List<String> tmp = new ArrayList<String>();

		int index = 0;
		for (RecentTaskInfo ri : recentTaskInfos) {

			cName = ri.baseIntent.getComponent();

			// Log.e(TAG, "class name :" + cName.getClassName() + "");
			tmp.add(cName.getClassName());
		}

		SQLiteDatabase db = runningDatabase.getWritableDatabase();
		runningDatabase.insertStuff(db, train, tmp);
		// runningDatabase.clearDB(db);
		Log.e(TAG, "   DONE ");
	}

	public void readRunningAppsInformation() {
		SQLiteDatabase db = runningDatabase.getReadableDatabase();
		runningDatabase.readStuff(db, train);
	}

	public HashMap<String, Float> readRunningAppsPercentageInformation() {
		//pollForTasks();
		SQLiteDatabase db = runningDatabase.getReadableDatabase();
		return runningDatabase.getRelativePercentage(db, train);

	}

	public void clearDB() {
		SQLiteDatabase db = runningDatabase.getReadableDatabase();
		runningDatabase.clearDB(db, train);
	}

	static class RunningAppsDB extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "RecentAppsDB";
		private static final String RECENT_APPS_TRAINING_TABLE_NAME = "RecentAppsTrain";
		private static final String RECENT_APPS_TESTING_TABLE_NAME = "RecentAppsTest";
		private static final String RECENT_APPS_NAME = "RECENT_APPS_NAME";
		private static final String RECENT_APPS_IMPORTANCE_NAME = "RECENT_APPS_IMPORTANCE";
		private static final String RECENT_TRAINING_TABLE_CREATE = "CREATE TABLE "
				+ RECENT_APPS_TRAINING_TABLE_NAME
				+ " ("
				+ RECENT_APPS_NAME
				+ " TEXT, " + RECENT_APPS_IMPORTANCE_NAME + " TEXT);";
		private static final String RECENT_TESTING_TABLE_CREATE = "CREATE TABLE "
				+ RECENT_APPS_TESTING_TABLE_NAME
				+ " ("
				+ RECENT_APPS_NAME
				+ " TEXT, " + RECENT_APPS_IMPORTANCE_NAME + " TEXT);";
		private static final String TAG = "R_APPS_DB";

		RunningAppsDB(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(RECENT_TRAINING_TABLE_CREATE);
			db.execSQL(RECENT_TESTING_TABLE_CREATE);

		}

		private void insertStuff(SQLiteDatabase db, boolean train,
				List<String> recentAppsName) {

			Log.e(TAG, ">>insert begin");

			String myTable = "";
			if (train == true) {
				myTable = RECENT_APPS_TRAINING_TABLE_NAME;
			} else {
				myTable = RECENT_APPS_TESTING_TABLE_NAME;
			}

			Cursor output = null;
			String[] emptyWhereArgs = {};
			ContentValues updateValues = new ContentValues();
			ContentValues insertValues = new ContentValues();

			List<String> appNamesInDB = new ArrayList<String>();
			List<Integer> appImportancesInDB = new ArrayList<Integer>();
			// search for recent appNames in db

			output = db.rawQuery("SELECT * FROM " + myTable,
			// + " WHERE " + RECENT_APPS_NAME + " = " + "'" + appName
			// + "'",
					emptyWhereArgs);

			// if values(appNames) already exist in db,
			// increase their importance.
			while (output.moveToNext()) {
				Log.e(TAG,
						"Recent Apps Name: "
								+ output.getString(output
										.getColumnIndex(RECENT_APPS_NAME))
								+ "Recent Apps Importance: "
								+ output.getString(output
										.getColumnIndex(RECENT_APPS_IMPORTANCE_NAME)));

				appNamesInDB.add(output.getString(output
						.getColumnIndex(RECENT_APPS_NAME)));
				appImportancesInDB.add(Integer.parseInt(output.getString(output
						.getColumnIndex(RECENT_APPS_IMPORTANCE_NAME))));

			}

			Log.e(TAG, ">>check if exists");
			int index = 0;
			for (String appName : recentAppsName) {

				if (appNamesInDB.contains(appName)) {

					index = appNamesInDB.indexOf(appName);

					updateValues.put(RECENT_APPS_IMPORTANCE_NAME,
							(IMPORTANCE_INCREMENTER + appImportancesInDB
									.get(index)));
					db.update(myTable, updateValues, RECENT_APPS_NAME + "='"
							+ appName + "'", emptyWhereArgs);

				} else {
					Log.e(TAG, ">>put new values if they dont exist");

					insertValues.put(RECENT_APPS_NAME, appName);
					insertValues.put(RECENT_APPS_IMPORTANCE_NAME,
							IMPORTANCE_INCREMENTER);
					db.insert(myTable, null, insertValues);
					Log.e(TAG, "inserting " + appName + " with importance "
							+ IMPORTANCE_INCREMENTER);

					Log.e(TAG, "<<put new values if they dont exist");

				}

			}
			Log.e(TAG, "<<check if exists");
			output.close();
			Log.e(TAG, "<<insert done");
			db.close();

		}

		private void clearDB(SQLiteDatabase db, boolean train) {

			String myTable = "";
			if (train == true) {
				myTable = RECENT_APPS_TRAINING_TABLE_NAME;
			} else {
				myTable = RECENT_APPS_TESTING_TABLE_NAME;
			}

			db.delete(myTable, null, null);
			Log.e(TAG, "<<table deleted");

		}

		private void readStuff(SQLiteDatabase db, boolean train) {

			Log.e(TAG, "<<begin read");

			String myTable = "";
			if (train == true) {
				myTable = RECENT_APPS_TRAINING_TABLE_NAME;
			} else {
				myTable = RECENT_APPS_TESTING_TABLE_NAME;
			}

			Cursor output = db.rawQuery("SELECT * FROM " + myTable,
					new String[] {});

			while (output.moveToNext()) {
				Log.e(TAG,
						"Recent Apps Name: "
								+ output.getString(output
										.getColumnIndex(RECENT_APPS_NAME))
								+ "Recent Apps Importance: "
								+ output.getString(output
										.getColumnIndex(RECENT_APPS_IMPORTANCE_NAME)));

			}
			output.close();
			db.close();
			Log.e(TAG, ">>READ SUCCess");

		}

		private HashMap<String, Float> getRelativePercentage(SQLiteDatabase db,
				boolean train) {

			Log.e(TAG, "<<begin read");

			String myTable = "";
			if (train == true) {
				myTable = RECENT_APPS_TRAINING_TABLE_NAME;
			} else {
				myTable = RECENT_APPS_TESTING_TABLE_NAME;
			}

			ArrayList<String> apps = new ArrayList<String>();
			ArrayList<Float> importances = new ArrayList<Float>();
			ArrayList<Float> percentages = new ArrayList<Float>();

			Cursor output = db.rawQuery("SELECT * FROM " + myTable,
					new String[] {});

			while (output.moveToNext()) {
				Log.e(TAG,
						"Recent Apps Name: "
								+ output.getString(output
										.getColumnIndex(RECENT_APPS_NAME))
								+ "Recent Apps Importance: "
								+ output.getString(output
										.getColumnIndex(RECENT_APPS_IMPORTANCE_NAME)));

				apps.add(output.getString(output
						.getColumnIndex(RECENT_APPS_NAME)));

				importances.add(Float.parseFloat(output.getString(output
						.getColumnIndex(RECENT_APPS_IMPORTANCE_NAME))));

			}

			int sum = 0;
			for (Float i : importances) {
				sum += i;
			}
			for (Float p : importances) {

				if (sum == 0)
					percentages.add(100F);
				else
					percentages.add(p *100/ sum);

			}

			HashMap<String, Float> returnOuter = new HashMap<String, Float>();

			for (int i = 0; i < apps.size(); i++) {
				returnOuter.put(apps.get(i), percentages.get(i));
				Log.e(TAG, apps.get(i) + "," + percentages.get(i) + "%");
			}

			output.close();
			db.close();
			Log.e(TAG, ">>READ SUCCess");

			return returnOuter;
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

}
