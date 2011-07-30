package com.nolanlawson.logcat.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.text.TextUtils;

import com.nolanlawson.logcat.util.UtilLogger;

public class LogcatHelper {

	private static final UtilLogger log = new UtilLogger(LogcatHelper.class);
	
	public static final String BUFFER_MAIN = "main";
	public static final String BUFFER_EVENTS = "events";
	public static final String BUFFER_RADIO = "radio";
	
	public static Process getLogcatProcess(String buffer) throws IOException {
		
		
		List<String> args = getLogcatArgs(buffer);
		Process process = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
		
		ProcessHelper.incrementProcesses();
		
		return process;
	}
	
	private static List<String> getLogcatArgs(String buffer) {
		List<String> args = new ArrayList<String>(Arrays.asList("logcat", "-v", "time"));
		
		// for some reason, adding -b main excludes log output from AndroidRuntime runtime exceptions,
		// whereas just leaving it blank keeps them in.  So do not specify the buffer if it is "main"
		if (!buffer.equals(BUFFER_MAIN)) {
			args.add("-b");
			args.add(buffer);
		}
		
		return args;
	}
	
	public static String getLastLogLine(String buffer) {
		Process dumpLogcatProcess = null;
		BufferedReader reader = null;
		String result = null;
		try {
			
			List<String> args = getLogcatArgs(buffer);
			args.add("-d"); // -d just dumps the whole thing
			
			dumpLogcatProcess = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
			ProcessHelper.incrementProcesses();
			reader = new BufferedReader(new InputStreamReader(dumpLogcatProcess
					.getInputStream()), 8192);
			
			String line;
			while ((line = reader.readLine()) != null) {
				result = line;
			}
		} catch (IOException e) {
			log.e(e, "unexpected exception");
		} finally {		
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.e(e, "unexpected exception");
				}
			}
			
			if (dumpLogcatProcess != null) {
				dumpLogcatProcess.destroy();
				ProcessHelper.decrementProcesses();
			}
		}
		
		return result;
	}
	
	public static Comparator<String> getLogComparator() {

		return new Comparator<String>(){

			@Override
			public int compare(String left, String right) {
				
				// timestamped logs need to go after non-timestamped logs, such as "start of dev/log/main"
				
				if (TextUtils.isEmpty(left) && TextUtils.isEmpty(right)) {
					return 0;
				} else if (TextUtils.isEmpty(left)) {
					return -1;
				} else if (TextUtils.isEmpty(right)) {
					return 1;
				} else if (Character.isDigit(left.charAt(0)) &&  Character.isDigit(right.charAt(0))) {
					return left.compareTo(right);
				} else if (Character.isDigit(left.charAt(0))) {
					return 1; 
				} else if (Character.isDigit(right.charAt(0))) {
					return -1;
				} else {
					return left.compareTo(right);
				}
			}
		};
	}
}
