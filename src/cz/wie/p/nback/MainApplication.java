package cz.wie.p.nback;

import android.app.Application;
import android.preference.PreferenceManager;

public final class MainApplication extends Application {
	Settings mSettings;
	SoundsInitializer mSoundsInitializer;

	@Override
	public void onCreate() {
		super.onCreate();
		mSettings = new Settings(PreferenceManager.getDefaultSharedPreferences(this), this);
		mSoundsInitializer = new SoundsInitializer(this);
	}

	public void initSoundsInBackground() {
		new Thread() {
			public void run() {
				mSoundsInitializer.initializeSounds();
			}
		}.start();	
	}

	public SoundsInitializer soundInitializer() {
		return mSoundsInitializer;
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		mSoundsInitializer.close();
	}
}
