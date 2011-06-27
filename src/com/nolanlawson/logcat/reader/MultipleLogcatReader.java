package com.nolanlawson.logcat.reader;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.nolanlawson.logcat.util.UtilLogger;

/**
 * Combines multipe buffered readers into a single reader that merges all input synchronously.
 * @author nolan
 *
 */
public class MultipleLogcatReader implements LogcatReader {

	private static UtilLogger log = new UtilLogger(MultipleLogcatReader.class);
	
	private List<SingleLogcatReader> readers = new LinkedList<SingleLogcatReader>();
	private BlockingQueue<String> queue = new ArrayBlockingQueue<String>(1);
	
	
	public MultipleLogcatReader(List<String> logBuffers) throws IOException {
		for (String logBuffer : logBuffers) {
			readers.add(new SingleLogcatReader(logBuffer));
		}
		
		for (SingleLogcatReader reader : readers) {
			ReaderThread readerThread = new ReaderThread(reader);
			readerThread.start();
		}
	}
	
	public String readLine() throws IOException {
		try {
			return queue.take();
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
	}
	
	
	
	private class ReaderThread extends Thread {

		private SingleLogcatReader reader;
		
		public ReaderThread(SingleLogcatReader reader) {
			this.reader = reader;
		}
		
		@Override
		public void run() {
			super.run();
			
			String line;
			
			try {
				while ((line = reader.readLine()) != null) {
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
