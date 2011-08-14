package com.nolanlawson.logcat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Filter.FilterListener;
import android.widget.TextView.OnEditorActionListener;

import com.nolanlawson.logcat.data.ColorScheme;
import com.nolanlawson.logcat.data.LogFileAdapter;
import com.nolanlawson.logcat.data.LogLine;
import com.nolanlawson.logcat.data.LogLineAdapter;
import com.nolanlawson.logcat.data.SortedFilterArrayAdapter;
import com.nolanlawson.logcat.data.TagAndProcessIdAdapter;
import com.nolanlawson.logcat.helper.DialogHelper;
import com.nolanlawson.logcat.helper.PreferenceHelper;
import com.nolanlawson.logcat.helper.ProcessHelper;
import com.nolanlawson.logcat.helper.SaveLogHelper;
import com.nolanlawson.logcat.helper.ServiceHelper;
import com.nolanlawson.logcat.helper.UpdateHelper;
import com.nolanlawson.logcat.helper.ProcessHelper.ProcessType;
import com.nolanlawson.logcat.reader.LogcatReader;
import com.nolanlawson.logcat.reader.LogcatReaderLoader;
import com.nolanlawson.logcat.util.LogLineAdapterUtil;
import com.nolanlawson.logcat.util.StringUtil;
import com.nolanlawson.logcat.util.UtilLogger;

