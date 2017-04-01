package com.nolanlawson.logcat.helper;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import android.text.TextUtils;
import android.util.Log;

import com.nolanlawson.logcat.util.ArrayUtil;

/**
 * Helper functions for running processes.
 * 
 * @author nolan
 * 
 */
public class RuntimeHelper {
	private static final String TAG = RuntimeHelper.class.getSimpleName();

	/**
	 * Exec the arguments, using root if necessary.
	 * 
	 * @param args
	 */
	public static Process exec(List<String> args) throws IOException {
		// since JellyBean, sudo is required to read other apps' logs
		Process process = null;
		PrintStream outputStream = null;

		try {
			if (VersionHelper.getVersionSdkIntCompat() >= VersionHelper.VERSION_JELLYBEAN
					&& !SuperUserHelper.isFailedToObtainRoot()) {
				process = Runtime.getRuntime().exec("su");
				outputStream = new PrintStream(new BufferedOutputStream(process.getOutputStream(), 8192));
				outputStream.println(TextUtils.join(" ", args));
				outputStream.flush();
				return process;
			}
		}catch(Exception e){
			Log.w(TAG, "",e);
		} finally {
			if (outputStream != null) {
				outputStream.close();
			}
		}
		return Runtime.getRuntime().exec(ArrayUtil.toArray(args, String.class));
	}

	public static void destroy(Process process) {
		// if we're in JellyBean, then we need to kill the process as root,
		// which requires all this
		// extra UnixProcess logic
		if (VersionHelper.getVersionSdkIntCompat() >= VersionHelper.VERSION_JELLYBEAN
				&& !SuperUserHelper.isFailedToObtainRoot()) {
			SuperUserHelper.destroy(process);
		} else {
			process.destroy();
		}
	}

}