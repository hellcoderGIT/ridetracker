package com.adsamcik.signalcollector.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import com.adsamcik.signalcollector.services.TrackerService;
import com.adsamcik.signalcollector.utility.NotYetImplementedException;
import com.adsamcik.signalcollector.utility.Shortcuts;
import com.adsamcik.signalcollector.utility.Shortcuts.ShortcutType;
import com.google.firebase.crash.FirebaseCrash;

public class ShortcutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if(intent.getAction().equals(Shortcuts.ACTION)) {
			int value = intent.getIntExtra(Shortcuts.ACTION_STRING, -1);
			if (value >= 0 && value < ShortcutType.values().length) {
				ShortcutType type = ShortcutType.values()[value];
				Intent serviceIntent = new Intent(this, TrackerService.class);

				switch (type) {
					case START_COLLECTION:
						serviceIntent.putExtra("backTrack", false);
						startService(serviceIntent);
						break;
					case STOP_COLLECTION:
						if (TrackerService.isRunning())
							stopService(serviceIntent);
						break;
					default:
						throw new NotYetImplementedException(type.name() + " is not implemented.");
				}
			} else {
				FirebaseCrash.report(new Throwable("Invalid value " + value));
			}
		}
		finishAffinity();
	}
}