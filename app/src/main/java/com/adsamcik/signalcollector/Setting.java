package com.adsamcik.signalcollector;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import java.io.Serializable;

public class Setting implements Serializable {
	public static final int UPLOAD_JOB = 513;

	public static final String SCHEDULED_UPLOAD = "uploadSCHEDULED";
	public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
	public static final String REGISTERED_USER = "playUserRegistered";
	//0-no tracking; 1-onFoot tracking; 2-onFoot and vehicle tracking
	public static final String BACKGROUND_TRACKING = "backgroundTracking";
	//0-no auto upload;1-wifi autoUpload;2-autoUpload
	public static final String AUTO_UPLOAD = "autoUpload";
	public static final String HAS_BEEN_LAUNCHED = "hasBeenLaunched";
	public static final String STOP_TILL_RECHARGE = "stoppedTillRecharge";
	private static SharedPreferences sharedPreferences;

	/**
	 * Initialize shared preferences. It's usually good to call it.
	 * @param c context
	 */
	public static void initializeSharedPreferences(@NonNull Context c) {
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
	}

	/**
	 * Will stop tracking until phone is connected to charger
	 * @param c context
	 */
	public static void stopTillRecharge(@NonNull Context c) {
		getPreferences(c).edit().putBoolean(STOP_TILL_RECHARGE, true).apply();
	}

	/**
	 * Get shared preferences
	 * This function should never crash. Initializes preferences if needed.
	 *
	 * @param c Non-null context
	 * @return  Shared preferences
	 */
	public static SharedPreferences getPreferences(@NonNull Context c) {
		if(sharedPreferences == null)
			sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c);
		return sharedPreferences;
	}

	/**
	 * Get shared preferences
	 * This function will crash if shared preferences were never initialized
	 * Always prefer to send context if posssible.
	 *
	 * @return  Shared preferences
	 */
	public static SharedPreferences getPreferences() {
		if(sharedPreferences == null)
			throw new RuntimeException("Shared preferences are null and no context was provided");
		return sharedPreferences;
	}

}
