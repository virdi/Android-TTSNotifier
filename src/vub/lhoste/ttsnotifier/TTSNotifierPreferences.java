package vub.lhoste.ttsnotifier;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class TTSNotifierPreferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		if (!TTSNotifierService.isTtsInstalled(this))
			Toast.makeText(this, "TTSNotifier: TTS not installed! Install it from the market!", Toast.LENGTH_LONG).show();
		Intent svc = new Intent(this, TTSNotifierService.class);
		startService(svc);
		//Get the custom preference
		Preference customPref = (Preference) findPreference("btnTestTTS");
		customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				TTSNotifierService.waitForSpeechInitialised();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				TTSNotifierService.setLanguage(prefs.getBoolean("cbxChangeLanguage", false), prefs.getString("txtLanguage", "English"));
				TTSNotifierService.speak(TTSNotifierService.myLanguage.getTxtTest(), false);
				return true;
			}

		});
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		TTSNotifierService.setLanguage(prefs.getBoolean("cbxChangeLanguage", false), prefs.getString("txtLanguage", "English"));
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		TTSNotifierService.setLanguage(prefs.getBoolean("cbxChangeLanguage", false), prefs.getString("txtLanguage", "English"));
	}
}
