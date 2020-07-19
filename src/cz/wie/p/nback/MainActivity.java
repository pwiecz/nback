package cz.wie.p.nback;

import java.util.ArrayList;
import java.util.Collections;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public final class MainActivity extends AppCompatActivity implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
	MainApplication mApp;
	private Settings mSettings;
	private ResultsDb mResultsDb;
	private ResultsView mResultsView;
	private final TextView[] mLabels = new TextView[Common.sMaxButtons + 1];
	private Button mNewButton;
	TextView mTitleLeftText;
	TextView mTitleRightText;
    ActionBar mActionBar;
	final Handler mHandler = new Handler();
	// Activity codes;
	private static final int SHOW_SETTINGS = 0;
	private static final int START_GAME = 1;

	void resetTitle() {
		mTitleLeftText.setText(Common.gameModeToString(mSettings.numberOfFeatures, mSettings.nBackLevel));
		mTitleRightText.setText("");
		if (mSettings.showTimeStatistics) {
			new Thread() {
				@Override
				public void run() {
					String interval = getResources().getStringArray(
							R.array.time_interval_entries)[mSettings.timeInterval] + ": ";
					int days = 0;
					if (mSettings.timeInterval == Common.TODAY) {
						days = 1;
					} else if (mSettings.timeInterval == Common.LAST_7_DAYS) {
						days = 7;
					} else if (mSettings.timeInterval == Common.LAST_30_DAYS) {
						days = 30;
					} else if (mSettings.timeInterval == Common.TOTAL) {
						days = 300000;
					}
					int time;
					try {
						time = mResultsDb.getTimePlayedForDays(days) / 1000;
					} catch (IllegalStateException e) {
						// We might have already closed the database if we're closing - let's ignore it.
						return;
					}
					int sec = time % 60;
					time /= 60;
					int min = time % 60;
					time /= 60;
					final StringBuilder builder = new StringBuilder(interval);
					if (time > 0)
						builder.append(time).append("h");
					if (min > 0)
						builder.append(min).append("m");
					if (sec > 0 || (time == 0 && min == 0))
						builder.append(sec).append("s");
					mHandler.post(new Runnable() {
						public void run() {
							mTitleRightText.setText(builder.toString());
						}
					});
				}
			}.start();
		}
	}

	void updateAutoLevel(boolean notify) {
		if (mSettings.autoSetLevel) {
			int newLevel = mResultsDb.suggestNBackLevel(mSettings.features,
					mSettings.percentToAdvance, mSettings.percentToDrop, mSettings.byFeaturePercentage);
			if (notify && newLevel != mSettings.nBackLevel) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				if (newLevel > mSettings.nBackLevel) {
					builder.setMessage(getResources().getString(R.string.level_advance, newLevel));
				} else {
					builder.setMessage(getResources().getString(R.string.level_drop, newLevel));
				}
				builder
				.setNegativeButton(getResources().getText(R.string.close_button_label), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}})
					.show();
			}
			mSettings.nBackLevel = newLevel;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(false);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(false);
        Context actionBarContext = mActionBar.getThemedContext();
        LayoutInflater inflater = (LayoutInflater) actionBarContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.title_with_button, null);
        mActionBar.setCustomView(v);
        mTitleLeftText = (TextView)v.findViewById(R.id.title_left_text);
        mTitleRightText = (TextView)v.findViewById(R.id.title_right_text);
        mTitleRightText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_DOWN)
                    return false;

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Choose time interval:")
                        .setSingleChoiceItems(R.array.time_interval_entries, mSettings.timeInterval,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,	int which) {
                                        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                                        prefEditor.putString(MainActivity.this.getResources().getString(R.string.time_interval_preference),
                                                Integer.toString(which));
                                        prefEditor.commit();
                                        mSettings.timeInterval = which;
                                        resetTitle();
                                    }})
                        .setPositiveButton("OK", null)
                        .show();
                return true;
            }
        });

        mApp = (MainApplication) getApplication();
		mSettings = mApp.mSettings;
		mNewButton = (Button)findViewById(R.id.new_button);
		mNewButton.setOnClickListener(this);
		mResultsDb = new ResultsDb(this);
		mResultsView = (ResultsView)findViewById(R.id.results);
		mLabels[0] = (TextView)findViewById(R.id.label1);
		mLabels[1] = (TextView)findViewById(R.id.label2);
		mLabels[2] = (TextView)findViewById(R.id.label3);
		mLabels[3] = (TextView)findViewById(R.id.label4);
        mLabels[4] = (TextView)findViewById(R.id.label5);
		updateAutoLevel(false);
		resetTitle();
		mResultsView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() != MotionEvent.ACTION_DOWN)
					return false;

				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				if (!mSettings.autoSetLevel) {
					builder.setMultiChoiceItems(R.array.results_options,
							new boolean[]{mSettings.showResultsByFeature, mSettings.showResultsForAllLevels},
							new DialogInterface.OnMultiChoiceClickListener() {
						public void onClick(DialogInterface dialog, int which, boolean isChecked) {
							final String[] resultPrefs = new String[] {
									MainActivity.this.getResources().getString(R.string.show_results_by_feature),
									MainActivity.this.getResources().getString(R.string.show_results_for_all_levels)
							};
							SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
							prefEditor.putBoolean(resultPrefs[which], isChecked);
							prefEditor.commit();
						}
					});
				}
				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						// I don't know why the following code used to be here.
                        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        // mSettings.readFromPrefs(prefs);
						showCurrentResults();
					}
				});
				if (!mResultsView.empty()) {
					builder.setNeutralButton(getResources().getString(R.string.clear_history),
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							new AlertDialog.Builder(MainActivity.this)
							.setMessage(getResources().getString(R.string.clear_history_confirmation))
							.setPositiveButton(getResources().getString(R.string.yes),
                                    new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
									mResultsDb.clear();
									showCurrentResults();
                                    resetTitle();
									updateAutoLevel(false);
								}})
                            .setNegativeButton(getResources().getString(R.string.no),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                            .show();
						}
					})
                    .setNegativeButton(getResources().getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {}
                    });
				}
				builder.show();
				return true;
			}
		});
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}
	

	public void onClick(View v) {
		if (v == mNewButton) {
			Intent intent = new Intent(this, GameActivity.class);
			startActivityForResult(intent, START_GAME);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mSettings.features.contains(Common.sSound))
			//if (mSettings.selectedFeatures[Common.sSound])
			mApp.initSoundsInBackground();
		showCurrentResults();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		mResultsDb.close();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		//noinspection StatementWithEmptyBody
		if (requestCode == SHOW_SETTINGS) {
//			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//			mSettings.readFromPrefs(prefs);
//			updateAutoLevel(false);
//			resetTitle();
		} else if (requestCode == START_GAME) {
			if (resultCode == RESULT_OK) {
				int[] results = data.getIntArrayExtra(GameActivity.RESULTS);
				int result = data.getIntExtra(GameActivity.RESULT, 100);
				long playTimeMillis = data.getLongExtra(GameActivity.PLAY_TIME, 0);
				mResultsDb.recordResult(result, results, playTimeMillis, mSettings);
				updateAutoLevel(true);
				resetTitle();
			}
		}
	}

	private static final int MENU_SETTINGS = 0;
	private static final int MENU_INSTRUCTIONS = 1;
	//	private static final int MENU_RESULTS = 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int pos = 0;
		menu.add(pos, MENU_SETTINGS, pos++, R.string.settings_menu_option);
		//menu.add(pos, MENU_RESULTS, pos++, "Results");
		menu.add(pos, MENU_INSTRUCTIONS, pos/*++*/, R.string.instructions_menu_option);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch(menuItem.getItemId()) {
		case MENU_SETTINGS:
			Intent intent = new Intent(this, PreferencesActivity.class);
			startActivityForResult(intent, SHOW_SETTINGS);
			return true;
			//		case MENU_RESULTS:
			//			showModesGroups();
			//			return true;
		case MENU_INSTRUCTIONS:
			showInstructions();
			return true;
		}			
		return false;
	}

	private void showInstructions() {
		String instructions = getResources().getText(R.string.instructions_text).toString();
		instructions = instructions.replace("%game_mode%", Common.gameModeToString(mSettings.numberOfFeatures, mSettings.nBackLevel));
		instructions = instructions.replace("%features_adj%", Common.numberOfFeaturesAdj(mSettings.numberOfFeatures));
		instructions = instructions.replace("%features%", Integer.toString(mSettings.numberOfFeatures));
		instructions = instructions.replace("%features_list%", featuresListString());
		instructions = instructions.replace("%n%", Integer.toString(mSettings.nBackLevel));
		new AlertDialog.Builder(this)
		.setMessage(instructions)
		.setPositiveButton("Close", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) { }
		})
		.show();
	}

	String featuresListString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Common.sFeatureNames[mSettings.features.get(0)]);
		for (int i = 1; i < mSettings.numberOfFeatures - 1; ++i) {
			builder.append(", ").append(Common.sFeatureNames[mSettings.features.get(i)]);
		}
		if (mSettings.numberOfFeatures > 1) {
			builder.append(" and ").append(Common.sFeatureNames[mSettings.features.get(mSettings.numberOfFeatures - 1)]);
		}
		return builder.toString();
	}

	void showCurrentResults() {
		GameMode gameMode = new GameMode();
		gameMode.featuresString = mSettings.featuresString;
		gameMode.modesGroup = new GameModesGroup();
		gameMode.modesGroup.backLevel = mSettings.showResultsForAllLevels ? -1 : mSettings.nBackLevel;
		gameMode.modesGroup.numFeatures = mSettings.numberOfFeatures;
		ArrayList<ResultsView.Results> results;
		int top;
		if (mSettings.autoSetLevel) {
			results = mResultsDb.getLastLevels(mSettings.features);//mSettings.selectedFeatures);
			ArrayList<Integer> levels = results.get(0).results;
			levels.add(mSettings.nBackLevel);
			top = Collections.max(levels);
		} else {
			results = mResultsDb.getResults(gameMode);
			top = 100;
		}
		if (!mSettings.showResultsByFeature)
			results = new ArrayList<>(results.subList(0, 1));
		mResultsView.setResults(results, top);
		int i;
		for (i = 0; i < results.size(); ++i) {
			String label = results.get(i).label;
			mLabels[i].setText(label);
			PaintDrawable icon = new PaintDrawable(ResultsView.colors[i]);
			icon.setBounds(0, 0, 10, 10);
			icon.setCornerRadius(5);
			mLabels[i].setCompoundDrawables(icon, null, null, null);
		}
		for (; i < mLabels.length; ++i) {
			mLabels[i].setText("");
			mLabels[i].setCompoundDrawables(null, null, null, null);
		}
		mResultsView.invalidate();
	}



	public static class GameModesGroup {
		public int numFeatures;
		public int backLevel;
		public String toString() {
			return Common.gameModeToString(numFeatures, backLevel);
		}
	}

	public static class GameMode {
		public String featuresString;
		public GameModesGroup modesGroup;
		public String toString() {
			if (featuresString == null)
				return "All";
			ArrayList<Integer> features = ResultsDb.featureList(featuresString);
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < features.size(); ++i) {
				if (i > 0) builder.append(", ");
				builder.append(Common.sFeatureNames[features.get(i)]);
			}
			return builder.toString();
		}
	}

    @Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
		                                  String key) {
		mSettings.readFromPrefs(sharedPreferences);
		updateAutoLevel(false);
		resetTitle();
	}
}