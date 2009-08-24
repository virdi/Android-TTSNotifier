package vub.lhoste.ttsnotifier;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class TTSNotifierPreferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		if (!TTSNotifierService.isTtsInstalled(this))
			Toast.makeText(this, "TTSNotifier: TTS not installed! Install it from the market!", Toast.LENGTH_LONG).show();
	}
}
