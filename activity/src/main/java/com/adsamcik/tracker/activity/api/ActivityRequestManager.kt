package com.adsamcik.tracker.activity.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.SparseArray
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.core.util.isNotEmpty
import com.adsamcik.tracker.activity.ActivityRequestData
import com.adsamcik.tracker.activity.ActivityTransitionData
import com.adsamcik.tracker.activity.ActivityTransitionRequestData
import com.adsamcik.tracker.activity.logActivity
import com.adsamcik.tracker.activity.service.ActivityService
import com.adsamcik.tracker.common.data.ActivityInfo
import com.adsamcik.tracker.shared.utils.debug.LogData
import com.adsamcik.tracker.shared.utils.debug.Reporter
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import kotlin.reflect.KClass


object ActivityRequestManager {
	private val activeRequestArray = SparseArray<ActivityRequestData>()

	private var minInterval = Integer.MAX_VALUE
	private var transitions: Collection<ActivityTransitionData> = mutableListOf()

	val lastActivity: ActivityInfo get() = ActivityService.lastActivity

	/**
	 * Request activity updates
	 *
	 * @param context    context
	 * @param requestData Request data
	 * @return true if success
	 */
	fun requestActivity(context: Context, requestData: ActivityRequestData): Boolean {
		require(requestData.transitionData != null || requestData.changeData != null)

		logActivity(LogData(message = "new activity request", data = requestData))

		val hash = requestData.key.hashCode()
		activeRequestArray.put(hash, requestData)
		onRequestChange(context)
		return true
	}

	/**
	 * Removes previous activity request
	 */
	fun removeActivityRequest(context: Context, tClass: KClass<*>) {
		val index = activeRequestArray.indexOfKey(tClass.hashCode())
		if (index >= 0) {
			activeRequestArray.removeAt(index)

			logActivity(LogData(message = "removed request for ${tClass.java.name}"))

			if (activeRequestArray.isNotEmpty()) {
				onRequestChange(context)
			}
		} else {
			Reporter.report(Throwable("Trying to remove class that is not subscribed (" + tClass.java.name + ")"))
		}

		if (activeRequestArray.isEmpty()) {
			ActivityService.stopActivityRecognition(context)
		}
	}

	private fun getTransitions(): Collection<ActivityTransitionData> {
		val list = mutableSetOf<ActivityTransitionData>()
		activeRequestArray.forEach { _, value ->
			value.transitionData?.let { transitionData ->
				list.addAll(transitionData.transitionList)
			}
		}
		return list
	}

	private fun onRequestChange(context: Context) {
		val minInterval = getMinInterval()
		val transitions = getTransitions()

		if (minInterval != ActivityRequestManager.minInterval ||
				transitions.size != ActivityRequestManager.transitions.size ||
				!transitions.containsAll(ActivityRequestManager.transitions)) {
			updateActivityService(context, minInterval, transitions)
		}
	}

	private fun updateActivityService(
			context: Context,
			interval: Int,
			transitions: Collection<ActivityTransitionData>
	) {
		minInterval = interval
		ActivityRequestManager.transitions = transitions

		if (hasActivityRecognitionPermission(context)) {
			ActivityService.startActivityRecognition(context, minInterval, transitions)
		}
	}

	private fun getMinInterval(): Int {
		var min = Integer.MAX_VALUE
		activeRequestArray.forEach { _, value ->
			val detectionInterval = value.changeData?.detectionIntervalS ?: return@forEach
			if (detectionInterval < min) min = detectionInterval
		}
		return if (min == Integer.MAX_VALUE) Integer.MIN_VALUE else min
	}

	internal fun onActivityUpdate(context: Context, result: ActivityInfo, elapsedMillis: Long) {
		activeRequestArray.forEach { _, value ->
			value.changeData?.callback?.invoke(context, result, elapsedMillis)
		}
	}

	private fun onActivityTransition(
			context: Context,
			requestData: ActivityTransitionRequestData,
			descendingEvents: List<ActivityTransitionEvent>
	) {
		requestData.transitionList.forEach {
			descendingEvents.forEach { transition ->
				if (transition.transitionType == it.type.value &&
						transition.activityType == it.activity.value) {
					requestData.callback.invoke(context, it, transition.elapsedRealTimeNanos)
					return
				}
			}
		}
	}

	internal fun onActivityTransition(context: Context, result: ActivityTransitionResult) {
		val reversedEvents = result.transitionEvents.reversed()
		activeRequestArray.forEach { _, value ->
			value.transitionData?.let { onActivityTransition(context, it, reversedEvents) }
		}
	}

	fun hasActivityRecognitionPermission(context: Context): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			val permissionState = ContextCompat.checkSelfPermission(
					context,
					Manifest.permission.ACTIVITY_RECOGNITION
			)

			permissionState == PackageManager.PERMISSION_GRANTED
		} else {
			true
		}
	}
}

