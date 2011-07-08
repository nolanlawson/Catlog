package com.nolanlawson.logcat.reader;

import java.io.IOException;

public interface LogcatReader {

	/**
	 * Read a single log line, ala BufferedReader.readLine().
	 * @return
	 * @throws IOException
	 */
	public String readLine() throws IOException;
	
	/**
	 * Close without throwing any exceptions.
	 */
	public void closeQuietly();
	
	/**
	 * Kill the reader.
	 */
	public void kill();
}
