package vub.lhoste.ttsnotifier;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TTSNotifierPreferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
