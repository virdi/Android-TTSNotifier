package vub.lhoste.ttsnotifier;

public class TTSNotifierLanguageDE extends TTSNotifierLanguage {

	@Override
	public String getTTSShortName() {
		return "de";
	}

	@Override
	public String getTxtTest() {
		return "hallo";
	}

	@Override
	public String getTxtUnknown() {
		return "Unbekannt";
	}
	
	@Override
	public String getTxtOptionsBatteryLowWarningText() {
		return "Akku ist fast leer";
	}

	@Override
	public String getTxtOptionsIncomingCall() {
		return "Anruf von %s";
	}

	@Override
	public String getTxtOptionsIncomingSMS() {
		return "Neue SMS von %s";
	}

	@Override
	public String getTxtOptionsIncomingSMSBody() {
		return "Neue SMS von %s über %s";
	}

	@Override
	public String getTxtOptionsMediaBadRemovalText() {
		return "Media entfernt, bevor sie getrennt";
	}

	@Override
	public String getTxtOptionsMediaMountedText() {
		return "Media verbunden";
	}

	@Override
	public String getTxtOptionsMediaUnMountedText() {
		return "Media getrennt";
	}

	@Override
	public String getTxtOptionsProviderChangedText() {
		return "Telefon-Anbieter gewechselt";
	}

	@Override
	public String getTxtOptionsWifiConnected() {
		return "WiFi verbunden";
	}

	@Override
	public String getTxtOptionsWifiDisconnected() {
		return "WiFi getrennt";
	}

	@Override
	public String getTxtOptionsWifiDiscovered() {
		return "Open wifi gefunden";
	}

}
