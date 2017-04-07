package com.adsamcik.signalcollector.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.adsamcik.signalcollector.R;
import com.adsamcik.signalcollector.utility.Assist;
import com.adsamcik.signalcollector.utility.BottomSheetMenu;
import com.adsamcik.signalcollector.utility.Compress;
import com.adsamcik.signalcollector.utility.DataStore;
import com.adsamcik.signalcollector.utility.SnackMaker;

import java.io.File;
import java.util.ArrayList;

public class FileSharingActivity extends DetailActivity {
	private static final int SHARE_RESULT = 1;
	private static final String SHAREABLE_DIR_NAME = "shareable";
	File[] files;
	String[] fileNames;
	File shareableDir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		files = getFilesDir().listFiles((dir1, name) -> name.startsWith(DataStore.DATA_FILE));
		if (files.length == 0) {
			TextView tv = new TextView(this);
			tv.setText(R.string.share_nothing_to_share);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			createContentParent(true).addView(tv);
		} else {
			fileNames = new String[20];
			for (int i = 0; i < fileNames.length; i++)
				fileNames[i] = files[0].getName();

			LinearLayout parent = createContentParent(false);
			CoordinatorLayout layout = (CoordinatorLayout) ((ViewGroup) getLayoutInflater().inflate(R.layout.layout_file_share, parent)).getChildAt(parent.getChildCount() - 1);
			ListView listView = (ListView) layout.findViewById(R.id.share_list_view);
			ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, fileNames);
			listView.setAdapter(adapter);
			listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

			View.OnClickListener shareOnClickListener = v -> {
				SparseBooleanArray sba = listView.getCheckedItemPositions();
				ArrayList<String> temp = new ArrayList<>();
				for (int i = 1; i < fileNames.length; i++)
					if (sba.get(i))
						temp.add(fileNames[i]);

				if (temp.size() == 0)
					new SnackMaker(layout).showSnackbar(R.string.share_nothing_to_share, Snackbar.LENGTH_SHORT);
				else {
					String[] arr = new String[temp.size()];
					temp.toArray(arr);
					File c = Compress.zip(files[0].getParent(), arr, "export_" + System.currentTimeMillis());
					File target = new File(c.getParent() + File.separatorChar + SHAREABLE_DIR_NAME + File.separatorChar + c.getName() + ".zip");
					shareableDir = new File(c.getParent() + File.separatorChar + SHAREABLE_DIR_NAME);
					if (shareableDir.exists() || shareableDir.mkdir()) {
						if (c.renameTo(target)) {
							Uri fileUri = FileProvider.getUriForFile(
									FileSharingActivity.this,
									"com.asdamcik.signalcollector.fileprovider",
									target);
							Intent shareIntent = new Intent();
							shareIntent.setAction(Intent.ACTION_SEND);
							shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
							shareIntent.setType("application/zip");
							startActivityForResult(Intent.createChooser(shareIntent, getResources().getText(R.string.export_share_button)), SHARE_RESULT);

						} else
							new SnackMaker(parent).showSnackbar("Failed to rename to " + target.getPath().substring(30));
					} else
						new SnackMaker(parent).showSnackbar("Failed to create dirs");

					target.deleteOnExit();
					shareableDir.deleteOnExit();
				}
			};

			BottomSheetMenu bottomSheetMenu = new BottomSheetMenu(layout);
			bottomSheetMenu.addItem(R.string.export_share_button, shareOnClickListener);

			bottomSheetMenu.addItem(R.string.select_all, (v) -> {
				for (int i = 0; i < fileNames.length; i++)
					listView.setItemChecked(i, true);
			});

			bottomSheetMenu.addItem(R.string.deselect_all, (v) -> {
				for (int i = 0; i < fileNames.length; i++)
					listView.setItemChecked(i, false);
			});
		}

		setTitle(R.string.export_share_button);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SHARE_RESULT)
			DataStore.recursiveDelete(shareableDir);

		finish();
	}
}