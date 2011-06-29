package com.nolanlawson.logcat.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;

import com.nolanlawson.logcat.R;

public class LogcatHelper {

	public static Process getLogcatProcess(String buffer, Context context) throws IOException {
		
		List<String> args = new ArrayList<String>(Arrays.asList("logcat", "-v", "time"));
		
		// for some reason, adding -b main excludes log output from AndroidRuntime runtime exceptions,
		// whereas just leaving it blank keeps them in.  So do not specify the buffer if it is "main"
		if (!buffer.equals(context.getString(R.string.pref_buffer_choice_main_value))) {
			args.add("-b");
			args.add(buffer);
		}
		
		return Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
	}
}
