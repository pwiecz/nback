package cz.wie.p.nback;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.KeyEvent;

public final class Settings {
	int nBackLevel;
	final ArrayList<Integer> features = new ArrayList<Integer>(); // list of selected features
	String featuresString;
	int numberOfFeatures;
	int elementsInSeries;
	int repetitionRatio;
	boolean randomTargets;
	boolean vibrateOnError;
	private final Context mContext;
	int symbolShowTime;
	int symbolHideTime;
	boolean autoSetLevel;
	int percentToAdvance;
	int percentToDrop;
	boolean byFeaturePercentage;
	final int[] featureKeys = new int[Common.sMaxFeatures];
	final String[] featureKeyStrings = new String[Common.sMaxFeatures];
	boolean showKeyShortcuts;
	boolean showResultsForAllLevels;
	boolean showResultsByFeature;
	boolean showTimeStatistics;
	int timeInterval;
	boolean highlightButtons;
	boolean showProgress;
	
	public Settings(SharedPreferences prefs, Context context) {
		mContext = context;
		readFromPrefs(prefs);
	}

	public void readFromPrefs(SharedPreferences prefs) {
		Resources res = mContext.getResources();
		nBackLevel = Integer.parseInt(prefs.getString(res.getString(R.string.n_back_level_preference), "2"));
		numberOfFeatures = Integer.parseInt(prefs.getString(res.getString(R.string.number_of_features_preference), "2"));
		featuresString = prefs.getString(res.getString(R.string.features_preference), defaultFeaturesStr());
		String[] featuresStrs = featuresString.split(",");
		features.clear();
		numberOfFeatures = 0;
		//assert featuresStrs.length == Common.sMaxFeatures;
		for (int i = 0; i < Math.min(featuresStrs.length, Common.sMaxFeatures); ++i) {
			//noinspection StatementWithEmptyBody
			if (!featuresStrs[i].equals("0")) {
				numberOfFeatures++;
				features.add(i);
			} else {
			}
		}
		elementsInSeries = nBackLevel + Integer.parseInt(prefs.getString(res.getString(R.string.series_length_preference), "20"));
		vibrateOnError = prefs.getBoolean(res.getString(R.string.vibrate_on_error_preference), true);
		repetitionRatio = Integer.parseInt(prefs.getString(res.getString(R.string.repetition_ratio_preference), "30"));
		randomTargets = prefs.getBoolean(res.getString(R.string.random_targets_preference), false);
		symbolShowTime = Integer.parseInt(prefs.getString(res.getString(R.string.time_symbol_shown_preference), "500"));
		symbolHideTime = Integer.parseInt(prefs.getString(res.getString(R.string.time_symbol_hidden_preference), "2500"));
		autoSetLevel = prefs.getBoolean(res.getString(R.string.auto_set_level_preference), false);
		percentToAdvance = prefs.getInt(res.getString(R.string.percent_to_advance_preference), 80);
		percentToDrop = prefs.getInt(res.getString(R.string.percent_to_drop_preference), 50);
		byFeaturePercentage = prefs.getBoolean(res.getString(R.string.by_feature_percentage_preference), true);
		featureKeyStrings[0] = prefs.getString(res.getString(R.string.position_keyshortcut_preference), "A");
		featureKeyStrings[1] = prefs.getString(res.getString(R.string.shape_keyshortcut_preference), "D");
		featureKeyStrings[2] = prefs.getString(res.getString(R.string.color_keyshortcut_preference), "H");
		featureKeyStrings[3] = prefs.getString(res.getString(R.string.sound_keyshortcut_preference), "L");
		for (int i = 0; i < featureKeyStrings.length; ++i) {
			featureKeys[i] = keyStringToKeyCode(featureKeyStrings[i]);
		}
		showKeyShortcuts = prefs.getBoolean(res.getString(R.string.show_keyshortcuts_preference), false);
		showResultsForAllLevels = prefs.getBoolean(res.getString(R.string.show_results_for_all_levels), false);
		showResultsByFeature = prefs.getBoolean(res.getString(R.string.show_results_by_feature), true);
		showTimeStatistics = prefs.getBoolean(res.getString(R.string.show_time_statistics_preference), true);
		timeInterval = Integer.parseInt(prefs.getString(res.getString(R.string.time_interval_preference), "0"));
		highlightButtons = prefs.getBoolean(res.getString(R.string.highlight_correctness_preference), true);
		showProgress = prefs.getBoolean(res.getString(R.string.show_progress_preference), true);
	}

	private String defaultFeaturesStr() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < numberOfFeatures; ++i) {
			if (i > 0) builder.append(',');
			builder.append('1');
		}
		for (int i = numberOfFeatures; i < Common.sMaxFeatures; ++i) {
			builder.append(",0");
		}		
		return builder.toString();
	}

//	NBackActivity.NBackGameMode currentMode() {
//		NBackActivity.NBackGameMode mode = new NBackActivity.NBackGameMode();
//		mode.backLevel = nBackLevel;
//		mode.features = featuresString;
//		mode.numFeatures = numberOfFeatures;
//		return mode;
//	}
	
	int keyStringToKeyCode(String key) {
		return KeyEvent.KEYCODE_A + (key.charAt(0) - 'A');
	}
}
