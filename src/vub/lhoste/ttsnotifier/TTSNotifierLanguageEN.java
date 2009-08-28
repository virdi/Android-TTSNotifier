package vub.lhoste.ttsnotifier;

public class TTSNotifierLanguageEN extends TTSNotifierLanguage {

	@Override
	public String getTTSShortName() {
		return "en";
	}

	@Override
	public String getTxtTest() {
		return "hello";
	}

	@Override
	public String getTxtUnknown() {
		return "Unknown";
	}
	
	@Override
	public String getTxtOptionsBatteryLowWarningText() {
		return "Battery is low";
	}

	@Override
	public String getTxtOptionsIncomingCall() {
		return "Phone call from %s";
	}

	@Override
	public String getTxtOptionsIncomingSMS() {
		return "New text message from %s";
	}

	@Override
	public String getTxtOptionsIncomingSMSBody() {
		return "New text message from %s about %s";
	}

	@Override
	public String getTxtOptionsMediaBadRemovalText() {
		return "Media removed before it was unmounted";
	}

	@Override
	public String getTxtOptionsMediaMountedText() {
		return "Media mounted";
	}

	@Override
	public String getTxtOptionsMediaUnMountedText() {
		return "Media unmounted";
	}

	@Override
	public String getTxtOptionsProviderChangedText() {
		return "Phone provider changed";
	}

	@Override
	public String getTxtOptionsWifiConnected() {
		return "Wyfy connected";
	}

	@Override
	public String getTxtOptionsWifiDisconnected() {
		return "Wyfy disconnected";
	}

	@Override
	public String getTxtOptionsWifiDiscovered() {
		return "Wyfy signal in range";
	}

}
