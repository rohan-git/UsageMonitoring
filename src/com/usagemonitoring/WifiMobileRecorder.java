package com.usagemonitoring;

import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Records changes in network type, <br> 
 * <b>Wifi</b> or <b>Mobile(Edge/3g/4g)</b>
 * 
 * @author Rohan
 *
 */
public class WifiMobileRecorder {

	private static final String TAG = "WIFI";

	// represents seconds.
	//private static final int LOWER_LIMIT = 2; // 5 seconds.
	private static final int UPPER_LIMIT = 31556926; // seconds in a year
	//

	private BroadcastReceiver networkReceiver;
	private WifiDB wifidatabase;

	Context context;

	private String initialConnectionType = "DEFAULT";
	private long startTime = 0L;
	private long endTime = 0L;
	private long runningTime = 0L;

	public WifiMobileRecorder(Context context) {

		this.context = context;
		wifidatabase = new WifiDB(context);

	}

	void destroy() {
		context.unregisterReceiver(networkReceiver);
		networkReceiver = null;
		wifidatabase.close();
	}

	void createReceiver() {

		getInitialNetworkInformation();
		networkReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// Log.e("IN RE", "ENTER");
				writeNetworkInformation();
				// Log.e("IN RE", "EXIT");

			}
		};
		context.registerReceiver(networkReceiver, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private void getInitialNetworkInformation() {

		ConnectivityManager ci = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo ni = ci.getActiveNetworkInfo();

		// set it to whatever type of connection is seen 1st.
		if (ni != null && initialConnectionType.equalsIgnoreCase("DEFAULT")) {
			initialConnectionType = ni.getTypeName();
			Log.e(TAG, initialConnectionType + " <><>");
			startTime = System.currentTimeMillis();
		}

	}

	// Called by the receiver upon receiving broadcast of n/w type change
	private void writeNetworkInformation() {

		Log.e(TAG, ">> nw information");

		ConnectivityManager ci = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo ni = ci.getActiveNetworkInfo();
		StringBuilder niStuff = new StringBuilder();

		if (ni != null) {

			String newConnectionType = ni.getTypeName();
			Log.e(TAG, initialConnectionType + " ~~~b4write");
			Log.e(TAG, newConnectionType + " ~~~b4write");

			if (!newConnectionType.equalsIgnoreCase(initialConnectionType)) {
				Log.e(TAG, "here");

				endTime = System.currentTimeMillis();
				runningTime = (endTime - startTime) / 1000;
				startTime = endTime;

				// clean values, ignore v small or v large ones
				if (runningTime < UPPER_LIMIT){
					//	&& initialConnectionType != "") {
					// GET DATABASE
					// startTime = endTime;
					SQLiteDatabase db = wifidatabase.getWritableDatabase();
					wifidatabase.insertStuff(db, initialConnectionType,
							(int) runningTime);
					//
					niStuff.append("connection: " + initialConnectionType
							+ "\n");
					niStuff.append("active for: " + runningTime + " sec");
					Log.e(TAG, initialConnectionType + " <after><write>");
					Log.e(TAG, newConnectionType + " <afer><write>");
					initialConnectionType = newConnectionType;
					Log.e(TAG, initialConnectionType + " <after><interchange>");
					Log.e(TAG, newConnectionType + " <afer><interchange>");

				}

			}

		}
		if (niStuff.toString() != "")
			Toast.makeText(context, niStuff.toString(), Toast.LENGTH_SHORT)
					.show();

		Log.e(TAG, "<< got nw information");

	}

	public void readNetworkInformation() {
		SQLiteDatabase db = wifidatabase.getReadableDatabase();
		wifidatabase.readStuff(db);
		wifidatabase.close();
	}

	public void readAverageTime() {
		SQLiteDatabase db = wifidatabase.getReadableDatabase();
		wifidatabase.getAverageMobileTime(db);
		wifidatabase.close();
	}

	public void clearDB() {
		SQLiteDatabase db = wifidatabase.getWritableDatabase();
		wifidatabase.clearDB(db);
		wifidatabase.close();
	}

	static class WifiDB extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "WifiDB";
		private static final String CONNECTION_NAMES_TABLE = "CONNECTION_NAMES";
		private static final String CONNECTION_TYPE_COLUMN = "CONNECTION_TYPE";
		private static final String CONNECTION_DURATION_COLUMN = "CONNECTION_DURATION";
		private static final String CONNECTION_TABLE_CREATE = "CREATE TABLE "
				+ CONNECTION_NAMES_TABLE + " (" + CONNECTION_TYPE_COLUMN
				+ " TEXT, " + CONNECTION_DURATION_COLUMN + " TEXT);";

		private static final String TAG = "WIFI_DB";

		WifiDB(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(CONNECTION_TABLE_CREATE);

		}

		public void insertStuff(SQLiteDatabase db, String connectionType,
				int connectionDuration) {

			Log.i(TAG, "<<begin insert");
			ContentValues cv = new ContentValues();
			cv.put(CONNECTION_TYPE_COLUMN, connectionType);
			cv.put(CONNECTION_DURATION_COLUMN, connectionDuration);

			db.insert(CONNECTION_NAMES_TABLE, null, cv);

			Log.i(TAG, "<<INSERT SUCCess");
			db.close();

		}

		public void readStuff(SQLiteDatabase db) {

			Log.e(TAG, "<<begin read");
			Cursor output = db.query(CONNECTION_NAMES_TABLE, new String[] {
					CONNECTION_TYPE_COLUMN, CONNECTION_DURATION_COLUMN }, null,
					null, null, null, null);

			while (output.moveToNext()) {
				Log.e(TAG,
						"Connection Type: "
								+ output.getString(output
										.getColumnIndex(CONNECTION_TYPE_COLUMN))
								+ " : "
								+ "Connection Duration: "
								+ output.getString(output
										.getColumnIndex(CONNECTION_DURATION_COLUMN)));
			}

			output.close();
			db.close();
			Log.e(TAG, ">>READ SUCCess");

		}

		public HashMap<String, String> getAverageMobileTime(SQLiteDatabase db) {

			Log.e(TAG, "<<mobile time");
			Cursor output = db.rawQuery("Select " + CONNECTION_TYPE_COLUMN
					+ ", avg(" + CONNECTION_DURATION_COLUMN + ") from "
					+ CONNECTION_NAMES_TABLE + "" + " Group BY "
					+ CONNECTION_TYPE_COLUMN 
					, null);

			// CONNECTION_NAMES_TABLE, new String[] {
			// CONNECTION_TYPE_COLUMN, CONNECTION_DURATION_COLUMN }, null,
			// null, null, null, null);

			int columnIndex = 0;
			HashMap<String, String> temp = new HashMap<String, String>();

			// Log.e(TAG, output.getColumnCount()+":  -- no. cols");
			// Log.e(TAG, output.getColumnName(0)+":  -- 0 cols");
			// Log.e(TAG, output.getColumnName(1)+":  -- 1 cols");
			//
			while (output.moveToNext()) {

				String key = output.getString(output
						.getColumnIndexOrThrow(CONNECTION_TYPE_COLUMN));
				String value = output.getString(output
						.getColumnIndexOrThrow("avg("
								+ CONNECTION_DURATION_COLUMN + ")"));
				temp.put(key, value);
				Log.e(TAG, "Connection Type: " + key
						+ " :: Avg Connection Time: " + value);
				columnIndex++;

			}

			output.close();
			db.close();
			Log.e(TAG, ">>mobile time");

			return temp;
		}

		public void clearDB(SQLiteDatabase db) {
			db.delete(CONNECTION_NAMES_TABLE, null, new String[] {});
			db.close();

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

}
