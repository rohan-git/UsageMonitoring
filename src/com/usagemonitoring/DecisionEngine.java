package com.usagemonitoring;

//import net.sourceforge.jFuzzyLogic.FIS;
//import java.beans.beancontext.BeanContext;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.logging.FileHandler;

import net.sf.classifier4J.ClassifierException;
import net.sf.classifier4J.IClassifier;
import net.sf.classifier4J.bayesian.BayesianClassifier;
import net.sf.classifier4J.bayesian.IWordsDataSource;
import net.sf.classifier4J.bayesian.SimpleWordsDataSource;
import net.sf.classifier4J.bayesian.WordsDataSourceException;

import com.usagemonitoring.PhoneSettingsRecorder.PhoneSettingsDB;

//import jtp.*;
//import jtp.ReasoningException;
//import jtp.ReasoningStep;
//import jtp.ReasoningStepIterator;
//import jtp.context.BasicReasoningContext;
//import jtp.disp.DispatcherUtils;
//import jtp.fol.Literal;
//import jtp.fol.Variable;
//import jtp.func.Less;
//import jtp.gmp.ClauseOrientationKB;
//import jtp.modelim.AskingQueryProcessor;
import android.content.Context;
import android.location.Address;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

//import net.sourceforge.jFuzzyLogic.FIS;

public class DecisionEngine {

	private static final String TAG = "DMKR";

	private static final int ALLOWED_LIMIT = 30;
	private static final int ALLOWED_RUNNING_LIMIT = 2;

	private static final float ALLOWED_DISTANCE_LIMIT = 5;

	Context context;

	WifiMobileRecorder wifiMobileRecorder;
	GPSLocationRecorder gpsLocationRecorder;
	PhoneSettingsRecorder phoneSettingsRecorder;
	RunningAppsRecorder runningAppsRecorder;
	ContactsRecorder contactsRecorder;

	List<MultipleValues> phSettingsTrainingData;
	List<MultipleValues> phSettingsTestingData;

	HashMap<String, Float> runningAppsTrainingData;
	HashMap<String, Float> runningAppsTestingData;

	List<LocationsReturns> locationsTrainingData;
	List<LocationsReturns> locationsTestingData;

	StringBuilder warnings;

	public DecisionEngine(Context context) {

		this.context = context;

		wifiMobileRecorder = new WifiMobileRecorder(context);
		gpsLocationRecorder = new GPSLocationRecorder(context);

		gpsLocationRecorder.setMinDist(0);
		gpsLocationRecorder.setMinTime(1000);
		contactsRecorder = new ContactsRecorder(context);
		runningAppsRecorder = new RunningAppsRecorder(context);
		phoneSettingsRecorder = new PhoneSettingsRecorder(context);

		phSettingsTrainingData = new ArrayList<MultipleValues>();
		phSettingsTestingData = new ArrayList<MultipleValues>();

		runningAppsTrainingData = new HashMap<String, Float>();
		runningAppsTestingData = new HashMap<String, Float>();

		locationsTrainingData = new ArrayList<LocationsReturns>();
		locationsTestingData = new ArrayList<LocationsReturns>();
	}

	public void initStuff() {

		phoneSettingsRecorder.setTrainOrTest(false);
		phoneSettingsRecorder.createReceivers();

		runningAppsRecorder.setTrain(false);
		runningAppsRecorder.createRecorder();

		gpsLocationRecorder.setTrain(false);
		gpsLocationRecorder.createListener();
	}

	public void addPhSettingsTrainingData(List<MultipleValues> trainingData) {

		this.phSettingsTrainingData = trainingData;
		Log.e(TAG, "ph training  recd");

	}

	public void addRunningAppsTrainingData(HashMap<String, Float> trainingData) {

		this.runningAppsTrainingData = trainingData;
		Log.e(TAG, "r training  recd");

	}

	public void addGPSTrainingData(
			ArrayList<LocationsReturns> locationsTrainingData) {

		this.locationsTrainingData = locationsTrainingData;
		Log.e(TAG, "g training  recd");

	}

	public void readDatabases() {

		// wifiMobileRecorder.readNetworkInformation();
		// gpsLocationRecorder.readGpsInformation();
		// contactsRecorder.readContactInformation();
		// runningAppsRecorder.readRunningAppsInformation();
		// phoneSettingsRecorder.readSettingsInformation();

	}

	int sendA() {
		return 10;
	}

	public void update(Observable observable, Object data) {

	}

