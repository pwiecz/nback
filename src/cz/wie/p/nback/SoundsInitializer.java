package cz.wie.p.nback;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundsInitializer {
	private final MediaPlayer[] mMediaPlayers;

	private final List<OnSoundInitialized> callbacks = new LinkedList<OnSoundInitialized>();
	final Context mContext;
	private int soundsInitializationState;
	static final int SOUNDS_NOT_INITIALIZED = 0;
	static final int SOUNDS_INITIALIZING = 1;
	static final int SOUNDS_INITIALIZATION_FAILED = 2;
	static final int SOUNDS_INITIALIZED = 3;

	private static final String[] words = new String[] {
		"b", "h", "k", "m", "q", "r", "w", "y" 
	};

	public SoundsInitializer(Context context) {
		soundsInitializationState = SOUNDS_NOT_INITIALIZED;
		mContext = context;
		mMediaPlayers = new MediaPlayer[words.length];
	}

	public synchronized void close() {
		for (int i = 0; i < mMediaPlayers.length; ++i) {
			if (mMediaPlayers[i] != null) {
				mMediaPlayers[i].release();
				mMediaPlayers[i] = null;
			}
		}
	}

	public synchronized boolean areSoundsInitialized() {
		return soundsInitializationState == SOUNDS_INITIALIZED;
	}

	public synchronized MediaPlayer[] mediaPlayers() {
		return mMediaPlayers;
	}

	public interface OnSoundInitialized {
		void soundInitialized(boolean success);
	}

	public synchronized void addSoundInitializationCallback(OnSoundInitialized callback) {
		if (soundsInitializationState == SOUNDS_INITIALIZED) {
			callback.soundInitialized(true);
		} else if (soundsInitializationState == SOUNDS_INITIALIZATION_FAILED) {
			callback.soundInitialized(false);
		} else {
			callbacks.add(callback);
		}
	}

	private void failSoundInit() {
		synchronized (this) {
			if (soundsInitializationState == SOUNDS_INITIALIZATION_FAILED)
				return;
			soundsInitializationState = SOUNDS_INITIALIZATION_FAILED;
			for (OnSoundInitialized callback : callbacks) {
				callback.soundInitialized(false);
			}
			callbacks.clear();
		}
	}

	private static MediaPlayer createMediaPlayer(Context context, int resId,
                                                 MediaPlayer.OnPreparedListener onPrepared) {
		try {
			AssetFileDescriptor afd = context.getResources().openRawResourceFd(resId);
            MediaPlayer player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
            afd.close();
            player.setOnPreparedListener(onPrepared);
            player.prepareAsync();
			return player;
		} catch (IOException e) {
            Log.e("NBACK", "Error initializing media player " + e);
			return null;
		}
	}

    private static class PrepareBarrier implements MediaPlayer.OnPreparedListener {
        private int mNumNonPrepared;
        private final SoundsInitializer mInitializer;
        public PrepareBarrier(int numPlayers, SoundsInitializer initializer) {
            this.mNumNonPrepared = numPlayers;
            this.mInitializer = initializer;
        }

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            boolean invokeCallbacks = false;
            synchronized(this) {
                mNumNonPrepared--;
                if (mNumNonPrepared == 0) {
                    invokeCallbacks = true;
                } else if (mNumNonPrepared < 0) {
                    throw new AssertionError("Too many media players initialized");
                }
            }
            if (invokeCallbacks) {
                synchronized(mInitializer) {
                    mInitializer.soundsInitializationState = SOUNDS_INITIALIZED;
                    for (OnSoundInitialized callback : mInitializer.callbacks) {
                        callback.soundInitialized(true);
                    }
                    mInitializer.callbacks.clear();
                }
            }
        }
    }

    private static final int[] sSoundResources = new int[]{
            R.raw.b, R.raw.h, R.raw.k, R.raw.m,
            R.raw.q, R.raw.r, R.raw.w, R.raw.y};

	public void initializeSounds() {
		synchronized (this) {
			if (soundsInitializationState == SOUNDS_INITIALIZING ||
					soundsInitializationState == SOUNDS_INITIALIZED)
				return;
			soundsInitializationState = SOUNDS_INITIALIZING;
		}
        MediaPlayer.OnPreparedListener onPrepare = new PrepareBarrier(sSoundResources.length, this);
        for (int i = 0; i < sSoundResources.length; ++i) {
            mMediaPlayers[i] = createMediaPlayer(mContext, sSoundResources[i], onPrepare);
            if (mMediaPlayers[i] == null) {
                failSoundInit();
                return;
            }
        }
		synchronized (this) {
			soundsInitializationState = SOUNDS_INITIALIZED;
			for (OnSoundInitialized callback : callbacks) {
				callback.soundInitialized(true);
			}
			callbacks.clear();
		}
	}
}
