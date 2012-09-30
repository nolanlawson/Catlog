package com.nolanlawson.logcat.helper;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	private static boolean failedToObtainRoot = false;
	
	private static final Pattern PID_PATTERN = Pattern.compile("\\d+");
	
	private static void showToast(final Context context, final int resId) {
		Handler handler = new Handler(Looper.getMainLooper());
		
		handler.post(new Runnable(){

			@Override
			public void run() {
				Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
				
			}});
	}
	
	public static void destroy(Process process) {
	    // stupid method for getting the pid, but it actually works
	    Matcher matcher = PID_PATTERN.matcher(process.toString());
	    matcher.find();
	    int pid = Integer.parseInt(matcher.group());
        
        Process suProcess2;
        PrintStream outputStream2 = null;
        try {
            suProcess2 = Runtime.getRuntime().exec("su");
            outputStream2 = new PrintStream(new BufferedOutputStream(suProcess2.getOutputStream(), 8192));
            outputStream2.println("kill " + pid);
            outputStream2.flush();
        } catch (IOException e) {
            log.e(e, "cannot kill process " + process);
        } finally {
            if (outputStream2 != null) {
                outputStream2.close();
            }
        }
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
				failedToObtainRoot = true;
			} else {
				// success
                PreferenceHelper.setJellybeanRootRan(context);
			}

		} catch (IOException e) {
			log.w(e, "Cannot obtain root");
			showToast(context, R.string.toast_no_root);
			failedToObtainRoot = true;
		} catch (InterruptedException e) {
			log.w(e, "Cannot obtain root");
			showToast(context, R.string.toast_no_root);
			failedToObtainRoot = true;
		}

	}
	
	public static boolean isFailedToObtainRoot() {
		return failedToObtainRoot;
	}
}
