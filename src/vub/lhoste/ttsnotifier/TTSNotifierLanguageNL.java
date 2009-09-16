package vub.lhoste.ttsnotifier;

import java.util.Locale;

public class TTSNotifierLanguageNL extends TTSNotifierLanguage {

	@Override
	public Locale getLocale() {
		return Locale.ENGLISH;
	}

	@Override
	public String getTxtTest() {
		return "hallo";
	}

	@Override
	public String getTxtUnknown() {
		return "Onbekend";
	}
	
	@Override
	public String getTxtOptionsBatteryLowWarningText() {
		return "Batterij bijna leeg";
	}

	@Override
	public String getTxtOptionsIncomingCall() {
		return "%s belt";
	}

	@Override
	public String getTxtOptionsIncomingSMS() {
		return "%s stuurde een bericht";
	}

	@Override
	public String getTxtOptionsIncomingSMSBody() {
		return "Nieuw bericht van %s over %s";
	}

	@Override
	public String getTxtOptionsMediaBadRemovalText() {
		return "Kaart verwijderd zonder los te koppelen";
	}

	@Override
	public String getTxtOptionsMediaMountedText() {
		return "Kaart gekoppeld";
	}

	@Override
	public String getTxtOptionsMediaUnMountedText() {
		return "Kaart losgekoppeld";
	}

	@Override
	public String getTxtOptionsProviderChangedText() {
		return "Provider gewijzigd";
	}

	@Override
	public String getTxtOptionsWifiConnected() {
		return "Draadloos internet verbonden";
	}

	@Override
	public String getTxtOptionsWifiDisconnected() {
		return "Draadloos internet verbroken";
	}

	@Override
	public String getTxtOptionsWifiDiscovered() {
		return "Open draadloos internet gevonden";
	}
}
