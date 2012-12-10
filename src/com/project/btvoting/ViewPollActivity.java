package com.project.btvoting;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewPollActivity extends Activity {

	static String TAG = "viewPoll";

	int numPolls;
	protected LinearLayout pollOptions;
	protected List<String> options;
	protected List<String> pollNames;
	protected String pollName;
	TextView topName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		options = new ArrayList<String>();
		pollNames = new ArrayList<String>();
		pollName = "";
		initLayout();
	}

	private void initLayout() {
		setContentView(R.layout.activity_view);
		pollOptions = (LinearLayout) findViewById(R.id.pollList);

		// load the list of poll names to view
		CreatePollActivity.loadArray(MainActivity.POLL_NAMES, pollNames,
				getBaseContext());

		Boolean pale = true;

		for (String name : pollNames) {
			LinearLayout box = new LinearLayout(getBaseContext());
			box.setOrientation(LinearLayout.VERTICAL);
			pollOptions.addView(box, LinearLayout.LayoutParams.WRAP_CONTENT);
			// load the poll options
			CreatePollActivity.loadArray(name, options, getBaseContext());
			TextView tv = new TextView(getBaseContext());
			tv.setTextColor(Color.BLACK);
			tv.setText(name);
			if (pale) {
				box.setBackgroundColor(Color.TRANSPARENT);
			} else {
				box.setBackgroundColor(Color.LTGRAY);
			}

			// add poll name
			box.addView(tv, LinearLayout.LayoutParams.WRAP_CONTENT);

			// add each option
			for (String o : options) {
				tv = new TextView(getBaseContext());
				tv.setTextColor(Color.BLACK);
				tv.setTypeface(null, Typeface.ITALIC);
				tv.setText("\t" + o);
				box.addView(tv, LinearLayout.LayoutParams.WRAP_CONTENT);
			}
			pale = !pale;
			box.setOnLongClickListener(new OnLongClickListener() {

				public boolean onLongClick(final View v) {
					TextView theNameTV = (TextView) ((LinearLayout) v)
							.getChildAt(0);
					final String name = theNameTV.getText().toString();
					AlertDialog.Builder ad = new AlertDialog.Builder(
							ViewPollActivity.this);
					ad.setTitle("Delete poll");
					ad.setMessage("Are you sure you want to delete the poll \""
							+ name + "\"?");
					ad.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// delete the poll
									// load the list of poll names
									CreatePollActivity.loadArray(
											MainActivity.POLL_NAMES, pollNames,
											getBaseContext());
									pollNames.remove(name);
									// save it back modified
									CreatePollActivity.saveArray(
											MainActivity.POLL_NAMES, pollNames,
											getBaseContext());
									// remove poll views
									pollOptions.removeView(v);
									// recolor the background
									Boolean light = true;
									for (int i = 0; i < pollOptions
											.getChildCount(); i++) {
										int color = Color.TRANSPARENT;
										if (!light) {
											color = Color.LTGRAY;
										}
										pollOptions.getChildAt(i)
												.setBackgroundColor(color);
										light = !light;
									}
								}
							});
					ad.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// do nothing
								}
							}).show();
					return false;
				}
			});
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_other, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.returnToMain:
			Intent myIntent = new Intent(ViewPollActivity.this,
					MainActivity.class);
			ViewPollActivity.this.startActivity(myIntent);
			return true;
		}
		return false;
	}

	public boolean saveArray(String key, List<String> options) {
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(
				MainActivity.POLL_OPTIONS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		// I don't think you need to remove first but oh well
		editor.remove(key + "Size"); // remove what might be there
		editor.putInt(key + "Size", options.size());

		for (int i = 0; i < options.size(); i++) {
			editor.remove(key + i); // remove what might be there
			editor.putString(key + i, options.get(i)); // put the right thing
		}

		return editor.commit();
	}

}
