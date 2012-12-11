package com.project.btvoting;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class InteractPollActivity extends Activity {

	private static final String TAG = "InteractPollActivity";

	protected Button initiateFind;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	public static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private static final String DUPLICATE = "duplicate";

	// Layout Views
	private TextView mTitle;
	private ListView mPollsView;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mPollArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	private TextView title;

	private Boolean isRequestor = false;
	private Boolean pollsSent = false;
	private Boolean pollsReceived = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "in on Create");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_interact);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// Get the local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "ON START");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		Log.d(TAG, "ON RESUME");

		// Performing this check in onResume() covers the case in which BT was
		// not enabled during onStart(), so we were paused to enable it...
		// onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if (mChatService != null) {
			// Only if the state is STATE_NONE, do we know that we haven't started already
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// Start the Bluetooth chat services
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// Initialize the array adapter for the conversation thread
		mPollArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mPollsView = (ListView) findViewById(R.id.foundPolls);
		mPollsView.setAdapter(mPollArrayAdapter);

		// Find and set up the ListView for paired devices
		mPollsView.setOnItemClickListener(mDeviceClickListener);

		initiateFind = (Button) findViewById(R.id.initiateFind);

		initiateFind.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// people can't find the poll if you're not discoverable
				isRequestor = true;
				Intent serverIntent = new Intent(InteractPollActivity.this, FindPollActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
		});

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		Log.e(TAG, "--- ON DESTROY ---");
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "in the handler");
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				Log.i(TAG, "Handler is MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					Log.d(TAG, "Handler is STATE_CONNECTED!!!!!!!!!!");

					mTitle.setText(R.string.title_connected_to);
					mTitle.append(cutToEightChars(mConnectedDeviceName));

					if (!isRequestor) {
						Log.d(TAG, "sent polls as requestee");
						if (!pollsSent) {
							sendPolls();
						}
					} else {
						Log.d(TAG, "is requestor");
						title = (TextView) findViewById(R.id.otherPollNames);
						title.setText(cutToEightChars(mConnectedDeviceName) + "'s polls");
					}

					mPollArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					Log.d(TAG, "Handler is STATE_CONNECTING");
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					Log.d(TAG, "Handler is STATE_NONE");
					mTitle.setText(R.string.title_not_connected);
					title = (TextView) findViewById(R.id.otherPollNames);
					title.setText("");
					break;
				}
				break;
			case MESSAGE_WRITE:
				//				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				//				String writeMessage = new String(writeBuf);
				//				mPollArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				Log.d(TAG, "received the message " + readMessage);

				if (isRequestor && !pollsReceived) {
					processPollsReceived(readMessage);
				} else if (!isRequestor) {
					receivePollChoice(readMessage);
				} else if (isRequestor) {
					if (readMessage.equals(DUPLICATE)) {
						Toast.makeText(getApplicationContext(),
								"You have already voted on this poll!", Toast.LENGTH_SHORT).show();
					}
				}
				//				mPollArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);

				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + cutToEightChars(mConnectedDeviceName), Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}

	};

	public String cutToEightChars(String s) {
		String newName = s;
		if (s.length() > 8) {
			newName = s.substring(0, 8);
		}
		return newName;
	}

	private void processPollsReceived(String readMessage) {
		String[] polls = readMessage.split(":");
		for (String string : polls) {
			String poll = "";
			pollsReceived = true;
			String[] options = string.split(",");
			poll = poll.concat(options[0] + "\n");
			for (int i = 1; i < options.length; i++) {
				poll = poll.concat("\t" + i + ") " + options[i] + "\n");
			}
			mPollArrayAdapter.add(poll);
		}
	}

	private void sendPolls() {
		ArrayList<String> pollNames = new ArrayList<String>();
		ArrayList<String> options = new ArrayList<String>();

		String pollCsv = "";
		pollsSent = true;

		// load the list of poll names to view
		CreatePollActivity.loadArray(MainActivity.POLL_NAMES, pollNames, getBaseContext());

		for (String name : pollNames) {
			Log.d(TAG, "now in poll Name " + name);
			pollCsv = pollCsv.concat(name + ",");
			// load the poll options
			CreatePollActivity.loadArray(name, options, getBaseContext());
			for (String s : options) {
				pollCsv = pollCsv.concat(s + ",");
			}
			pollCsv = pollCsv.concat(":");
			pollCsv = pollCsv.replace(",:", ":");
		}
		// send them
		sendMessage(pollCsv);

		Log.d(TAG, "sent the polls");
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "in onActivityResult");
		Log.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			Log.d(TAG, "in requestConnectDevice!!!!");
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				String address = data.getExtras().getString(FindPollActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				Log.d(TAG, "address is " + address + "!!!!!!!!!!");
				// Attempt to connect to the device
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			Log.d(TAG, "in requestEnableBT!!!!");
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	/**
	* Sends a message.
	* @param message  A string of text to send.
	*/
	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {

			Log.d(TAG, "sending the message " + message);

			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			//            mOutEditText.setText(mOutStringBuffer);
		}
	}

	private String getNameFromPollString(String pollString) {
		String[] tokens = pollString.split("\n");
		String name = "";
		if (tokens.length > 0) {
			name = tokens[0];
		}
		return name;
	}

	private String[] getChoicesFromPollString(String pollString) {
		String[] tokens = pollString.split("\n");
		String[] choices = new String[tokens.length - 1];
		// the first token is the name
		// so do all but the first token
		for (int i = 1; i < tokens.length; i++) {
			String raw = tokens[i];
			// the raw choice is "1) xx"
			// the second bit is the actual choice
			String[] s = raw.split(" ");
			String choiceName = s[1];
			choices[i - 1] = choiceName;
		}

		return choices;
	}

	private void receivePollChoice(String choiceString) {
		// if the requestee receives a string/number thing
		Log.d(TAG, "received poll choice " + choiceString); // :) :) :)
		String[] infos = choiceString.split(" ");
		String name = infos[0];
		int choice = Integer.parseInt(infos[1]);

		Log.d(TAG, "poll name is " + name);

		ArrayList<Integer> counts = new ArrayList<Integer>();
		ArrayList<String> responders = new ArrayList<String>();

		// now make the same device id not vote twice
		CreatePollActivity.loadArray((name + MainActivity.POLL_RESPONDERS), responders,
				getBaseContext());

		// if their name is already in the list, uh oh
		if (responders.contains(mConnectedDeviceName)) {
			sendMessage(DUPLICATE);
		} else {

			// it will append one to the count of that poll's number choice	
			CreatePollActivity.loadIntArray((name + MainActivity.POLL_COUNTS), counts,
					getBaseContext());
			for (Integer integer : counts) {
				Log.d(TAG, "counts is " + integer);
			}

			int count = counts.get(choice);
			int newCount = count + 1;
			Log.d(TAG, "newCount is " + newCount);
			counts.set(choice, newCount);
			CreatePollActivity.saveIntArray((name + MainActivity.POLL_COUNTS), counts,
					getBaseContext());

			CreatePollActivity.loadIntArray((name + MainActivity.POLL_COUNTS), counts,
					getBaseContext());
			for (Integer integer : counts) {
				Log.d(TAG, "counts is now " + integer);
			}

			responders.add(mConnectedDeviceName);
			CreatePollActivity.saveArray((name + MainActivity.POLL_RESPONDERS), responders,
					getBaseContext());
		}

	}

	// The on-click listener for all devices in the poll ListViews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			String pollString = ((TextView) v).getText().toString();
			final String name = getNameFromPollString(pollString);
			final String[] items = getChoicesFromPollString(pollString);

			// open up a dialog with the poll name and the poll options
			AlertDialog.Builder choiceSelection = new AlertDialog.Builder(InteractPollActivity.this);
			choiceSelection.setTitle(name);
			choiceSelection.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
				// when the user selects one of the options
				public void onClick(DialogInterface dialog, int item) {
					// send a message to the requestee the poll name 
					// and number that was picked (0...n)
					String message = name + " " + item;
					sendMessage(message);

					dialog.dismiss();
					//					Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
				}
			});

			AlertDialog alert = choiceSelection.create();
			alert.show();
		}

	};
}