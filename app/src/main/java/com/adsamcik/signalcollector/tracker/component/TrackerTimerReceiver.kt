package com.adsamcik.signalcollector.tracker.component

import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.adsamcik.signalcollector.tracker.data.collection.MutableCollectionTempData

internal interface TrackerTimerReceiver {
	/**
	 * Called when update is triggered.
	 * Executed on main thread so longer running tasks should be run on worker thread.
	 */
	@MainThread
	fun onUpdate(tempData: MutableCollectionTempData)

	/**
	 * Called when error occurs.
	 * Executed on main thread.
	 */
	@MainThread
	fun onError(errorData: TrackerTimerErrorData)
}

internal data class TrackerTimerErrorData(val severity: TrackerTimerErrorSeverity, @StringRes val messageRes: Int, val internalMessage: String = String())

internal enum class TrackerTimerErrorSeverity {
	STOP_SERVICE,
	REPORT,
	NOTIFY_USER,
	WARNING
}