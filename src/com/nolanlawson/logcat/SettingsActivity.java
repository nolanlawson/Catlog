package com.nolanlawson.logcat;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

import com.nolanlawson.logcat.helper.PreferenceHelper;

public class SettingsActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.settings);
		
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
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		PreferenceHelper.clearCache();
	}


	
	
}
