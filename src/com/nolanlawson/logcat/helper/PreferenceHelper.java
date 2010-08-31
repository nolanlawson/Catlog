package com.nolanlawson.logcat.helper;

import com.nolanlawson.logcat.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceHelper {
	
	private static float textSize = -1;
	
	public static float getTextSizePreference(Context context) {
		
		if (textSize == -1) {
		
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			String textSizePref = sharedPrefs.getString(
					context.getText(R.string.pref_text_size).toString(), 
					context.getText(R.string.text_size_small_value).toString());
			
			if (textSizePref.equals(context.getText(R.string.text_size_small_value))) {
				cacheTextsize(context, R.dimen.text_size_small);
			} else if (textSizePref.equals(context.getText(R.string.text_size_medium_value))) {
				cacheTextsize(context, R.dimen.text_size_medium);
			} else if (textSizePref.equals(context.getText(R.string.text_size_large_value))) {
				cacheTextsize(context, R.dimen.text_size_large);
			} else { // xlarge
				cacheTextsize(context, R.dimen.text_size_xlarge);
			}
		}
		
		return textSize;
		
	}
	
	public static void clearCache() {
		textSize = -1;
	}
	
	private static void cacheTextsize(Context context, int dimenId) {
		textSize = context.getResources().getDimension(dimenId);
	}
	
	public static boolean getExpandedByDefaultPreference(Context context) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return sharedPrefs.getBoolean(
				context.getText(R.string.pref_expanded_by_default).toString(), false);
	}
	
}
