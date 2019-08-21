package com.adsamcik.tracker.map.layer.legend

import android.graphics.drawable.GradientDrawable
import androidx.appcompat.graphics.drawable.DrawableWrapper
import com.adsamcik.tracker.common.style.marker.StyleableForegroundDrawable

class LegendColorDrawable(val drawable: GradientDrawable) : DrawableWrapper(drawable),
		StyleableForegroundDrawable {
	override fun onForegroundStyleChanged(foregroundColor: Int) {
		drawable.setStroke(4, foregroundColor)
	}
}
