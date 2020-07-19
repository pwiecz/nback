package cz.wie.p.nback;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class IntPreference extends DialogPreference {
	private final Context mContext;
	private int mSelectedValue;
	private int mDialogSelectedValue;
	final private int mMaxValue;
	final private int mMinValue;
	private TextView mValueTextView;
	
	public IntPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IntPreference, 0, 0);
		mMaxValue = a.getInt(R.styleable.IntPreference_max, 100);
		mMinValue = a.getInt(R.styleable.IntPreference_min, 0);
		a.recycle();
	}
	
	private void setValue(int value) {
		mSelectedValue = value;
		updateValue();
	}
	
	@SuppressLint("SetTextI18n")
	private void updateTextView(int value) {
		if (mValueTextView != null)
			mValueTextView.setText(Integer.toString(value));
	}
	
	private int getValue() {
		return mSelectedValue;
	}
	
	private void updateValue() {
		updateTextView(mSelectedValue);
		persistInt(mSelectedValue);
	}
	
	@SuppressLint("SetTextI18n")
	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);

		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(20, 20, 20, 20);

		mValueTextView = new TextView(mContext);
		mValueTextView.setText(Integer.toString(mSelectedValue));
		layout.addView(mValueTextView);
		SeekBar seekBar = new SeekBar(mContext);
		seekBar.setMax(mMaxValue - mMinValue);
		seekBar.setProgress(mSelectedValue - mMinValue);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) { }
			
			public void onStartTrackingTouch(SeekBar seekBar) {	}
			
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				mDialogSelectedValue = progress + mMinValue;
				updateTextView(progress + mMinValue);
			}
		});
		layout.addView(seekBar);
		builder.setView(layout);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			setValue(mDialogSelectedValue);
		} else {
			updateTextView(mSelectedValue);
		}
		mValueTextView = null;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, 100);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mSelectedValue = restoreValue ? getPersistedInt(mSelectedValue) : (Integer) defaultValue;
		updateValue();
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		if (isPersistent()) {
			// No need to save instance state since it's persistent
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.value = getValue();
		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		setValue(myState.value);
	}
	
	private static class SavedState extends BaseSavedState {
		int value;

		public SavedState(Parcel source) {
			super(source);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(value);
		}

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR =
			new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
