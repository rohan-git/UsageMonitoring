package com.usagemonitoring;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.widget.Toast;

public class ContactsRecorder {

	private static final String TAG = "CNT_AC";

	// private HelperDB wifidatabase;

	Context context;
	ContentObserver contentObserver;

	private String initialConnectionType = "";
	private long startTime = 0L;
	private long endTime = 0L;
	private long runningTime = 0L;

	public ContactsRecorder(Context context) {

		this.context = context;

	}

	void destroy() {

		 context.getContentResolver().unregisterContentObserver(contentObserver);
	}

	void createReceiver() {

		contentObserver = new ContentObserver(null) {
			@Override
			public void onChange(boolean selfChange) {

				super.onChange(selfChange);
				writeContactInformation();
			}
		};

		context.getContentResolver().registerContentObserver(
				Contacts.CONTENT_URI, false, contentObserver);

	}

	private void writeContactInformation() {
		Log.e(TAG, "TODO write contact information to db");
	}

	// Using old- CONTENT_URI
	// The new- CONTENT_LOOKUP_URI doesnt seem to work ..
	public void readContactInformation() {

		Log.e(TAG, ">> contact information");
		Log.e(TAG, "<<begin read");
		Cursor output = context.getContentResolver().query(
				Contacts.CONTENT_URI,
				new String[] { Contacts._ID, Contacts.DISPLAY_NAME }, null,
				null, null);

		while (output.moveToNext()) {
			Log.e(TAG,
					"Conntact ID: "
							+ output.getString(output
									.getColumnIndex(Contacts._ID))
							+ " : "
							+ "Contact Name: "
							+ output.getString(output
									.getColumnIndex(Contacts.DISPLAY_NAME)));
		}

		output.close();
		Log.e(TAG, ">>READ SUCCess");
		// Toast.makeText(context, c.toString(), Toast.LENGTH_SHORT).show();
		Log.e(TAG, "<< got contact information");

	}

	static class HelperDB extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "ContactsDB";
		private static final String CONTACTS_TABLE_NAME = "contacts";
		private static final String NUM_CONTACTS_COLUMN = "NUM_CONTACTS";
		private static final String CONTACTS_TABLE_CREATE = "CREATE TABLE "
				+ CONTACTS_TABLE_NAME + " (" + NUM_CONTACTS_COLUMN + " TEXT);";

		private static final String TAG = "CNTC_DB";

		HelperDB(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(CONTACTS_TABLE_CREATE);

		}

		public void insertStuff(SQLiteDatabase db, String connectionType,
				int numContacts) {

			Log.i(TAG, "<<begin insert");
			ContentValues cv = new ContentValues();
			cv.put(NUM_CONTACTS_COLUMN, numContacts);

			db.insert(CONTACTS_TABLE_NAME, null, cv);

			Log.i(TAG, "<<INSERT SUCCess");
			db.close();

		}

		public void readStuff(SQLiteDatabase db) {

			Log.e(TAG, "<<begin read");
			Cursor output = db.query(CONTACTS_TABLE_NAME,
					new String[] { NUM_CONTACTS_COLUMN }, null, null, null,
					null, null);

			while (output.moveToNext()) {
				Log.e(TAG,
						"Num Contacts: "
								+ output.getString(output
										.getColumnIndex(NUM_CONTACTS_COLUMN)));
			}

			output.close();
			Log.e(TAG, ">>READ SUCCess");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

}
