package com.nolanlawson.logcat.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;
import android.util.Log;

import com.nolanlawson.logcat.util.LogLineAdapterUtil;
import com.nolanlawson.logcat.util.UtilLogger;


public class LogLine {


	public static final String LOGCAT_DATE_FORMAT = "MM-dd HH:mm:ss.SSS";
	
	private static Pattern logPattern = Pattern.compile("(\\w)/([^(]+)\\(\\s*(\\d+)\\): (.*)");
	
	private static UtilLogger log = new UtilLogger(LogLine.class);
	
	private String originalLine;
	private int logLevel;
	private String tag;
	private String logOutput;
	private int processId = -1;
	private String timestamp;
	private boolean expanded = false;
	
	public String getOriginalLine() {
		return originalLine;
	}
	public void setOriginalLine(String originalLine) {
		this.originalLine = originalLine;
	}
	public int getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getLogOutput() {
		return logOutput;
	}
	public void setLogOutput(String logOutput) {
		this.logOutput = logOutput;
	}
	
	
	public int getProcessId() {
		return processId;
	}
	public void setProcessId(int processId) {
		this.processId = processId;
	}
	
	
	
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public boolean isExpanded() {
		return expanded;
	}
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	public static LogLine newLogLine(String originalLine, boolean expanded) {
		
		LogLine logLine = new LogLine();
		logLine.setOriginalLine(originalLine);
		logLine.setExpanded(expanded);
		
		// first get the timestamp
		String timestamp = null;
		
		// if the first char is a digit, then this starts out with a timestamp
		// otherwise, it's a legacy log or the beginning of the log output or something
		if (!TextUtils.isEmpty(originalLine) && TextUtils.isDigitsOnly(Character.toString(originalLine.charAt(0)))) {
			timestamp = originalLine.substring(0,19);
			originalLine = originalLine.substring(19); // cut off timestamp
		}
		
		logLine.setTimestamp(timestamp);
		
		Matcher matcher = logPattern.matcher(originalLine);
		
		if (matcher.matches()) {
			char logLevelChar = matcher.group(1).charAt(0);
			
			logLine.setLogLevel(convertCharToLogLevel(logLevelChar));
			logLine.setTag(matcher.group(2));
			logLine.setProcessId(Integer.parseInt(matcher.group(3)));
			logLine.setLogOutput(matcher.group(4));
			
			
		} else {
			log.d("Line doesn't match pattern: " + originalLine);
			logLine.setLogOutput(originalLine);
			logLine.setLogLevel(-1);
		}
		
		return logLine;
		
	}
	public static int convertCharToLogLevel(char logLevelChar) {
		
		int logLevel = -1;
		switch (logLevelChar) {
		case 'D':
			logLevel = Log.DEBUG;
			break;
		case 'E':
			logLevel = Log.ERROR;
			break;
		case 'I':
			logLevel = Log.INFO;
			break;
		case 'V':
			logLevel = Log.VERBOSE;
			break;
		case 'W':
			logLevel = Log.WARN;
			break;
		case 'F':
			logLevel = LogLineAdapterUtil.LOG_WTF; // 'F' actually stands for 'WTF', which is a real Android log level in 2.2
			break;
		}
		return logLevel;
	}
	
	public static char convertLogLevelToChar(int logLevel) {
		
		char result = ' ';
		switch (logLevel) {
		case Log.DEBUG:
			result = 'D';
			break;
		case Log.ERROR:
			result = 'E';
			break;
		case Log.INFO:
			result = 'I';
			break;
		case Log.VERBOSE:
			result = 'V';
			break;
		case Log.WARN:
			result = 'W';
			break;
		case LogLineAdapterUtil.LOG_WTF:
			result = 'F';
		}
		return result;
	}	
}
