package com.nolanlawson.logcat.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.nolanlawson.logcat.util.UtilLogger;

public class LogHelper {

	public static final String[] LOG_BUFFERS = {"main", "radio", "events"};
	
	private static UtilLogger log = new UtilLogger(LogHelper.class);
	
	public static List<String> dumpLog(String buffer) {
		Process dumpLogcatProcess = null;
		BufferedReader reader = null;
		List<String> result = new ArrayList<String>();
		try {
			
			dumpLogcatProcess = Runtime.getRuntime().exec(
					new String[] { "logcat", "-d", "-b", buffer, "-v", "time" }); // -d just dumps the whole thing

			reader = new BufferedReader(new InputStreamReader(dumpLogcatProcess
					.getInputStream()), 8192);
			
			String line;
			while ((line = reader.readLine()) != null) {
				result.add(line);
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
	
}
