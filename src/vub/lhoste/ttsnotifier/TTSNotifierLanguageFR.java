package vub.lhoste.ttsnotifier;

import java.util.Locale;

public class TTSNotifierLanguageFR extends TTSNotifierLanguage {

	// Don't mind the spelling, it's hard to let the TTS speak french >.>
	
	@Override
	public Locale getLocale() {
		return Locale.FRENCH;
	}

	@Override
	public String getTxtTest() {
		return "Bonjour";
	}

	@Override
	public String getTxtUnknown() {
		return "Inconnu";
	}
	
	@Override
	public String getTxtOptionsBatteryLowWarningText() {
		return "La batterie est faible";
	}

	@Override
	public String getTxtOptionsIncomingCall() {
		return "Appel de %s";
	}

	@Override
	public String getTxtOptionsIncomingSMS() {
		return "Nouveau message a partir de %s";
	}

	@Override
	public String getTxtOptionsIncomingSMSBody() {
		return "Nouveau message a partir de %s pour %s";
	}

	@Override
	public String getTxtOptionsMediaBadRemovalText() {
		return "Media enlever avant deconnecter";
	}

	@Override
	public String getTxtOptionsMediaMountedText() {
		return "Media connecter";
	}

	@Override
	public String getTxtOptionsMediaUnMountedText() {
		return "Media deeconnectee";
	}

	@Override
	public String getTxtOptionsProviderChangedText() {
		return "Fournisseur de telephonie changer";
	}

	@Override
	public String getTxtOptionsWifiConnected() {
		return "WiFi connecter";
	}

	@Override
	public String getTxtOptionsWifiDisconnected() {
		return "WiFi deeconnectee";
	}

	@Override
	public String getTxtOptionsWifiDiscovered() {
		return "WiFi ouvert trouver";
	}

}
