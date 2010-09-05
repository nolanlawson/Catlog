package com.nolanlawson.logcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Filter.FilterListener;
import android.widget.TextView.OnEditorActionListener;

import com.nolanlawson.logcat.data.LogFileAdapter;
import com.nolanlawson.logcat.data.LogLine;
import com.nolanlawson.logcat.data.LogLineAdapter;
import com.nolanlawson.logcat.helper.DialogHelper;
import com.nolanlawson.logcat.helper.PreferenceHelper;
import com.nolanlawson.logcat.helper.SaveLogHelper;
import com.nolanlawson.logcat.helper.ServiceHelper;
import com.nolanlawson.logcat.util.UtilLogger;

public class LogcatActivity extends ListActivity implements TextWatcher, OnScrollListener, FilterListener, OnEditorActionListener {
	
	private static final int REQUEST_CODE_SETTINGS = 1;
	
	// maximum number of log lines to display bofore truncating.
	// this avoids OutOfMemoryErrors
	private static final int MAX_NUM_LOG_LINES = 1000;
	// how often to check to see if we've gone over the max size
	private static final int UPDATE_CHECK_INTERVAL = 30;
	
	private static UtilLogger log = new UtilLogger(LogcatActivity.class);
	
	private EditText searchEditText;
	private ProgressBar progressBar;
	private LogLineAdapter adapter;
	private LogReaderAsyncTask task;
	
	private int firstVisibleItem = -1;
	private boolean autoscrollToBottom = true;
	private boolean collapsedMode;
	
	private String currentlyOpenLog = null;
	
	private Handler handler = new Handler(Looper.getMainLooper());
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        setContentView(R.layout.main);
        
        collapsedMode = !PreferenceHelper.getExpandedByDefaultPreference(this);
        
        log.d("initial collapsed mode is %s", collapsedMode);
        
        setUpWidgets();
        
        setUpAdapter();
        
        Intent intent = getIntent();
        
        if (intent == null || !intent.hasExtra("filename")) {
        	startUpMainLog();
        } else {
        	String filename = intent.getStringExtra("filename");
        	openLog(filename);
        }
        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log.d("onActivityResult()");
		
		collapsedMode = !PreferenceHelper.getExpandedByDefaultPreference(getApplicationContext());
		
