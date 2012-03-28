package com.usagemonitoring;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class LocationsReturns implements Serializable, Parcelable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String name;
	String distance;
	String duration;

	public LocationsReturns() {
		name = distance = duration = "";
	}

	public LocationsReturns(String name, String distance, String duration) {

		this.name = name;
		this.distance = distance;
		this.duration = duration;
	}

	public LocationsReturns(Parcel in) {
		// TODO Auto-generated constructor stub

		name = in.readString();
		distance = in.readString();
		duration = in.readString();
	}

	public String getName() {
		return name;
	}

	public String getDistance() {
		return distance;
	}

	public String getDuration() {
		return duration;
	}

	public void setDistance(String distance) {
		this.distance = distance;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {

		arg0.writeString(name);
		arg0.writeString(distance);
		arg0.writeString(duration);

	}

	public static final Parcelable.Creator<LocationsReturns> CREATOR = new Parcelable.Creator<LocationsReturns>() {
		public LocationsReturns createFromParcel(Parcel in) {
			return new LocationsReturns(in);
		}

		public LocationsReturns[] newArray(int size) {
			return new LocationsReturns[size];
		}
	};

}
