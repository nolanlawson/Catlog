package com.nolanlawson.logcat.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;

import com.nolanlawson.logcat.helper.LogcatHelper;
import com.nolanlawson.logcat.util.UtilLogger;

public class SingleLogcatReader implements LogcatReader {

	private static UtilLogger log = new UtilLogger(SingleLogcatReader.class);
	
	private Process logcatProcess;
	private BufferedReader bufferedReader;
	private String logBuffer;
	
	public SingleLogcatReader(String logBuffer, Context context) throws IOException {
		this.logBuffer = logBuffer;
		init(context);
	}
	
	
	private void init(Context context) throws IOException {
		// use the "time" log so we can see what time the logs were logged at
		logcatProcess = LogcatHelper.getLogcatProcess(logBuffer, context);
		
		bufferedReader = new BufferedReader(new InputStreamReader(logcatProcess
				.getInputStream()), 8192);
	}
	

	public String getLogBuffer() {
		return logBuffer;
	}


	@Override
	public void closeQuietly() {
		if (bufferedReader != null) {
			try {
				bufferedReader.close();
			} catch (IOException e) {
				log.e(e, "unexpected exception");
			}
		}
		
		if (logcatProcess != null) {
			logcatProcess.destroy();
		}
	}

	@Override
	public String readLine() throws IOException {
		return bufferedReader.readLine();
	}
	
	@Override
	public void kill() {
		if (logcatProcess != null) {
			logcatProcess.destroy();
		}
	}
}
