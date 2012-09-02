package com.nolanlawson.logcat.helper;

import java.io.DataOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.nolanlawson.logcat.R;
import com.nolanlawson.logcat.util.UtilLogger;
/* 
 * Starting in JellyBean, the READ_LOGS permission must be requested as super user
 * or else you can only read your own app's logs.
 * 
 * This class contains helper methods to correct the problem.
 */
public class SuperUserHelper {
	
	private static UtilLogger log = new UtilLogger(SuperUserHelper.class);
	
	private static void showToast(final Context context, final int resId) {
		Handler handler = new Handler(Looper.getMainLooper());
		
		handler.post(new Runnable(){

			@Override
			public void run() {
				Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
				
			}});
	}
	
	public static void requestRoot(Context context) {

		showToast(context, R.string.toast_request_root);
		
		Process process = null;
		try {
			// Preform su to get root privledges
			process = Runtime.getRuntime().exec("su");

			// confirm that we have root
			DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
			outputStream.writeBytes("echo hello\n");

			// Close the terminal
			outputStream.writeBytes("exit\n");
			outputStream.flush();

			process.waitFor();
			if (process.exitValue() != 0) {
				showToast(context, R.string.toast_no_root);
			} else {
				// success
                PreferenceHelper.setJellybeanRootRan(context);
			}

		} catch (IOException e) {
			log.w(e, "Cannot obtain root");
			showToast(context, R.string.toast_no_root);
		} catch (InterruptedException e) {
			log.w(e, "Cannot obtain root");
			showToast(context, R.string.toast_no_root);
		}

	}
}
