package com.project.btvoting;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CreatePollActivity extends Activity {

	static String TAG = "createPoll";
	static String POLLS = "polls";
	int numPolls;

	protected Button addNew;
	protected Button createPoll;
	protected LinearLayout pollOptions;
	protected EditText editPollName;
	protected EditText editOptionName;
	protected List<String> options;
	protected List<String> pollNames;
	protected String pollName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		options = new ArrayList<String>();
		pollNames = new ArrayList<String>();
		pollName = "";
		initLayout();
		initButtonListeners();

	}

	private void initLayout() {
		setContentView(R.layout.activity_create);
		addNew = (Button) findViewById(R.id.addPollOption);
		createPoll = (Button) findViewById(R.id.createPoll2);
		pollOptions = (LinearLayout) findViewById(R.id.pollOptions);
		editOptionName = (EditText) findViewById(R.id.editOptionName);
		editPollName = (EditText) findViewById(R.id.editPollName);
	}

	protected void initButtonListeners() {
		createPoll.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// get the list of existing poll names
				loadArray(MainActivity.POLL_NAMES, pollNames, getBaseContext());
				Log.d(TAG, "pollNames is length " + pollNames.size());
				// if the name isn't long enough, reject it
				pollName = editPollName.getText().toString();
				if (pollName.length() < 1) {
					Toast.makeText(getBaseContext(), "Missing poll name!",
							Toast.LENGTH_SHORT).show();
					// if there aren't at least two options, reject
				} else if (options.size() < 2) {
					Toast.makeText(getBaseContext(),
							"Need at least two poll options!",
							Toast.LENGTH_SHORT).show();
					// if the name is in use, reject
				} else if (pollNames.contains(pollName)) {
					Toast.makeText(getBaseContext(),
							"That poll name is already in use!",
							Toast.LENGTH_SHORT).show();
				} else {
					Log.d("CreatePoll", "pollName is " + pollName
							+ " and there are " + options.size() + " options");
					// persist the poll
					saveArray(pollName, options, getBaseContext());
					pollNames.add(pollName);
					Log.d(TAG, "pollNames after creation is length "
							+ pollNames.size());
					// persist the list of polls
					Boolean saved = saveArray(MainActivity.POLL_NAMES,
							pollNames, getBaseContext());
					if (saved) {
						options.clear();
						Toast.makeText(getBaseContext(),
								"Poll \"" + pollName + "\" created!",
								Toast.LENGTH_SHORT).show();
						// clear all the poll boxes
						editPollName.setText("");
						pollOptions.removeViews(0, pollOptions.getChildCount());
					} else {
						Toast.makeText(getBaseContext(),
								"Error while saving poll", Toast.LENGTH_SHORT)
								.show();
					}
				}
			}
		});
		addNew.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Editable text = editOptionName.getText();
				// only do it if an option has been entered
				if (text.length() > 0) {
					options.add(text.toString());
					TextView tv = new TextView(getBaseContext());
					tv.setText(options.size() + ".\t" + text);
					tv.setTextColor(Color.BLACK);
					pollOptions.addView(tv,
							LinearLayout.LayoutParams.WRAP_CONTENT);
					editOptionName.setText("");
				}
			}
		});
	}

	static public boolean saveArray(String key, List<String> options,
			Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
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

	// retrieves a String array List
	static public void loadArray(String key, List<String> options,
			Context context) {
		SharedPreferences prefs = context.getSharedPreferences(
				MainActivity.POLL_OPTIONS, Context.MODE_PRIVATE);
		if (options != null)
			options.clear(); // remove what might be there

		int size = prefs.getInt(key + "Size", 0);

		for (int i = 0; i < size; i++) {
			options.add(prefs.getString(key + i, null));

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
			Intent myIntent = new Intent(CreatePollActivity.this,
					MainActivity.class);
			CreatePollActivity.this.startActivity(myIntent);
			return true;
		}
		return false;
	}

}
