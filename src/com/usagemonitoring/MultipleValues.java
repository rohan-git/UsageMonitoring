package com.usagemonitoring;

import java.io.ObjectInputStream.GetField;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class MultipleValues implements Serializable, Parcelable {

	// ArrayList<String> arrayOne;
	// ArrayList<String> arrayTwo;

	// HashMap<String, String> hashOne;
	// HashMap<String, String> hashTwo;

	// HashMap<String,HashMap<List<String>,List<String>>> theMap;

	// HashMap<String, List<String>> keyValues;
	// HashMap<String, List<String>> keyPercentages;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String key;
	String value;
	String percentage;

	// ArrayList<String> arrayFour;

	public MultipleValues(String key, String value, String percentage) {

		this.key = key;
		this.value = value;
		this.percentage = percentage;
	}

	public MultipleValues() {
		key = value = percentage = "";

	}

	public MultipleValues(Parcel in) {
		// TODO Auto-generated constructor stub
		key = in.readString();
		value = in.readString();
		percentage = in.readString();

	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public String getPercentage() {
		return percentage;
	}

	@Override
	public int describeContents() {
 
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {

		arg0.writeString(key);
		arg0.writeString(value);
		arg0.writeString(percentage);

	}

	public static final Parcelable.Creator<MultipleValues> CREATOR = new Parcelable.Creator<MultipleValues>() {
		public MultipleValues createFromParcel(Parcel in) {
			return new MultipleValues(in);
		}

		public MultipleValues[] newArray(int size) {
			return new MultipleValues[size];
		}
	};

	// public HashMap<String, List<String>> getKeyPercentages() {
	// return keyPercentages;
	// }
	//
	// public HashMap<String, List<String>> getKeyValues() {
	// return keyValues;
	// }

	// public HashMap<String, HashMap<List<String>, List<String>>> getTheMap() {
	// return theMap;
	// }
	//
	// public HashMap<String, String> getHashOne() {
	// return hashOne;
	// }
	//
	// public HashMap<String, String> getHashTwo() {
	// return hashTwo;
	// }

}
