package com.nolanlawson.logcat.reader;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

import android.text.TextUtils;

import com.nolanlawson.logcat.data.LogLine;
import com.nolanlawson.logcat.helper.LogHelper;
import com.nolanlawson.logcat.util.UtilLogger;

/**
 * Combines multipe buffered readers into a single reader that merges all input synchronously.
 * @author nolan
 *
 */
public class MultipleLogcatReader implements LogcatReader {

	private static final DatedLogLine DUMMY_NULL = new DatedLogLine("");
	
	private static UtilLogger log = new UtilLogger(MultipleLogcatReader.class);
	
	private List<SingleLogcatReader> readers = new LinkedList<SingleLogcatReader>();
	private PriorityBlockingQueue<DatedLogLine> queue = new PriorityBlockingQueue<DatedLogLine>();
	
	public MultipleLogcatReader() throws IOException {
		
		// read from all three buffers at once
		for (String logBuffer : LogHelper.LOG_BUFFERS) {
			readers.add(new SingleLogcatReader(logBuffer));
		}
		
		for (SingleLogcatReader reader : readers) {
			ReaderThread readerThread = new ReaderThread(reader);
			readerThread.start();
		}
	}
	
	public String readLine() throws IOException {
		try {
			DatedLogLine result = queue.take();
			return result == DUMMY_NULL ? null : result.getLine();
		} catch (InterruptedException e) {
			throw new IOException("reader exception", e);
		}
	}
	
	public void closeQuietly() {
		for (SingleLogcatReader reader : readers) {
			reader.closeQuietly();
		}
	}
	
	public void kill() {
		for (SingleLogcatReader reader : readers) {
			reader.kill();
		}
		
		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				// queue does not accept null values, so have to use a dummy value
				queue.put(DUMMY_NULL);

			}
		};
		new Thread(runnable).start();
	}
	
	
	
	private class ReaderThread extends Thread {

		private SingleLogcatReader reader;
		private DatedLogLine skipPastLogLine;
		private boolean doneSkipping;
		
		public ReaderThread(SingleLogcatReader reader) {
			this.reader = reader;
			init();
		}
		
		private void init() {
			
			// when initializing, need to dump the entire log so that we can properly
			// interleave logs that have already been written
			
			List<String> dumpedLogLines = LogHelper.dumpLog(reader.getLogBuffer());
			
			for (String line : dumpedLogLines) {
				queue.put(new DatedLogLine(line));

			}
			
			if (!dumpedLogLines.isEmpty()) {
				skipPastLogLine = new DatedLogLine(dumpedLogLines.get(dumpedLogLines.size() - 1));
			}
		}

		@Override
		public void run() {
			super.run();
			
			String line;
			
			try {
				while ((line = reader.readLine()) != null) {
					DatedLogLine datedLogLine = new DatedLogLine(line);
					
					// TODO: this logic will not work if the skipPastLogLine has the same timestamp as nearby lines
					// it will skip those lines unnecessarily
					if (doneSkipping || skipPastLogLine == null || datedLogLine.compareTo(skipPastLogLine) > 0) {
						doneSkipping = true;
						queue.put(datedLogLine);
					}
				}
			} catch (IOException e) {
				log.d(e, "exception");
			}
			log.d("thread died");
		}
	}
	
	/**
	 * Need to sort log lines by earliest first, so that when we first read in all the buffers we can
	 * interleave them correctly
	 * @author nolan
	 *
	 */
	private static class DatedLogLine implements Comparable<DatedLogLine>{
		
		private static final SimpleDateFormat dateFormat = new SimpleDateFormat(LogLine.LOGCAT_DATE_FORMAT);
		
		private Date date;
		private String line;
		
		public DatedLogLine(String line) {
			this.line = line;
			LogLine logLine = LogLine.newLogLine(line, false);
			if (!TextUtils.isEmpty(logLine.getTimestamp())) {
				synchronized (dateFormat) {
					// DateFormats are not threadsafe
					try {
						date = dateFormat.parse(logLine.getTimestamp());
					} catch (ParseException e) {
						log.d(e, "exception");
					}
				}
			}
		}

		public String getLine() {
			return line;
		}

		@Override
		public int compareTo(DatedLogLine another) {
			
			if (this == DUMMY_NULL) {
				return 1;
			} else if (another == DUMMY_NULL) {
				return -1;
			} else if (date == null && another.date == null) {
				return 0;
			} else if (another.date == null) {
				return 1;
			} else if (date == null) {
				return -1;
			} else {
				return date.compareTo(another.date);
			}
		}
	}
}