	void gatherSettingsInformation() {

		Log.e(TAG, "<<in gather info");

		this.phSettingsTestingData = phoneSettingsRecorder
				.readPercentageSettingsTimes();

		StringBuilder warnings = new StringBuilder();
		// if(trainingData.size()<=testingData.size()){
		warnings.append("-- WARNINGS --\n");
		float difference;
		int count = 0;

		IWordsDataSource wds = new SimpleWordsDataSource();
		IClassifier classifier = new BayesianClassifier(wds);
		double errors = 0;

		for (MultipleValues trainingValues : phSettingsTrainingData) {

			for (MultipleValues testingValues : phSettingsTestingData) {

				if (trainingValues.key.equalsIgnoreCase(testingValues.key)
						&& trainingValues.value
								.equalsIgnoreCase(testingValues.value)) {

					// Log.e(TAG, "found "+ count++ +" common");
					// Log.e(TAG, "trainKey "+ trainingValues.key);
					// Log.e(TAG, "trainVal "+ trainingValues.value);
					// Log.e(TAG, "testKey  "+ testingValues.key);
					// Log.e(TAG, "testVal  "+ testingValues.value);
					try {
						wds.addMatch(trainingValues.percentage);
						wds.addNonMatch(Math.abs((Float
								.parseFloat(testingValues.percentage) - ALLOWED_LIMIT))
								+ "");

					} catch (WordsDataSourceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					try {
						errors += classifier.classify(testingValues.percentage);
					} catch (ClassifierException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					difference = Math.abs(Float
							.parseFloat(trainingValues.percentage)
							- Float.parseFloat(testingValues.percentage));

					Log.e(TAG, "diff " + difference);
					if (difference > ALLOWED_LIMIT) {

						Log.e(TAG, "Warning");
						warnings.append("For setting: " + trainingValues.key
								+ " : ");
						warnings.append("The value: " + trainingValues.value
								+ " : ");
						warnings.append("is " + difference
								+ "% different from normal usage! \n");
					}

				}

			}
		}
		// }
		Toast.makeText(context, "", Toast.LENGTH_SHORT).show();

		Toast.makeText(
				context,
				"AVerage Settings Error: "
						+ (errors / phSettingsTrainingData.size()),
				Toast.LENGTH_SHORT).show();
		// Toast.makeText(context, warnings, Toast.LENGTH_SHORT).show();
	}

	StringBuilder gatherRunningAppsInfo() {

		// for (int i = 0; i < 10; i++) {
		runningAppsRecorder.pollForTasks();
		// }
		StringBuilder warnings = new StringBuilder();
		// if(trainingData.size()<=testingData.size()){
		warnings.append("-- WARNINGS --\n");

		runningAppsTestingData = this.runningAppsRecorder
				.readRunningAppsPercentageInformation();

		int kFloat = 0;
		IWordsDataSource wds = new SimpleWordsDataSource();
		IClassifier classifier = new BayesianClassifier(wds);
		double errors = 0;

		for (String k : runningAppsTestingData.keySet()) {
			if (!runningAppsTrainingData.containsKey(k)) {
				warnings.append("Possibly unkown program: " + k);
				Log.e(TAG, "Possibly unkown program: " + k);
			} else {

				try {
					wds.addMatch(runningAppsTrainingData.get(k) + "");
					wds.addNonMatch(Math.abs(runningAppsTrainingData.get(k)
							- ALLOWED_RUNNING_LIMIT)
							+ "");

				} catch (WordsDataSourceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				try {
					errors += classifier.classify(runningAppsTestingData.get(k)
							+ "");
				} catch (ClassifierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				float rDifference = runningAppsTestingData.get(k)
						- runningAppsTrainingData.get(k);

				Log.e(TAG, "rDifferecne: " + rDifference);

				if (Math.abs(rDifference) > ALLOWED_RUNNING_LIMIT) {

					if (rDifference >= 0) {
						warnings.append("program: " + k + " used more by "
								+ rDifference + "%\n");
						Log.e(TAG, "program: " + k + " used more by "
								+ rDifference + "%");
					} else {
						warnings.append("program: " + k + " used less\n");
						Log.e(TAG, "program: " + k + " used less");
					}
				}
			}
		}
		Toast.makeText(
				context,
				"AVerage APPS error: "
						+ (errors / runningAppsTrainingData.size()),
				Toast.LENGTH_SHORT).show();
		Toast.makeText(context, "APPS:" + warnings, Toast.LENGTH_SHORT).show();
		Log.e(TAG, ">>in gather info");

		return warnings;

	}

	void printRunningAppsWarnings() {
		// Toast.makeText(context, warnings, Toast.LENGTH_SHORT).show();
	}

	void gatherLocationInfo() {

		Log.e(TAG, "<<in loca info");

		StringBuilder warnings = new StringBuilder();
		warnings.append("-- WARNINGS --\n");

		locationsTestingData = this.gpsLocationRecorder
				.readAverageGpsInformation();
		if (locationsTestingData.get(0).name != null) {
			float distance = 0L;

			IWordsDataSource wds = new SimpleWordsDataSource();
			IClassifier classifier = new BayesianClassifier(wds);
			double errors = 0;

			for (LocationsReturns training : locationsTrainingData) {
				for (LocationsReturns testing : locationsTestingData) {

					// train = (Address)training.name;
					Log.e(TAG, "tr" + training.name);
					Log.e(TAG, "Te" + testing.name);

					if (training.name.contains(testing.name)) {

						try {
							wds.addMatch(training.distance + "");
							wds.addNonMatch(Math.abs(Math.abs(Float
									.parseFloat(training.distance)
									- ALLOWED_DISTANCE_LIMIT)) + "");

						} catch (WordsDataSourceException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						try {
							errors += classifier.classify(training.distance
									+ "");
						} catch (ClassifierException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						distance = Float.parseFloat(training.distance)
								- Float.parseFloat(training.distance);
						Log.e(TAG, "dist: " + distance);

						if (distance >= ALLOWED_DISTANCE_LIMIT) {

							warnings.append("Too far:" + distance);
						}

					}
				}

			}

			Toast.makeText(
					context,
					"AVerage LOC error: "
							+ (errors / (1 + locationsTestingData.size())),
					Toast.LENGTH_SHORT).show();
		}
		Log.e(TAG, ">>in loca info");
	}

	/*
	 * void doFuzz() {
	 * 
	 * // Variable<Integer> v1 = Variable.newVariable("v1", 0, 10);
	 * 
	 * // /new DecisionMaker().writeToFile();
	 * 
	 * // Load from 'FCL' file S // String fileName = "tipper.fcl";
	 * 
	 * }
	 */

	void clearDBS() {

		// runningAppsRecorder.clearDB();
	}

	public void destroy() {

		phoneSettingsRecorder.clearDB();
		phoneSettingsRecorder.destroy();

		runningAppsRecorder.clearDB();
		runningAppsRecorder.destroy();

		gpsLocationRecorder.clearDB();
		gpsLocationRecorder.destroy();

	}

}
