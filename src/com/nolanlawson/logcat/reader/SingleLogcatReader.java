package com.nolanlawson.logcat.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;

import com.nolanlawson.logcat.helper.LogcatHelper;
import com.nolanlawson.logcat.util.UtilLogger;

public class SingleLogcatReader extends AbsLogcatReader {

	private static UtilLogger log = new UtilLogger(SingleLogcatReader.class);
	
	private Process logcatProcess;
	private BufferedReader bufferedReader;
	private String logBuffer;
	private String lastLine;
	
	public SingleLogcatReader(boolean recordingMode, String logBuffer, Context context) throws IOException {
		super(recordingMode);
		this.logBuffer = logBuffer;
		init(context);
	}
	
	
	private void init(Context context) throws IOException {
		// use the "time" log so we can see what time the logs were logged at
		logcatProcess = LogcatHelper.getLogcatProcess(logBuffer, context);
		
		bufferedReader = new BufferedReader(new InputStreamReader(logcatProcess
				.getInputStream()), 8192);
		
		if (recordingMode) {
			lastLine = LogcatHelper.getLastLogLine(logBuffer, context);
		}
	}
	

	public String getLogBuffer() {
		return logBuffer;
	}


	@Override
	public void killQuietly() {
		if (logcatProcess != null) {
			logcatProcess.destroy();
		}
		if (bufferedReader != null) {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				log.e(e, "unexpected exception");
			}
		}
	}

	@Override
	public String readLine() throws IOException {
		String line = bufferedReader.readLine();
		
		if (recordingMode && lastLine != null) {
			if (lastLine.equals(line)) {
				lastLine = null; // indicates we've passed the last line
			}
		}
		
		return line;
		
	}
	
	@Override
	public boolean readyToRecord() {
		if (!recordingMode) {
			return false;
		}
		return lastLine == null;
	}
}
