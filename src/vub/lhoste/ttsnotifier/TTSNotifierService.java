package vub.lhoste.ttsnotifier;

import java.io.IOException;
import java.util.Locale;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.NetworkInfo;
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
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import android.speech.tts.TextToSpeech;

@SuppressWarnings("deprecation") //added my virdi
public class TTSNotifierService extends Service {

	public volatile static TTSNotifierLanguage myLanguage = null;

	private static final String ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
	private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private static final String ACTION_BATTERY_LOW = "android.intent.action.ACTION_BATTERY_LOW";
	private static final String ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.ACTION_MEDIA_BAD_REMOVAL";
	private static final String ACTION_BOOT_COMPLETED = "android.intent.action.ACTION_BOOT_COMPLETED";
	private static final String ACTION_PROVIDER_CHANGED = "android.intent.action.ACTION_PROVIDER_CHANGED";
	private static final String ACTION_MEDIA_MOUNTED = "android.intent.action.ACTION_MEDIA_MOUNTED";
	private static final String ACTION_MEDIA_UNMOUNTED = "android.intent.action.ACTION_MEDIA_UNMOUNTED";
	private static final String ACTION_PICK_WIFI_NETWORK = "android.net.wifi.PICK_WIFI_NETWORK";
	private static final String ACTION_WIFI_STATE_CHANGE = "android.net.wifi.STATE_CHANGE";
	private static final String ACTION_SUPPLICANT_CONNECTION_CHANGE_ACTION  = "android.net.wifi.supplicant.CONNECTION_CHANGE";

	private static final int MEDIUM_THREADWAIT = 300;
	private static final int SHORT_THREADWAIT = 50;

	private volatile static TextToSpeech myTts = null;
	private volatile static boolean ttsReady = false;
	private volatile MediaPlayer myRingTonePlayer = null;
	private volatile boolean stopRingtone = false;

	private Context context;
	private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;
	private SharedPreferences mPrefs;
	private AudioManager mAudioManager;
	private TelephonyManager mTelephonyManager;
	private Thread mRingtoneThread;

	// State
	private boolean silentMode = false;
	private volatile int oldStreamRingtoneVolume = 0;
	private volatile int oldStreamMusicVolume = 0;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.v("TTSNotifierService", "onCreate()");
		context = getApplicationContext();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		HandlerThread thread;
		setLanguage(mPrefs.getBoolean("cbxChangeLanguage", false), mPrefs.getString("txtLanguage", "English"));
		if (mPrefs.getBoolean("cbxRunWithHighPriority", false))
			thread = new HandlerThread("HandleIntentTTSNotifier", Process.THREAD_PRIORITY_URGENT_AUDIO);
		else
			thread = new HandlerThread("HandleIntentTTSNotifier", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.v("TTSNotifierService", "onStart()");
		if (myTts == null) {
			try {
				myTts = new TextToSpeech(context, ttsInitListener);
			} catch (java.lang.ExceptionInInitializerError e) { e.printStackTrace(); }
		}
		if (mPrefs.getBoolean("cbxChangeLanguage", false))
			setLanguageTts(myLanguage.getLocale());
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	}

