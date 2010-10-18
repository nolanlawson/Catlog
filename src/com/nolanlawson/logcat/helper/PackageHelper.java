package com.nolanlawson.logcat.helper;

import android.content.Context;

public class PackageHelper {

	public static boolean isCatlogDonateInstalled(Context context) {
		return context.getPackageManager().checkSignatures(
				context.getPackageName(), "com.nolanlawson.logcat.donate") >= 0;
	}
	
}
