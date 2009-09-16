package vub.lhoste.ttsnotifier;

import java.util.Locale;

public abstract class TTSNotifierLanguage {
	abstract public Locale getLocale();
	abstract public String getTxtTest();
	abstract public String getTxtUnknown();
	abstract public String getTxtOptionsIncomingCall();
	abstract public String getTxtOptionsIncomingSMS();
	abstract public String getTxtOptionsIncomingSMSBody();
	abstract public String getTxtOptionsBatteryLowWarningText();
	abstract public String getTxtOptionsMediaBadRemovalText();
	abstract public String getTxtOptionsProviderChangedText();
	abstract public String getTxtOptionsMediaMountedText();
	abstract public String getTxtOptionsMediaUnMountedText();
	abstract public String getTxtOptionsWifiDiscovered();
	abstract public String getTxtOptionsWifiConnected();
	abstract public String getTxtOptionsWifiDisconnected();
}
