package com.nolanlawson.catlog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Filter.FilterListener;
import android.widget.TextView.OnEditorActionListener;

import com.nolanlawson.catlog.data.LogLine;
import com.nolanlawson.catlog.data.LogLineAdapter;
import com.nolanlawson.catlog.util.UtilLogger;

public class CatlogActivity extends ListActivity implements TextWatcher, OnScrollListener, FilterListener, OnEditorActionListener {
	
	private static UtilLogger log = new UtilLogger(CatlogActivity.class);
	
	private EditText searchEditText;
	private LogLineAdapter adapter;
	private LogReaderAsyncTask task;
	
	private boolean autoscrollToBottom = true;
	
	private int logLevelLimit = 0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        
        setContentView(R.layout.main);
        
        setUpWidgets();
        
        setUpAdapter();
        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
        adapter.clear();
    	
    	if (task != null && !task.isCancelled()) {
    		task.cancel(true);
    	}
    	
    	task = new LogReaderAsyncTask();
    	
    	task.execute((Void)null);
    	
    }
    
    @Override
    public void onPause() {
    	
    	super.onPause();
    	
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
	    }
	    return false;
	}




	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
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
				
				while ((line = reader.readLine()) != null) {
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
		autoscrollToBottom = firstVisibleItem + visibleItemCount == totalItemCount;
		
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