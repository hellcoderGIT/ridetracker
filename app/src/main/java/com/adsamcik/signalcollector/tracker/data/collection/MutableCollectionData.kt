package com.adsamcik.signalcollector.tracker.data.collection

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.os.Build
import android.telephony.*
import com.adsamcik.signalcollector.activity.ActivityInfo
import com.crashlytics.android.Crashlytics
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Object containing raw collection data.
 * Data in here might have been reordered to different objects
 * but they have not been modified in any way.
 */
@JsonClass(generateAdapter = true)
data class MutableCollectionData(
		override val time: Long = System.currentTimeMillis(),
		override var location: Location? = null,
		override var activity: ActivityInfo? = null,
		override var cell: CellData? = null,
		override var wifi: WifiData? = null) : CollectionData {


	/**
	 * Sets collection location
	 *
	 * @param location location
	 * @return this
	 */
	fun setLocation(location: android.location.Location): MutableCollectionData {
		this.location = Location(location)
		return this
	}

	/**
	 * Sets wifi and time of wifi collection
	 *
	 * @param data data
	 * @param time time of collection
	 * @return this
	 */
	fun setWifi(location: android.location.Location, time: Long, data: Array<ScanResult>?): MutableCollectionData {
		if (data != null && time > 0) {
			val scannedWifi = data.map { scanResult -> WifiInfo(scanResult) }
			this.wifi = WifiData(Location(location), time, scannedWifi)
		}
		return this
	}

	/**
	 * Sets activity
	 *
	 * @param activity activity
	 * @return this
	 */
	fun setActivity(activity: ActivityInfo): MutableCollectionData {
		this.activity = activity
		return this
	}

	@Suppress("DEPRECATION")
	fun addCell(telephonyManager: TelephonyManager) {
//Annoying lint bug CoarseLocation permission is not required when android.permission.ACCESS_FINE_LOCATION is present
		@SuppressLint("MissingPermission") val cellInfo = telephonyManager.allCellInfo ?: return
		val nOp = telephonyManager.networkOperator
		if (nOp.isNotEmpty()) {
			val mcc = nOp.substring(0, 3)
			val mnc = nOp.substring(3)

			val registeredOperator = RegisteredOperator(mcc, mnc, telephonyManager.networkOperatorName)

			val registeredCells = ArrayList<CellInfo>(if (Build.VERSION.SDK_INT >= 23) telephonyManager.phoneCount else 1)

			for (ci in cellInfo) {
				if (ci.isRegistered) {
					convertToCellInfo(ci, registeredOperator).let {
						if (it != null) registeredCells.add(it)
					}
				}
			}


			this.cell = CellData(registeredCells.toTypedArray(), cellInfo.size)
		}
	}

	private fun convertToCellInfo(cellInfo: android.telephony.CellInfo, registeredOperator: RegisteredOperator): CellInfo? {
		return when (cellInfo) {
			is CellInfoLte ->
				if (registeredOperator.sameNetwork(cellInfo))
					CellInfo.newInstance(cellInfo, registeredOperator.name)
				else
					CellInfo.newInstance(cellInfo, null)
			is CellInfoGsm ->
				if (registeredOperator.sameNetwork(cellInfo))
					CellInfo.newInstance(cellInfo, registeredOperator.name)
				else
					CellInfo.newInstance(cellInfo, null as String?)
			is CellInfoWcdma ->
				if (registeredOperator.sameNetwork(cellInfo))
					CellInfo.newInstance(cellInfo, registeredOperator.name)
				else
					CellInfo.newInstance(cellInfo, null as String?)
			is CellInfoCdma ->
				if (registeredOperator.sameNetwork(cellInfo))
					CellInfo.newInstance(cellInfo, registeredOperator.name)
				else
					CellInfo.newInstance(cellInfo, null as String?)
			else -> {
				Crashlytics.logException(Throwable("UNKNOWN CELL TYPE ${cellInfo.javaClass.simpleName}"))
				null
			}
		}
	}

	private data class RegisteredOperator(val mcc: String, val mnc: String, val name: String) {
		fun sameNetwork(info: CellInfoLte): Boolean {
			val identity = info.cellIdentity
			return if (Build.VERSION.SDK_INT >= 28) identity.mncString == mnc && identity.mccString == mcc
			else {
				@Suppress("deprecation")
				identity.mnc.toString() == mnc && identity.mcc.toString() == mcc
			}
		}

		fun sameNetwork(info: CellInfoCdma): Boolean {
			//todo add cdma network matching (can be done if only 1 cell is registered)
			return false
		}

		fun sameNetwork(info: CellInfoGsm): Boolean {
			val identity = info.cellIdentity
			return if (Build.VERSION.SDK_INT >= 28) identity.mncString == mnc && identity.mccString == mcc
			else {
				@Suppress("deprecation")
				identity.mnc.toString() == mnc && identity.mcc.toString() == mcc
			}
		}

		fun sameNetwork(info: CellInfoWcdma): Boolean {
			val identity = info.cellIdentity
			return if (Build.VERSION.SDK_INT >= 28) identity.mncString == mnc && identity.mccString == mcc
			else {
				@Suppress("deprecation")
				identity.mnc.toString() == mnc && identity.mcc.toString() == mcc
			}
		}
	}
}
