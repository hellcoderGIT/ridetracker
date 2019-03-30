package com.adsamcik.signalcollector.map.heatmap

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.adsamcik.signalcollector.database.data.Database2DLocationWeightedMinimal
import com.adsamcik.signalcollector.map.MapFunctions
import com.adsamcik.signalcollector.misc.extension.toByteArray
import kotlin.math.roundToInt

class HeatmapTile(
		val heatmapSize: Int,
		val stamp: HeatmapStamp,
		val x: Int,
		val y: Int,
		zoom: Int,
		maxHeat: Float = 0f,
		dynamicHeat: Boolean = maxHeat <= 0f) {
	val heatmap = Heatmap(heatmapSize, heatmapSize, maxHeat, dynamicHeat)

	val tileCount = MapFunctions.getTileCount(zoom)

	val maxHeat get() = heatmap.maxHeat

	fun addAll(list: List<Database2DLocationWeightedMinimal>) {
		list.forEach { add(it) }
	}

	fun add(location: Database2DLocationWeightedMinimal) {
		val tx = MapFunctions.toTileX(location.longitude, tileCount)
		val ty = MapFunctions.toTileY(location.latitude, tileCount)
		val x = ((tx - x) * heatmapSize).roundToInt()
		val y = ((ty - y) * heatmapSize).roundToInt()
		heatmap.addWeightedPointWithStamp(x, y, location.normalizedWeight.toFloat(), stamp)
	}


	fun toByteArray(bitmapSize: Int): ByteArray {
		val array = heatmap.renderDefaultTo()
		val bitmap = Bitmap.createBitmap(array, heatmapSize, heatmapSize, Bitmap.Config.ARGB_8888)

		return if (heatmapSize != bitmapSize) {
			bitmap.scale(bitmapSize, bitmapSize, false).toByteArray()
		} else
			bitmap.toByteArray()
	}

	companion object {
		const val BASE_HEATMAP_SIZE = 128
	}
}