package com.nolanlawson.logcat.data;

import android.view.View;
import android.widget.TextView;

import com.nolanlawson.logcat.R;

/**
 * Improves performance of the ListView.  Watch Romain Guy's video about ListView to learn more.
 * @author nlawson
 *
 */
public class LogLineViewWrapper {

	private View view;
	private TextView levelTextView;
	private TextView outputTextView;
	private TextView tagTextView;
	
	public LogLineViewWrapper(View view) {
		this.view = view;
	}

	
	public TextView getTagTextView() {
		if (tagTextView == null) {
			tagTextView = (TextView) view.findViewById(R.id.tag_text);
		}
		return tagTextView;
	}	
	
	public TextView getLevelTextView() {
		if (levelTextView == null) {
			levelTextView = (TextView) view.findViewById(R.id.log_level_text);
		}
		return levelTextView;
	}
	
	public TextView getOutputTextView() {
		if (outputTextView == null) {
			outputTextView = (TextView) view.findViewById(R.id.log_output_text);
		}
		return outputTextView;
	}
}
