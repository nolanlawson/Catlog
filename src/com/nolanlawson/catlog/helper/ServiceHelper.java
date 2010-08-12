package com.nolanlawson.catlog.helper;

import java.util.List;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.nolanlawson.catlog.CatlogRecordingService;
import com.nolanlawson.catlog.util.UtilLogger;

public class ServiceHelper {

	private static UtilLogger log = new UtilLogger(ServiceHelper.class);
	
	public static synchronized void stopBackgroundServiceIfRunning(Context context) {
		boolean alreadyRunning = ServiceHelper.checkIfServiceIsRunning(context);
		
		log.d("Is CatlogService running: %s", alreadyRunning);
		
		if (alreadyRunning) {
			Intent intent = new Intent(context, CatlogRecordingService.class);
			context.stopService(intent);
		}
		
	}
	
	public static synchronized void startBackgroundServiceIfNotAlreadyRunning(
			Context context, String filename) {
		
		boolean alreadyRunning = ServiceHelper.checkIfServiceIsRunning(context);
		
		log.d("Is CatlogService already running: %s", alreadyRunning);
		
		if (!alreadyRunning) {

			Intent intent = new Intent(context, CatlogRecordingService.class);
			intent.putExtra("filename", filename);
			context.startService(intent);
		}
	}
	
	public static boolean checkIfServiceIsRunning(Context context) {
		
		String serviceName = CatlogRecordingService.class.getName();
		
		ComponentName componentName = new ComponentName(context.getPackageName(), serviceName);
		
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		List<ActivityManager.RunningServiceInfo> procList = activityManager.getRunningServices(Integer.MAX_VALUE);

		if (procList != null) {

			for (ActivityManager.RunningServiceInfo appProcInfo : procList) {
				if (appProcInfo != null && componentName.equals(appProcInfo.service)) {
					log.d("%s is already running", serviceName);
					return true;
				}
			}
		}
		log.d("%s is not running", serviceName);
		return false;	
	}
}
