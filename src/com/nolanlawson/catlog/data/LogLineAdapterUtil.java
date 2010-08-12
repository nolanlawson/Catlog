package com.nolanlawson.catlog.data;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.nolanlawson.catlog.R;

public class LogLineAdapterUtil {
	
	public static final int LOG_WTF = 100; // arbitrary int to signify 'wtf' log level
	
	private static final int[] TAG_COLORS = new int[]{
		R.color.tag_color_01,
		R.color.tag_color_02,
		R.color.tag_color_03,
		R.color.tag_color_04,
		R.color.tag_color_05,
		R.color.tag_color_06,
		R.color.tag_color_07,
		R.color.tag_color_08,
		R.color.tag_color_09,
		R.color.tag_color_10,
		R.color.tag_color_11,
		R.color.tag_color_12,
		R.color.tag_color_13,
		R.color.tag_color_14,
		R.color.tag_color_15,
		R.color.tag_color_16,
	};
	private static int tagColorIndex = 0;
	
	// used to cycle through colors for each tag to make the UI more visually appealing
	private static Map<String, Integer> tagsToColors = new HashMap<String, Integer>();
	
	
	
	public static int getBackgroundColorForLogLevel(Context context, int logLevel) {
		int result = android.R.color.black;
		switch (logLevel) {
		case Log.DEBUG:
			result = R.color.background_debug;
			break;
		case Log.ERROR:
			result = R.color.background_error;
			break;
		case Log.INFO:
			result = R.color.background_info;
			break;
		case Log.VERBOSE:
			result = R.color.background_verbose;
			break;
		case Log.WARN:
			result = R.color.background_warn;
			break;
		case LOG_WTF:
			result = R.color.background_wtf;
			break;
		}	



		return context.getResources().getColor(result);
	}

	public static int getForegroundColorForLogLevel(Context context, int logLevel) {
		int result = android.R.color.primary_text_dark;
		switch (logLevel) {
		case Log.DEBUG:
			result = R.color.foreground_debug;
			break;
		case Log.ERROR:
			result = R.color.foreground_error;
			break;
		case Log.INFO:
			result = R.color.foreground_info;
			break;
		case Log.VERBOSE:
			result = R.color.foreground_verbose;
			break;
		case Log.WARN:
			result = R.color.foreground_warn;
			break;
		case LOG_WTF:
			result = R.color.foreground_wtf;
			break;
		}
		return context.getResources().getColor(result);
	}	
	
	public static synchronized int getTagColor(Context context, String tag, String mustBeDifferentFromTag) {
		
		if (TextUtils.isEmpty(tag)) {
			return context.getResources().getColor(android.R.color.black); // color doesn't matter in this case
		}
		
		Integer result = tagsToColors.get(tag);
		
		if (result == null) {
			
			Integer mustBeDifferentFrom = tagsToColors.get(mustBeDifferentFromTag);
			
			do {
			
				result = TAG_COLORS[tagColorIndex];
				
				// cycle through
				if (tagColorIndex == TAG_COLORS.length - 1) {
					tagColorIndex = 0;
				} else {
					tagColorIndex++;
				}
				
			} while (mustBeDifferentFrom != null && result.equals(mustBeDifferentFrom));
			
			tagsToColors.put(tag, result);
		}
		
		return context.getResources().getColor(result);
		
		
	}
	
	public static boolean logLevelIsAcceptableGivenLogLevelLimit(int logLevel, int logLevelLimit) {
			
		int minVal = 0;
		switch (logLevel) {
			
			case Log.VERBOSE:
				minVal = 0;
				break;
			case Log.DEBUG:
				minVal = 1;
				break;
			case Log.INFO:
				minVal = 2;
				break;
			case Log.WARN:
				minVal = 3;
				break;
			case Log.ERROR:
				minVal = 4;
				break;
			case LOG_WTF:
				minVal = 5;
				break;
			default: // e.g. the starting line that says "output of log such-and-such"
				return true;
		}
		
		return minVal >= logLevelLimit;

	}
}
