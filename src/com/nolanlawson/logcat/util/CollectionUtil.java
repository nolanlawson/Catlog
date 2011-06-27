package com.nolanlawson.logcat.util;

import java.util.Comparator;
import java.util.List;

public class CollectionUtil {

	public static <T> int binarySearch(List<T> collection, Comparator<T> comparator, T key) {
		
		int low = 0;
		int high = collection.size();
		
		while (low < high) {
			int mid = (low + high) / 2;
			T midValue = collection.get(mid);
			
			int comparison = comparator.compare(midValue, key);
			if (comparison < 0) {
				low = mid + 1;
			} else if (comparison > 0) {
				high = mid;
			} else {
				return mid;
			}
		}
		return low;
	}	
}
