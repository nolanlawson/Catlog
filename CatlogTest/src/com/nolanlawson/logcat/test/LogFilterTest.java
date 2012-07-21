package com.nolanlawson.logcat.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.test.ActivityInstrumentationTestCase2;

import com.nolanlawson.logcat.LogcatActivity;
import com.nolanlawson.logcat.data.LogLine;
import com.nolanlawson.logcat.data.SearchCriteria;

public class LogFilterTest extends
	ActivityInstrumentationTestCase2<LogcatActivity> {

    private static final List<String> TEST_LOG_LINES = Arrays.asList(
	    "07-21 21:34:09.900 D/dalvikvm(  257): GC_CONCURRENT freed 385K, 50% free 3190K/6279K, external 0K/0K, paused 2ms+2ms",
	    "07-21 21:34:10.050 D/Vold    (  114): USB connected",
	    "07-21 21:34:10.050 D/Vold    (  114): Share method ums now available",
	    "07-21 21:34:10.050 I/StorageNotification(  244): UMS connection changed to true (media state mounted)",
	    "07-21 21:34:10.080 D/Vold    (  114): USB connected",
	    "07-21 21:34:10.090 D/Tethering(  167): sendTetherStateChangedBroadcast 1, 0, 0",
	    "07-21 21:34:10.090 D/Tethering(  167): interfaceAdded :usb0",
	    "07-21 21:34:10.100 D/BluetoothNetworkService(  167): updating tether state",
	    "07-21 21:34:10.100 D/BluetoothNetworkService(  167): interface usb0",
	    "07-21 21:34:10.110 W/Tethering(  167): active iface (usb0) reported as added, ignoring");
    
    public LogFilterTest() {
	super("com.nolanlawson.logcat", LogcatActivity.class);
    }

    public void testFilterBasic() {
	testFilter("dalvik", 1);
	testFilter("tethering", 3);
	testFilter("114", 3);
	testFilter("usb connected", 2);
	testFilter("connected usb", 0);
	testFilter("pid:167", 5);
	testFilter("tag:Vold", 3);
    }
    
    private void testFilter(String text, int expectedLogLines) {
	SearchCriteria criteria = new SearchCriteria(text);
	List<String> matches = new ArrayList<String>();
	for (String line : TEST_LOG_LINES) {
	    if (criteria.matches(LogLine.newLogLine(line, false))) {
		matches.add(line);
	    }
	}
	assertEquals(expectedLogLines, matches.size());
    }

}
