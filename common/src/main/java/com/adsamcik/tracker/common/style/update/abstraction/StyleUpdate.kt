package com.adsamcik.tracker.common.style.update.abstraction

import android.content.Context
import com.adsamcik.tracker.common.style.update.data.RequiredColors
import com.adsamcik.tracker.common.style.update.data.StyleConfigData
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal abstract class StyleUpdate {
	abstract val nameRes: Int
	abstract val requiredColorData: RequiredColors

	val colorList: MutableList<Int> = mutableListOf()

	val id: String
		get() = this::class.java.simpleName

	private var configData: StyleConfigData? = null

	protected var isEnabled: Boolean = false

	protected val updateLock = ReentrantLock()

	fun requireConfigData(): StyleConfigData = requireNotNull(configData)

	fun onEnable(context: Context, configData: StyleConfigData) {
		require(colorList.isEmpty())

		updateLock.withLock {
			this.configData = configData

			if (configData.preferenceColorList.isNotEmpty() &&
					configData.preferenceColorList.size == requiredColorData.list.size) {
				colorList.addAll(configData.preferenceColorList)
			} else {
				colorList.addAll(requiredColorData.list.map { it.defaultColor })
			}

			onPostEnable(context, configData)
			isEnabled = true
		}
	}

	fun onDisable(context: Context) {
		onPreDisable(context)

		updateLock.withLock {
			isEnabled = false
			this.configData = null
			colorList.clear()
		}
	}

	protected abstract fun onPostEnable(context: Context, configData: StyleConfigData)
	protected abstract fun onPreDisable(context: Context)
}
