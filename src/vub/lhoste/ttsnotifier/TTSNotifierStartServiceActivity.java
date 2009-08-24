package vub.lhoste.ttsnotifier;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TTSNotifierStartServiceActivity extends Activity {
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		try {
			Intent svc = new Intent(this, TTSNotifierService.class);
			//svc.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			startService(svc);
		}
		catch (Exception e) { }
	}

	@Override protected void onDestroy() {
		super.onDestroy();
		Intent svc = new Intent(this, TTSNotifierService.class);
		stopService(svc);
	}
}
