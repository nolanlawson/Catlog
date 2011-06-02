package com.nolanlawson.logcat.helper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.nolanlawson.logcat.R;
import com.nolanlawson.logcat.util.UtilLogger;

public class SaveLogHelper {

	private static UtilLogger log = new UtilLogger(SaveLogHelper.class);
	
	public static boolean checkSdCard(Context context) {
		
		boolean result = SaveLogHelper.checkIfSdCardExists();
		
		if (!result) {
			Toast.makeText(context, R.string.sd_card_not_found, Toast.LENGTH_LONG).show();
		}
		return result;
	}
	public static boolean checkIfSdCardExists() {
		
		File sdcardDir = Environment.getExternalStorageDirectory();
			
		return sdcardDir != null && sdcardDir.listFiles() != null;
		
	}
	
	public static File getFile(String filename) {
		
		File catlogDir = getCatlogDirectory();
		
		File file = new File(catlogDir, filename);
	
		return file;
	}
	
	public static void deleteLogIfExists(String filename) {
		
		File catlogDir = getCatlogDirectory();
		
		File file = new File(catlogDir, filename);
		
		if (file.exists()) {
			file.delete();
		}
		
	}
	
	public static Date getLastModifiedDate(String filename) {
		
		File catlogDir = getCatlogDirectory();
		
		File file = new File(catlogDir, filename);
		
		if (file.exists()) {
			return new Date(file.lastModified());
		} else {
			// shouldn't happen
			log.e("file last modified date not found: %s", filename);
			return new Date();
		}
	}
	
	/**
	 * Get all the log filenames, order by last modified descending
	 * @return
	 */
	public static List<String> getLogFilenames() {
		
		File catlogDir = getCatlogDirectory();
		
		File[] filesArray = catlogDir.listFiles();
		
		if (filesArray == null) {
			return Collections.emptyList();
		}
		
		List<File> files = new ArrayList<File>(Arrays.asList(filesArray));
		
		Collections.sort(files, new Comparator<File>(){

			@Override
			public int compare(File object1, File object2) {
				return new Long(object2.lastModified()).compareTo(object1.lastModified());
			}});
		
		List<String> result = new ArrayList<String>();
		
		for (File file : files) {
			result.add(file.getName());
		}
		
		return result;
		
	}
	
	public static List<String> openLog(String filename) {
		
		File catlogDir = getCatlogDirectory();
		File logFile = new File(catlogDir, filename);	
		
		List<String> result = new ArrayList<String>();
		
		BufferedReader bufferedReader = null;
		
		try {
			
			bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)), 8192);
			
			while (bufferedReader.ready()) {
				result.add(bufferedReader.readLine());
			}
		} catch (IOException ex) {
			log.e(ex, "couldn't read file");
			
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					log.e(e, "couldn't close buffered reader");
				}
			}
		}
		
		return result;
	}
	
	public static synchronized boolean saveLog(CharSequence logString, String filename) {
		return saveLog(null, logString, filename);
	}
	
	public static synchronized boolean saveLog(List<CharSequence> logLines, String filename) {
		return saveLog(logLines, null, filename);
	}
	
	private static boolean saveLog(List<CharSequence> logLines, CharSequence logString, String filename) {
		
		File catlogDir = getCatlogDirectory();
		
		File newFile = new File(catlogDir, filename);
		try {
			if (!newFile.exists()) {
				newFile.createNewFile();
			}
		} catch (IOException ex) {
			log.e(ex, "couldn't create new file");
			return false;
		}
		PrintStream out = null;
		try {
			// specifying 8192 gets rid of an annoying warning message
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream(newFile, true), 8192));
			
			// save a log as either a list of strings or as a charsequence
			if (logLines != null) {
				for (CharSequence line : logLines) {
					out.println(line);
				}				
			} else if (logString != null) {
				out.print(logString);
			}
			
			
		} catch (FileNotFoundException ex) {
			log.e(ex,"unexpected exception");
			return false;
		} finally {
			if (out != null) {
				out.close();
			}
		}
		
		return true;
		
		
	}
	
	private static File getCatlogDirectory() {
		File sdcardDir = Environment.getExternalStorageDirectory();
		
		File catlogDir = new File(sdcardDir, "catlog_saved_logs");
		
		if (!catlogDir.exists()) {
			catlogDir.mkdir();
		}
		
		return catlogDir;
		
	}
	
}
