package com.nolanlawson.catlog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
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

import com.nolanlawson.catlog.data.LogLine;
import com.nolanlawson.catlog.data.LogLineAdapter;
import com.nolanlawson.catlog.helper.SaveLogHelper;
import com.nolanlawson.catlog.helper.ServiceHelper;
import com.nolanlawson.catlog.util.UtilLogger;

public class LogcatActivity extends ListActivity implements TextWatcher, OnScrollListener, FilterListener, OnEditorActionListener {
	
	private static UtilLogger log = new UtilLogger(LogcatActivity.class);
	
	private EditText searchEditText;
	private ProgressBar progressBar;
	private LogLineAdapter adapter;
	private LogReaderAsyncTask task;
	
	private boolean autoscrollToBottom = true;
	private boolean collapsedMode = true;
	
	private String currentlyOpenLog = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        setContentView(R.layout.main);
        
        setUpWidgets();
        
        setUpAdapter();
        
    	startUpMainLog();
        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
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
	    	startRecordingLog();
	    	return true;
	    case R.id.menu_stop_recording_log:
	    	stopRecordingLog();
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



	private void expandOrCollapseAll(boolean expanded) {
		
		collapsedMode = !expanded;
		
		for (LogLine logLine : adapter.getTrueValues()) {
			logLine.setExpanded(expanded);
		}
		
		adapter.notifyDataSetChanged();
		
		
	}
	
