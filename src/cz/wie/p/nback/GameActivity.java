package cz.wie.p.nback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public final class GameActivity extends AppCompatActivity {
    private static final String CORRECTLY_GUESSED_FEATURES = "correctly_guessed_features";
    private static final String INCORRECTLY_GUESSED = "incorrectly_guessed";
    private static final String CORRECTLY_GUESSED = "correctly_guessed";
    private static final String CURRENT_ITEM = "current_item";
    private static final String LAST_PROCESSED_ITEM = "last_processed_item";
    private static final String INCORRECTLY_GUESSED_FEATURES = "incorrectly_guessed_features";
    private static final String ITEMS = "items";

    public static final String RESULT = "result";
    public static final String RESULTS = "results";
    public static final String PLAY_TIME = "playTime";

    MainApplication mApp;
    ActionBar mActionBar;
	BoardView mView;
	final Button[] mFeatureButtons = new Button[Common.sMaxButtons];
	ColorFilter mRedFilter;
	ColorFilter mGreenFilter;
	final boolean[] mGuessedFeature = new boolean[Common.sMaxFeatures];
	final boolean[] mButtonToUnpress = new boolean[Common.sMaxButtons];
	TextView mRight;
	TextView mWrong;
	TextView mRound;
	Vibrator mVibrator;
	AtomicInteger mCurrentItem = new AtomicInteger();
	AtomicInteger mLastProcessedItem = new AtomicInteger();
	int mCorrectlyGuessed;
	int mIncorrectlyGuessed;
	final int[] mCorrectlyGuessedFeatures = new int[Common.sMaxFeatures];
	final int[] mIncorrectlyGuessedFeatures = new int[Common.sMaxFeatures];
    ArrayList<Item> mItems = new ArrayList<>();

	final ItemGenerator mGenerator = new ItemGenerator();
	Thread mGameThread;
	final Handler mHandler = new Handler();

	Settings mSettings;
	MediaPlayer[] mMediaPlayers;
	long mGameStartTime;

    ViewGroup createButtons(ViewGroup parent, int numButtons) {
        LayoutInflater inflater = getLayoutInflater();
        switch (numButtons) {
            case 1: return (ViewGroup) inflater.inflate(R.layout.buttons_1, parent);
            case 2: return (ViewGroup) inflater.inflate(R.layout.buttons_2, parent);
            case 3: return (ViewGroup) inflater.inflate(R.layout.buttons_3, parent);
            case 4: return (ViewGroup) inflater.inflate(R.layout.buttons_4, parent);
            default: throw new AssertionError("Unexpected number of buttons: " + numButtons);
        }
    }

    Button getNthButton(int buttonIndex, View parent) {
        switch (buttonIndex) {
            case 0: return (Button) parent.findViewById(R.id.button1);
            case 1: return (Button) parent.findViewById(R.id.button2);
            case 2: return (Button) parent.findViewById(R.id.button3);
            case 3: return (Button) parent.findViewById(R.id.button4);
            default: throw new AssertionError("Unexpected button index: " + buttonIndex);
        }
    }

    ProgressDialog initSoundsDialog;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(false);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayShowTitleEnabled(false);
        Context actionBarContext = mActionBar.getThemedContext();
        LayoutInflater inflater = (LayoutInflater) actionBarContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.title, null);
        mActionBar.setCustomView(v);

		mApp = (MainApplication) getApplication();
		mSettings = mApp.mSettings;
		TextView leftText = (TextView) v.findViewById(R.id.title_left_text);
		leftText.setText(Common.gameModeToString(mSettings.numberOfFeatures, mSettings.nBackLevel));
		mRound = (TextView) v.findViewById(R.id.title_right_text);
		mView = (BoardView) findViewById(R.id.nbackview);
		ViewGroup buttonsExternalContainer = (ViewGroup) findViewById(R.id.buttons);
        ViewGroup buttonsContainer = createButtons(buttonsExternalContainer, mSettings.numberOfFeatures);
        for (int i = 0; i < mFeatureButtons.length; ++i) {
            mFeatureButtons[i] = getNthButton(i, buttonsContainer);
            mFeatureButtons[i].setOnClickListener(createButtonClickListener(i));
        }
        mRight = (TextView) findViewById(R.id.right);
		mWrong = (TextView) findViewById(R.id.wrong);
		mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mRedFilter = new PorterDuffColorFilter(Color.argb(64, 255, 0, 0), PorterDuff.Mode.SRC_ATOP);
		mGreenFilter = new PorterDuffColorFilter(Color.argb(64, 0, 255, 0), PorterDuff.Mode.SRC_ATOP);
        for (int featureNum = 0; featureNum < mFeatureButtons.length; ++featureNum) {
            if (featureNum < mSettings.numberOfFeatures) {
                int feature = mSettings.features.get(featureNum);
                String label = Common.sFeatureNames[feature];
                if (mSettings.showKeyShortcuts)
                    label += " (" + mSettings.featureKeyStrings[feature] + ")";
                mFeatureButtons[featureNum].setText(label);
            }
        }
        if (mSettings.features.contains(Common.sSound)) {
            mApp.initSoundsInBackground();
            if (!mApp.soundInitializer().areSoundsInitialized())
                initSoundsDialog =
                        ProgressDialog.show(this, null, getResources().getText(R.string.sound_initializing_in_process), true, true);
            else
                initSoundsDialog = null;
        }
        if (savedInstanceState == null) {
            mCurrentItem.set(0);
            mLastProcessedItem.set(0);
            mCorrectlyGuessed = 0;
            mIncorrectlyGuessed = 0;
            Arrays.fill(mCorrectlyGuessedFeatures, 0);
            Arrays.fill(mIncorrectlyGuessedFeatures, 0);
            if (mSettings.randomTargets)
                mItems = mGenerator.createItemsRandom(mSettings.elementsInSeries, mSettings.repetitionRatio, mSettings.nBackLevel);
            else
                mItems = mGenerator.createItemsDeterministic(mSettings.elementsInSeries, mSettings.numberOfFeatures, mSettings.repetitionRatio, mSettings.nBackLevel);
        } else {
            mCurrentItem.set(savedInstanceState.getInt(CURRENT_ITEM));
            mLastProcessedItem.set(savedInstanceState.getInt(LAST_PROCESSED_ITEM));
            mItems = savedInstanceState.getParcelableArrayList(ITEMS);
            mCorrectlyGuessed = savedInstanceState.getInt(CORRECTLY_GUESSED);
            mIncorrectlyGuessed = savedInstanceState.getInt(INCORRECTLY_GUESSED);
            System.arraycopy(savedInstanceState.getIntArray(CORRECTLY_GUESSED_FEATURES), 0,
                    mCorrectlyGuessedFeatures, 0, mCorrectlyGuessedFeatures.length);
            System.arraycopy(savedInstanceState.getIntArray(INCORRECTLY_GUESSED_FEATURES), 0,
                    mIncorrectlyGuessedFeatures, 0, mIncorrectlyGuessedFeatures.length);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENT_ITEM, mCurrentItem.get());
        outState.putInt(LAST_PROCESSED_ITEM, mLastProcessedItem.get());
        outState.putParcelableArrayList(ITEMS, mItems);
        outState.putInt(CORRECTLY_GUESSED, mCorrectlyGuessed);
        outState.putInt(INCORRECTLY_GUESSED, mIncorrectlyGuessed);
        outState.putIntArray(CORRECTLY_GUESSED_FEATURES, mCorrectlyGuessedFeatures);
        outState.putIntArray(INCORRECTLY_GUESSED_FEATURES, mIncorrectlyGuessedFeatures);
	}

	@Override
	protected void onResume() {
		super.onResume();
		final int elementsInSeries = mSettings.elementsInSeries;
        final int symbolShowTime = mSettings.symbolShowTime;
        final int symbolHideTime = mSettings.symbolHideTime;
        final boolean highlightButtons = mSettings.highlightButtons;
        mGameThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                for (; mCurrentItem.get() < elementsInSeries; mCurrentItem.incrementAndGet()) {
                    if (Thread.interrupted()) {
                        return;
                    }
                    mHandler.post(showItemRunnable);
                    try {
                        Thread.sleep(symbolShowTime);
                    } catch (InterruptedException e) {
                        return;
                    }

                    if (Thread.interrupted()) {
                        return;
                    }
                    mHandler.post(hideItemRunnable);
                    try {
                        Thread.sleep(symbolHideTime);
                    } catch (InterruptedException e) {
                        return;
                    }
                    mLastProcessedItem.set(mCurrentItem.get());
                    mHandler.post(evaluateUserResponseRunnable);
                    if (highlightButtons) {
                        mHandler.post(removeButtonColorFiltersRunnable);
                        if (mCurrentItem.get() < elementsInSeries - 1)
                            mHandler.postDelayed(depressButtonsRunnable, 500);
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                mHandler.post(endGameRunnable);
            }
        };
		if (mSettings.features.contains(Common.sSound)) {
			mApp.soundInitializer().addSoundInitializationCallback(
					new SoundsInitializer.OnSoundInitialized() {
						public void soundInitialized(final boolean success) {
							if (initSoundsDialog != null) {
                                initSoundsDialog.dismiss();
                                initSoundsDialog = null;
                            }
							mHandler.post(new Runnable() {
								public void run() {
									if (!success) {
										AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
										builder.setMessage(getResources().getText(R.string.sound_initializing_failure)).show();
										finish();
									} else {
                                        mMediaPlayers = mApp.soundInitializer().mediaPlayers();
                                        startGame();
                                    }
								}});
						}
					});
		} else {
			startGame();
		}
	}

    @Override
    protected void onStop() {
        super.onStop();
    }

	@Override
	protected void onPause() {
		super.onPause();
        if (mGameThread != null) {
			mGameThread.interrupt();
			mGameThread = null;
		}
	}

	View.OnClickListener createButtonClickListener(final int number) {
		return new View.OnClickListener() {
			public void onClick(View v) {
				if (mGuessedFeature[number])
					return;
				mGuessedFeature[number] = true;
                int currentItem = mCurrentItem.get();
				if (currentItem < mSettings.elementsInSeries && currentItem >= mSettings.nBackLevel &&
						mItems.get(currentItem).sameFeature(number, mItems.get(currentItem - mSettings.nBackLevel))) {
					mCorrectlyGuessed++;
					mCorrectlyGuessedFeatures[number]++;
					if (mSettings.highlightButtons) {
                        Drawable b = getResources().getDrawable(android.R.drawable.btn_default).mutate();
                        b.setColorFilter(mGreenFilter);
                        v.setBackgroundDrawable(b);
						v.invalidate();
					}
				} else {
					mIncorrectlyGuessed++;
					mIncorrectlyGuessedFeatures[number]++;
					if (mVibrator != null && mSettings.vibrateOnError) {
						mVibrator.vibrate(100);
					}
					if (mSettings.highlightButtons) {
                        Drawable b = getResources().getDrawable(android.R.drawable.btn_default).mutate();
                        b.setColorFilter(mRedFilter);
                        v.setBackgroundDrawable(b);
                        v.invalidate();
                    }
				}
				updateInterface();
			}};
	}

	private void startGame() {
		mGameStartTime = System.currentTimeMillis();

		updateInterface();
        // The thread may be null if onPause is called just after onResume.
        if (mGameThread != null) {
            mGameThread.start();
        }
	}

	private void endGame() {
		final long elapsedTimeMillis = System.currentTimeMillis() - mGameStartTime;
		final int correctlyGuessedRatio;
		if (mCorrectlyGuessed + mIncorrectlyGuessed == 0)
			correctlyGuessedRatio = 1;
		else
			correctlyGuessedRatio = 100 * mCorrectlyGuessed / (mCorrectlyGuessed + mIncorrectlyGuessed);

		final int[] correctlyGuessedRatios = new int[mSettings.numberOfFeatures];
		for (int i = 0; i < mSettings.numberOfFeatures; ++i) {
			if (mCorrectlyGuessedFeatures[i] + mIncorrectlyGuessedFeatures[i] == 0) {
				correctlyGuessedRatios[i] = 100;
			} else {
				correctlyGuessedRatios[i] = 100 * mCorrectlyGuessedFeatures[i] / (mCorrectlyGuessedFeatures[i] + mIncorrectlyGuessedFeatures[i]);
			}
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		StringBuilder messageBuilder = new StringBuilder("Result (total): ");
		messageBuilder.append(correctlyGuessedRatio).append("%\n");
		for (int i = 0, L = mSettings.numberOfFeatures; i < L; ++i) {
			messageBuilder.append("\n")
			.append(Common.sFeatureNames[mSettings.features.get(i)]).append(": ")
			.append(correctlyGuessedRatios[i]).append("%");
		}
		builder.setMessage(messageBuilder.toString())
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra(RESULT, correctlyGuessedRatio);
				resultIntent.putExtra(RESULTS, correctlyGuessedRatios);
				resultIntent.putExtra(PLAY_TIME, elapsedTimeMillis);
				setResult(RESULT_OK, resultIntent);
				stopGame();
			}
		})
		.setPositiveButton("Close", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int button) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra(RESULT, correctlyGuessedRatio);
				resultIntent.putExtra(RESULTS, correctlyGuessedRatios);
				resultIntent.putExtra(PLAY_TIME, elapsedTimeMillis);
				setResult(RESULT_OK, resultIntent);
				stopGame();
			}});
		builder.create().show();
	}

	private void stopGame() {
		if (mGameThread != null) {
			mGameThread.interrupt();
			mGameThread = null;
		}
		finish();
	}

	void updateInterface() {
		if (mSettings.showProgress) {
            Resources res = getResources();
			mRight.setText(res.getString(R.string.right_answers_label, mCorrectlyGuessed));
			mWrong.setText(res.getString(R.string.wrong_answers_label, mIncorrectlyGuessed));
			mRound.setText(res.getString(R.string.round_number_label,
                    Math.min(mCurrentItem.get() + 1, mSettings.elementsInSeries),
                    mSettings.elementsInSeries));
		}
		mView.invalidate();
	}

	final Runnable showItemRunnable = new Runnable() {
		public void run() {
            int currentItem = mCurrentItem.get();
			if (currentItem < mSettings.elementsInSeries) {
				for (int feature = 0; feature < mGuessedFeature.length; ++feature)
					mGuessedFeature[feature] = false;
				Item item = mItems.get(currentItem);
				mView.showItem(item, mSettings);
				if (item.sound(mSettings) >= 0 && mApp.soundInitializer().areSoundsInitialized()) {
					MediaPlayer player = mMediaPlayers[item.sound(mSettings)];
					if (player != null) player.start();
				}
			}
			updateInterface();
		}
	};

	final Runnable hideItemRunnable = new Runnable() {
		public void run() {
			mView.hideItem();
			updateInterface();
		}
	};

	final Runnable depressButtonsRunnable = new Runnable() {
		public void run() {
			for (Button button : mFeatureButtons) {
                if (button.isPressed()) {
                    button.setPressed(false);
                    button.invalidate();
                }
			}
		}
	};

	final Runnable removeButtonColorFiltersRunnable = new Runnable() {
		public void run() {
            for (Button button : mFeatureButtons) {
                if (button.getVisibility() == View.VISIBLE) {
                    Drawable background = getResources().getDrawable(android.R.drawable.btn_default).mutate();
                    background.clearColorFilter();
                    button.setBackgroundDrawable(background);
                    button.invalidate();
                }
			}
		}
	};
	
	final Runnable evaluateUserResponseRunnable = new Runnable() {
		public void run() {
            int lastProcessedItem = mLastProcessedItem.get();
			if (lastProcessedItem < mSettings.elementsInSeries && lastProcessedItem >= mSettings.nBackLevel) {
				Item current = mItems.get(lastProcessedItem);
				Item previous = mItems.get(lastProcessedItem - mSettings.nBackLevel);
				boolean madeError = false;
				for (int feature = 0; feature < mSettings.numberOfFeatures; ++feature) {
					if (current.sameFeature(feature, previous)) {
						if (mSettings.highlightButtons) {
							mFeatureButtons[feature].setPressed(true);
						}
						if (!mGuessedFeature[feature]) {
							if (mSettings.highlightButtons) {
                                Drawable b = getResources().getDrawable(android.R.drawable.btn_default).mutate();
                                b.setColorFilter(mRedFilter);
                                mFeatureButtons[feature].setBackgroundDrawable(b);
								mFeatureButtons[feature].invalidate();
							}
							mIncorrectlyGuessedFeatures[feature]++;
							mIncorrectlyGuessed++;
							madeError = true;
						}
					}
					mButtonToUnpress[feature] = false;
				}
				if (madeError && mVibrator != null && mSettings.vibrateOnError)
					mVibrator.vibrate(100);
			}
			updateInterface();
		}
	};

	final Runnable endGameRunnable = new Runnable() {
		public void run() {
			endGame();
		}
	};


	private Runnable createUnpressButtonRunnable(final int button) {
		return new Runnable() {
			public void run() {
				if (mButtonToUnpress[button])
					mFeatureButtons[button].setPressed(false);
				mButtonToUnpress[button] = false;
			}
		};
	}

	private void pressButton(int i) {
		mFeatureButtons[i].performClick();
		mFeatureButtons[i].setPressed(true);
		mButtonToUnpress[i] = true;
		mHandler.postDelayed(createUnpressButtonRunnable(i), sUnpressDelay);
	}
	private static final int sUnpressDelay = 100;

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (super.onKeyUp(keyCode, event)) return true;
		switch (keyCode) {
		case KeyEvent.KEYCODE_1:
			pressButton(0);
			return true;
		case KeyEvent.KEYCODE_2:
			pressButton(1);
			return true;
		case KeyEvent.KEYCODE_3:
			pressButton(2);
			return true;
		}
		int selectedFeature = -1;
		for (int i = 0; i < mSettings.featureKeys.length; ++i) {
			if (keyCode == mSettings.featureKeys[i]) {
				selectedFeature = i;
				break;
			}
		}
		if (selectedFeature == -1)
			return false;
		for (int i = 0; i < mSettings.features.size(); ++i) {
			if (mSettings.features.get(i) == selectedFeature) {
				pressButton(i);
				break;
			}
		}
		return true;
	}
}
