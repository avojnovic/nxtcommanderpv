package android.nxt;

import java.io.IOException;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;




public class EmbeddedBrowser extends Activity implements BTConnectable
{
	public static final int UPDATE_TIME = 200;
	public static final int MENU_TOGGLE_CONNECT = Menu.FIRST;
	public static final int MENU_QUIT = Menu.FIRST + 1;

	private static final int REQUEST_CONNECT_DEVICE = 1000;
	private static final int REQUEST_ENABLE_BT = 2000;
	private BTCommunicator myBTCommunicator = null;
	private boolean connected = false;
	private ProgressDialog connectingProgressDialog;
	private Handler btcHandler;
	private Menu myMenu;
	private Activity thisActivity;
	private boolean btErrorPending = false;
	private boolean pairing;
	private static boolean btOnByUs = false;
	//int mRobotType;
	int motorLeft;
	private int directionLeft; // +/- 1
	int motorRight;
	private boolean stopAlreadySent = false;
	private int directionRight; // +/- 1
	private int motorAction;
	private int directionAction; // +/- 1
	private String programToStart;
	private Toast reusableToast;
	private final int TTS_CHECK_CODE = 9991;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.browser);				
		
		thisActivity = this;		
		setUpByType();
		

		WebView wv =  (WebView)findViewById(R.id.webView);		
		WebSettings wsettings = wv.getSettings();
		wsettings.setJavaScriptEnabled(true);
		wv.addJavascriptInterface(this, "Cmd");
		wv.loadUrl("http://192.168.100.102/NXTCommander.Web/MessagePage.aspx");
		
		
		reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
	}

	@Override
	protected void onStart()
	{
		super.onStart();		
				
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
		{
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		else
		{
			
			selectNXT();
		}		
	}
	
	@Override
	public void onResume()
	{
		super.onResume();		
	}

	@Override
	public void onPause()
	{		
		destroyBTCommunicator();
		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		destroyBTCommunicator();
	}

	
	private void setUpByType()
	{		
		motorLeft = BTCommunicator.MOTOR_C;
		directionLeft = 1;
		motorRight = BTCommunicator.MOTOR_B;
		directionRight = 1;
		motorAction = BTCommunicator.MOTOR_A;
		directionAction = 1;			
	}
	
	public boolean isPairing()
	{
		return pairing;
	}

	public static boolean isBtOnByUs()
	{
		return btOnByUs;
	}
	
	public static void setBtOnByUs(boolean btOnByUs)
	{
		EmbeddedBrowser.btOnByUs = btOnByUs;
	}
	
	private void updateButtonsAndMenu()
	{

	}

	private void createBTCommunicator()
	{
		// interestingly BT adapter needs to be obtained by the UI thread - so
		// we pass it in in the constructor
		myBTCommunicator = new BTCommunicator(this, myHandler,
				BluetoothAdapter.getDefaultAdapter(), getResources());
		btcHandler = myBTCommunicator.getHandler();
	}
	
	private void startBTCommunicator(String mac_address)
	{
		connected = false;
		connectingProgressDialog = ProgressDialog.show(this, "", getResources()
				.getString(R.string.connecting_please_wait), true);

		if (myBTCommunicator != null)
		{
			try
			{
				myBTCommunicator.destroyNXTconnection();
			}
			catch (IOException e)
			{
			}
		}
		createBTCommunicator();
		myBTCommunicator.setMACAddress(mac_address);
		myBTCommunicator.start();
		updateButtonsAndMenu();
	}

	public void destroyBTCommunicator()
	{

		if (myBTCommunicator != null)
		{
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT,
					0, 0);
			myBTCommunicator = null;
		}

		connected = false;
		updateButtonsAndMenu();
	}
	
	public boolean isConnected()
	{
		return connected;
	}

	public void actionButtonPressed()
	{
		if (myBTCommunicator != null)
		{			
			sendBTCmessage(BTCommunicator.NO_DELAY, motorAction, 75 * directionAction, 0);
			sendBTCmessage(500, motorAction, -75 * directionAction, 0);
			sendBTCmessage(1000, motorAction, 0, 0);
		}
	}
	
	public void updateMotorControl(int left, int right)
	{

		if (myBTCommunicator != null)
		{
			// don't send motor stop twice
			if ((left == 0) && (right == 0))
			{
				if (stopAlreadySent)
					return;
				else
					stopAlreadySent = true;
			}
			else
				stopAlreadySent = false;
			
			
			//showToast("Left:" + left + " / Right: " + right, 2000);

			// send messages via the handler
			sendBTCmessage(BTCommunicator.NO_DELAY, motorLeft, left
					* directionLeft, 0);
			sendBTCmessage(BTCommunicator.NO_DELAY, motorRight, right
					* directionRight, 0);
		}
	}
	
	void sendBTCmessage(int delay, int message, int value1, int value2)
	{
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putInt("value1", value1);
		myBundle.putInt("value2", value2);
		Message myMessage = myHandler.obtainMessage();
		myMessage.setData(myBundle);

		if (delay == 0)
			btcHandler.sendMessage(myMessage);

		else
			btcHandler.sendMessageDelayed(myMessage, delay);
	}
	
	void sendBTCmessage(int delay, int message, String name)
	{
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putString("name", name);
		Message myMessage = myHandler.obtainMessage();
		myMessage.setData(myBundle);

		if (delay == 0)
			btcHandler.sendMessage(myMessage);
		else
			btcHandler.sendMessageDelayed(myMessage, delay);
	}
	
	@Override
	public void onSaveInstanceState(Bundle icicle)
	{
		super.onSaveInstanceState(icicle);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		myMenu = menu;
		myMenu.add(0, MENU_TOGGLE_CONNECT, 1,
				getResources().getString(R.string.connect)).setIcon(
				R.drawable.ic_menu_connect);
		myMenu.add(0, MENU_QUIT, 2, getResources().getString(R.string.quit))
				.setIcon(R.drawable.ic_menu_exit);
		updateButtonsAndMenu();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_TOGGLE_CONNECT:

				if (myBTCommunicator == null || connected == false)
				{
					selectNXT();

				}
				else
				{
					destroyBTCommunicator();
					updateButtonsAndMenu();
				}

				return true;
			case MENU_QUIT:
				destroyBTCommunicator();
				finish();

				if (btOnByUs)
					showToast(R.string.bt_off_message, Toast.LENGTH_SHORT);

				HomeMenu.quitApplication();
				return true;
		}

		return false;
	}
	
	private void showToast(String textToShow, int length)
	{
		reusableToast.setText(textToShow);
		reusableToast.setDuration(length);
		reusableToast.show();
	}
	
	private void showToast(int resID, int length)
	{
		reusableToast.setText(resID);
		reusableToast.setDuration(length);
		reusableToast.show();
	}
	
	final Handler myHandler = new Handler()
	{
		@Override
		public void handleMessage(Message myMessage)
		{
			switch (myMessage.getData().getInt("message"))
			{
				case BTCommunicator.DISPLAY_TOAST:
					showToast(myMessage.getData().getString("toastText"),
							Toast.LENGTH_SHORT);
					break;
				case BTCommunicator.STATE_CONNECTED:
					connected = true;					
					connectingProgressDialog.dismiss();
					
					//TODO DAMI: Descomentar esta linea despues
					//updateButtonsAndMenu();
					sendBTCmessage(BTCommunicator.NO_DELAY,
							BTCommunicator.GET_FIRMWARE_VERSION, 0, 0);
					break;
				case BTCommunicator.MOTOR_STATE:

					if (myBTCommunicator != null)
					{
						byte[] motorMessage = myBTCommunicator
								.getReturnMessage();
						int position = byteToInt(motorMessage[21])
								+ (byteToInt(motorMessage[22]) << 8)
								+ (byteToInt(motorMessage[23]) << 16)
								+ (byteToInt(motorMessage[24]) << 24);
						showToast(
								getResources().getString(
										R.string.current_position)
										+ position, Toast.LENGTH_SHORT);
					}

					break;

				case BTCommunicator.STATE_CONNECTERROR_PAIRING:
					connectingProgressDialog.dismiss();
					destroyBTCommunicator();
					break;

				case BTCommunicator.STATE_CONNECTERROR:
					connectingProgressDialog.dismiss();
				case BTCommunicator.STATE_RECEIVEERROR:
				case BTCommunicator.STATE_SENDERROR:

					destroyBTCommunicator();
					if (btErrorPending == false)
					{
						btErrorPending = true;
						// inform the user of the error with an AlertDialog
						AlertDialog.Builder builder = new AlertDialog.Builder(
								thisActivity);
						builder.setTitle(
								getResources().getString(
										R.string.bt_error_dialog_title))
								.setMessage(
										getResources()
												.getString(
														R.string.bt_error_dialog_message))
								.setCancelable(false)
								.setPositiveButton("OK",
										new DialogInterface.OnClickListener()
										{
											@Override
											public void onClick(
													DialogInterface dialog,
													int id)
											{
												btErrorPending = false;
												dialog.cancel();
												selectNXT();
											}
										});
						builder.create().show();
					}

					break;

				case BTCommunicator.FIRMWARE_VERSION:

					if (myBTCommunicator != null)
					{
						byte[] firmwareMessage = myBTCommunicator
								.getReturnMessage();
						// check if we know the firmware
						boolean isLejosMindDroid = true;
						for (int pos = 0; pos < 4; pos++)
						{
							if (firmwareMessage[pos + 3] != LCPMessage.FIRMWARE_VERSION_LEJOSMINDDROID[pos])
							{
								isLejosMindDroid = false;
								break;
							}
						}
						if (isLejosMindDroid)
						{
							//mRobotType = R.id.robot_type_4;
							setUpByType();
						}
						// afterwards we search for all files on the robot
						sendBTCmessage(BTCommunicator.NO_DELAY,
								BTCommunicator.FIND_FILES, 0, 0);
					}

					break;

//				case BTCommunicator.FIND_FILES:
//
//					if (myBTCommunicator != null)
//					{
//						byte[] fileMessage = myBTCommunicator
//								.getReturnMessage();
//						String fileName = new String(fileMessage, 4, 20);
//						fileName = fileName.replaceAll("\0", "");
//
//						if (mRobotType == R.id.robot_type_4 || fileName.endsWith(".nxj") || fileName.endsWith(".rxe"))
//						{
//							programList.add(fileName);
//						}
//
//						// find next entry with appropriate handle,
//						// limit number of programs (in case of error (endless
//						// loop))
//						if (programList.size() <= MAX_PROGRAMS)
//							sendBTCmessage(BTCommunicator.NO_DELAY,
//									BTCommunicator.FIND_FILES, 1,
//									byteToInt(fileMessage[3]));
//					}
//
//					break;

				case BTCommunicator.PROGRAM_NAME:
					
					break;

				case BTCommunicator.SAY_TEXT:
					
					break;

				case BTCommunicator.VIBRATE_PHONE:
					if (myBTCommunicator != null)
					{
						byte[] vibrateMessage = myBTCommunicator
								.getReturnMessage();
						Vibrator myVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
						myVibrator.vibrate(vibrateMessage[2] * 10);
					}

					break;
			}
		}
	};

	private int byteToInt(byte byteValue)
	{
		int intValue = (byteValue & (byte) 0x7f);

		if ((byteValue & (byte) 0x80) != 0)
			intValue |= 0x80;

		return intValue;
	}

	void selectNXT()
	{
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CONNECT_DEVICE:

				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK)
				{
					// Get the device MAC address and start a new bt
					// communicator thread
					String address = data.getExtras().getString(
							DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					pairing = data.getExtras().getBoolean(
							DeviceListActivity.PAIRING);
					startBTCommunicator(address);
				}

				break;

			case REQUEST_ENABLE_BT:

				// When the request to enable Bluetooth returns
				switch (resultCode)
				{
					case Activity.RESULT_OK:
						btOnByUs = true;
						selectNXT();
						break;
					case Activity.RESULT_CANCELED:
						showToast(R.string.bt_needs_to_be_enabled,
								Toast.LENGTH_SHORT);
						finish();
						break;
					default:
						showToast(R.string.problem_at_connecting,
								Toast.LENGTH_SHORT);
						finish();
						break;
				}

				break;

			// will not be called now, since the check intent is not generated
			case TTS_CHECK_CODE:
				if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
				{
					// success, create the TTS instance
					//mTts = new TextToSpeech(this, this);
				}
				else
				{
					// missing data, install it
					Intent installIntent = new Intent();
					installIntent
							.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
					startActivity(installIntent);
				}

				break;
		}
	}

	
	public void showToast(String toast)	
	{
		//updateMotorControl(50, 50);
		
		Toast.makeText(this.getBaseContext(), toast, Toast.LENGTH_SHORT).show();
	}
	
	public void up()	
	{
		updateMotorControl(50, 50);
	}
	
	public void down()	
	{
		updateMotorControl(-50, -50);
	}
	
	public void left()	
	{
		updateMotorControl(50, 0);
	}
	
	public void right()	
	{
		updateMotorControl(0, 50);
	}
	
	public void stop()	
	{
		updateMotorControl(0, 0);
	}
	
	public void shoot()	
	{
		actionButtonPressed();
	}

	

	
	
	
	
}
