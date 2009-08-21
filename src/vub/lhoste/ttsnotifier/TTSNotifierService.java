package vub.lhoste.ttsnotifier;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Contacts;
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

	private static final int TTS_MAXTRIES = 30;
	private static final int TTS_THREADWAIT = 200;
	
	private static TTS myTts = null;
	private static boolean myTtsReady = false;

	private Context context;
	private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.v("TTSNotifierService", "onCreate()");
		HandlerThread thread = new HandlerThread("HandleIntentTTSNotifier", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		context = getApplicationContext();
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Log.v("TTSNotifierService", "onStart()");
		if (myTts == null) {
			myTts = new TTS(this, ttsInitListener, true);		
		}
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.sendMessage(msg);
	} 

	@Override
	public void onDestroy() {
		myTtsReady = false;
		if (myTts != null) {
			myTts.shutdown();			
		}
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
			if (action == null) return;
			Log.v("LODE", action);
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

	private void speak(String str) {
		try {
			for (int i = 0; i < TTS_MAXTRIES; i++) {
				Log.v("LODE", "SPEAK: " + str);
				if (myTtsReady) {
					myTts.speak(str, 0, null);
					return;
				} else {
					Thread.sleep(TTS_THREADWAIT);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void handleACTION_PHONE_STATE(Intent intent) {
		TelephonyManager myTelManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		if(myTelManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
			String phoneNr = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
			speak("Incoming call from " + getContactNameFromNumber(phoneNr));
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
		speak("Pick a Wi-Fi network to connect to");	
	}

	private void handleSUPPLICANT_CONNECTION_CHANGE_ACTION(Intent intent) {
		if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, true)) {
			speak("Wifi connected");			
		} else {
			speak("Wifi disconnected");
		}
	}

	private TTS.InitListener ttsInitListener = new TTS.InitListener() {
		public void onInit(int version) {
			Log.v("LODE", "INITDONE");
			myTtsReady = true;
		}
	};

	private String getContactNameFromNumber(String number) {
		// define the columns I want the query to return
		String[] projection = new String[] {
				Contacts.Phones.DISPLAY_NAME,
				Contacts.Phones.NUMBER };

		// encode the phone number and build the filter URI
		Uri contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(number));

		// query time
		Cursor c = getContentResolver().query(contactUri, projection, null,
				null, null);

		// if the query returns 1 or more results
		// return the first result
		if (c.moveToFirst()) {
			String name = c.getString(c
					.getColumnIndex(Contacts.Phones.DISPLAY_NAME));
			return name;
		}

		// return the original number if no match was found
		return "Unknown";
	}

	public static void beginStartingService(Context context, Intent intent) {
		context.startService(intent);
	}

}