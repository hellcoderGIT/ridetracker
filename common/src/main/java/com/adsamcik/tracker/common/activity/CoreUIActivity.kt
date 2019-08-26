package com.adsamcik.tracker.common.activity

import android.os.Bundle
import androidx.annotation.CallSuper
import com.adsamcik.tracker.common.style.StyleController
import com.adsamcik.tracker.common.style.StyleManager

abstract class CoreUIActivity : CoreActivity() {
	protected val styleController: StyleController = StyleManager.createController()

	@CallSuper
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initializeColors()
	}

	@CallSuper
	override fun onDestroy() {
		StyleManager.recycleController(styleController)
		super.onDestroy()
	}

	@CallSuper
	override fun onPause() {
		styleController.isSuspended = true
		super.onPause()
	}

	@CallSuper
	override fun onResume() {
		styleController.isSuspended = false
		super.onResume()
	}

	private fun initializeColors() {
		StyleManager.initializeFromPreferences(this)
	}
}
