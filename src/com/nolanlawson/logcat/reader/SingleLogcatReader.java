package com.nolanlawson.logcat.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.text.TextUtils;

import com.nolanlawson.logcat.data.LogLine;
import com.nolanlawson.logcat.helper.LogcatHelper;
import com.nolanlawson.logcat.util.UtilLogger;

public class SingleLogcatReader extends AbsLogcatReader {

	private static UtilLogger log = new UtilLogger(SingleLogcatReader.class);
	
	private Process logcatProcess;
	private BufferedReader bufferedReader;
	private String logBuffer;
	private String lastLine;
	private Date lastLineDate;
	private DateFormat dateFormat = new SimpleDateFormat(LogLine.LOGCAT_DATE_FORMAT);
	
	public SingleLogcatReader(boolean recordingMode, String logBuffer, String lastLine) throws IOException {
		super(recordingMode);
		this.logBuffer = logBuffer;
		this.lastLine = lastLine;
		this.lastLineDate = toDate(lastLine);
		init();
	}
	
	
	private Date toDate(String line) {
		if (!TextUtils.isEmpty(line)) {
			if (line.length() >= 19) { // length of timestamp at beginning of line
				try {
					return dateFormat.parse(line);
				} catch (ParseException ignore) {}
			}
		}
		return null;
	}


	private void init() throws IOException {
		// use the "time" log so we can see what time the logs were logged at
		logcatProcess = LogcatHelper.getLogcatProcess(logBuffer);
		
		bufferedReader = new BufferedReader(new InputStreamReader(logcatProcess
				.getInputStream()), 8192);
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
			if (lastLine.equals(line) || isAfterLastTime(line)) {
				lastLine = null; // indicates we've passed the last line
			}
		}
		
		return line;
		
	}
	
	private boolean isAfterLastTime(String line) {
		Date lineDate = toDate(line);
		return lineDate != null && lastLineDate != null && lineDate.after(lastLineDate);
	}


	@Override
	public boolean readyToRecord() {
		if (!recordingMode) {
			return false;
		}
		return lastLine == null;
	}
}
