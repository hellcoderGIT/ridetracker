package com.adsamcik.tracker.tracker.component.timer

import android.Manifest
import android.content.Context
import android.os.Looper
import com.adsamcik.tracker.common.debug.Reporter
import com.adsamcik.tracker.common.Time
import com.adsamcik.tracker.common.preference.Preferences
import com.adsamcik.tracker.tracker.R
import com.adsamcik.tracker.tracker.component.TrackerTimerErrorData
import com.adsamcik.tracker.tracker.component.TrackerTimerErrorSeverity
import com.adsamcik.tracker.tracker.component.TrackerTimerReceiver
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

internal class FusedLocationTrackerTimer : LocationTrackerTimer() {
	override val requiredPermissions: Collection<String>
		get() = listOf(Manifest.permission.ACCESS_FINE_LOCATION)

	private val locationCallback: LocationCallback = object : LocationCallback() {
		override fun onLocationResult(result: LocationResult) {
			if (receiver == null) {
				Reporter.report("Received location update with null callback")
				return
			}

			onNewData(result.locations)
		}

		override fun onLocationAvailability(availability: LocationAvailability) {
			if (!availability.isLocationAvailable) {
				val errorData = TrackerTimerErrorData(
						TrackerTimerErrorSeverity.NOTIFY_USER,
						R.string.notification_looking_for_gps
				)
				receiver?.onError(errorData)
			}
		}
	}

	override fun onEnable(context: Context, receiver: TrackerTimerReceiver) {
		super.onEnable(context, receiver)

		val preferences = Preferences.getPref(context)
		val minUpdateDelayInSeconds = preferences.getIntRes(
				R.string.settings_tracking_min_time_key,
				R.integer.settings_tracking_min_time_default
		)
		val minDistanceInMeters = preferences.getIntRes(
				R.string.settings_tracking_min_distance_key,
				R.integer.settings_tracking_min_distance_default
		)

		val client = LocationServices.getFusedLocationProviderClient(context)
		val request = LocationRequest.create().apply {
			interval = minUpdateDelayInSeconds * Time.SECOND_IN_MILLISECONDS
			fastestInterval = minUpdateDelayInSeconds * Time.SECOND_IN_MILLISECONDS
			smallestDisplacement = minDistanceInMeters.toFloat()
			priority = LocationRequest.PRIORITY_HIGH_ACCURACY
		}

		client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
	}

	override fun onDisable(context: Context) {
		super.onDisable(context)
		LocationServices.getFusedLocationProviderClient(context)
				.removeLocationUpdates(locationCallback)
	}
}

