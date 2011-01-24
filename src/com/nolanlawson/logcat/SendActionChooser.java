
package com.nolanlawson.logcat;
 
import java.util.Collections;
import java.util.List;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nolanlawson.logcat.helper.BitmapHelper;
import com.nolanlawson.logcat.util.UtilLogger;
 
public class SendActionChooser extends ListActivity {
	
	private static UtilLogger log = new UtilLogger(SendActionChooser.class);
	
	private AppAdapter adapter=null;
	private String subject = null;
	private String body = null;
	private Object filenameUri = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState)	{
		
		Bundle extras = getIntent().getExtras();
		
		if (extras.containsKey("title")) {
			setTitle(extras.getString("title"));
		} else {
			setTitle(R.string.send_log);
		}
		
		super.onCreate(savedInstanceState);

		Intent actionSendIntent= createIntent();

		PackageManager packageManager=getPackageManager();
 
		List<ResolveInfo> launchables=packageManager.queryIntentActivities(actionSendIntent, 0);
 		
		for (int i = launchables.size() - 1; i >=0 ; i--) {
			ResolveInfo launchable = launchables.get(i);
			ActivityInfo activity=launchable.activityInfo;
			
			if (activity.name.equals("com.facebook.katana.ShareLinkActivity")) { 
				// forget facebook - it doesn't work with anything but URLs
				launchables.remove(i);
				
			} 	
			log.d("activity name: %s", activity.name);
		} 
		Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(packageManager)); 
 		
		adapter = new AppAdapter(packageManager, launchables);
		setListAdapter(adapter);
		
		if (extras.containsKey(Intent.EXTRA_TEXT)) {
			body = extras.getString(Intent.EXTRA_TEXT);
		}
		
		subject = extras.getString(Intent.EXTRA_SUBJECT);
		
		if (extras.containsKey(Intent.EXTRA_STREAM)) {
			filenameUri = extras.getParcelable(Intent.EXTRA_STREAM);
			log.d("filename is %s", filenameUri);
		}
		
		if (body != null) {
			launchables.add(getDummyClipboardLaunchable());
		}
		
	}
 
	private ResolveInfo getDummyClipboardLaunchable() {
		return new ResolveInfo(){
			
			@Override
			public Drawable loadIcon(PackageManager pm) {
				return null;
			}

			@Override
			public CharSequence loadLabel(PackageManager pm) {
				return getResources().getString(R.string.copy_to_clipboard);
			}
			
		};
	}

	@Override
	protected void onListItemClick(ListView l, View v,
																 int position, long id) {
		ResolveInfo launchable=adapter.getItem(position);
		ActivityInfo activity=launchable.activityInfo;
		
		if (activity == null) {
			// dummy clipboard launchable
			ClipboardManager clipboard = 
			      (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
			
			 clipboard.setText(body);
			 Toast t = Toast.makeText(getApplicationContext(), 
					 getResources().getString(R.string.copied_to_clipboard), 
					 Toast.LENGTH_SHORT);
			 t.show();		
			 finish();
		} else {
		
			ComponentName name=new ComponentName(activity.applicationInfo.packageName,activity.name);
			
			Intent actionSendIntent= createIntent();
			actionSendIntent.setComponent(name);
	 
			startActivity(actionSendIntent);	
		}
	}
 
	private Intent createIntent() {
		Intent actionSendIntent=new Intent(android.content.Intent.ACTION_SEND);

		actionSendIntent.setType("text/plain");
		actionSendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
		
		if (body != null) {
			actionSendIntent.putExtra(Intent.EXTRA_TEXT, body);
		}
		
		if (filenameUri != null) {
			log.d("filename extra is %s", filenameUri);
			actionSendIntent.putExtra(Intent.EXTRA_STREAM, (Uri)filenameUri);
		}
		return actionSendIntent;
	}

	private class AppAdapter extends ArrayAdapter<ResolveInfo> {
		private PackageManager pm=null;
 
		AppAdapter(PackageManager pm, List<ResolveInfo> apps) {
			super(SendActionChooser.this, R.layout.chooser_row, apps);
			this.pm=pm;
		}
 
		@Override
		public View getView(int position, View convertView,
													ViewGroup parent) {
			if (convertView==null) {
				convertView=newView(parent);
			}
 
			bindView(position, convertView);
 
			return(convertView);
		}
 
		private View newView(ViewGroup parent) {
			return(getLayoutInflater().inflate(R.layout.chooser_row, parent, false));
		}
 
		private void bindView(int position, View row) {
			TextView label=(TextView)row.findViewById(R.id.label);
 
			label.setText(getItem(position).loadLabel(pm));
 
			ImageView iconImageView=(ImageView)row.findViewById(R.id.icon);
 
			Drawable drawableIcon = getItem(position).loadIcon(pm);
			
			if (drawableIcon != null) {
			
				Bitmap iconBitmap = BitmapHelper.convertIconToBitmap(getApplicationContext(), drawableIcon);
				
				iconImageView.setImageBitmap(iconBitmap);
				iconImageView.setVisibility(View.VISIBLE);
			
			} else {
				iconImageView.setVisibility(View.INVISIBLE);
			}
		}
	}

}