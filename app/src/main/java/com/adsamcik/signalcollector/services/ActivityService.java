package com.adsamcik.signalcollector.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.adsamcik.signalcollector.Assist;
import com.adsamcik.signalcollector.Setting;
import com.adsamcik.signalcollector.classes.DataStore;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityService extends IntentService {
	private static final String TAG = "Signals" + ActivityService.class.getSimpleName();
	private PowerManager powerManager;
	public static int lastActivity;
	public static int lastConfidence;
	public static long lastTime;

	public static final int GOOGLE_API_ID = 77285;
	public static final int REQUIRED_CONFIDENCE = 75;

	public static void requestUpdate(@NonNull GoogleApiClient client, @NonNull final Context context) {
		Intent i = new Intent(context, ActivityService.class);
		PendingIntent activityPendingIntent = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(client, 0, activityPendingIntent);
	}

	public ActivityService() {
		super("ActivityService");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		DataStore.setContext(getApplicationContext());
		powerManager = (PowerManager) getSystemService(POWER_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (ActivityRecognitionResult.hasResult(intent)) {
			ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
			DetectedActivity detectedActivity = result.getMostProbableActivity();

			lastConfidence = detectedActivity.getConfidence();
			lastActivity = detectedActivity.getType();
			lastTime = System.currentTimeMillis();

			if (lastConfidence >= REQUIRED_CONFIDENCE) {
				if (TrackerService.service != null) {
					if (TrackerService.isBackgroundActivated() && canContinueBackgroundTracking(lastActivity))
						stopService(TrackerService.service);
				} else if (canBackgroundTrack(Assist.evaluateActivity(detectedActivity.getType())) && !TrackerService.isAutoLocked() && !powerManager.isPowerSaveMode()) {
					Intent trackerService = new Intent(this, TrackerService.class);
					trackerService.putExtra("approxSize", DataStore.sizeOfData());
					trackerService.putExtra("backTrack", true);
					startService(trackerService);
				}
			}
		} else {
			Log.d(TAG, "Intent had no data returned");
		}
	}

	/**
	 * Checks if background tracking can be activated
	 *
	 * @param evalActivity evaluated activity
	 * @return true if background tracking can be activated
	 */
	private boolean canBackgroundTrack(int evalActivity) {
		if (evalActivity == 3 || evalActivity == 0 || TrackerService.service != null || Setting.getPreferences(this).getBoolean(Setting.STOP_TILL_RECHARGE, false))
			return false;
		int val = Setting.getPreferences(this).getInt(Setting.BACKGROUND_TRACKING, 1);
		return val != 0 && (val == evalActivity || val > evalActivity);
	}

	/**
	 * Checks if background tracking should stop
	 *
	 * @param evalActivity evaluated activity
	 * @return true if background tracking can continue running
	 */
	private boolean canContinueBackgroundTracking(int evalActivity) {
		if (evalActivity == 0)
			return false;
		int val = Setting.getPreferences(this).getInt(Setting.BACKGROUND_TRACKING, 1);
		return val == 2 || (val == 1 && (evalActivity == 1 || evalActivity == 3));
	}
}
