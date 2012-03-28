package com.usagemonitoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class GPSLocationRecorder {

	private static final String TAG = "LOC";

	// represents seconds.
	private static final int LOWER_LIMIT = 5; // 5 seconds.
	private static final int UPPER_LIMIT = 31556926; // seconds in a year
	//

	private long minTime;
	private int minDist;

	private LocationManager locationManager;
	private LocationListener locationListener;
	private LocationDB locationDatabase;

	Location previousLocation;
	Location currentLocation;
	boolean firstTime = true;

	Context context;

	boolean train = true;

	private String initialConnectionType = "";
	private long startTime = 0L;
	private long endTime = 0L;
	private long runningTime = 0L;

	public GPSLocationRecorder(Context context) {

		this.context = context;

		locationDatabase = new LocationDB(context);
		currentLocation = previousLocation = new Location("");
	}

	void destroy() {
		
		locationDatabase.close();
		locationManager.removeUpdates(locationListener);
		locationManager = null;
	}

	public void setTrain(boolean train) {
		this.train = train;
	}

	void createListener() {

		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		locationListener = new LocationListener() {

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}

			public void onLocationChanged(Location location) {

				if (firstTime == true) {
					currentLocation = previousLocation = location;
					firstTime = false;
				}

				endTime = System.currentTimeMillis();
				runningTime = (endTime - startTime) / 1000;

				Geocoder geocoder = new Geocoder(context);
				List<Address> address = new ArrayList<Address>();
				try {
					address = geocoder.getFromLocation(location.getLatitude(),
							location.getLongitude(), 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
				for (Address a : address) {
					Log.e(TAG, "Lcation " + a.toString());
					writeLocationInformation(a.toString(),
							(int) previousLocation.distanceTo(currentLocation),
							(int) runningTime);
//					 Toast.makeText(context, a.getAddressLine(0).toString(),
//					 Toast.LENGTH_SHORT).show();
				}

				startTime = endTime;

//				previousLocation = location;
//				if (previousLocation.distanceTo(currentLocation) > 1L) {
//					// Toast.makeText(context, "too far!..", Toast.LENGTH_SHORT)
//					// .show();
//					currentLocation = previousLocation;
//				}

			}
		};

		// String locationProvider = LocationManager.NETWORK_PROVIDER;

		// Toast.makeText(context, "listnneing..", Toast.LENGTH_SHORT).show();
		String bestProvider = locationManager.getBestProvider(new Criteria(),
				true);
		// Toast.makeText(context, "received provider.." + bestProvider,
		// Toast.LENGTH_SHORT).show();
		locationManager.requestLocationUpdates(bestProvider, getMinTime(),
				getMinDist(), locationListener);

	}

	// void getLastLocation() {
	//
	// String p = locationManager.getBestProvider(new Criteria(), true);
	// Location lastKnownLocation = locationManager.getLastKnownLocation(p);
	// if (lastKnownLocation != null)
	// Toast.makeText(context,
	// "last location is " + lastKnownLocation.toString(),
	// Toast.LENGTH_SHORT).show();
	// }

	public void setMinTime(long minTime) {
		this.minTime = minTime;
	}

	public long getMinTime() {
		return minTime;
	}

	public void setMinDist(int minDist) {
		this.minDist = minDist;
	}

	public int getMinDist() {
		return minDist;
	}

	// Called by the receiver upon receiving broadcast of n/w type change
	private void writeLocationInformation(String locationName,
			int locationDistance, int locationDuration) {

		Log.e(TAG, ">> LOC Write information");

		SQLiteDatabase db = locationDatabase.getWritableDatabase();
		locationDatabase.insertStuff(db, train, locationName, locationDistance,
				locationDuration);

		Log.e(TAG, "<< LOC write information");

	}

	public void readGpsInformation() {
		SQLiteDatabase db = locationDatabase.getReadableDatabase();
		locationDatabase.readStuff(db, train);
	}

	public ArrayList<LocationsReturns> readAverageGpsInformation() {
		
		SQLiteDatabase db = locationDatabase.getReadableDatabase();
		return locationDatabase.getAverageStuff(db, train);
	}

	public void clearDB() {
		SQLiteDatabase db = locationDatabase.getReadableDatabase();
		locationDatabase.clearDB(db, train);
		db.close();
	}

	static class LocationDB extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "LocationDB";
		private static final String LOCATION_TRAIN_TABLE = "locationTrain";
		private static final String LOCATION_TEST_TABLE = "locationTest";
		private static final String LOCATION_NAME_COLUMN = "LOCATION_NAME";
		private static final String LOCATION_DISTANCE_COLUMN = "LOCATION_DISTANCE";
		private static final String LOCATION_DURATION_COLUMN = "LOCATION_DURATION";

		private static final String LOCATION_TRAIN_TABLE_CREATE = "CREATE TABLE "
				+ LOCATION_TRAIN_TABLE
				+ " ("
				+ LOCATION_NAME_COLUMN
				+ " TEXT, "
				+ LOCATION_DISTANCE_COLUMN
				+ " TEXT, "
				+ LOCATION_DURATION_COLUMN + " TEXT " + ");";
		private static final String LOCATION_TEST_TABLE_CREATE = "CREATE TABLE "
				+ LOCATION_TEST_TABLE
				+ " ("
				+ LOCATION_NAME_COLUMN
				+ " TEXT, "
				+ LOCATION_DISTANCE_COLUMN
				+ " TEXT, "
				+ LOCATION_DURATION_COLUMN + " TEXT " + ");";

		private static final String TAG = "LCN_DB";

		LocationDB(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL(LOCATION_TRAIN_TABLE_CREATE);
			db.execSQL(LOCATION_TEST_TABLE_CREATE);

		}

		public void insertStuff(SQLiteDatabase db, boolean train,
				String locationName, int locationDistance, int locationDuration) {

			Log.i(TAG, "<<begin insert");
			String myTable = "";
			if (train == true) {
				myTable = LOCATION_TRAIN_TABLE;
			} else {
				myTable = LOCATION_TEST_TABLE;
			}
			ContentValues cv = new ContentValues();
			cv.put(LOCATION_NAME_COLUMN, locationName);
			cv.put(LOCATION_DISTANCE_COLUMN, locationDistance);
			cv.put(LOCATION_DURATION_COLUMN, locationDuration);

			db.insert(myTable, null, cv);

			Log.i(TAG, ">>INSERT SUCCess");

		}

		public void readStuff(SQLiteDatabase db, boolean train) {

			Log.e(TAG, "<<begin read");

			String myTable = "";
			if (train == true) {
				myTable = LOCATION_TRAIN_TABLE;
			} else {
				myTable = LOCATION_TEST_TABLE;
			}
			Cursor output = db.query(myTable, new String[] {
					LOCATION_NAME_COLUMN, LOCATION_DISTANCE_COLUMN,
					LOCATION_DURATION_COLUMN }, null, null, null, null, null);

			while (output.moveToNext()) {
				Log.e(TAG,
						"Location Name: "
								+ output.getString(output
										.getColumnIndex(LOCATION_NAME_COLUMN))
								+ " : "
								+ "Location Distance: "
								+ output.getString(output
										.getColumnIndex(LOCATION_DISTANCE_COLUMN))
								+ " : "
								+ "Location Duration: "
								+ output.getString(output
										.getColumnIndex(LOCATION_DURATION_COLUMN)));
			}

			output.close();
			db.close();
			Log.e(TAG, ">>READ SUCCess");

		}

		public ArrayList<LocationsReturns> getAverageStuff(SQLiteDatabase db,
				boolean train) {

			Log.e(TAG, "<<begin read");

			String myTable = "";
			if (train == true) {
				myTable = LOCATION_TRAIN_TABLE;
			} else {
				myTable = LOCATION_TEST_TABLE;
			}

			Cursor output = db.rawQuery("select " + LOCATION_NAME_COLUMN
					+ ",avg(" + LOCATION_DISTANCE_COLUMN + "),avg("
					+ LOCATION_DURATION_COLUMN + ") from " + myTable, null);

			ArrayList<String> locNames = new ArrayList<String>();
			ArrayList<String> locDistances = new ArrayList<String>();
			ArrayList<String> locDurations = new ArrayList<String>();

			while (output.moveToNext()) {
				Log.e(TAG,
						"Location Name: "
								+ output.getString(output
										.getColumnIndex(LOCATION_NAME_COLUMN))
								+ " : "
								+ "Location Distance: "
								+ output.getString(output
										.getColumnIndexOrThrow("avg("+LOCATION_DISTANCE_COLUMN+")"))
								+ " : "
								+ "Location Duration: "
								+ output.getString(output
										.getColumnIndexOrThrow("avg("+LOCATION_DURATION_COLUMN+")")));

				locNames.add(output.getString(output
						.getColumnIndex(LOCATION_NAME_COLUMN)));
				locDistances.add(output.getString(output
						.getColumnIndex("avg("+LOCATION_DISTANCE_COLUMN+")")));
				locDurations.add(output.getString(output
						.getColumnIndex("avg("+LOCATION_DURATION_COLUMN+")")));

			}

			ArrayList<LocationsReturns> returnValues = new ArrayList<LocationsReturns>();
			LocationsReturns oneValue;

			for (int i = 0; i < locNames.size(); i++) {

				oneValue = new LocationsReturns();
				oneValue.setName(locNames.get(i));
				oneValue.setDistance(locDistances.get(i));
				oneValue.setDuration(locDurations.get(i));
				Log.e(TAG, "oneValue: "+oneValue);
				returnValues.add(oneValue);
				
			}

			output.close();
			Log.e(TAG, ">>READ SUCCess");

			return returnValues;
		}

		private void clearDB(SQLiteDatabase db, boolean train) {

			String myTable = "";
			if (train == true) {
				myTable = LOCATION_TRAIN_TABLE;
			} else {
				myTable = LOCATION_TEST_TABLE;
			}

			db.delete(myTable, null, null);
			Log.e(TAG, "<<table deleted");

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}

	}

}
