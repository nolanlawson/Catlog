package com.nolanlawson.logcat.helper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import android.app.Activity;
import android.os.Build;
import android.provider.Settings;

public class BuildHelper {

	// public static final Strings of android.os.Build
	private static final List<String> BUILD_FIELDS = Arrays.asList(
			"BOARD", "BOOTLOADER", "BRAND", "CPU_ABI", "CPU_ABI2", 
			"DEVICE", "DISPLAY", "FINGERPRINT", "HARDWARE", "HOST", 
			"ID", "MANUFACTURER", "MODEL", "PRODUCT", "RADIO", 
			"SERIAL", "TAGS", "TIME", "TYPE", "USER");
	
	// public static final Strings of android.os.Build.Version
	private static final List<String> BUILD_VERSION_FIELDS = Arrays.asList(
			"CODENAME", "INCREMENTAL", "RELEASE", "SDK_INT");

   // public static final Strings of android.provider.Settings.Secure (String values only)
   private static final List<String> SETTINGS_SECURE_CONSTANTS = Arrays.asList(
         Settings.Secure.ANDROID_ID);

   public static String getBuildInformationAsString(Activity androidActivity) {
		SortedMap<String, String> keysToValues = new TreeMap<String, String>();
		
		for (String buildField : BUILD_FIELDS) {
			putKeyValue(Build.class, buildField, keysToValues);
		}
		for (String buildVersionField : BUILD_VERSION_FIELDS) {
			putKeyValue(Build.VERSION.class, buildVersionField, keysToValues);
		}
      for (String settingsSecureConstant : SETTINGS_SECURE_CONSTANTS) {
         putSettingsSecure(androidActivity, settingsSecureConstant, keysToValues);
      }

      StringBuilder stringBuilder = new StringBuilder();
		for (Entry<String, String> entry : keysToValues.entrySet()) {
			stringBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
		}
		return stringBuilder.toString();
	}

	private static void putKeyValue(Class<?> clazz, String fieldName, SortedMap<String, String> keysToValues) {
		try {
			Field field = clazz.getField(fieldName);
			Object value = field.get(null);
			String key = clazz.getSimpleName().toLowerCase() + "." + fieldName.toLowerCase();
			keysToValues.put(key, String.valueOf(value));
		} catch (SecurityException e) {
			// ignore
		} catch (NoSuchFieldException e) {
			// ignore
		} catch (IllegalAccessException e) {
			// ignore
		}
	}

   private static void putSettingsSecure(Activity androidActivity, String constantName,
                                         SortedMap<String, String> keysToValues) {
      try {
         Object value = Settings.Secure.getString(androidActivity.getContentResolver(), constantName);
         String key = "settings.secure" + "." + constantName.toLowerCase();
         keysToValues.put(key, String.valueOf(value));
      } catch (Exception e) {
         // ignore
      }
   }
}
