package com.project.btvoting;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class InteractPollActivity extends Activity {

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

	private static final String TAG = "InteractPollActivity";

	// Array adapter for the conversation thread
	private ArrayAdapter<String> mPollArrayAdapter;
	// String buffer for outgoing messages
	private StringBuffer mOutStringBuffer;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	// Layout Views
	private TextView mTitle;
	private ListView mPollsView;

	// Name for the SDP record when creating server socket
	public static final String NAME = "BluetoothPoll";

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote device

	private static String REQUEST_POLLS_LIST = "reqpolls";

	// Intent request codes
	public static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private int mState;
	private BluetoothAdapter mBluetoothAdapter;
	public String mConnectedDeviceName;

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "in the handler");
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				Log.i(TAG, "Handler is MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case STATE_CONNECTED:
					Log.d(TAG, "Handler is STATE_CONNECTED!!!!!!!!!!");
					mTitle.setText(R.string.title_connected_to);
					mTitle.append(mConnectedDeviceName);

					title = (TextView) findViewById(R.id.otherPollNames);
					title.setText(mConnectedDeviceName + "'s polls");

					getPollsFromOtherDevice();

					mPollArrayAdapter.clear();
					break;
				case STATE_CONNECTING:
					Log.d(TAG, "Handler is STATE_CONNECTING");
					mTitle.setText(R.string.title_connecting);
					break;
				case STATE_LISTEN:
				case STATE_NONE:
					Log.d(TAG, "Handler is STATE_NONE");
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				mPollArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);

				if (readMessage.equals(REQUEST_POLLS_LIST)) {
					Log.d(TAG, "OHMIGAWD POLLS REQUEST SUCCESSFULLY TRANSMITTED!!!!");
				}

				mPollArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}

	};

	private TextView title;

	private void getPollsFromOtherDevice() {
		// request them
		sendMessage(REQUEST_POLLS_LIST);
		// get them back
		// put them in mPollArrayAdapter
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initLayout();

		Log.d(TAG, "in on Create");

		// Get the local Bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the session
		} else {
			setupChat();
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		// Initialize the array adapter for the conversation thread
		mPollArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mPollsView = (ListView) findViewById(R.id.foundPolls);
		mPollsView.setAdapter(mPollArrayAdapter);

		//		// Initialize the compose field with a listener for the return key
		//		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		//		mOutEditText.setOnEditorActionListener(mWriteListener);
		//
		//		// Initialize the send button with a listener that for click events
		//		mSendButton = (Button) findViewById(R.id.button_send);
		//		mSendButton.setOnClickListener(new OnClickListener() {
		//			public void onClick(View v) {
		//				// Send a message using content of the edit text widget
		//				TextView view = (TextView) findViewById(R.id.edit_text_out);
		//				String message = view.getText().toString();
		//				sendMessage(message);
		//			}
		//		});

		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);

		// Initialize the buffer for outgoing messages
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		Log.d(TAG, "+ ON RESUME +");

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

	private void initLayout() {
		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_interact);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

		// Set up the custom title
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		initiateFind = (Button) findViewById(R.id.initiateFind);
		initiateFind = (Button) findViewById(R.id.initiateFind);

		initiateFind.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// people can't find the poll if you're not discoverable
				Intent serverIntent = new Intent(InteractPollActivity.this, FindPollActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			}
		});
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
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			//            mOutEditText.setText(mOutStringBuffer);
		}
	}
}