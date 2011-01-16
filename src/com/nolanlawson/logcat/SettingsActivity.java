package com.nolanlawson.logcat;

import java.util.Arrays;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.KeyEvent;
import android.widget.Toast;

import com.nolanlawson.logcat.helper.DonateHelper;
import com.nolanlawson.logcat.helper.PreferenceHelper;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener{
	
	private static final int MAX_LOG_LINE_PERIOD = 1000;
	private static final int MIN_LOG_LINE_PERIOD = 1;
	
	private EditTextPreference logLinePeriodPreference;
	private ListPreference textSizePreference;
	private ListPreference themePreference;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		setUpPreferences();
	}
	
	private void setUpPreferences() {
		
		logLinePeriodPreference = (EditTextPreference) findPreference(getText(R.string.pref_log_line_period));
		
		int logLinePrefValue = PreferenceHelper.getLogLinePeriodPreference(this);
		
		logLinePeriodPreference.setSummary(String.format(getText(R.string.pref_log_line_period_summary).toString(),
				logLinePrefValue, getPluralSuffix(logLinePrefValue), getText(R.string.pref_log_line_period_default)));
		
		logLinePeriodPreference.setOnPreferenceChangeListener(this);
		
		textSizePreference = (ListPreference) findPreference(getText(R.string.pref_text_size));
		
		textSizePreference.setSummary(textSizePreference.getEntry());
		
		textSizePreference.setOnPreferenceChangeListener(this);
		
		themePreference = (ListPreference) findPreference(getText(R.string.pref_theme));
		
		themePreference.setOnPreferenceChangeListener(this);
		
		boolean donateInstalled = DonateHelper.isDonateVersionInstalled(this) ;
		
		String themeSummary = getText(donateInstalled 
				? PreferenceHelper.getColorScheme(this).getNameResource()
				: R.string.pref_theme_summary_free).toString();
		
		themeSummary = String.format(themeSummary, getText(PreferenceHelper.getColorScheme(this).getNameResource()));
		
		themePreference.setSummary(themeSummary);
		
		themePreference.setEnabled(donateInstalled);
		
	}
	
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		
		if (preference.getKey().equals(getText(R.string.pref_log_line_period))) {
			
			String input = ((String)newValue).trim();

			try {
				
				int value = Integer.parseInt(input);
				if (value >= MIN_LOG_LINE_PERIOD && value <= MAX_LOG_LINE_PERIOD) {
					PreferenceHelper.setLogLinePeriodPreference(this, value);
					logLinePeriodPreference.setSummary(String.format(getText(R.string.pref_log_line_period_summary).toString(),
							value, getPluralSuffix(value), getText(R.string.pref_log_line_period_default)));
					return true;
				}
				
			} catch (NumberFormatException ignore) { }
			

			Toast.makeText(this, R.string.pref_log_line_period_error, Toast.LENGTH_LONG).show();
			return false;
			
		} else if (preference.getKey().equals(getText(R.string.pref_theme))) {
			themePreference.setSummary(newValue.toString());
			return true;
			
		} else { // text size pref
			
			int index = Arrays.asList(textSizePreference.getEntryValues()).indexOf(newValue);
			CharSequence newEntry = textSizePreference.getEntries()[index];
			
			textSizePreference.setSummary(newEntry);
			return true;
		}
		
	}

	private Object getPluralSuffix(int value) {
		return value == 1 ? "" : "s";
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 ) {
	    	
	    	// set result and finish
	    	setResult(RESULT_OK);
	    	finish();
	    	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
}
