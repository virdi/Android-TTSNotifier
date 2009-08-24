package vub.lhoste.ttsnotifier;

import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsMessage;
import android.util.Log;
import android.widget.Toast;

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
	private SharedPreferences mPrefs;

	// State
	private boolean silentMode = false;
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
		Log.v("LODE", "F "+intent.getFlags());
		Log.v("TTSNotifierService", "onStart()");
		if (myTts == null)
			myTts = new TTS(context, ttsInitListener, true);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
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

			readState();

			boolean cbxEnable = mPrefs.getBoolean("cbxEnable", false);
			boolean cbxObeySilentMode = mPrefs.getBoolean("cbxObeySilentMode", true);

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

	private void readState() {
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
		// Load Preferences
		boolean cbxEnableIncomingCall = mPrefs.getBoolean("cbxEnableIncomingCall", true);
		final String txtOptionsIncomingCall;
		if (mPrefs.getBoolean("cbxOptionsIncomingCallUserDefinedText", false))
			txtOptionsIncomingCall = mPrefs.getString("txtOptionsIncomingCall", "Phone call from %s");
		else
			txtOptionsIncomingCall = "Phone call from %s";
		final boolean cbxOptionsIncomingCallUseTTSRingtone = mPrefs.getBoolean("cbxOptionsIncomingCallUseTTSRingtone", false);
		final String txtOptionsIncomingCallRingtone = mPrefs.getString("txtOptionsIncomingCallRingtone", "DEFAULT_RINGTONE_URI");
		final int intOptionsIncomingCallMinimalRingCountTTSDelay = Integer.parseInt(mPrefs.getString("intOptionsIncomingCallMinimalRingCountTTSDelay", "2"));
		// Logic
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
									Log.v("LODE", "CALL STATE :" + ringCounter + " - "+ intOptionsIncomingCallMinimalRingCountTTSDelay);
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
		String txtOptionsIncomingSMS;
		// Preferences
		if (mPrefs.getBoolean("cbxOptionsIncomingSMSOnlyAnnouncePerson", true))
			txtOptionsIncomingSMS = "New text message from %s";
		else if (mPrefs.getBoolean("cbxOptionsIncomingSMSUserDefinedText", true))
			txtOptionsIncomingSMS = mPrefs.getString("txtOptionsIncomingSMS", "New text message from %s about %s");
		else
			txtOptionsIncomingSMS = "New text message from %s about %s";
		// Logic
		if (intent.getAction().equals(ACTION_SMS_RECEIVED)) {  
			SmsMessage[] messages = getMessagesFromIntent(intent);
			if (messages == null) return;
			SmsMessage sms = messages[0];
			if (sms.getMessageClass() != SmsMessage.MessageClass.CLASS_0 && !sms.isReplace()) {
				String body;
				if (messages.length == 1) {
					body = messages[0].getDisplayMessageBody();
				} else {
					StringBuilder bodyText = new StringBuilder();
					for (int i = 0; i < messages.length; i++) {
						bodyText.append(messages[i].getMessageBody());
					}   
					body = bodyText.toString();
				}
				String address = messages[0].getDisplayOriginatingAddress();
				Log.v("LODE", "PHONE: " +address);
				Log.v("LODE", "BODY: "+body);
				speak(String.format(txtOptionsIncomingSMS, getContactNameFromNumber(address), body), true);
			}
		}
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
		// Preferences
		boolean cbxEnableWifiDiscovered = mPrefs.getBoolean("cbxEnableWifiDiscovered", true);
		String txtOptionsWifiDiscovered;
		if (mPrefs.getBoolean("cbxOptionsWifiDiscoveredUserDefinedText", false))
			txtOptionsWifiDiscovered = mPrefs.getString("txtOptionsWifiDiscovered", "Wyfy Signal in Range");
		else
			txtOptionsWifiDiscovered = "Wyfy Signal in Range";
		// Logic
		if (!cbxEnableWifiDiscovered) return;
		speak(txtOptionsWifiDiscovered, false);	
	}

	private void handleSUPPLICANT_CONNECTION_CHANGE_ACTION(Intent intent) {
		// Preferences
		boolean cbxEnableWifiConnectDisconnect = mPrefs.getBoolean("cbxEnableWifiConnectDisconnect", true);
		String txtOptionsWifiConnected;
		if (mPrefs.getBoolean("cbxOptionsWifiConnectedUserDefinedText", false))
			txtOptionsWifiConnected = mPrefs.getString("txtOptionsWifiConnected", "Wyfy connected");
		else
			txtOptionsWifiConnected = "Wyfy connected";
		String txtOptionsWifiDisconnected;
		if (mPrefs.getBoolean("cbxOptionsWifiDisconnectedUserDefinedText", true))
			txtOptionsWifiDisconnected = mPrefs.getString("txtOptionsWifiDisconnected", "Wyfy disconnected");
		else
			txtOptionsWifiDisconnected = "Wyfy disconnected";
		// Logic
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


	public static boolean isTtsInstalled(Context ctx) {
		PackageManager pm = ctx.getPackageManager();
		Intent intent = new Intent("android.intent.action.USE_TTS");
		intent.addCategory("android.intent.category.TTS");
		ResolveInfo info = pm.resolveService(intent, 0); 
		if (info == null) {
			return false;
		}   
		return true;
	}

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

	private SmsMessage[] getMessagesFromIntent(Intent intent)
	{
		SmsMessage retMsgs[] = null;
		Bundle bdl = intent.getExtras();
		try{
			Object pdus[] = (Object [])bdl.get("pdus");
			retMsgs = new SmsMessage[pdus.length];
			for(int n=0; n < pdus.length; n++)
			{
				byte[] byteData = (byte[])pdus[n];
				retMsgs[n] =
					SmsMessage.createFromPdu(byteData);
			}        
		}
		catch(Exception e)
		{
			Log.e("GetMessages", "fail", e);
		}
		return retMsgs;
	}

	public static void beginStartingService(Context context, Intent intent) {
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
		if (isTtsInstalled(context))
			context.startService(intent);
		else
			Toast.makeText(context, "TTSNotifier: TTS not installed! Install it from the market!", Toast.LENGTH_LONG).show();
	}
}