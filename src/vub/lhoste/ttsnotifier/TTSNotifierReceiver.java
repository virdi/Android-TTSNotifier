package vub.lhoste.ttsnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TTSNotifierReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		intent.setClass(context, TTSNotifierService.class);
		intent.putExtra("result", getResultCode());
		TTSNotifierService.beginStartingService(context, intent);
	}
	

}
