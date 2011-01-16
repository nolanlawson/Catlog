package com.nolanlawson.logcat.data;

import android.content.Context;

import com.nolanlawson.logcat.R;


public enum ColorScheme {
	Dark (R.string.pref_theme_choice_dark, R.color.main_background_dark, 
			R.color.main_foreground_dark, R.array.dark_theme_colors),
	Light (R.string.pref_theme_choice_light, R.color.main_background_light, 
			R.color.main_foreground_light, R.array.light_theme_colors),
	Android (R.string.pref_theme_choice_android, R.color.main_background_android, 
			R.color.main_foreground_android, R.array.android_theme_colors),
	;
	
	private int nameResource;
	private int backgroundColorResource;
	private int foregroundColorResource;
	private int tagColorsResource;
	
	private int backgroundColor = -1;
	private int foregroundColor = -1;
	private int[] tagColors;
	
	private ColorScheme(int nameResource, int backgroundColorResource, int foregroundColorResource,
			int tagColorsResource) {
		this.nameResource = nameResource;
		this.backgroundColorResource = backgroundColorResource;
		this.foregroundColorResource = foregroundColorResource;
		this.tagColorsResource = tagColorsResource;
	}

	public int getNameResource() {
		return nameResource;
	}	
	
	public int getBackgroundColor(Context context) {
		if (backgroundColor == -1) {
			backgroundColor = context.getResources().getColor(backgroundColorResource);
		}
		return backgroundColor;
	}
	
	public int getForegroundColor(Context context) {
		if (foregroundColor == -1) {
			foregroundColor = context.getResources().getColor(foregroundColorResource);
		}
		return foregroundColor;
	}
	
	public int[] getTagColors(Context context) {
		if (tagColors == null) {
			tagColors = context.getResources().getIntArray(tagColorsResource);
		}
		return tagColors;
	}
}
