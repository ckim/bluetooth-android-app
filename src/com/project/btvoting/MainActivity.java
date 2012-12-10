package com.project.btvoting;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	static final String POLL_OPTIONS = "pollOptions";
	static final String POLL_NAMES = "pollNames";

	private static final int REQUEST_ENABLE_BT = 2;

	protected Button createPoll;
	protected Button findPolls;
	protected Button viewPolls;

	// Member object for the chat services
	private BluetoothAdapter mBluetoothAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initLayout();
		initButtonListeners();

		// Get local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish(); // nope
			return;
		}
	}

	private void initLayout() {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.activity_main);
		createPoll = (Button) findViewById(R.id.createPoll);
		findPolls = (Button) findViewById(R.id.findPolls);
		viewPolls = (Button) findViewById(R.id.viewPolls);

	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "++ ON START ++");

		// If Bluetooth is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		Log.d(TAG, "+ ON RESUME +");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "--- ON DESTROY ---");
	}

	protected void initButtonListeners() {
		createPoll.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// people can't find the poll if you're not discoverable
				Intent myIntent = new Intent(MainActivity.this, CreatePollActivity.class);
				MainActivity.this.startActivity(myIntent);
			}
		});

		findPolls.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent myIntent = new Intent(MainActivity.this, InteractPollActivity.class);
				MainActivity.this.startActivity(myIntent);
			}
		});

		viewPolls.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent myIntent = new Intent(MainActivity.this, ViewPollActivity.class);
				MainActivity.this.startActivity(myIntent);

			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return false;
	}

	// From Android Bluetooth example
	private void ensureDiscoverable() {
		Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
			startActivity(discoverableIntent);
		}
	}
}