public class LogcatActivity extends ListActivity implements TextWatcher, OnScrollListener, 
		FilterListener, OnEditorActionListener, OnItemLongClickListener, OnClickListener, OnLongClickListener {
	
	private static final int REQUEST_CODE_SETTINGS = 1;
	
	// maximum number of log lines to display before truncating.
	// this avoids OutOfMemoryErrors
	private static final int MAX_NUM_LOG_LINES = 1000;
	// how often to check to see if we've gone over the max size
	private static final int UPDATE_CHECK_INTERVAL = 200;
	
	private static UtilLogger log = new UtilLogger(LogcatActivity.class);
	
	private LinearLayout backgroundLinearLayout, mainFilenameLinearLayout;
	private AutoCompleteTextView searchEditText;
	private ProgressBar darkProgressBar, lightProgressBar;
	private LogLineAdapter adapter;
	private LogReaderAsyncTask task;
	private Button clearButton, expandButton, pauseButton;
	private TextView filenameTextView;
	private View borderView1, borderView2, borderView3, borderView4;
	
	private int firstVisibleItem = -1;
	private boolean autoscrollToBottom = true;
	private boolean collapsedMode;
	private boolean partialSelectMode;
	private List<LogLine> partiallySelectedLogLines = new ArrayList<LogLine>(2);
	private Set<String> searchSuggestionsSet = new HashSet<String>();
	private SortedFilterArrayAdapter<String> searchSuggestionsAdapter;
	
	private String currentlyOpenLog = null;
	
	private Handler handler = new Handler(Looper.getMainLooper());
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        setContentView(R.layout.main);
        
        collapsedMode = !PreferenceHelper.getExpandedByDefaultPreference(this);
        
        log.d("initial collapsed mode is %s", collapsedMode);
        
        UpdateHelper.runUpdatesIfNecessary(LogcatActivity.this);
        
        setUpWidgets();
        
        setUpAdapter();
        
        updateBackgroundColor();
        
        Intent intent = getIntent();
        
        if (intent == null || !intent.hasExtra("filename")) {
        	startUpMainLog();
        } else {
        	String filename = intent.getStringExtra("filename");
        	openLog(filename);
        }
        
        showInitialMessage();
        
    }
    
    
    
    
    private void showInitialMessage() {

		boolean isFirstRun = PreferenceHelper.getFirstRunPreference(getApplicationContext());
		if (isFirstRun) {
			
			View view = View.inflate(this, R.layout.intro_dialog, null);
			TextView textView = (TextView) view.findViewById(R.id.first_run_text_view_2);
			textView.setMovementMethod(LinkMovementMethod.getInstance());
			textView.setLinkTextColor(ColorStateList.valueOf(getResources().getColor(R.color.linkColorBlue)));
			new AlertDialog.Builder(this)
					.setTitle(R.string.first_run_title)
			        .setView(view)
			        .setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
	
							public void onClick(DialogInterface dialog, int which) {
								PreferenceHelper.setFirstRunPreference(getApplicationContext(), false);
								dialog.dismiss();
							}
						})
					.setCancelable(false)
			        .setIcon(R.drawable.icon).show();

		}

		
	}

	@Override
    public void onResume() {
    	super.onResume();
    	
    	if (getListView().getCount() > 0){
			// scroll to bottom, since for some reason it always scrolls to the top, which is annoying
			getListView().setSelection(getListView().getCount() - 1);
		}
    }
    
	private void restartMainLog() {
    	adapter.clear();
    	
    	startUpMainLog();
		
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		// launched from the widget or notification
        if (intent != null && intent.hasExtra("filename")) {
        	String filename = intent.getStringExtra("filename");
        	openLog(filename);
        }
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log.d("onActivityResult()");
		
		// preferences may have changed
		PreferenceHelper.clearCache();
		
		collapsedMode = !PreferenceHelper.getExpandedByDefaultPreference(getApplicationContext());
		

		if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
			handler.post(new Runnable(){

				@Override
				public void run() {
					
					updateBackgroundColor();
					
					if (data.hasExtra("bufferChanged") && data.getBooleanExtra("bufferChanged", false)
							&& currentlyOpenLog == null) {
						// log buffer changed, so update list
						restartMainLog();
					} else {
						// settings activity returned - text size might have changed, so update list
						expandOrCollapseAll(false);
						adapter.notifyDataSetChanged();
					}
				}
			});
		}
		adapter.notifyDataSetChanged();
		updateBackgroundColor();
		updateDisplayedFilename();
	}

	private void startUpMainLog() {
    	
    	if (task != null && !task.isCancelled()) {
    		task.cancel(true);
    	}
    	
    	task = new LogReaderAsyncTask();
    	
    	task.execute((Void)null);
		
	}

	@Override
    public void onPause() {
    	
    	super.onPause();
    	
    	cancelPartialSelect();
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	if (task != null && !task.isCancelled()) {
    		task.cancel(true);
    	}
    	ProcessHelper.killAll();
    }
    

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    switch (item.getItemId()) {
	    case R.id.menu_log_level:
	    	showLogLevelDialog();
	    	return true;
	    case R.id.menu_open_log:
	    	showOpenLogDialog();
	    	return true;
	    case R.id.menu_save_log:
	    case R.id.menu_save_as_log:
	    	showSaveLogDialog();
	    	return true;
	    case R.id.menu_record_log:
	    	DialogHelper.startRecordingLog(this);
	    	return true;
	    case R.id.menu_stop_recording_log:
	    	DialogHelper.stopRecordingLog(this);
	    	return true;	    	
	    case R.id.menu_send_log:
	    	sendLog();
	    	return true;
	    case R.id.menu_main_log:
	    	startUpMainLog();
	    	return true;
	    case R.id.menu_about:
	    	startAboutActivity();
	    	return true;
	    case R.id.menu_delete_saved_log:
	    	startDeleteSavedLogsDialog();
	    	return true;
	    case R.id.menu_settings:
	    	startSettingsActivity();
	    	return true;
	    case R.id.menu_crazy_logger_service:
	    	ServiceHelper.startOrStopCrazyLogger(this);
	    	return true;
	    case R.id.menu_partial_select:
	    	startPartialSelectMode();
	    	return true;
	    case R.id.menu_process_report:
	    	String report = String.format("Created: %d.  Killed: %d.",
	    			ProcessHelper.getProcessesCreated(), ProcessHelper.getProcessesKilled());
	    	Toast.makeText(this, report, Toast.LENGTH_LONG).show();
	    	return true;
	    }
	    return false;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		boolean showingMainLog = (task != null && !task.isCancelled());
		
		MenuItem mainLogMenuItem = menu.findItem(R.id.menu_main_log);
		MenuItem saveLogMenuItem = menu.findItem(R.id.menu_save_log);
		MenuItem saveAsLogMenuItem = menu.findItem(R.id.menu_save_as_log);
		
		mainLogMenuItem.setEnabled(!showingMainLog);
		mainLogMenuItem.setVisible(!showingMainLog);
		List<String> bufferNames = PreferenceHelper.getBufferNames(this);

		// change the displayed menu name depending on how many logs are to be shown
		String mainLogTitle;
		switch (bufferNames.size()) {
			case 1:
				mainLogTitle = String.format(getString(R.string.play_x1_log), bufferNames.get(0));
				break;
			case 2:
				mainLogTitle = String.format(getString(R.string.play_x2_log), 
						bufferNames.get(0), bufferNames.get(1));
				break;
			default: // 3
				mainLogTitle = getString(R.string.play_x3_log);				
				break;
				
		}

		mainLogMenuItem.setTitle(mainLogTitle);			
		
		saveLogMenuItem.setEnabled(showingMainLog);
		saveLogMenuItem.setVisible(showingMainLog);
		
		saveAsLogMenuItem.setEnabled(!showingMainLog);
		saveAsLogMenuItem.setVisible(!showingMainLog);
		
		boolean recordingInProgress = ServiceHelper.checkIfServiceIsRunning(getApplicationContext(), LogcatRecordingService.class);
	
		MenuItem recordMenuItem = menu.findItem(R.id.menu_record_log);
		MenuItem stopRecordingMenuItem = menu.findItem(R.id.menu_stop_recording_log);
		
		recordMenuItem.setEnabled(!recordingInProgress);
		recordMenuItem.setVisible(!recordingInProgress);
		
		stopRecordingMenuItem.setEnabled(recordingInProgress);
		stopRecordingMenuItem.setVisible(recordingInProgress);
		
		MenuItem crazyLoggerMenuItem = menu.findItem(R.id.menu_crazy_logger_service);
		crazyLoggerMenuItem.setEnabled(UtilLogger.DEBUG_MODE);
		crazyLoggerMenuItem.setVisible(UtilLogger.DEBUG_MODE);
		
		MenuItem processReportMenuItem = menu.findItem(R.id.menu_process_report);
		processReportMenuItem.setEnabled(UtilLogger.DEBUG_MODE);
		processReportMenuItem.setVisible(UtilLogger.DEBUG_MODE);
		
		
		MenuItem partialSelectMenuItem = menu.findItem(R.id.menu_partial_select);
		partialSelectMenuItem.setEnabled(!partialSelectMode);
		partialSelectMenuItem.setVisible(!partialSelectMode);
		
		return super.onPrepareOptionsMenu(menu);
	}

	private void startPartialSelectMode() {
		
		boolean hideHelp = PreferenceHelper.getHidePartialSelectHelpPreference(this);
		
		if (hideHelp) {
			partialSelectMode = true;
			partiallySelectedLogLines.clear();
			Toast.makeText(this, R.string.toast_started_select_partial, Toast.LENGTH_SHORT).show();
		} else {
		
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View helpView = inflater.inflate(R.layout.partial_select_help, null);
			// don't show the scroll bar
			helpView.setVerticalScrollBarEnabled(false);
			helpView.setHorizontalScrollBarEnabled(false);
			final CheckBox checkBox = (CheckBox) helpView.findViewById(android.R.id.checkbox);
			
			new AlertDialog.Builder(this)
				.setTitle(R.string.menu_title_partial_select)
				.setCancelable(true)
				.setView(helpView)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						partialSelectMode = true;
						partiallySelectedLogLines.clear();
						Toast.makeText(LogcatActivity.this, R.string.toast_started_select_partial, Toast.LENGTH_SHORT).show();
						
						if (checkBox.isChecked()) {
							// hide this help dialog in the future
							PreferenceHelper.setHidePartialSelectHelpPreference(LogcatActivity.this, true);
						}
						
						dialog.dismiss();
					}
				})
				.show();
		}
	}
	
	private void startSettingsActivity() {
		
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivityForResult(intent, REQUEST_CODE_SETTINGS);
	}

	private void expandOrCollapseAll(boolean change) {
		
		collapsedMode = change ? !collapsedMode : collapsedMode;
		
		int oldFirstVisibleItem = firstVisibleItem;
		
		for (LogLine logLine : adapter.getTrueValues()) {
			if (logLine != null) {
				logLine.setExpanded(!collapsedMode);
			}
		}
		
		expandButton.setCompoundDrawablesWithIntrinsicBounds(
				collapsedMode ? R.drawable.ic_menu_more_32 : R.drawable.ic_menu_less_32, 0, 0, 0);
		
		adapter.notifyDataSetChanged();
		
		// ensure that we either stay autoscrolling at the bottom of the list...
		
		if (autoscrollToBottom) {
			
			getListView().setSelection(getListView().getCount() - 1);
			
		// ... or that whatever was the previous first visible item is still the current first 
		// visible item after expanding/collapsing
			
		} else if (oldFirstVisibleItem != -1) {
			
			getListView().setSelection(oldFirstVisibleItem);
		}
		
		
	}

	
	private void startDeleteSavedLogsDialog() {
		
		if (!SaveLogHelper.checkSdCard(this)) {
			return;
		}
		
		List<CharSequence> filenames = new ArrayList<CharSequence>(SaveLogHelper.getLogFilenames());
		
		if (filenames.isEmpty()) {
			Toast.makeText(this, R.string.no_saved_logs, Toast.LENGTH_SHORT).show();
			return;			
		}
		
		final CharSequence[] filenameArray = filenames.toArray(new CharSequence[filenames.size()]);
		
		final LogFileAdapter dropdownAdapter = new LogFileAdapter(
				this, filenames, -1, true);
		
		final TextView messageTextView = new TextView(LogcatActivity.this);
		messageTextView.setText(R.string.select_logs_to_delete);
		messageTextView.setPadding(3, 3, 3, 3);
		
		Builder builder = new Builder(this);
		
		builder.setTitle(R.string.manage_saved_logs)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, null)
			.setNeutralButton(R.string.delete_all, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean[] allChecked = new boolean[dropdownAdapter.getCount()];
					
					for (int i = 0; i < allChecked.length; i++) {
						allChecked[i] = true;
					}
					verifyDelete(filenameArray, allChecked, dialog);
					
				}
			})
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					verifyDelete(filenameArray, dropdownAdapter.getCheckedItems(), dialog);
					
				}
			})
			.setView(messageTextView)
			.setSingleChoiceItems(dropdownAdapter, 0, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dropdownAdapter.checkOrUncheck(which);
					
				}
			});
		
		builder.show();
		
	}

	protected void verifyDelete(final CharSequence[] filenameArray,
			final boolean[] checkedItems, final DialogInterface parentDialog) {
		
		Builder builder = new Builder(this);
		
		int deleteCount = 0;
		
		for (int i = 0; i < checkedItems.length; i++) {
			if (checkedItems[i]) {
				deleteCount++;
			}
		}
		
		
		final int finalDeleteCount = deleteCount;
		
		if (finalDeleteCount > 0) {
			
			builder.setTitle(R.string.delete_saved_log)
				.setCancelable(true)
				.setMessage(String.format(getText(R.string.are_you_sure).toString(), finalDeleteCount))
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// ok, delete
					
					for (int i = 0; i < checkedItems.length; i++) {
						if (checkedItems[i]) {
							SaveLogHelper.deleteLogIfExists(filenameArray[i].toString());
						}
					}
					
					String toastText = String.format(getText(R.string.files_deleted).toString(), finalDeleteCount);
					Toast.makeText(LogcatActivity.this, toastText, Toast.LENGTH_SHORT).show();
					
					dialog.dismiss();
					parentDialog.dismiss();
					
				}
			});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.show();
		}
		
		
	}
	


	private void startAboutActivity() {
		
		Intent intent = new Intent(this,AboutActivity.class);
		
		startActivity(intent);
		
	}
	
	
	private void sendLog() {
		
		CharSequence[] items = new CharSequence[]{getText(R.string.as_text), getText(R.string.as_attachment)};
		
		new AlertDialog.Builder(this)
			.setTitle(R.string.choose_format)
			.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendLog(which == 0);
					dialog.dismiss();
				}
			})
			.show();
		
	}
	
	private void sendLog(final boolean asText) {

		if (!asText && currentlyOpenLog == null) { // no filename
			Toast.makeText(LogcatActivity.this, R.string.save_file_first, Toast.LENGTH_LONG).show();
		} else {

			String title = getString(asText ? R.string.send_as_text : R.string.send_as_attachment);
			final File attachment = asText ? null : SaveLogHelper.getFile(currentlyOpenLog);
			final String subject = getString(R.string.subject_log_report);
			final SenderAppAdapter adapter = new SenderAppAdapter(LogcatActivity.this, asText);

			new AlertDialog.Builder(LogcatActivity.this).setTitle(title).setCancelable(true)
					.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {

						public void onClick(final DialogInterface dialog, final int which) {
							
							final ProgressDialog getBodyProgressDialog = new ProgressDialog(LogcatActivity.this);
							getBodyProgressDialog.setCancelable(false);
							
							// do in the background to avoid jank
							AsyncTask<Void, Void, String> getBodyTask = new AsyncTask<Void, Void, String>() {
								
								@Override
								protected void onPreExecute() {
									super.onPreExecute();
									
									dialog.dismiss();
									
									if (asText) {
									
										getBodyProgressDialog.setTitle(R.string.dialog_please_wait);
										getBodyProgressDialog.setMessage(getString(R.string.dialog_compiling_log));
										getBodyProgressDialog.show();
									}
								}

								@Override
								protected String doInBackground(Void... params) {

									return asText ? getCurrentLogAsCharSequence().toString() : null;
								}

								@Override
								protected void onPostExecute(String body) {
									super.onPostExecute(body);

									adapter.respondToClick(which, subject, body, attachment);
									if (getBodyProgressDialog != null && getBodyProgressDialog.isShowing()) {
										getBodyProgressDialog.dismiss();
									}
								}
							};
							getBodyTask.execute((Void) null);
						}
					}).show();

		}
	}
	
	private List<CharSequence> getCurrentLogAsListOfStrings() {
		
		List<CharSequence> result = new ArrayList<CharSequence>(adapter.getCount());
		
		for (int i = 0; i < adapter.getCount(); i ++) {
			result.add(adapter.getItem(i).getOriginalLine());
		}
		
		return result;
	}
	
	private CharSequence getCurrentLogAsCharSequence() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < adapter.getCount(); i ++) {
			if (i > 0) {
				result.append('\n');
			}
			result.append(adapter.getItem(i).getOriginalLine());
		}
		return result;
	}

	private void showSaveLogDialog() {
		
		if (!SaveLogHelper.checkSdCard(this)) {
			return;
		}
		
		final EditText editText = DialogHelper.createEditTextForFilenameSuggestingDialog(this);
		
		DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				
				if (DialogHelper.isInvalidFilename(editText.getText())) {
					Toast.makeText(LogcatActivity.this, R.string.enter_good_filename, Toast.LENGTH_SHORT).show();
				} else {
					String filename = editText.getText().toString();
					saveLog(filename);
				}
				
				
				dialog.dismiss();
				
			}
		};
		
		DialogHelper.showFilenameSuggestingDialog(this, editText, onClickListener, null, R.string.save_log);
	}
	
	private void savePartialLog(final String filename, LogLine first, LogLine last) {
		
		final List<CharSequence> logLines = new ArrayList<CharSequence>(adapter.getCount());
		
		// filter based on first and last
		boolean started = false;
		boolean foundLast = false;
		for (int i = 0; i < adapter.getCount(); i ++) {
			LogLine logLine = adapter.getItem(i);
			if (logLine == first) {
				started = true;
			}
			if (started) {
				logLines.add(logLine.getOriginalLine());
			}
			if (logLine == last) {
				foundLast = true;
				break;
			}
		}
		
		if (!foundLast || logLines.isEmpty()) {
			Toast.makeText(this, R.string.toast_invalid_selection, Toast.LENGTH_LONG).show();
			cancelPartialSelect();
			return;
		}
		
		AsyncTask<Void,Void,Boolean> saveTask = new AsyncTask<Void, Void, Boolean>(){

			@Override
			protected Boolean doInBackground(Void... params) {
				SaveLogHelper.deleteLogIfExists(filename);
				return SaveLogHelper.saveLog(logLines, filename);
				
			}

			@Override
			protected void onPostExecute(Boolean successfullySavedLog) {
				
				super.onPostExecute(successfullySavedLog);
				
				if (successfullySavedLog) {
					Toast.makeText(getApplicationContext(), R.string.log_saved, Toast.LENGTH_SHORT).show();
					openLog(filename);
				} else {
					Toast.makeText(getApplicationContext(), R.string.unable_to_save_log, Toast.LENGTH_LONG).show();
				}
				
				cancelPartialSelect();
			}
			
			
		};
		
		saveTask.execute((Void)null);
		
	}

	private void saveLog(final String filename) {
		
		// do in background to avoid jankiness
		
		final List<CharSequence> logLines = getCurrentLogAsListOfStrings();
		
		AsyncTask<Void,Void,Boolean> saveTask = new AsyncTask<Void, Void, Boolean>(){

			@Override
			protected Boolean doInBackground(Void... params) {
				SaveLogHelper.deleteLogIfExists(filename);
				return SaveLogHelper.saveLog(logLines, filename);
				
			}

			@Override
			protected void onPostExecute(Boolean successfullySavedLog) {
				
				super.onPostExecute(successfullySavedLog);
				
				if (successfullySavedLog) {
					Toast.makeText(getApplicationContext(), R.string.log_saved, Toast.LENGTH_SHORT).show();
					openLog(filename);
				} else {
					Toast.makeText(getApplicationContext(), R.string.unable_to_save_log, Toast.LENGTH_LONG).show();
				}
			}
			
			
		};
		
		saveTask.execute((Void)null);
		
	}

	private void showOpenLogDialog() {
		
		if (!SaveLogHelper.checkSdCard(this)) {
			return;
		}
		
		final List<CharSequence> filenames = new ArrayList<CharSequence>(SaveLogHelper.getLogFilenames());
		
		if (filenames.isEmpty()) {
			Toast.makeText(this, R.string.no_saved_logs, Toast.LENGTH_SHORT).show();
			return;
		}
		

		
		int logToSelect = currentlyOpenLog != null ? filenames.indexOf(currentlyOpenLog) : -1;
		
		ArrayAdapter<CharSequence> dropdownAdapter = new LogFileAdapter(
				this, filenames, logToSelect, false);
		
		Builder builder = new Builder(this);
		
		builder.setTitle(R.string.open_log)
			.setCancelable(true)
			.setSingleChoiceItems(dropdownAdapter, logToSelect == -1 ? 0 : logToSelect, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					String filename = filenames.get(which).toString();
					openLog(filename);
					
				}
			});
		
		builder.show();
		
	}	
	private void openLog(final String filename) {
		
		if (task != null && !task.isCancelled()) {
			task.cancel(true);
			task = null;
		}
		
		// do in background to avoid jank
		
		AsyncTask<Void, Void, List<String>> openFileTask = new AsyncTask<Void, Void, List<String>>(){

			@Override
			protected List<String> doInBackground(Void... params) {

				List<String> logLines = SaveLogHelper.openLog(filename);
				return logLines;
			}

			@Override
			protected void onPostExecute(List<String> logLines) {
				super.onPostExecute(logLines);
				resetDisplayedLog(filename);
				hideProgressBar();
				
				for (String line : logLines) {
					LogLine logLine = LogLine.newLogLine(line, !collapsedMode);
					adapter.add(logLine);
					addToAutocompleteSuggestions(logLine);
					
				}
				
				// scroll to bottom
				getListView().setSelection(getListView().getCount() - 1);
			}
		};
		
		openFileTask.execute((Void)null);
		
	}
	
	private void hideProgressBar() {
		darkProgressBar.setVisibility(View.GONE);
		lightProgressBar.setVisibility(View.GONE);
	}
	
	private void showProgressBar() {
		ColorScheme colorScheme = PreferenceHelper.getColorScheme(LogcatActivity.this);
		darkProgressBar.setVisibility(colorScheme.isUseLightProgressBar() ? View.GONE : View.VISIBLE);
		lightProgressBar.setVisibility(colorScheme.isUseLightProgressBar() ? View.VISIBLE : View.GONE);
	}




	private void resetDisplayedLog(String filename) {
		
		adapter.clear();
		currentlyOpenLog = filename;
		collapsedMode = !PreferenceHelper.getExpandedByDefaultPreference(getApplicationContext());
		clearButton.setVisibility(filename == null? View.VISIBLE : View.GONE);
		pauseButton.setVisibility(filename == null? View.VISIBLE : View.GONE);
		pauseButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause, 0, 0, 0);
		expandButton.setCompoundDrawablesWithIntrinsicBounds(
				collapsedMode ? R.drawable.ic_menu_more_32 : R.drawable.ic_menu_less_32, 0, 0, 0);
		searchSuggestionsAdapter.clear();
		searchSuggestionsSet.clear();
		
		resetFilter();
		updateDisplayedFilename();
		
	}

	private void updateDisplayedFilename() {
		mainFilenameLinearLayout.setVisibility(currentlyOpenLog != null ? View.VISIBLE : View.GONE);
		if (currentlyOpenLog != null) {
			
			filenameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, PreferenceHelper.getTextSizePreference(this) + 2);
			ColorScheme colorScheme = PreferenceHelper.getColorScheme(this);
			borderView1.setBackgroundColor(colorScheme.getForegroundColor(this));
			borderView2.setBackgroundColor(colorScheme.getForegroundColor(this));
			borderView3.setBackgroundColor(colorScheme.getForegroundColor(this));
			borderView4.setBackgroundColor(colorScheme.getForegroundColor(this));
			filenameTextView.setTextColor(colorScheme.getForegroundColor(this));
			filenameTextView.setBackgroundColor(colorScheme.getBubbleBackgroundColor(this));
			filenameTextView.setText(currentlyOpenLog);
		}
		
	}

	private void resetFilter() {

		adapter.setLogLevelLimit(0);
		
		// silently change edit text
		searchEditText.removeTextChangedListener(this);
		searchEditText.setText("");
		searchEditText.addTextChangedListener(this);
		
	}

	private void showLogLevelDialog() {
	
		Builder builder = new Builder(this);
		
		builder.setTitle(R.string.log_level)
			.setCancelable(true)
			.setSingleChoiceItems(R.array.log_levels, adapter.getLogLevelLimit(), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				adapter.setLogLevelLimit(which);
				logLevelChanged();
				dialog.dismiss();
				
			}
		});
		
		builder.show();
	}
	
	private void setUpWidgets() {
		
		searchEditText = (AutoCompleteTextView) findViewById(R.id.main_edit_text);
		searchEditText.addTextChangedListener(this);
		searchEditText.setOnEditorActionListener(this);
		searchEditText.setOnClickListener(this);
		
		searchSuggestionsAdapter = new SortedFilterArrayAdapter<String>(
				this, R.layout.simple_list_item_small, new ArrayList<String>());
		searchEditText.setAdapter(searchSuggestionsAdapter);
		
		darkProgressBar = (ProgressBar) findViewById(R.id.main_dark_progress_bar);
		lightProgressBar = (ProgressBar) findViewById(R.id.main_light_progress_bar);
		
		backgroundLinearLayout = (LinearLayout) findViewById(R.id.main_background);
		
		clearButton = (Button) findViewById(R.id.main_clear_button);
		expandButton = (Button) findViewById(R.id.main_more_button);
		pauseButton = (Button) findViewById(R.id.main_pause_button);
		
		for (Button button : new Button[]{clearButton, expandButton, pauseButton}) {
			button.setOnClickListener(this);
		}
		clearButton.setOnLongClickListener(this);
		
		filenameTextView = (TextView) findViewById(R.id.main_filename_text_view);
		mainFilenameLinearLayout = (LinearLayout) findViewById(R.id.main_filename_linear_layout);
		borderView1 = findViewById(R.id.main_border_view_1);
		borderView2 = findViewById(R.id.main_border_view_2);
		borderView3 = findViewById(R.id.main_border_view_3);
		borderView4 = findViewById(R.id.main_border_view_4);
		
	}
	
	private void setUpAdapter() {
		
		adapter = new LogLineAdapter(this, R.layout.logcat_list_item, new ArrayList<LogLine>());
		
		setListAdapter(adapter);
		
		getListView().setOnScrollListener(this);
		getListView().setOnItemLongClickListener(this);
		
		
	}	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		LogLine logLine = adapter.getItem(position);
		
		if (partialSelectMode) {

			logLine.setHighlighted(true);
			
			partiallySelectedLogLines.add(logLine);

			handler.post(new Runnable() {

				@Override
				public void run() {
	
					adapter.notifyDataSetChanged();
				}
			});
			

			if (partiallySelectedLogLines.size() == 2) {
				// last line
				handler.post(new Runnable() {

					@Override
					public void run() {
		
						completePartialSelect();
					}
				});
			}
			
		
		} else {
			
			logLine.setExpanded(!logLine.isExpanded());
			adapter.notifyDataSetChanged();			
		}
		
	}

	private void completePartialSelect() {

		if (!SaveLogHelper.checkSdCard(this)) {
			cancelPartialSelect();
			return;
		}
		
		final EditText editText = DialogHelper.createEditTextForFilenameSuggestingDialog(this);
		
		DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				
				if (DialogHelper.isInvalidFilename(editText.getText())) {
					cancelPartialSelect();
					Toast.makeText(LogcatActivity.this, R.string.enter_good_filename, Toast.LENGTH_SHORT).show();
					
				} else {
					String filename = editText.getText().toString();
					savePartialLog(filename, partiallySelectedLogLines.get(0), partiallySelectedLogLines.get(1));
				}
				
				dialog.dismiss();
				
			}
		};
		
		
		DialogInterface.OnClickListener onCancelListener = new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				cancelPartialSelect();
			}
		};
		
		DialogHelper.showFilenameSuggestingDialog(this, editText, onClickListener, onCancelListener, R.string.save_log);
		
	}
	
	private void cancelPartialSelect() {
		partialSelectMode = false;
		
		boolean changed = false;
		for (LogLine logLine : partiallySelectedLogLines) {
			if (logLine.isHighlighted()) {
				logLine.setHighlighted(false);
				changed = true;
			}
		}
		partiallySelectedLogLines.clear();
		if (changed) {
			handler.post(new Runnable() {
				
				@Override
				public void run() {
					adapter.notifyDataSetChanged();
				}
			});
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
		
		final LogLine logLine = adapter.getItem(position);
		
		if (logLine.getProcessId() == -1) {
			// invalid line
			return false;
		}
		
		List<CharSequence> choices = Arrays.<CharSequence>asList(getResources().getStringArray(R.array.filter_choices));
		List<CharSequence> choicesSubtexts = Arrays.<CharSequence>asList(logLine.getTag(), Integer.toString(logLine.getProcessId()));
		
		int tagColor = LogLineAdapterUtil.getOrCreateTagColor(this, logLine.getTag());
		
		TagAndProcessIdAdapter textAndSubtextAdapter = new TagAndProcessIdAdapter(this, choices, choicesSubtexts, tagColor, -1);
		
		
		new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle(R.string.filter_choice)
			.setIcon(R.drawable.ic_search_category_default)
	        .setSingleChoiceItems(textAndSubtextAdapter, -1, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {

					if (which == 0) { // tag
						searchEditText.setText(logLine.getTag());
					} else { // which == 1, i.e. process id
						searchEditText.setText(Integer.toString(logLine.getProcessId()));
					}
					
					// put the cursor at the end
					searchEditText.setSelection(searchEditText.length());
					dialog.dismiss();
					
				}
			}).create().show();
		
		
		
		return true;
	}
	
	

	@Override
	public void afterTextChanged(Editable s) {
		// do nothing
		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// do nothing
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
		CharSequence filterText = searchEditText.getText();
		
		log.d("filtering: %s", filterText);
		
		filter(filterText);
		
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		
	    if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0 ) {
	    	
	    	// show keyboard
	    	
	    	searchEditText.requestFocus();
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(searchEditText, 0);
			
	    	return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	private void filter(CharSequence filterText) {
		
		Filter filter = adapter.getFilter();

		filter.filter(filterText, this);
		
	}

	@Override
	public boolean onLongClick(View v) {
		// clear button long-pressed, undo clear
		startUpMainLog();
		return true;
		
	}
	
	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.main_edit_text:
				if (searchEditText.length() > 0) {
					// I think it's intuitive to click an edit text and have all the text selected
					searchEditText.setSelection(0, searchEditText.length());
				}
				break;
			case R.id.main_clear_button:
				searchEditText.setText("");
				adapter.clear();
				Toast.makeText(this, R.string.log_cleared, Toast.LENGTH_LONG).show();
				break;
			case R.id.main_more_button:
				expandOrCollapseAll(true);
				break;
			case R.id.main_pause_button:
				pauseOrUnpause();
				break;
		}
		
	}
	
	private void pauseOrUnpause() {
		if (task.isPaused()) {
			task.unpause();
		} else {
			task.pause();
		}
		
		pauseButton.setCompoundDrawablesWithIntrinsicBounds(
				task.isPaused() ? R.drawable.ic_media_play : R.drawable.ic_media_pause, 0, 0, 0);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		// update what the first viewable item is
		this.firstVisibleItem = firstVisibleItem;
		
		// if the bottom of the list isn't visible anymore, then stop autoscrolling
		autoscrollToBottom = (firstVisibleItem + visibleItemCount == totalItemCount);
		
		// only hide the fast scroll if we're unpaused and at the bottom of the list
		boolean enableFastScroll = task == null || task.isPaused() || !autoscrollToBottom;
		getListView().setFastScrollEnabled(enableFastScroll);
		
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// do nothing
		
	}

	@Override
	public void onFilterComplete(int count) {
		// always scroll to the bottom when searching
		getListView().setSelection(count);
		
	}	
	
	private void dismissSoftKeyboard() {
		log.d("dismissSoftKeyboard()");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

	}	

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		
		log.d("actionId: " + actionId+" event:" + event);
		
		if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
			dismissSoftKeyboard();
			return true;
		}
		
		
		return false;
	}	
	
	private void logLevelChanged() {
		filter(searchEditText.getText());
	}
	
	private void updateBackgroundColor() {
		ColorScheme colorScheme = PreferenceHelper.getColorScheme(this);

		final int color = colorScheme.getBackgroundColor(LogcatActivity.this);
		
		handler.post(new Runnable() {
			public void run() {
				backgroundLinearLayout.setBackgroundColor(color);
			}
		});
		
		getListView().setCacheColorHint(color);
		getListView().setDivider(new ColorDrawable(color));
		
	}
	

	private void addToAutocompleteSuggestions(LogLine logLine) {
		// add the tags to the autocompletetextview
		
		if (!StringUtil.isEmptyOrWhitespaceOnly(logLine.getTag())) {
			String trimmed = logLine.getTag().trim();
			if (!searchSuggestionsSet.contains(trimmed)) {
				searchSuggestionsSet.add(trimmed);
				searchSuggestionsAdapter.add(trimmed);
			}
		}
	}
	
	private class LogReaderAsyncTask extends AsyncTask<Void,String,Void> {
		
		private int counter = 0;
		private volatile boolean paused;
		private final Object lock = new Object();
		
		@Override
		protected Void doInBackground(Void... params) {
			log.d("doInBackground()");
			
			LogcatReader reader = null;
			
			try {
						
				LogcatReaderLoader loader = LogcatReaderLoader.create(LogcatActivity.this);
				reader = loader.loadReader(ProcessType.Main);

				String line;
				
				while ((line = reader.readLine()) != null && !isCancelled()) {
					if (paused) {
						synchronized (lock) {
							if (paused) {
								lock.wait();
							}
						}
					}
					publishProgress(line);
				} 
			} catch (InterruptedException e) {
				log.d(e, "expected error");
			} catch (Exception e) {
				log.d(e, "unexpected error");
			} finally {
				if (reader != null) {
					reader.killQuietly();
				}

				log.d("AsyncTask has died");
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			log.d("onPostExecute()");
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			log.d("onPreExecute()");
			
			resetDisplayedLog(null);
			
			showProgressBar();
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);

			String line = values[0];
			
			hideProgressBar();
			
			LogLine logLine = LogLine.newLogLine(line, !collapsedMode);
			adapter.addWithFilter(logLine, searchEditText.getText());
			addToAutocompleteSuggestions(logLine);
			
			// check to see if the list needs to be truncated to avoid out of memory errors
			if (++counter % UPDATE_CHECK_INTERVAL == 0 
					&& adapter.getTrueValues().size() > MAX_NUM_LOG_LINES) {
				int numItemsToRemove = adapter.getTrueValues().size() - MAX_NUM_LOG_LINES;
				adapter.removeFirst(numItemsToRemove);
				log.d("truncating %d lines from log list to avoid out of memory errors", numItemsToRemove);
			}
			
			if (autoscrollToBottom) {
				getListView().setSelection(getListView().getCount());
			}
			
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			log.d("onCancelled()");
			if (paused) {
				unpause();
			}
		}
		
		public void pause() {
			synchronized (lock) {
				paused = true;
			}
		}
		
		public void unpause() {
			synchronized (lock) {
				paused = false;
				lock.notify();
			}
		}
		
		public boolean isPaused() {
			return paused;
		}
	}
}