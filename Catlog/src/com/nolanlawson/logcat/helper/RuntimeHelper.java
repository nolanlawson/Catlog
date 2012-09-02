package com.nolanlawson.logcat.helper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import android.text.TextUtils;

import com.nolanlawson.logcat.util.ArrayUtil;

/**
 * Helper functions for running processes.
 * @author nolan
 *
 */
public class RuntimeHelper {
	
	/**
	 * Exec the arguments, using root if necessary.
	 * @param args
	 */
	public static Process exec(List<String> args) throws IOException {
		
		// since JellyBean, sudo is required to read other apps' logs
		boolean requireSu = VersionHelper.getVersionSdkIntCompat() >= VersionHelper.VERSION_JELLYBEAN;
		if (requireSu) {
			Process process = Runtime.getRuntime().exec("su");
			
			PrintStream outputStream = null;
			try {
				outputStream = new PrintStream(new BufferedOutputStream(process.getOutputStream(), 8192));
				outputStream.println(TextUtils.join(" ", args));
				outputStream.flush();
			} finally {
				if (outputStream != null) {
					outputStream.close();
				}
			}
			
			return process;
			/*args = Arrays.asList("su", "-c" , TextUtils.join(" ", args));
			return Runtime.getRuntime().exec(ArrayUtil.toArray(args, String.class));*/
		}
		return Runtime.getRuntime().exec(ArrayUtil.toArray(args, String.class));
	}
	
}