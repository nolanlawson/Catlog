package com.nolanlawson.logcat.data;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.nolanlawson.logcat.R;


public enum ColorScheme {
	Dark (R.string.pref_theme_choice_dark, R.color.main_background_dark, 
			R.color.main_foreground_dark, R.array.dark_theme_colors, R.color.spinner_droptown_dark),
	Light (R.string.pref_theme_choice_light, R.color.main_background_light, 
			R.color.main_foreground_light, R.array.light_theme_colors, R.color.spinner_droptown_light),
	Android (R.string.pref_theme_choice_android, R.color.main_background_android, 
			R.color.main_foreground_android, R.array.android_theme_colors, R.color.spinner_droptown_android),
	Verizon (R.string.pref_theme_choice_verizon, R.color.main_background_verizon, 
			R.color.main_foreground_verizon, R.array.dark_theme_colors, R.color.spinner_droptown_verizon),
	Att (R.string.pref_theme_choice_att, R.color.main_background_att, 
			R.color.main_foreground_att, R.array.light_theme_colors, R.color.spinner_droptown_att),
	Sprint (R.string.pref_theme_choice_sprint, R.color.main_background_sprint, 
			R.color.main_foreground_sprint, R.array.dark_theme_colors, R.color.spinner_droptown_sprint),
	Tmobile (R.string.pref_theme_choice_tmobile, R.color.main_background_tmobile, 
			R.color.main_foreground_tmobile, R.array.light_theme_colors, R.color.spinner_droptown_tmobile),

	;
	
	private int nameResource;
	private int backgroundColorResource;
	private int foregroundColorResource;
	private int spinnerColorResource;
	private int tagColorsResource;
	
	private int backgroundColor = -1;
	private int foregroundColor = -1;
	private int spinnerColor = -1;
	private int[] tagColors;
	
	private static Map<String, ColorScheme> preferenceNameToColorScheme = new HashMap<String, ColorScheme>();
	
	private ColorScheme(int nameResource, int backgroundColorResource, int foregroundColorResource,
			int tagColorsResource, int spinnerColorResource) {
		this.nameResource = nameResource;
		this.backgroundColorResource = backgroundColorResource;
		this.foregroundColorResource = foregroundColorResource;
		this.tagColorsResource = tagColorsResource;
		this.spinnerColorResource = spinnerColorResource;
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
	
	public int getSpinnerColor(Context context) {
		if (spinnerColor == -1) {
			spinnerColor = context.getResources().getColor(spinnerColorResource);
		}
		return spinnerColor;
	}
	
	public static ColorScheme findByPreferenceName(String name, Context context) {
		if (preferenceNameToColorScheme.isEmpty()) {
			// initialize map
			for (ColorScheme colorScheme : values()) {
				preferenceNameToColorScheme.put(context.getText(colorScheme.getNameResource()).toString(), colorScheme);
			}
		}
		return preferenceNameToColorScheme.get(name);
	}
}
