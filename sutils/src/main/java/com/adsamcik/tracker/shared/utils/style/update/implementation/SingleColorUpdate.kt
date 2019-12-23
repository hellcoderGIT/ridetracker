package com.adsamcik.tracker.shared.utils.style.update.implementation

import android.content.Context
import com.adsamcik.tracker.shared.base.R
import com.adsamcik.tracker.shared.utils.style.update.abstraction.StyleUpdate
import com.adsamcik.tracker.shared.utils.style.update.data.RequiredColorData
import com.adsamcik.tracker.shared.utils.style.update.data.RequiredColors
import com.adsamcik.tracker.shared.utils.style.update.data.StyleConfigData

internal class SingleColorUpdate : StyleUpdate() {
	override val nameRes: Int
		get() = R.string.settings_color_update_single_title

	override val requiredColorData: RequiredColors
		get() = RequiredColors(
				listOf(
						RequiredColorData(
								defaultColor = -16619100,
								nameRes = R.string.settings_color_static_title
						)
				)
		)

	override fun onPostEnable(context: Context, configData: StyleConfigData) {
		configData.callback(colorList.first())
	}

	override fun onPreDisable(context: Context) = Unit
}
