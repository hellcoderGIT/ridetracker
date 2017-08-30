package com.adsamcik.signalcollector.file;

import android.content.Context;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.MalformedJsonException;

import com.adsamcik.signalcollector.BuildConfig;
import com.adsamcik.signalcollector.data.RawData;
import com.adsamcik.signalcollector.utility.Constants;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.adsamcik.signalcollector.file.DataStore.PREF_CACHE_FILE_INDEX;
import static com.adsamcik.signalcollector.file.DataStore.PREF_DATA_FILE_INDEX;
import static com.adsamcik.signalcollector.file.DataStore.delete;

public class DataFile {
	public static final int STANDARD = 0;
	public static final int CACHE = 1;

	public static final String SEPARATOR = " ";

	private File file;
	private final String fileNameTemplate;
	private final Gson gson = new Gson();
	private int collectionCount;
	private boolean writeable;

	private boolean empty;

	@FileType
	private int type;

	public DataFile(@NonNull File file, @Nullable String fileNameTemplate, @Nullable String userID, @FileType int type) {
		this.file = file;
		this.fileNameTemplate = fileNameTemplate;
		this.type = userID == null ? CACHE : type;
		if (!file.exists() || file.length() == 0) {
			if (this.type == STANDARD)
				FileStore.saveString(file, "{\"userID\":\"" + userID + "\"," +
						"\"model\":\"" + Build.MODEL +
						"\",\"manufacturer\":\"" + Build.MANUFACTURER +
						"\",\"api\":" + Build.VERSION.SDK_INT +
						",\"version\":" + BuildConfig.VERSION_CODE + "," +
						"\"data\":", false);
			empty = true;
			writeable = true;
			collectionCount = 0;
		} else {
			String ascii = null;
			try {
				ascii = FileStore.loadLastAscii(file, 2);
			} catch (FileNotFoundException e) {
				FirebaseCrash.report(e);
			}

			writeable = ascii == null || !ascii.equals("]}");
			empty = ascii == null || ascii.endsWith(":");
			collectionCount = getCollectionCount(file);
		}
	}

	public static int getCollectionCount(@NonNull File file) {
		String fileName = file.getName();
		int indexOf = fileName.indexOf(SEPARATOR) + SEPARATOR.length();
		if (indexOf > 2)
			return Integer.parseInt(fileName.substring(indexOf));
		else
			return 0;
	}

	public static String getTemplate(@NonNull File file) {
		String fileName = file.getName();
		int indexOf = fileName.indexOf(SEPARATOR);
		if (indexOf > 2)
			return fileName.substring(0, indexOf);
		else
			return fileName;
	}

	private void updateCollectionCount(int collectionCount) {
		this.collectionCount += collectionCount;
		File newFile;
		if(fileNameTemplate != null)
			newFile = new File(file.getParentFile(), fileNameTemplate + SEPARATOR + this.collectionCount);
		else
			newFile = new File(file.getParentFile(), getTemplate(file) + SEPARATOR + this.collectionCount);

		if(!file.renameTo(newFile))
			FirebaseCrash.report(new Throwable("Failed to rename file"));
		else
			file = newFile;
	}

	@IntDef({STANDARD, CACHE})
	@Retention(RetentionPolicy.SOURCE)
	public @interface FileType {
	}

	public boolean addData(@NonNull String jsonArray, int collectionCount) {
		if (jsonArray.charAt(0) != '[')
			throw new IllegalArgumentException("Given string is not json array!");
		if(saveData(jsonArray)) {
			updateCollectionCount(collectionCount);
			return true;
		} else
			return false;
	}

	public boolean addData(@NonNull RawData[] data) {
		if (!writeable) {
			try {
				new FileOutputStream(file, true).getChannel().truncate(file.length() - 2).close();
			} catch (IOException e) {
				FirebaseCrash.report(e);
				return false;
			}
			writeable = true;
		}

		if(saveData(gson.toJson(data))) {
			updateCollectionCount(data.length);
			return true;
		} else
			return false;
	}

	private boolean saveData(@NonNull String jsonArray) {
		try {
			boolean status = FileStore.saveAppendableJsonArray(file, jsonArray, true, empty);
			if (status)
				empty = false;
			return status;
		} catch (MalformedJsonException e) {
			//Should never happen, but w/e
			FirebaseCrash.report(e);
			return false;
		}
	}

	public boolean close() {
		try {
			String last2 = FileStore.loadLastAscii(file, 2);
			assert last2 != null;
			writeable = false;
			return last2.equals("]}") || FileStore.saveString(file, "]}", true);
		} catch (FileNotFoundException e) {
			FirebaseCrash.report(e);
			writeable = true;
			return false;
		}
	}

	public long size() {
		return file.length();
	}

	public boolean isWriteable() {
		return writeable;
	}

	public @FileType
	int getType() {
		return type;
	}

	public String getPreference() {
		switch (type) {
			case CACHE:
				return PREF_CACHE_FILE_INDEX;
			case STANDARD:
				return PREF_DATA_FILE_INDEX;
			default:
				return null;
		}
	}

	public boolean isFull() {
		return file.length() > Constants.MAX_DATA_FILE_SIZE;
	}
}
