package com.nolanlawson.logcat.reader;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.Context;

import com.nolanlawson.logcat.helper.LogcatHelper;
import com.nolanlawson.logcat.util.UtilLogger;

/**
 * Combines multipe buffered readers into a single reader that merges all input synchronously.
 * @author nolan
 *
 */
public class MultipleLogcatReader implements LogcatReader {
	
	private static UtilLogger log = new UtilLogger(MultipleLogcatReader.class);

	private static final String DUMMY_NULL = new String("");
	
	private List<ReaderThread> readerThreads = new LinkedList<ReaderThread>();
	private BlockingQueue<String> queue = new ArrayBlockingQueue<String>(1);
	
	public MultipleLogcatReader(Context context) throws IOException {
		
		// read from all three buffers at once
		for (String logBuffer : LogcatHelper.BUFFERS) {
			ReaderThread readerThread = new ReaderThread(logBuffer, context);
			readerThread.start();
			readerThreads.add(readerThread);
		}
	}

	public String readLine() throws IOException {
		
		try {
			String value = queue.take();
			if (value != DUMMY_NULL) {
				return value;
			}
		} catch (InterruptedException e) {
			log.d(e, "");
		}
		return null;
	}
	
	public void killQuietly() {
		for (ReaderThread thread : readerThreads) {
			thread.killed = true;
		}
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				for (ReaderThread thread : readerThreads) {
					thread.reader.killQuietly();
				}
			}
		}).start();
		
		try {
			queue.put(DUMMY_NULL);
		} catch (InterruptedException e) {
			log.d(e, "");
		}
	}
	
	private class ReaderThread extends Thread {

		SingleLogcatReader reader;
		
		private boolean killed;
		
		public ReaderThread(String logBuffer, Context context) throws IOException {
			this.reader = new SingleLogcatReader(logBuffer, context);
		}

		@Override
		public void run() {
			String line;
			
			try {
				while (!killed && (line = reader.readLine()) != null && !killed) {
					queue.put(line);
				}
			} catch (IOException e) {
				log.d(e, "exception");
			} catch (InterruptedException e) {
				log.d(e, "exception");
			}
			log.d("thread died");
		}
	}	
}
