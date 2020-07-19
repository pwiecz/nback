package cz.wie.p.nback;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public final class PreferencesActivity extends PreferenceActivity {
	private String mNBackLevelPreferenceStr;

	private void updateNBackLevelEnabled(Boolean autoSetLevel) {
		if (autoSetLevel) findPreference(mNBackLevelPreferenceStr).setEnabled(false);
		else findPreference(mNBackLevelPreferenceStr).setEnabled(true);
	}

	@Override protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mNBackLevelPreferenceStr = getResources().getString(R.string.n_back_level_preference);
		
		String autoSetLevelPreferenceStr = getResources().getString(R.string.auto_set_level_preference);
		Preference autoSetLevelPreference = findPreference(autoSetLevelPreferenceStr);
		boolean autoSetLevel = autoSetLevelPreference.getSharedPreferences().getBoolean(autoSetLevelPreferenceStr, false);
		updateNBackLevelEnabled(autoSetLevel);
		autoSetLevelPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				updateNBackLevelEnabled((Boolean)newValue);
				return true;
			}
		});
	}
}
