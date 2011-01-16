package com.nolanlawson.logcat.helper;

import android.content.Context;

public class DonateHelper {

	public static boolean isDonateVersionInstalled(Context context) {
		return context.getPackageManager().checkSignatures(
				context.getPackageName(), "com.nolanlawson.logcat.donate") >= 0;
	}
	
}
