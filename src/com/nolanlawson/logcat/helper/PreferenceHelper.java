package com.nolanlawson.logcat.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.nolanlawson.logcat.R;
import com.nolanlawson.logcat.util.UtilLogger;

public class PreferenceHelper {
	
	private static float textSize = -1;
	
	private static UtilLogger log = new UtilLogger(PreferenceHelper.class);
	
	private static final String widgetExistsPrefix = "widget_";
	
	public static boolean getWidgetExistsPreference(Context context, int appWidgetId) {
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		String widgetExists = widgetExistsPrefix.concat(Integer.toString(appWidgetId));
		
		return sharedPrefs.getBoolean(widgetExists, false);
	}
	
	public static void setWidgetExistsPreference(Context context, int[] appWidgetIds) {
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor editor = sharedPrefs.edit();
		
		for (int appWidgetId : appWidgetIds) {
			String widgetExists = widgetExistsPrefix.concat(Integer.toString(appWidgetId));
			editor.putBoolean(widgetExists, true);
		}
		
		editor.commit();
		
		
	}
	
	public static float getTextSizePreference(Context context) {
		
		if (textSize == -1) {
		
			SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
			
			String textSizePref = sharedPrefs.getString(
					context.getText(R.string.pref_text_size).toString(), 
					context.getText(R.string.text_size_small_value).toString());

			if (textSizePref.equals(context.getText(R.string.text_size_xsmall_value))) {
				cacheTextsize(context, R.dimen.text_size_xsmall);
			} else if (textSizePref.equals(context.getText(R.string.text_size_small_value))) {
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
		
		float unscaledSize = context.getResources().getDimension(dimenId);
		
		log.d("unscaledSize is %g", unscaledSize);
		
		textSize = unscaledSize;
	}
	
	public static boolean getExpandedByDefaultPreference(Context context) {

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		return sharedPrefs.getBoolean(
				context.getText(R.string.pref_expanded_by_default).toString(), false);
	}
	
}
