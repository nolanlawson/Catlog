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
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Filter.FilterListener;
import android.widget.TextView.OnEditorActionListener;

import com.nolanlawson.catlog.data.LogLine;
import com.nolanlawson.catlog.data.LogLineAdapter;
import com.nolanlawson.catlog.helper.SaveLogHelper;
import com.nolanlawson.catlog.util.UtilLogger;

public class CatlogActivity extends ListActivity implements TextWatcher, OnScrollListener, FilterListener, OnEditorActionListener {
	
	private static UtilLogger log = new UtilLogger(CatlogActivity.class);
	
	private EditText searchEditText;
	private LogLineAdapter adapter;
	private LogReaderAsyncTask task;
	
	private boolean autoscrollToBottom = true;
	
	private int logLevelLimit = 0;
	
	private int currentlyOpenLog = -1;
	
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
	    	
	    }
	    return false;
	}



	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		boolean showingMainLog = (task != null && !task.isCancelled());
		
		menu.findItem(R.id.menu_clear).setEnabled(showingMainLog);
		menu.findItem(R.id.menu_clear).setVisible(showingMainLog);
		
		menu.findItem(R.id.menu_main_log).setEnabled(!showingMainLog);
		menu.findItem(R.id.menu_main_log).setVisible(!showingMainLog);
		
		return super.onPrepareOptionsMenu(menu);
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
		Builder builder = new Builder(this);
		
		final EditText editText = new EditText(this);
		
		// create an initial filename to suggest to the user
		String filename = createLogFilename();
		editText.setText(filename);
		
		// highlight everything but the .txt at the end
		editText.setSelection(0, filename.length() - 4);
		
		builder.setTitle(R.string.save_log)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					
					int toastText;
					
					if (TextUtils.isEmpty(editText.getText()) || editText.getText().toString().contains("/")) {
						toastText = R.string.enter_good_filename;
					} else {
						
						boolean logSaved = saveLog(editText.getText().toString());
						
						toastText = logSaved ? R.string.log_saved : R.string.enter_good_filename;
					}
					
					Toast.makeText(CatlogActivity.this, toastText, Toast.LENGTH_SHORT).show();
					dialog.dismiss();
					
				}
			})
			.setView(editText);
		
		builder.show();
	}

	protected boolean saveLog(String filename) {
		List<String> logLines = getCurrentLogAsListOfStrings();
		
		return SaveLogHelper.saveLog(logLines, filename);
		
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
		String millisecond = twoDigitDecimalFormat.format(calendar.get(Calendar.MILLISECOND));
		
		StringBuilder stringBuilder = new StringBuilder();
		
		stringBuilder.append(year).append("-").append(month).append("-")
				.append(day).append("-").append(hour).append("-")
				.append(minute).append("-").append(second).append(millisecond);
		
		stringBuilder.append(".txt");
		
		return stringBuilder.toString();
	}

	private void showOpenLogDialog() {
		
		final List<String> filenames = SaveLogHelper.getLogFilenames();
		
		if (filenames.isEmpty()) {
			Toast.makeText(this, R.string.no_saved_logs, Toast.LENGTH_SHORT).show();
			return;
		}
		
		ArrayAdapter<CharSequence> dropdownAdapter = new ArrayAdapter<CharSequence>(
				this, R.layout.simple_spinner_dropdown_item_small, filenames.toArray(new CharSequence[filenames.size()]));
		
		Builder builder = new Builder(this);
		
		int logToSelect = (currentlyOpenLog >= 0 && currentlyOpenLog < filenames.size()) 
				? currentlyOpenLog : 0; 
		
		builder.setTitle(R.string.open_log)
			.setCancelable(true)
			.setSingleChoiceItems(dropdownAdapter, logToSelect, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					currentlyOpenLog = which;
					openLog(filenames.get(which));
					
				}
			});
		
		builder.show();
		
	}	
	private void openLog(String filename) {
		
		if (task != null && !task.isCancelled()) {
			task.cancel(true);
		}
		
		adapter.clear();
		
		resetFilter();
		
		List<String> logLines = SaveLogHelper.openLog(filename);
		
		for (String line : logLines) {
			adapter.add(LogLine.newLogLine(line));
		}
		// scroll to bottom
		getListView().setSelection(getListView().getCount());
		
	}
	
	private void resetFilter() {
		
		logLevelLimit = 0;
		
		// silently change edit text
		searchEditText.removeTextChangedListener(this);
		searchEditText.setText("");
		searchEditText.addTextChangedListener(this);
		
	}

	private void showLogLevelDialog() {
	
		Builder builder = new Builder(this);
		
		builder.setTitle(R.string.log_level)
			.setCancelable(true)
			.setSingleChoiceItems(R.array.log_levels, logLevelLimit, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				logLevelLimit = which;
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

				log.i("AsyncTask has died");

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
			
	    	currentlyOpenLog = -1;
	    	adapter.clear();
	    	resetFilter();

		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			//log.d("onProgressUpdate()");
			adapter.add(LogLine.newLogLine(values[0]));
			
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
		adapter.setLogLevelLimit(logLevelLimit);
		filter(searchEditText.getText());
	}
}