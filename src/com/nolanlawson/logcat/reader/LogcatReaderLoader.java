package com.nolanlawson.logcat.reader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.nolanlawson.logcat.helper.LogcatHelper;
import com.nolanlawson.logcat.helper.PreferenceHelper;

public class LogcatReaderLoader implements Parcelable {

	private Map<String,String> lastLines = new HashMap<String, String>();
	private boolean recordingMode;
	private boolean multiple;
	
	private LogcatReaderLoader(Parcel in) {
		this.recordingMode = in.readInt() == 1;
		this.multiple = in.readInt() == 1;
		Bundle bundle = in.readBundle();
		for (String key : bundle.keySet()) {
			lastLines.put(key, bundle.getString(key));
		}
	}
	
	private LogcatReaderLoader(List<String> buffers, boolean recordingMode) {
		this.recordingMode = recordingMode;
		this.multiple = buffers.size() > 1;
		if (recordingMode) {
			for (String buffer: buffers) {
				lastLines.put(buffer, LogcatHelper.getLastLogLine(buffer));
			}
		}
	}
	
	public LogcatReader loadReader() throws IOException {
		if (!multiple) {
			// single reader
			String buffer = lastLines.keySet().iterator().next();
			String lastLine = lastLines.values().iterator().next();
			return new SingleLogcatReader(recordingMode, buffer, lastLine);
		} else {
			// multiple reader
			return new MultipleLogcatReader(recordingMode, lastLines);
		}
	}
	
	public static LogcatReaderLoader create(Context context) {
		List<String> buffers = PreferenceHelper.getBuffers(context);
		LogcatReaderLoader loader = new LogcatReaderLoader(buffers, true);
		return loader;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(recordingMode ? 1 : 0);
		dest.writeInt(multiple ? 1 : 0);
		Bundle bundle = new Bundle();
		for (Entry<String,String> entry : lastLines.entrySet()) {
			bundle.putString(entry.getKey(), entry.getValue());
		}
		dest.writeBundle(bundle);
	}
	
	public static final Parcelable.Creator<LogcatReaderLoader> CREATOR = new Parcelable.Creator<LogcatReaderLoader>() {
		public LogcatReaderLoader createFromParcel(Parcel in) {
			return new LogcatReaderLoader(in);
		}

		public LogcatReaderLoader[] newArray(int size) {
			return new LogcatReaderLoader[size];
		}
	};
}
