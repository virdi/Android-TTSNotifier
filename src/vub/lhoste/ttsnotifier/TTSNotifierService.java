package vub.lhoste.ttsnotifier;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.tts.TTS;

public class TTSNotifierService extends Service {

	private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
	private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private static final String ACTION_BATTERY_LOW = "android.intent.action.ACTION_BATTERY_LOW";
	private static final String ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.ACTION_MEDIA_BAD_REMOVAL";
	private static final String ACTION_BOOT_COMPLETED = "android.intent.action.ACTION_BOOT_COMPLETED";
	private static final String ACTION_PROVIDER_CHANGED = "android.intent.action.ACTION_PROVIDER_CHANGED";
	private static final String ACTION_UMS_CONNECTED = "android.intent.action.ACTION_UMS_CONNECTED";
	private static final String ACTION_UMS_DISCONNECTED = "android.intent.action.ACTION_UMS_CONNECTED";
	private static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";
	private static final String ACTION_SUPPLICANT_CONNECTION_CHANGE_ACTION = "android.net.wifi.supplicant.CONNECTION_CHANGE";

	private static final int MEDIUM_THREADWAIT = 300;
	private static final int FAST_THREADWAIT = 50;

	private static TTS myTts = null;
	private MediaPlayer myRingTonePlayer = null;
	private volatile boolean stopRingtone = false;
	private volatile boolean ttsReady = false;

	private Context context;
	private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;

	// Settings
	private boolean cbxEnable;
	private boolean cbxEnableIncomingCall;
	private boolean cbxEnableWifiConnectDisconnect;
	private boolean cbxEnableWifiDiscovered;
	private boolean cbxObeySilentMode;
	private String txtOptionsIncomingCall;
	private String txtOptionsWifiDiscovered;
	private String txtOptionsWifiConnected;
	private String txtOptionsWifiDisconnected;

