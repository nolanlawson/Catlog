package com.nolanlawson.catlog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.nolanlawson.catlog.data.LogLine;
import com.nolanlawson.catlog.data.LogLineAdapter;
import com.nolanlawson.catlog.util.UtilLogger;

public class CatlogActivity extends ListActivity implements TextWatcher {
	
	private static UtilLogger log = new UtilLogger(CatlogActivity.class);
	
	private EditText searchEditText;
	private LogLineAdapter adapter;
	private LogReaderAsyncTask task;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setUpWidgets();
        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
        setUpAdapter();
    	
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



	private void setUpWidgets() {
		searchEditText = (EditText) findViewById(R.id.main_edit_text);
		searchEditText.addTextChangedListener(this);
		
	}
	
	private void setUpAdapter() {
		
		adapter = new LogLineAdapter(this, R.layout.logcat_list_item, new ArrayList<LogLine>());
		
		setListAdapter(adapter);
		
		
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
			
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			log.d("onCancelled()");

			
		}
		
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
		
		adapter.getFilter().filter(filterText);
		
	}	
}