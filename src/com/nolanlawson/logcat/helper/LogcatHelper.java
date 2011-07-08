package com.nolanlawson.logcat.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.nolanlawson.logcat.R;
import com.nolanlawson.logcat.reader.LogcatReader;
import com.nolanlawson.logcat.reader.MultipleLogcatReader;
import com.nolanlawson.logcat.reader.SingleLogcatReader;
import com.nolanlawson.logcat.util.UtilLogger;

public class LogcatHelper {

	private static final UtilLogger log = new UtilLogger(LogcatHelper.class);
	
	public static final String[] BUFFERS = {"main", "events", "radio"};
	
	public static Process getLogcatProcess(String buffer, Context context) throws IOException {
		
		
		List<String> args = getLogcatArgs(buffer, context);
		return Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
	}
	
	private static List<String> getLogcatArgs(String buffer, Context context) {
		List<String> args = new ArrayList<String>(Arrays.asList("logcat", "-v", "time"));
		
		// for some reason, adding -b main excludes log output from AndroidRuntime runtime exceptions,
		// whereas just leaving it blank keeps them in.  So do not specify the buffer if it is "main"
		if (!buffer.equals(context.getString(R.string.pref_buffer_choice_main_value))) {
			args.add("-b");
			args.add(buffer);
		}
		
		return args;
	}

	public static LogcatReader getLogcatReader(String bufferPref, Context context) throws IOException {
		if (bufferPref.equals(context.getString(R.string.pref_buffer_choice_all_value))) {
			return new MultipleLogcatReader(context);
		} else {
			return new SingleLogcatReader(bufferPref, context);
		}
	}
	
	public static String getLastLogLine(String buffer, Context context) {
		Process dumpLogcatProcess = null;
		BufferedReader reader = null;
		String result = null;
		try {
			
			List<String> args = getLogcatArgs(buffer, context);
			args.add("-d"); // -d just dumps the whole thing
			
			dumpLogcatProcess = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));

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
