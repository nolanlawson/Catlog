package com.nolanlawson.logcat.reader;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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

	private static final long MIN_INDIVIDUAL_WAIT_TIME = 10;
	private static final long STARTING_INDIVIDUAL_WAIT_TIME = 200;
	private static final double INDIVIDUAL_WAIT_TIME_DECREMENT = 2;
	private static final long LOOP_WAIT_TIME = 0;
	
	private boolean killed;
	private PriorityQueue<String> recentLogsBuffer = new PriorityQueue<String>(16, LogcatHelper.getLogComparator());
	private List<ReaderThread> readerThreads = new LinkedList<ReaderThread>();
	
	public MultipleLogcatReader(Context context) throws IOException {
		
		// read from all three buffers at once
		for (String logBuffer : LogcatHelper.BUFFERS) {
			ReaderThread readerThread = new ReaderThread(logBuffer, context);
			readerThread.start();
			readerThreads.add(readerThread);
		}
	}

	public String readLine() throws IOException {
		// sort everything in the queue by string comparison, because due to the logcat format, that
		// will ensure that the most recent entries end up on top
		while (true) {
			for (ReaderThread thread : readerThreads) {
				try {
					String line = thread.queue.poll(thread.waitTime, TimeUnit.MILLISECONDS);
					if (line != null) {
						recentLogsBuffer.add(line);
					} else {
						// this buffer made me wait, so we'll be less tolerant next time; cut the wait time by half
						thread.decreaseWaitTime();
					}
				} catch (InterruptedException e) {
					log.d(e, "unexpected exception");
				}
			}
			
			if (recentLogsBuffer.isEmpty()) {
				// keep polling for data from the readers
				try {
					Thread.sleep(LOOP_WAIT_TIME);
				} catch (InterruptedException e) {
					log.d(e, "unexpected exception");
				}
			} else {
				return recentLogsBuffer.poll();
			}
			if (killed) {
				return null;
			}
		}
	}
	
	public void killQuietly() {
		killed = true;
		new Thread(new Runnable(){

			@Override
			public void run() {
				for (ReaderThread thread : readerThreads) {
					thread.reader.killQuietly();
					thread.killed = true;
				}
				
			}
		}).start();
	}
	
	private class ReaderThread extends Thread {

		SingleLogcatReader reader;
		BlockingQueue<String> queue = new ArrayBlockingQueue<String>(1);
		private long waitTime = STARTING_INDIVIDUAL_WAIT_TIME;
		private boolean killed;
		
		public ReaderThread(String logBuffer, Context context) throws IOException {
			this.reader = new SingleLogcatReader(logBuffer, context);
		}

		public void decreaseWaitTime() {
			waitTime = Math.max(
					MIN_INDIVIDUAL_WAIT_TIME, 
					new Double(waitTime / INDIVIDUAL_WAIT_TIME_DECREMENT).longValue());
			
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
