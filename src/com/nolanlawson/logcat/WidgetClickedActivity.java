package com.nolanlawson.logcat;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nolanlawson.logcat.helper.DialogHelper;
import com.nolanlawson.logcat.helper.ServiceHelper;

public class WidgetClickedActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        
		setContentView(R.layout.record_log_dialog);
		
		// ask the user the save a file to record the log
		
		final EditText editText = DialogHelper.createEditTextForFilenameSuggestingDialog(this);
		
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.record_dialog_linear_layout);
		
		// insert the edit text below the prompt text
		linearLayout.addView(editText, 2);
		
		Button okButton = (Button) findViewById(android.R.id.button1);
		Button cancelButton = (Button) findViewById(android.R.id.button2);
		
		okButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (DialogHelper.isInvalidFilename(editText.getText())) {
					
					Toast.makeText(WidgetClickedActivity.this, R.string.enter_good_filename, Toast.LENGTH_SHORT).show();
				} else {
					
					String filename = editText.getText().toString();
					ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(WidgetClickedActivity.this, filename);
					
				}
				finish();
			}
		});
		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
				
			}
		});
	}

	
	
}
