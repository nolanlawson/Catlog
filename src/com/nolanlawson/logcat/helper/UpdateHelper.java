package com.nolanlawson.logcat.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.nolanlawson.logcat.R;

public class UpdateHelper {

	public static void runUpdatesIfNecessary(Context context) {
		runUpdate1(context);
	}
	
	/**
	 * This update changes the buffer preference from "all_combined" (used before version 25) to just a list of
	 * "main,radio,events"
	 * @param context
	 */
	public static void runUpdate1(Context context) {
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		String bufferPref = sharedPrefs.getString(
				context.getString(R.string.pref_buffer), context.getString(R.string.pref_buffer_choice_main));
		
		if (bufferPref.equals("all_combined")) {
			Editor editor = sharedPrefs.edit();
			editor.putString(context.getString(R.string.pref_buffer), "main,events,radio");
			editor.commit();
		}
	}
	
}