	@Override
	public void onDestroy() {
		stopRingtone = true;
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
			Log.v("TTSNotifierService", "handleMessage()");

			Intent intent = (Intent) msg.obj;
			String action = intent.getAction();

			readState();
			storeAndUpdateVolume();

			boolean cbxEnable = mPrefs.getBoolean("cbxEnable", false);
			boolean cbxObeySilentMode = mPrefs.getBoolean("cbxObeySilentMode", true);

			if (action == null) return;
			if (!cbxEnable) return;
			if (cbxObeySilentMode && silentMode) return;

			// When calling ignore other notifications
			if (!ACTION_PHONE_STATE.equals(action) && (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE))
				return;

			Log.v("TTSNotifierService", "Action: " + action);

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
			} else if (ACTION_MEDIA_MOUNTED.equals(action)) {
				handleACTION_MEDIA_MOUNTED(intent);
			} else if (ACTION_MEDIA_UNMOUNTED.equals(action)) {
				handleACTION_MEDIA_UNMOUNTED(intent);
			} else if (ACTION_PICK_WIFI_NETWORK.equals(action)) {
				handleACTION_PICK_WIFI_NETWORK(intent);
			} else if (ACTION_WIFI_STATE_CHANGE.equals(action)) {
				handleACTION_WIFI_STATE_CHANGE(intent);
			} else if (ACTION_SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
				handleSUPPLICANT_CONNECTION_CHANGE_ACTION(intent);
			}
			// ACTION_PHONE_STATE is different because we are using a thread in it
			if (!ACTION_PHONE_STATE.equals(action))
				restoreVolume();
		}
	}

	public static void setLanguage(boolean changeLanguage, String language) {
		if (!changeLanguage)
			language = java.util.Locale.getDefault().getLanguage();
		if (language.equals("English")
				|| language.equals("en_US")
				|| language.equals("en_GB")
				|| language.equals("en_CA")
				|| language.equals("en_AU")
				|| language.equals("en_NZ")
				|| language.equals("en_SG") )
			myLanguage = new TTSNotifierLanguageEN();
		else if (language.equals("Nederlands")
				|| language.equals("nl_NL")
				|| language.equals("nl_BE"))
			myLanguage = new TTSNotifierLanguageNL();
		else if (language.equals("FranÃ§ais")
				|| language.equals("fr_FR")
				|| language.equals("fr_BE")
				|| language.equals("fr_CA")
				|| language.equals("fr_CH"))
			myLanguage = new TTSNotifierLanguageFR();
		else if (language.equals("Deutsch")
				|| language.equals("de_DE")
				|| language.equals("de_AT")
				|| language.equals("de_CH")
				|| language.equals("de_LI"))
			myLanguage = new TTSNotifierLanguageDE();
		else 
			myLanguage = new TTSNotifierLanguageEN();
		setLanguageTts(myLanguage.getLocale());
	} 

	private static void setLanguageTts(Locale languageShortName) {
		if (myTts != null) {
			myTts.setLanguage(languageShortName);
		}		
	}

	private void readState() {
		if (mAudioManager != null)
			silentMode = mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
	}

	private void storeAndUpdateVolume() {
		if (mPrefs.getBoolean("cbxChangeVolume", false) && mAudioManager != null && mRingtoneThread == null) {
			int intOptionsTTSVolume = Math.min(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), Math.max(0, Integer.parseInt(mPrefs.getString("intOptionsTTSVolume", "14"))));
			oldStreamMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			oldStreamRingtoneVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < intOptionsTTSVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > intOptionsTTSVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
		}
	}

	private void restoreVolume() {
		if (mPrefs.getBoolean("cbxChangeVolume", false) && mAudioManager != null) {
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > oldStreamMusicVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < oldStreamMusicVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
			while (mAudioManager.getStreamVolume(AudioManager.STREAM_RING) < oldStreamRingtoneVolume)
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, 0);			
		}
	}

	public static void waitForSpeechInitialised() {
		while (!ttsReady) {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void waitForSpeechFinished() {
		try {
			Thread.sleep(MEDIUM_THREADWAIT);
		} catch (InterruptedException e) { }
		while (myTts.isSpeaking()) {
			try {
				Thread.sleep(MEDIUM_THREADWAIT);
			} catch (InterruptedException e) { }
		}
		try {
			Thread.sleep(MEDIUM_THREADWAIT);
		} catch (InterruptedException e) { }
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
				Thread.sleep(SHORT_THREADWAIT);
			} catch (InterruptedException e) { 
				e.printStackTrace();
			}
		}
	}

	private void playRingtone(boolean waitForFinish) throws IllegalStateException, IOException {
		Log.v("TTSNotifierService", "playRingtone()" + myRingTonePlayer);
		if (myRingTon