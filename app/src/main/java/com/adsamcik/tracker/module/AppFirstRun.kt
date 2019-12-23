package com.adsamcik.tracker.module

import android.content.Context
import com.adsamcik.tracker.R
import com.adsamcik.tracker.shared.utils.module.FirstRun
import com.adsamcik.tracker.shared.utils.module.OnDoneListener
import com.adsamcik.tracker.common.preferences.Preferences

class AppFirstRun : FirstRun() {
	override fun onFirstRun(context: Context, onDoneListener: OnDoneListener) {
		createDialog(context) {
			title(res = R.string.first_run_app_title)
			message(res = R.string.first_run_app_description)
			positiveButton { errorReporting(context, onDoneListener) }
			negativeButton(res = R.string.skip_introduction) {
				onDoneListener.invoke(context, true)
			}
		}
	}

	private fun setReportingPreference(context: Context, value: Boolean) {
		com.adsamcik.tracker.common.preferences.Preferences.getPref(context).edit {
			setBoolean(
					com.adsamcik.tracker.common.R.string.settings_error_reporting_key,
					value
			)
		}
	}

	private fun errorReporting(context: Context, onDoneListener: OnDoneListener) {
		createDialog(context) {
			title(res = R.string.first_run_error_reporting_title)
			message(res = R.string.first_run_error_reporting_description) {}
			positiveButton(res = com.adsamcik.tracker.common.R.string.yes) {
				setReportingPreference(it.context, true)
			}
			negativeButton(res = com.adsamcik.tracker.common.R.string.no) {
				setReportingPreference(it.context, false)
			}

			onDismiss { onDoneListener(context, false) }
		}
	}
}
