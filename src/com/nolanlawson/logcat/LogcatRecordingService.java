package com.nolanlawson.logcat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.nolanlawson.logcat.helper.PreferenceHelper;
import com.nolanlawson.logcat.helper.SaveLogHelper;
import com.nolanlawson.logcat.helper.ServiceHelper;
import com.nolanlawson.logcat.helper.WidgetHelper;
import com.nolanlawson.logcat.reader.LogcatReader;
import com.nolanlawson.logcat.reader.LogcatReaderLoader;
import com.nolanlawson.logcat.util.UtilLogger;

/**
 * Reads logs.
 * 
 * @author nolan
 * 
 */
public class LogcatRecordingService extends IntentService {

	private static final Class<?>[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {
	    boolean.class};
	
	private static final String ACTION_STOP_RECORDING = "com.nolanlawson.catlog.action.STOP_RECORDING";
	public static final String URI_SCHEME = "catlog_recording_service";
	
	private static UtilLogger log = new UtilLogger(LogcatRecordingService.class);

	private LogcatReader reader;

	private NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private boolean killed;
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			log.d("onReceive()");
			
			// received broadcast to kill service
			killProcess();
			ServiceHelper.stopBackgroundServiceIfRunning(context);
		}
	};
	
	private Handler handler;


	public LogcatRecordingService() {
		super("AppTrackerService");
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		log.d("onCreate()");
		
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		IntentFilter intentFilter = new IntentFilter(ACTION_STOP_RECORDING);
		intentFilter.addDataScheme(URI_SCHEME);
		
		registerReceiver(receiver, intentFilter);
		
		handler = new Handler(Looper.getMainLooper());
		
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			log.d(e,"running on older platform; couldn't find startForeground method");
			mStartForeground = mStopForeground = null;
		}

	}


	private void initializeReader(Intent intent) {
		try {
			// use the "time" log so we can see what time the logs were logged at
			LogcatReaderLoader loader = intent.getParcelableExtra("loader");
			reader = loader.loadReader();
		
			while (!reader.readyToRecord() && !killed) {
				reader.readLine();
				// keep skipping lines until we find one that is past the last log line, i.e.
				// it's ready to record
			}			
			if (!killed) {
				makeToast(R.string.log_recording_started, Toast.LENGTH_SHORT);
			}
		} catch (IOException e) {
			log.d(e, "");
		}
		
	}


	@Override
	public void onDestroy() {
		log.d("onDestroy()");
		super.onDestroy();
		killProcess();

		unregisterReceiver(receiver);
		
		stopForegroundCompat(R.string.notification_title);
		
		WidgetHelper.updateWidgets(getApplicationContext(), false);
		
		
	}

    // This is the old onStart method that will be called on the pre-2.0
    // platform.
    @Override
    public void onStart(Intent intent, int startId) {
    	log.d("onStart()");
    	super.onStart(intent, startId);
        handleCommand(intent);
    }

	private void handleCommand(Intent intent) {
        
		// notify the widgets that we're running
		WidgetHelper.updateWidgets(getApplicationContext());
		
        CharSequence tickerText = getText(R.string.notification_ticker);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.status_icon, tickerText,
                System.currentTimeMillis());
        

        Intent stopRecordingIntent = new Intent();
        stopRecordingIntent.setAction(ACTION_STOP_RECORDING);
        // have to make this unique for God knows what reason
        stopRecordingIntent.setData(Uri.withAppendedPath(Uri.parse(URI_SCHEME + "://stop/"), 
        		Long.toHexString(new Random().nextLong())));
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                    0 /* no requestCode */, stopRecordingIntent, PendingIntent.FLAG_ONE_SHOT);
        
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.notification_title),
                       getText(R.string.notification_subtext), pendingIntent);

        startForegroundCompat(R.string.notification_title, notification);

		
	}


	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	private void startForegroundCompat(int id, Notification notification) {
	    // If we have the new startForeground API, then use it.
	    if (mStartForeground != null) {
	        mStartForegroundArgs[0] = Integer.valueOf(id);
	        mStartForegroundArgs[1] = notification;
	        try {
	            mStartForeground.invoke(this, mStartForegroundArgs);
	        } catch (InvocationTargetException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke startForeground");
	        } catch (IllegalAccessException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke startForeground");
	        }
	        return;
	    }

	    // Fall back on the old API.
	    setForeground(true);
	    mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	private void stopForegroundCompat(int id) {
	    // If we have the new stopForeground API, then use it.
	    if (mStopForeground != null) {
	        mStopForegroundArgs[0] = Boolean.TRUE;
	        try {
	            mStopForeground.invoke(this, mStopForegroundArgs);
	        } catch (InvocationTargetException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke stopForeground");
	        } catch (IllegalAccessException e) {
	            // Should not happen.
	            log.d(e, "Unable to invoke stopForeground");
	        }
	        return;
	    }

	    // Fall back on the old API.  Note to cancel BEFORE changing the
	    // foreground state, since we could be killed at that point.
	    mNM.cancel(id);
	    setForeground(false);
	}

	protected void onHandleIntent(Intent intent) {
		log.d("onHandleIntent()");
		handleIntent(intent);
	}
	
	private void handleIntent(Intent intent) {
		
		log.d("Starting up %s now with intent: %s", LogcatRecordingService.class.getSimpleName(), intent);
		
		String filename = intent.getStringExtra("filename");
		
		SaveLogHelper.deleteLogIfExists(filename);
		
		initializeReader(intent);
		
		StringBuilder stringBuilder = new StringBuilder();
		
		try {
			
			String line;
			int lineCount = 0;
			int logLinePeriod = PreferenceHelper.getLogLinePeriodPreference(getApplicationContext());
			while ((line = reader.readLine()) != null && !killed) {
				
				stringBuilder.append(line).append("\n");
				
				if (++lineCount % logLinePeriod == 0) {
					// avoid OutOfMemoryErrors; flush now
					SaveLogHelper.saveLog(stringBuilder, filename);
					stringBuilder.delete(0, stringBuilder.length()); // clear
				}
			}
		} catch (IOException e) {
			log.e(e, "unexpected exception");
		} finally {
			if (reader != null) {
				reader.killQuietly();
			}

			log.d("CatlogService ended");
			
			boolean logSaved = SaveLogHelper.saveLog(stringBuilder, filename);
			
			if (logSaved) {
				makeToast(R.string.log_saved, Toast.LENGTH_SHORT);
				startLogcatActivityToViewSavedFile(filename);
			} else {
				makeToast(R.string.unable_to_save_log, Toast.LENGTH_LONG);
			}
		}
	}

	private void startLogcatActivityToViewSavedFile(String filename) {
		
		// start up the logcat activity if necessary and show the saved file
		
		Intent targetIntent = new Intent(getApplicationContext(), LogcatActivity.class);
		targetIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
		targetIntent.setAction(Intent.ACTION_MAIN);
		targetIntent.putExtra("filename", filename);
		
		startActivity(targetIntent);
		
	}


	private void makeToast(final int stringResId, final int toastLength) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				
				Toast.makeText(LogcatRecordingService.this, stringResId, toastLength).show();
				
			}
		});
		
	}
	
	private void killProcess() {
		// kill the logcat process
		if (reader != null) {
			reader.killQuietly();
		}
		killed = true;
	}
	
}