	private void startDeleteSavedLogsDialog() {
		
		if (!checkSdCard()) {
			return;
		}
		
		List<String> filenames = SaveLogHelper.getLogFilenames();
		
		if (filenames.isEmpty()) {
			Toast.makeText(this, R.string.no_saved_logs, Toast.LENGTH_SHORT).show();
			return;			
		}
		
		final CharSequence[] filenameArray = filenames.toArray(new CharSequence[filenames.size()]);
		final boolean[] checkedItems = new boolean[filenames.size()];
		
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
					boolean[] allChecked = new boolean[checkedItems.length];
					
					for (int i = 0; i < allChecked.length; i++) {
						allChecked[i] = true;
					}
					verifyDelete(filenameArray, allChecked, dialog);
					
				}
			})
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					verifyDelete(filenameArray, checkedItems, dialog);
					
				}
			})
			.setView(messageTextView)
			.setMultiChoiceItems(filenameArray, checkedItems, 
					new OnMultiChoiceClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					checkedItems[which] = isChecked;
					
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
	
	private boolean checkSdCard() {
		
		boolean result = SaveLogHelper.checkIfSdCardExists();
		
		if (!result) {
			Toast.makeText(getApplicationContext(), R.string.sd_card_not_found, Toast.LENGTH_LONG).show();
		}
		return result;
	}

	private void startAboutActivity() {
		
		Intent intent = new Intent(this,AboutActivity.class);
		
		startActivity(intent);
		
	}
	private void startRecordingLog() {
		
		if (!checkSdCard()) {
			return;
		}
		
		final EditText editText = createEditTextForFilenameSuggestingDialog();
		
		OnClickListener onClickListener = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				if (isInvalidFilename(editText.getText())) {
					
					Toast.makeText(LogcatActivity.this, R.string.enter_good_filename, Toast.LENGTH_SHORT).show();
				} else {
					
					String filename = editText.getText().toString();
					ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(getApplicationContext(), filename);
					
				}
				
				dialog.dismiss();
				
			}
		};		
		
		showFilenameSuggestingDialog(editText, onClickListener, R.string.record_log);
		
	}
	
	private void stopRecordingLog() {
		
		ServiceHelper.stopBackgroundServiceIfRunning(getApplicationContext());
		
	}
	private void sendLog() {
		
		String text = TextUtils.join("\n", getCurrentLogAsListOfStrings());
		
		Bundle extras = new Bundle();
		
		extras.putString(Intent.EXTRA_TEXT, text);
		
		Intent sendActionChooserIntent = new Intent(this, SendActionChooser.class);
		
		sendActionChooserIntent.putExtras(extras);
		startActivity(sendActionChooserIntent);
		
	}
	
	private List<String> getCurrentLogAsListOfStrings() {
		
		List<String> result = new ArrayList<String>();
		
		for (int i = 0; i < adapter.getCount(); i ++) {
			result.add(adapter.getItem(i).getOriginalLine());
		}
		
		return result;
	}

	private void showSaveLogDialog() {
		
		if (!checkSdCard()) {
			return;
		}
		
		final EditText editText = createEditTextForFilenameSuggestingDialog();
		
		OnClickListener onClickListener = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {

				
				if (isInvalidFilename(editText.getText())) {
					Toast.makeText(LogcatActivity.this, R.string.enter_good_filename, Toast.LENGTH_SHORT).show();
				} else {
					
					saveLog(editText.getText().toString());
				}
				
				
				dialog.dismiss();
				
			}
		};
		
		showFilenameSuggestingDialog(editText, onClickListener, R.string.save_log);
	}
	
	private boolean isInvalidFilename(CharSequence filename) {
		
		String filenameAsString = null;
		
		return TextUtils.isEmpty(filename)
				|| (filenameAsString = filename.toString()).contains("/")
				|| filenameAsString.contains(":")
				|| filenameAsString.contains(" ")
				|| !filenameAsString.endsWith(".txt");
				
	}

	private EditText createEditTextForFilenameSuggestingDialog() {
		
		final EditText editText = new EditText(this);
		editText.setSingleLine();
		editText.setSingleLine(true);
		editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
		editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				
				if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
					// dismiss soft keyboard
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
					return true;
				}
				
				
				return false;
			}
		});
		
		// create an initial filename to suggest to the user
		String filename = createLogFilename();
		editText.setText(filename);
		
		// highlight everything but the .txt at the end
		editText.setSelection(0, filename.length() - 4);
		
		return editText;
	}
	
	private void showFilenameSuggestingDialog(EditText editText, OnClickListener onClickListener, int titleResId) {
		
		Builder builder = new Builder(this);
		
		builder.setTitle(titleResId)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, onClickListener)
			.setMessage(R.string.enter_filename)
			.setView(editText);
		
		builder.show();
		
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

	private String createLogFilename() {
		Date date = new Date();
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		
		DecimalFormat twoDigitDecimalFormat = new DecimalFormat("00");
		DecimalFormat fourDigitDecimalFormat = new DecimalFormat("0000");
		
		String year = fourDigitDecimalFormat.format(calendar.get(Calendar.YEAR));
		String month = twoDigitDecimalFormat.format(calendar.get(Calendar.MONTH) + 1);
		String day = twoDigitDecimalFormat.format(calendar.get(Calendar.DAY_OF_MONTH));
		String hour = twoDigitDecimalFormat.format(calendar.get(Calendar.HOUR_OF_DAY));
		String minute = twoDigitDecimalFormat.format(calendar.get(Calendar.MINUTE));
		String second = twoDigitDecimalFormat.format(calendar.get(Calendar.SECOND));
		
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(year).append("-").append(month).append("-")
				.append(day).append("-").append(hour).append("-")
				.append(minute).append("-").append(second);
		
		stringBuilder.append(".txt");
		
		return stringBuilder.toString();
	}

	private void showOpenLogDialog() {
		
		if (!checkSdCard()) {
			return;
		}
		
		final List<String> filenames = SaveLogHelper.getLogFilenames();
		
		if (filenames.isEmpty()) {
			Toast.makeText(this, R.string.no_saved_logs, Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayAdapter<CharSequence> dropdownAdapter = new ArrayAdapter<CharSequence>(
				this, R.layout.simple_spinner_dropdown_item_small, filenames.toArray(new CharSequence[filenames.size()]));
		
		Builder builder = new Builder(this);
		
		int logToSelect = currentlyOpenLog != null ? filenames.indexOf(currentlyOpenLog) : 0;
		
		if (logToSelect == -1) {
			logToSelect = 0;
		}
		
		builder.setTitle(R.string.open_log)
			.setCancelable(true)
			.setSingleChoiceItems(dropdownAdapter, logToSelect, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					String filename = filenames.get(which);
					openLog(filename);
					
				}
			});
		
		builder.show();
		
	}	
	private void openLog(String filename) {
		
		if (task != null && !task.isCancelled()) {
			task.cancel(true);
		}
		
		resetDisplayedLog(filename);
		
		List<String> logLines = SaveLogHelper.openLog(filename);
		
		for (String line : logLines) {
			adapter.add(LogLine.newLogLine(line, !collapsedMode));
		}
		// scroll to bottom
		getListView().setSelection(getListView().getCount());
		
	}
	
	private void resetDisplayedLog(String filename) {
		
		adapter.clear();
		currentlyOpenLog = filename;
		collapsedMode = true;
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
			adapter.addWithFilter(LogLine.newLogLine(values[0], !collapsedMode), searchEditText.getText());
			
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

	private void filter(CharSequence filterText) {
		
		Filter filter = adapter.getFilter();

		filter.filter(filterText, this);
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

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