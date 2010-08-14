package com.nolanlawson.catlog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

import com.nolanlawson.catlog.helper.SaveLogHelper;
import com.nolanlawson.catlog.helper.ServiceHelper;
import com.nolanlawson.catlog.util.UtilLogger;

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

	private boolean kill = false;

	private NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	
	private String filename;
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			log.d("onReceive()");
			
			// received broadcast to kill service
			kill = true;
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


	@Override
	public void onDestroy() {
		log.d("onDestroy()");
		super.onDestroy();
		kill = true;

		unregisterReceiver(receiver);
		
		stopForegroundCompat(R.string.notification_title);
		
	}

    // This is the old onStart method that will be called on the pre-2.0
    // platform.
    @Override
    public void onStart(Intent intent, int startId) {
    	log.d("onStart()");
    	super.onStart(intent, startId);
    	filename = intent.getStringExtra("filename");
        handleCommand(intent);
    }

	private void handleCommand(Intent intent) {
        
        CharSequence tickerText = getText(R.string.notification_ticker);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(android.R.drawable.stat_notify_sync, tickerText,
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
		
		log.d("Starting up AppTrackerService now with intent: %s", intent);

		makeToast(R.string.log_recording_started);
		
		Process logcatProcess = null;
		BufferedReader reader = null;
		
		List<String> lines = new ArrayList<String>();
		
		try {
			
			int numLines = getNumberOfExistingLogLines();
			
			log.d("number of existing lines in logcat log is %d", numLines);
			
			int currentLine = 0;
			
			logcatProcess = Runtime.getRuntime().exec(
					new String[] { "logcat" });

			reader = new BufferedReader(new InputStreamReader(logcatProcess
					.getInputStream()));
			
			String line;
			
			while ((line = reader.readLine()) != null) {
								
				if (kill) {
					log.d("manually killed CatlogService");
					break;
				}
				if (++currentLine <= numLines) {
					log.d("skipping line %d", currentLine);
					continue;
				}
				lines.add(line);
			}

		}

		catch (IOException e) {
			log.e(e, "unexpected exception");
		}

		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.e(e, "unexpected exception");
				}
			}
			
			if (logcatProcess != null) {
				logcatProcess.destroy();
			}

			log.d("CatlogService ended");
			
			boolean logSaved = SaveLogHelper.saveLog(lines, filename);
			
			if (logSaved) {
				makeToast(R.string.log_saved);
			} else {
				makeToast(R.string.unable_to_save_log);
			}
			

		}
	}

	private void makeToast(final int stringResId) {
		handler.post(new Runnable() {
			
			@Override
			public void run() {
				
				Toast.makeText(LogcatRecordingService.this, stringResId, Toast.LENGTH_LONG).show();
				
			}
		});
		
	}


	private int getNumberOfExistingLogLines() throws IOException {
		
		// figure out how many lines are already in the logcat log
		// to do this, just use the -d (for "dump") command in logcat
		
		Process logcatProcess = Runtime.getRuntime().exec(
				new String[] { "logcat", "-d",});

		BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess
				.getInputStream()));
		try {
			int lines = 0;
			
			while (reader.readLine() != null) {
				lines++;
			}
			
			reader.close();
			logcatProcess.destroy();
			
			return lines;
		} finally {
			
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.e(e, "unexpected exception");
				}
			}
			
			if (logcatProcess != null) {
				logcatProcess.destroy();
			}
		}
	}
	
}
