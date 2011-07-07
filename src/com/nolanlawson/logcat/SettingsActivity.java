package com.nolanlawson.logcat;

import java.util.Arrays;

import android.content.Intent;
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
	private ListPreference textSizePreference, themePreference, bufferPreference;
	
	private boolean bufferChanged = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
		setUpPreferences();
	}
	
	private void setUpPreferences() {
		
		logLinePeriodPreference = (EditTextPreference) findPreference(getString(R.string.pref_log_line_period));
		
		int logLinePrefValue = PreferenceHelper.getLogLinePeriodPreference(this);
		
		logLinePeriodPreference.setSummary(String.format(getString(R.string.pref_log_line_period_summary).toString(),
				logLinePrefValue, getString(R.string.pref_log_line_period_default)));
		
		logLinePeriodPreference.setOnPreferenceChangeListener(this);
		
		textSizePreference = (ListPreference) findPreference(getString(R.string.pref_text_size));
		textSizePreference.setSummary(textSizePreference.getEntry());
		textSizePreference.setOnPreferenceChangeListener(this);
		
		themePreference = (ListPreference) findPreference(getString(R.string.pref_theme));
		themePreference.setOnPreferenceChangeListener(this);
		
		bufferPreference = (ListPreference) findPreference(getString(R.string.pref_buffer));
		bufferPreference.setOnPreferenceChangeListener(this);
		int bufferIdx = Arrays.asList(bufferPreference.getEntryValues()).indexOf(bufferPreference.getValue());
		CharSequence bufferEntry = bufferPreference.getEntries()[bufferIdx];
		bufferPreference.setSummary(bufferEntry);
		
		boolean donateInstalled = DonateHelper.isDonateVersionInstalled(this) ;
		
		String themeSummary = donateInstalled 
				? PreferenceHelper.getColorScheme(this).getDisplayableName(this)
				: getString(R.string.pref_theme_summary_free);
		
		themePreference.setSummary(themeSummary);
		
		themePreference.setEnabled(donateInstalled);
		
	}
	
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		
		if (preference.getKey().equals(getString(R.string.pref_log_line_period))) {
			
			String input = ((String)newValue).trim();

			try {
				
				int value = Integer.parseInt(input);
				if (value >= MIN_LOG_LINE_PERIOD && value <= MAX_LOG_LINE_PERIOD) {
					PreferenceHelper.setLogLinePeriodPreference(this, value);
					logLinePeriodPreference.setSummary(String.format(getString(R.string.pref_log_line_period_summary).toString(),
							value, getString(R.string.pref_log_line_period_default)));
					return true;
				}
				
			} catch (NumberFormatException ignore) { }
			

			Toast.makeText(this, R.string.pref_log_line_period_error, Toast.LENGTH_LONG).show();
			return false;
			
		} else if (preference.getKey().equals(getString(R.string.pref_theme))) {
			// update summary
			int index = Arrays.asList(themePreference.getEntryValues()).indexOf(newValue.toString());
			CharSequence newEntry = themePreference.getEntries()[index];
			themePreference.setSummary(newEntry);
			return true;
		} else if (preference.getKey().equals(getString(R.string.pref_buffer))) {
			// update summary
			int index = Arrays.asList(bufferPreference.getEntryValues()).indexOf(newValue.toString());
			CharSequence newEntry = bufferPreference.getEntries()[index];
			bufferPreference.setSummary(newEntry);
			// notify the LogcatActivity that the buffer has changed
			if (!newValue.toString().equals(bufferPreference.getValue())) {
				bufferChanged = true;
			}
			return true;			
		} else { // text size pref
			
			int index = Arrays.asList(textSizePreference.getEntryValues()).indexOf(newValue);
			CharSequence newEntry = textSizePreference.getEntries()[index];
			
			textSizePreference.setSummary(newEntry);
			return true;
		}
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 ) {
	    	
	    	// set result and finish
	    	Intent data = new Intent();
	    	data.putExtra("bufferChanged", bufferChanged);
	    	setResult(RESULT_OK, data);
	    	finish();
	    	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
}