	// State
	private boolean silentMode = false;
	private boolean cbxOptionsIncomingCallUseTTSRingtone;
	private String txtOptionsIncomingCallRingtone;
	private Integer intOptionsIncomingCallMinimalRingCountTTSDelay;
	private Thread myRingtoneThread;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.v("TTSNotifierService", "onCreate()");
		//HandlerThread thread = new HandlerThread("HandleIntentTTSNotifier", Process.THREAD_PRIORITY_BACKGROUND);
		HandlerThread thread = new HandlerThread("HandleIntentTTSNotifier", Process.THREAD_PRIORITY_URGENT_AUDIO);
		thread.start();
		context = getApplicationContext();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.v("TTSNotifierService", "onStart()");
		if (myTts == null)
			myTts = new TTS(this, ttsInitListener, true);
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	} 

	@Override
	public void onDestroy() {
		ttsReady = false;
		if (myTts != null) 
			myTts.shutdown();
		myTts = null;
		if (myRingTonePlayer != null)
			myRingTonePlayer.release();
		myRingTonePlayer = null;
		mServiceLooper.quit();
	} 

	private final class ServiceHandler extends Handler {

		public ServiceHandler(Looper serviceLooper) {
			super(serviceLooper);
		}

		@Override
		public void handleMessage(Message msg) {
			//int serviceId = msg.arg1;
			Intent intent = (Intent) msg.obj;
			String action = intent.getAction();
			//String dataType = intent.getType();
			readSettings();

			if (action == null) return;
			if (!cbxEnable) return;
			if (cbxObeySilentMode && silentMode) return;

			Log.v("TTSNotifier", "Action: " + action);

			if (ACTION_PHONE_STATE.equals(action)) {
				handleACTION_PHONE_STATE(intent);
			} else if (ACTION_SMS_RECEIVED.equals(action)) {
				handleACTION_SMS_RECEIVED(intent);
			} else if (ACTION_BATTERY_LOW.equals(action)) {
				handleACTION_BATTERY_LOW(intent);
			} else if (ACTION_MEDIA_BAD_REMOVAL.equals(action)) {
				handleACTION_MEDIA_BAD_REMOVAL(intent);
			} else if (ACTION_BOOT_COMPLETED.equals(action)) {
				handleACTION_BOOT_COMPLETED(intent);
			} else if (ACTION_PROVIDER_CHANGED.equals(action)) {
				handleACTION_PROVIDER_CHANGED(intent);
			} else if (ACTION_UMS_CONNECTED.equals(action)) {
				handleACTION_UMS_CONNECTED(intent);
			} else if (ACTION_UMS_DISCONNECTED.equals(action)) {
				handleACTION_UMS_DISCONNECTED(intent);
			} else if (ACTION_PICK_WIFI_NETWORK.equals(action)) {
				handleACTION_PICK_WIFI_NETWORK(intent);
			} else if (ACTION_SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
				handleSUPPLICANT_CONNECTION_CHANGE_ACTION(intent);
			}
		}
	}

	public static boolean isTtsInstalled(Context ctx){
		try {
			ctx.createPackageContext("com.google.tts", 0);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}

	private void readSettings() {
		// Preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		cbxEnable = prefs.getBoolean("cbxEnable", false);
		cbxEnableIncomingCall = prefs.getBoolean("cbxEnableIncomingCall", true);
		cbxEnableWifiDiscovered = prefs.getBoolean("cbxEnableWifiDiscovered", true);
		cbxEnableWifiConnectDisconnect = prefs.getBoolean("cbxEnableWifiConnectDisconnect", true);
		cbxObeySilentMode = prefs.getBoolean("cbxObeySilentMode", true);
		if (prefs.getBoolean("cbxOptionsIncomingCallUserDefinedText", false))
			txtOptionsIncomingCall = prefs.getString("txtOptionsIncomingCall", "Phone call from %s");
		else
			txtOptionsIncomingCall = "Phone call from %s";
		cbxOptionsIncomingCallUseTTSRingtone = prefs.getBoolean("cbxOptionsIncomingCallUseTTSRingtone", false);
		txtOptionsIncomingCallRingtone = prefs.getString("txtOptionsIncomingCallRingtone", "DEFAULT_RINGTONE_URI");
		intOptionsIncomingCallMinimalRingCountTTSDelay = Integer.parseInt(prefs.getString("intOptionsIncomingCallMinimalRingCountTTSDelay", "2"));
		if (prefs.getBoolean("cbxOptionsWifiDiscoveredUserDefinedText", false))
			txtOptionsWifiDiscovered = prefs.getString("txtOptionsWifiDiscovered", "Wyfy Signal in Range");
		else
			txtOptionsWifiDiscovered = "Wifi Signal in Range";
		if (prefs.getBoolean("cbxOptionsWifiConnectedUserDefinedText", false))
			txtOptionsWifiConnected = prefs.getString("txtOptionsWifiConnected", "Wyfy connected");
		else
			txtOptionsWifiConnected = "Wifi connected";
		if (prefs.getBoolean("cbxOptionsWifiDisconnectedUserDefinedText", true))
			txtOptionsWifiDisconnected = prefs.getString("txtOptionsWifiDisconnected", "Wyfy disconnected");
		else
			txtOptionsWifiDisconnected = "Wifi disconnected";
		// State
		AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		silentMode = am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
	}


	private void waitForSpeechInitialised() {
		while (!ttsReady) {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void waitForSpeechFinished() {
		do {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (myTts.isSpeaking());
	}

	private void waitForRingTonePlayed() {
		do {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (myRingTonePlayer.isPlaying());
	}

	private void waitForRingToneThreadStopped() {
		while (stopRingtone) {
			try {
				Thread.sleep(FAST_THREADWAIT);
			} catch (InterruptedException e) { 
				e.printStackTrace();
			}
		}
	}

	private void playRingtone(boolean waitForFinish) throws IllegalStateException, IOException {
		myRingTonePlayer.start();
		if (waitForFinish)
			waitForRingTonePlayed();
	}

	private void speak(String str, boolean waitForFinish) {
		waitForSpeechInitialised();
		myTts.speak(str, 0, null);
		if (waitForFinish)
			waitForSpeechFinished();
	}

	private void handleACTION_PHONE_STATE(Intent intent) {
		if (!cbxEnableIncomingCall) return;
		TelephonyManager telManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		final String phoneNr = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
		if (myRingtoneThread != null)
			stopRingtone = true;
		if (myTts != null)
			myTts.stop();
		if (myRingTonePlayer != null) {
			myRingTonePlayer.stop();
			try {
				myRingTonePlayer.prepare();
			} catch (IllegalStateException e) {
				myRingTonePlayer = null;
			} catch (IOException e) {
				myRingTonePlayer = null;
			}
		}
		if(telManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
			waitForRingToneThreadStopped();
			myRingtoneThread = new Thread() {
				public void run() {
					try {
						if (cbxOptionsIncomingCallUseTTSRingtone) {
							if (myRingTonePlayer == null)
								myRingTonePlayer = MediaPlayer.create(context, Uri.parse(txtOptionsIncomingCallRingtone));
							int ringtoneState = 0;
							int ringCounter = 1;
							while (!stopRingtone) {
								Log.v("LODE", "CALL STATE :" + ringtoneState);
								switch (ringtoneState) {
								case 0:
									playRingtone(true);
									break;
								case 1:
									Log.v("LODE", "CALL STATE :" + ringCounter + " - "+intOptionsIncomingCallMinimalRingCountTTSDelay);
									if (!ttsReady || ringCounter < intOptionsIncomingCallMinimalRingCountTTSDelay) {
										ringCounter += 1;
										ringtoneState = -1;
									}
									break;
								case 2:
									speak(String.format(txtOptionsIncomingCall, getContactNameFromNumber(phoneNr)), true);
									break;
								case 3:
									ringtoneState = -1;
									ringCounter = 1;
									break;
								}
								ringtoneState += 1;
							}
						} else {
							speak(String.format(txtOptionsIncomingCall, getContactNameFromNumber(phoneNr)), false);
						}
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					myRingtoneThread = null;
					stopRingtone = false;
				}
			};
			myRingtoneThread.start();
		}
	}

	private void handleACTION_SMS_RECEIVED(Intent intent) {
		// TODO
	}

	public void handleACTION_BATTERY_LOW(Intent intent) {
		// TODO Auto-generated method stub		
	}

	public void handleACTION_MEDIA_BAD_REMOVAL(Intent intent) {
		// TODO Auto-generated method stub	
	}

	public void handleACTION_BOOT_COMPLETED(Intent intent) {
		// TODO Auto-generated method stub		
	}

	public void handleACTION_PROVIDER_CHANGED(Intent intent) {
		// TODO Auto-generated method stub		
	}

	public void handleACTION_UMS_CONNECTED(Intent intent) {
		// TODO Auto-generated method stub
	}

	public void handleACTION_UMS_DISCONNECTED(Intent intent) {
		// TODO Auto-generated method stub
	}

	public void handleACTION_PICK_WIFI_NETWORK(Intent intent) {
		if (!cbxEnableWifiDiscovered) return;
		speak(txtOptionsWifiDiscovered, false);	
	}

	private void handleSUPPLICANT_CONNECTION_CHANGE_ACTION(Intent intent) {
		if (!cbxEnableWifiConnectDisconnect) return;
		Log.v("LODE", txtOptionsWifiConnected);
		if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, true)) {
			speak(txtOptionsWifiConnected, false);			
		} else {
			speak(txtOptionsWifiDisconnected, false);
		}
	}

	private TTS.InitListener ttsInitListener = new TTS.InitListener() {
		@Override
		public void onInit(int version) {
			Log.v("TTSNotifier", "TTS INIT DONE");
			ttsReady = true;
		}
	};

	private String getContactNameFromNumber(String number) {
		String[] projection = new String[] {
				Contacts.Phones.DISPLAY_NAME,
				Contacts.Phones.NUMBER };
		Uri contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(number));
		Cursor c = getContentResolver().query(contactUri, projection, null,
				null, null);
		if (c.moveToFirst()) {
			String name = c.getString(c
					.getColumnIndex(Contacts.Phones.DISPLAY_NAME));
			return name;
		}
		return "Unknown";
	}

	public static void beginStartingService(Context context, Intent intent) {
		context.startService(intent);
	}

}