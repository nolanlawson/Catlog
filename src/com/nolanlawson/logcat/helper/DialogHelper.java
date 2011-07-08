package com.nolanlawson.logcat.helper;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.nolanlawson.logcat.R;

public class DialogHelper {
	
	public static void startRecordingWithProgressDialog(final String filename, final Runnable onPostExecute, final Context context) {
		
		final ProgressDialog progressDialog = new ProgressDialog(context);
		progressDialog.setTitle(context.getString(R.string.dialog_please_wait));
		progressDialog.setMessage(context.getString(R.string.dialog_initializing_recorder));
		
		new AsyncTask<Void, Void, Void>(){

			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				progressDialog.show();
			}

			@Override
			protected Void doInBackground(Void... params) {
				ServiceHelper.startBackgroundServiceIfNotAlreadyRunning(context, filename);
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				if (onPostExecute != null) {
					onPostExecute.run();
				}
			}
		}
		.execute((Void)null);
		
	}
	
	public static boolean isInvalidFilename(CharSequence filename) {
		
		String filenameAsString = null;
		
		return TextUtils.isEmpty(filename)
				|| (filenameAsString = filename.toString()).contains("/")
				|| filenameAsString.contains(":")
				|| filenameAsString.contains(" ")
				|| !filenameAsString.endsWith(".txt");
				
	}
	public static void startRecordingLog(final Context context) {
		
		if (!SaveLogHelper.checkSdCard(context)) {
			return;
		}
		
		final EditText editText = DialogHelper.createEditTextForFilenameSuggestingDialog(context);
		
		OnClickListener onClickListener = new OnClickListener() {
			
			@Override
			public void onClick(final DialogInterface dialog, int which) {
				
				if (DialogHelper.isInvalidFilename(editText.getText())) {
					
					Toast.makeText(context, R.string.enter_good_filename, Toast.LENGTH_SHORT).show();
				} else {
					
					String filename = editText.getText().toString();
					Runnable runnable = new Runnable(){

						@Override
						public void run() {
							dialog.dismiss();
						}
					};
					startRecordingWithProgressDialog(filename, runnable, context);
					
				}
				
			}
		};		
		
		DialogHelper.showFilenameSuggestingDialog(context, editText, onClickListener, null, R.string.record_log);
		
	}
	
	public static void stopRecordingLog(Context context) {
		
		ServiceHelper.stopBackgroundServiceIfRunning(context);
		
	}
	public static EditText createEditTextForFilenameSuggestingDialog(final Context context) {
		
		final EditText editText = new EditText(context);
		editText.setSingleLine();
		editText.setSingleLine(true);
		editText.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
		editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		editText.setOnEditorActionListener(new OnEditorActionListener() {
			
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				
				if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
					// dismiss soft keyboard
					InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
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
	
	
	
	public static void showFilenameSuggestingDialog(Context context, EditText editText, 
			OnClickListener onClickListener, OnClickListener onCancelListener, int titleResId) {
		
		Builder builder = new Builder(context);
		
		builder.setTitle(titleResId)
			.setCancelable(true)
			.setNegativeButton(android.R.string.cancel, onCancelListener)
			.setPositiveButton(android.R.string.ok, onClickListener)
			.setMessage(R.string.enter_filename)
			.setView(editText);
		
		builder.show();
		
	}

	private static String createLogFilename() {
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
	
}