		// settings activity returned - text size might have changed, so update list
		if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
			handler.post(new Runnable(){

				@Override
				public void run() {
					
					expandOrCollapseAll(!collapsedMode);
					
					adapter.notifyDataSetChanged();
					
				}
			});
		}
		adapter.notifyDataSetChanged();
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

    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	if (task != null && !task.isCancelled()) {
    		task.cancel(true);
    	}
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
	    	showSaveLogDialog();
	    	return true;
	    case R.id.menu_record_log:
	    	DialogHelper.startRecordingLog(this);
	    	return true;
	    case R.id.menu_stop_recording_log:
	    	DialogHelper.stopRecordingLog(this);
	    	return true;	    	
	    case R.id.menu_clear:
	    	adapter.clear();
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
	    case R.id.menu_expand_all:
	    	expandOrCollapseAll(true);
	    	return true;
	    case R.id.menu_collapse_all:
	    	expandOrCollapseAll(false);
	    	return true;
	    case R.id.menu_settings:
	    	startSettingsActivity();
	    	return true;
	    	
	    }
	    return false;
	}


	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		boolean showingMainLog = (task != null && !task.isCancelled());
		
		MenuItem clearMenuItem = menu.findItem(R.id.menu_clear);
		MenuItem mainLogMenuItem = menu.findItem(R.id.menu_main_log);
		
		clearMenuItem.setEnabled(showingMainLog);
		clearMenuItem.setVisible(showingMainLog);
		
		mainLogMenuItem.setEnabled(!showingMainLog);
		mainLogMenuItem.setVisible(!showingMainLog);
		
		boolean recordingInProgress = ServiceHelper.checkIfServiceIsRunning(getApplicationContext());
	
		MenuItem recordMenuItem = menu.findItem(R.id.menu_record_log);
		MenuItem stopRecordingMenuItem = menu.findItem(R.id.menu_stop_recording_log);
		
		recordMenuItem.setEnabled(!recordingInProgress);
		recordMenuItem.setVisible(!recordingInProgress);
		
		stopRecordingMenuItem.setEnabled(recordingInProgress);
		stopRecordingMenuItem.setVisible(recordingInProgress);
		
		MenuItem expandAllMenuItem = menu.findItem(R.id.menu_expand_all);
		MenuItem collapseAllMenuItem = menu.findItem(R.id.menu_collapse_all);
		
		expandAllMenuItem.setEnabled(collapsedMode);
		expandAllMenuItem.setVisible(collapsedMode);
		
		collapseAllMenuItem.setEnabled(!collapsedMode);
		collapseAllMenuItem.setVisible(!collapsedMode);
		
		
		return super.onPrepareOptionsMenu(menu);
	}


	private void startSettingsActivity() {
		
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivityForResult(intent, REQUEST_CODE_SETTINGS);
	}

	private void expandOrCollapseAll(boolean expanded) {
		
		collapsedMode = !expanded;
		
		int oldFirstVisibleItem = firstVisibleItem;
		
		for (LogLine logLine : adapter.getTrueValues()) {
			logLine.setExpanded(expanded);
		}
		
		adapter.notifyDataSetChanged();
		
		// ensure that we either stay autoscrolling at the bottom of the list...
		
		if (autoscrollToBottom) {
			
			getListView().setSelection(getListView().getCount());
			
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
			.setNeutralButton(R.string.delete_all, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean[] allChecked = new boolean[dropdownAdapter.getCount()];
					
					for (int i = 0; i < allChecked.length; i++) {
						allChecked[i] = true;
					}
					verifyDelete(filenameArray, allChecked, dialog);
					
				}
			})
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					verifyDelete(filenameArray, dropdownAdapter.getCheckedItems(), dialog);
					
				}
			})
			.setView(messageTextView)
			.setSingleChoiceItems(dropdownAdapter, 0, new OnClickListener() {
				
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
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// ok, delete
					
					for (int i = 0; i < checkedItems.length; i++) {
						if (checkedItems[i]) {
							SaveLogHelper.deleteLog(filenameArray[i].toString());
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
			.setSingleChoiceItems(items, 0, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendLog(which == 0);
					dialog.dismiss();
				}
			})
			.show();
		
	}
	
	private void sendLog(final boolean asBody) {
		
		// do in the background to avoid jank
		AsyncTask<Void,Void,String> task = new AsyncTask<Void, Void, String>(){

			@Override
			protected String doInBackground(Void... params) {
				
				if (asBody) {
					String text = TextUtils.join("\n", getCurrentLogAsListOfStrings());
					return text;
				} else {
					return currentlyOpenLog;
				}
			}

			@Override
			protected void onPostExecute(String textOrFilename) {
				
				super.onPostExecute(textOrFilename);

				if (!asBody && textOrFilename == null) { // no filename
					Toast.makeText(LogcatActivity.this, R.string.save_file_first, Toast.LENGTH_LONG).show();
				} else {
					
					Bundle extras = new Bundle();
					
					if (asBody) {
						extras.putString(Intent.EXTRA_TEXT, textOrFilename);
					} else { // as attachment
						
						File file = SaveLogHelper.getFile(textOrFilename);
						Uri uri = Uri.fromFile(file);
						extras.putParcelable(Intent.EXTRA_STREAM, uri);
						
						log.d("uri is %s", uri);
						log.d("file is %s", file);
						
					}
					
					Intent sendActionChooserIntent = new Intent(LogcatActivity.this, SendActionChooser.class);
					
					sendActionChooserIntent.putExtras(extras);
					startActivity(sendActionChooserIntent);
				
				}
			}
			
		};
		
		task.execute((Void)null);
			
	}
	
	private List<String> getCurrentLogAsListOfStrings() {
		
		List<String> result = new ArrayList<String>();
		
		for (int i = 0; i < adapter.getCount(); i ++) {
			result.add(adapter.getItem(i).getOriginalLine());
		}
		
		return result;
	}

	private void showSaveLogDialog() {
		
		if (!SaveLogHelper.checkSdCard(this)) {
			return;
		}
		
		final EditText editText = DialogHelper.createEditTextForFilenameSuggestingDialog(this);
		
		OnClickListener onClickListener = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				
				if (DialogHelper.isInvalidFilename(editText.getText())) {
					Toast.makeText(LogcatActivity.this, R.string.enter_good_filename, Toast.LENGTH_SHORT).show();
				} else {
					
					saveLog(editText.getText().toString());
				}
				
				
				dialog.dismiss();
				
			}
		};
		
		DialogHelper.showFilenameSuggestingDialog(this, editText, onClickListener, R.string.save_log);
	}
	


	private void saveLog(final String filename) {
		
		// do in background to avoid jankiness
		
		final List<String> logLines = getCurrentLogAsListOfStrings();
		
		AsyncTask<Void,Void,Boolean> saveTask = new AsyncTask<Void, Void, Boolean>(){

			@Override
			protected Boolean doInBackground(Void... params) {
				return SaveLogHelper.saveLog(logLines, filename);
				
			}

			@Override
			protected void onPostExecute(Boolean successfullySavedLog) {
				
				super.onPostExecute(successfullySavedLog);
				
				if (successfullySavedLog) {
					Toast.makeText(getApplicationContext(), R.string.log_saved, Toast.LENGTH_SHORT).show();
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
			.setSingleChoiceItems(dropdownAdapter, logToSelect == -1 ? 0 : logToSelect, new OnClickListener() {
				
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
				progressBar.setVisibility(View.GONE);
				
				for (String line : logLines) {
					adapter.add(LogLine.newLogLine(line, !collapsedMode));
				}
				
				// scroll to bottom
				getListView().setSelection(getListView().getCount());
				
				
			}
		};
		
		openFileTask.execute((Void)null);
		
	}
	
	private void resetDisplayedLog(String filename) {
		
		adapter.clear();
		currentlyOpenLog = filename;
		collapsedMode = !PreferenceHelper.getExpandedByDefaultPreference(getApplicationContext());
		resetFilter();
		
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
			.setSingleChoiceItems(R.array.log_levels, adapter.getLogLevelLimit(), new OnClickListener() {
			
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
		
		searchEditText = (EditText) findViewById(R.id.main_edit_text);
		searchEditText.addTextChangedListener(this);
		searchEditText.setOnEditorActionListener(this);
		
		progressBar = (ProgressBar) findViewById(R.id.main_progress_bar);
		progressBar.setVisibility(View.VISIBLE);
		
		
	}
	
	private void setUpAdapter() {
		
		adapter = new LogLineAdapter(this, R.layout.logcat_list_item, new ArrayList<LogLine>());
		
		setListAdapter(adapter);
		
		getListView().setOnScrollListener(this);
		
		
	}	
	
	private class LogReaderAsyncTask extends AsyncTask<Void,String,Void> {
		
		private int counter = 0;
		
		@Override
		protected Void doInBackground(Void... params) {
			log.d("doInBackground()");
			
			Process logcatProcess = null;
			BufferedReader reader = null;
			
			try {

				logcatProcess = Runtime.getRuntime().exec(
						new String[] { "logcat"});

				reader = new BufferedReader(new InputStreamReader(logcatProcess
						.getInputStream()));
				
				String line;
				
				while ((line = reader.readLine()) != null && !isCancelled()) {
					publishProgress(line);
					
				} 
			} catch (Exception e) {
				log.e(e, "unexpected error");
				
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						log.e(e, "unexpected exception");
					}
				}
				
				if (logcatProcess != null) {
					logcatProcess.destroy();
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

		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);

			progressBar.setVisibility(View.GONE);
			//if (new Random().nextBoolean()) log.d("collapsed mode is %s", collapsedMode);
			adapter.addWithFilter(LogLine.newLogLine(values[0], !collapsedMode), searchEditText.getText());
			
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
			

			
		}
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		LogLine logLine = adapter.getItem(position);
		
		logLine.setExpanded(!logLine.isExpanded());
		adapter.notifyDataSetChanged();
		
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
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		// update what the first viewable item is
		this.firstVisibleItem = firstVisibleItem;
		
		// if the bottom of the list isn't visible anymore, then stop autoscrolling
		autoscrollToBottom = (firstVisibleItem + visibleItemCount == totalItemCount);
		
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
}