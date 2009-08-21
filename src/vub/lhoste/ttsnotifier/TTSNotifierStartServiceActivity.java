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
