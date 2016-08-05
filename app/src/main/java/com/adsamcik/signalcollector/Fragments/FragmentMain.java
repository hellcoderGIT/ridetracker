package com.adsamcik.signalcollector.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.adsamcik.signalcollector.classes.DataStore;
import com.adsamcik.signalcollector.Extensions;
import com.adsamcik.signalcollector.classes.Network;
import com.adsamcik.signalcollector.R;
import com.adsamcik.signalcollector.Setting;
import com.adsamcik.signalcollector.classes.Success;
import com.adsamcik.signalcollector.data.CellData;
import com.adsamcik.signalcollector.data.Data;
import com.adsamcik.signalcollector.interfaces.ICallback;
import com.adsamcik.signalcollector.interfaces.ITabFragment;
import com.adsamcik.signalcollector.services.TrackerService;
import com.google.firebase.crash.FirebaseCrash;

public class FragmentMain extends Fragment implements ITabFragment {
	private RelativeLayout layoutCell, layoutWifi, layoutMain, layoutOther;
	private TextView textTime, textPosition, textAccuracy, textWifiCount, textWifiTime, textCurrentCell, textCellCount, textPressure, textActivity, textCollected;

	private FloatingActionButton fabTrack, fabUp;

	private long lastWifiTime = 0;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_main, container, false);
		final String activity_name = "MainActivity";
		if (!getActivity().getClass().getSimpleName().equals(activity_name))
			throw new RuntimeException("Main fragment is attached to different activity than " + activity_name);

		textAccuracy = (TextView) view.findViewById(R.id.textAccuracy);
		textPosition = (TextView) view.findViewById(R.id.textPosition);
		textCellCount = (TextView) view.findViewById(R.id.textCells);
		textCurrentCell = (TextView) view.findViewById(R.id.textCurrentCell);
		textWifiCount = (TextView) view.findViewById(R.id.textWifiCount);
		textWifiTime = (TextView) view.findViewById(R.id.textWifiTime);
		textTime = (TextView) view.findViewById(R.id.textTime);
		textPressure = (TextView) view.findViewById(R.id.textPressure);
		textActivity = (TextView) view.findViewById(R.id.textActivity);
		textCollected = (TextView) view.findViewById(R.id.textCollected);

		layoutMain = (RelativeLayout) view.findViewById(R.id.layout_main);
		layoutWifi = (RelativeLayout) view.findViewById(R.id.layout_wifi);
		layoutCell = (RelativeLayout) view.findViewById(R.id.layout_cells);
		layoutOther = (RelativeLayout) view.findViewById(R.id.layout_other);

		layoutWifi.setVisibility(View.GONE);
		layoutCell.setVisibility(View.GONE);
		layoutOther.setVisibility(View.GONE);

		long dataSize = DataStore.sizeOfData();
		setCollected(dataSize);

		return view;
	}

	private void setCollected(long collected) {
		if (Network.cloudStatus == 0 && collected > 0)
			setCloudStatus(1);
		else if (collected == 0)
			setCloudStatus(0);
		if (textCollected != null && getResources() != null)
			textCollected.setText(String.format(getResources().getString(R.string.main_collected), Extensions.humanReadableByteCount(collected)));
	}

	/**
	 * 0 - start tracking icon
	 * 1 - stop tracking icon
	 * 2 - saving icon
	 */
	private void changeTrackerButton(int status) {
		switch (status) {
			case 0:
				fabTrack.setImageResource(R.drawable.ic_play_arrow_24dp);
				break;
			case 1:
				fabTrack.setImageResource(R.drawable.ic_pause_24dp);
				break;
			case 2:
				fabTrack.setImageResource(R.drawable.ic_loop_24dp);
				break;
		}
	}

	/**
	 * Enables or disables collecting service
	 *
	 * @param enable ensures intended action
	 */
	private void toggleCollecting(Activity activity, boolean enable) {
		if (TrackerService.service != null == enable)
			return;

		String[] requiredPermissions = Extensions.checkTrackingPermissions(activity);

		if (requiredPermissions == null) {
			if (TrackerService.service == null) {
				Setting.getPreferences(activity).edit().putBoolean(Setting.STOP_TILL_RECHARGE, false).apply();
				Intent trackerService = new Intent(activity, TrackerService.class);
				trackerService.putExtra("approxSize", DataStore.sizeOfData());
				activity.startService(trackerService);
			} else {
				if (TrackerService.service == null)
					FirebaseCrash.report(new Exception("Tracker service is null"));
				activity.stopService(TrackerService.service);
			}

		} else if (Build.VERSION.SDK_INT >= 23) {
			activity.requestPermissions(requiredPermissions, 0);
		}
	}


	/**
	 * 0 - No cloud sync required
	 * 1 - Data available for sync
	 * 2 - Syncing data
	 * 3 - Cloud error
	 */
	private void setCloudStatus(int status) {
		if (fabUp == null)
			return;
		else if (status < 0 || status > 3)
			throw new RuntimeException("Status is out of range");

		switch (status) {
			case 0:
				fabUp.hide();
				fabUp.setOnClickListener(null);
				break;
			case 1:
				fabUp.setImageResource(R.drawable.ic_file_upload_24dp);
				fabUp.setOnClickListener(
						v -> {
							setCloudStatus(2);
							DataStore.requestUpload(getContext(), false);
						}
				);
				fabUp.show();
				break;
			case 2:
				fabUp.setImageResource(R.drawable.ic_cloud_upload_24dp);
				fabUp.setOnClickListener(null);
				fabUp.show();
				break;
			case 3:
				fabUp.setImageResource(R.drawable.ic_cloud_off_24dp);
				fabUp.setOnClickListener(null);
				break;
		}

		Network.cloudStatus = status;
	}

	@Override
	public Success onEnter(final Activity activity, final FloatingActionButton fabOne, final FloatingActionButton fabTwo) {
		fabTrack = fabOne;
		fabUp = fabTwo;

		fabTrack.show();

		changeTrackerButton(TrackerService.service != null ? 1 : 0);
		fabTrack.setOnClickListener(
				v -> {
					if (TrackerService.service != null)
						TrackerService.setAutoLock();
					toggleCollecting(activity, TrackerService.service == null);
				}
		);
		DataStore.setOnDataChanged(() -> activity.runOnUiThread(() -> setCollected(TrackerService.approxSize)));

		DataStore.setOnUpload(() -> activity.runOnUiThread(() -> setCollected(DataStore.sizeOfData())));

		TrackerService.onNewDataFound = () -> activity.runOnUiThread(this::UpdateData);

		TrackerService.onServiceStateChange = () -> activity.runOnUiThread(() -> changeTrackerButton(TrackerService.service != null ? 1 : 0));

		long dataSize = DataStore.sizeOfData();
		setCollected(dataSize);
		setCloudStatus(dataSize == 0 ? 0 : 1);
		return new Success();
	}

	@Override
	public void onLeave() {
		DataStore.setOnDataChanged(null);
		DataStore.setOnUpload(null);
		TrackerService.onNewDataFound = null;
		TrackerService.onServiceStateChange = null;
	}

	private void UpdateData() {
		Context c = getContext();
		if (c == null)
			return;
		Resources res = c.getResources();
		Data d = TrackerService.dataEcho;
		setCollected(TrackerService.approxSize);

		if (d != null) {
			textTime.setText(String.format(res.getString(R.string.main_last_update), DateFormat.format("HH:mm:ss", d.time)));

			if (d.wifi != null) {
				textWifiCount.setText(String.format(res.getString(R.string.main_wifi_count), d.wifi.length));
				layoutWifi.setVisibility(View.VISIBLE);
				textWifiTime.setText(String.format(res.getString(R.string.main_wifi_time), d.time - d.wifiTime, TrackerService.distanceToWifi));
				lastWifiTime = d.time;
			} else if (lastWifiTime - d.time > 10000)
				layoutWifi.setVisibility(View.GONE);

			if (d.cell != null) {
				CellData active = d.getActiveCell();
				if (active != null)
					textCurrentCell.setText(String.format(res.getString(R.string.main_cell_current), active.getType(), active.dbm, active.asu));
				else
					textCurrentCell.setText("");
				textCellCount.setText(String.format(res.getString(R.string.main_cell_count), d.cell.length));
				layoutCell.setVisibility(View.VISIBLE);
			} else
				layoutCell.setVisibility(View.GONE);


			textAccuracy.setText(String.format(res.getString(R.string.main_accuracy), (int) d.accuracy));

			textPosition.setText(String.format(res.getString(R.string.main_position),
					Extensions.coordsToString(d.latitude),
					Extensions.coordsToString(d.longitude),
					(int) d.altitude));

			if (d.pressure > 0) {
				textPressure.setText(String.format(res.getString(R.string.main_pressure), d.pressure));
				layoutOther.setVisibility(View.VISIBLE);
			} else
				layoutOther.setVisibility(View.GONE);

			textActivity.setText(String.format(res.getString(R.string.main_activity), Extensions.getActivityName(d.activity)));
		}
	}

}
