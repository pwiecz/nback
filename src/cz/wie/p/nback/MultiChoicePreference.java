package cz.wie.p.nback;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class MultiChoicePreference extends DialogPreference {
	private CharSequence[] mEntries;
	private boolean[] mSelected;
	private boolean[] mDialogSelected;
	private int mNumSelected;
	private int mDialogNumSelected;
	private String mValue;

	public MultiChoicePreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultiChoicePreference, 0, 0);
		mEntries = a.getTextArray(R.styleable.MultiChoicePreference_entries);
		setValue(new boolean[mEntries.length]);
		a.recycle();
	}

	public boolean[] getValue() {
		return mSelected;
	}

	public void setValue(boolean[] value) {
		if (value.length != mEntries.length) {
			throw new AssertionError("Num values: " + value.length + " != "
					+ " num entries: " + mEntries.length);
		}
		mSelected = value;
		mNumSelected = 0;
		for (boolean b : mSelected) {
			if (b) mNumSelected++;
		}
		updateValue();
	}

	private void updateValue() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < mSelected.length; ++i) {
			if (i > 0) builder.append(',');
			builder.append(mSelected[i] ? '1' : '0');
		}
		mValue = builder.toString();
		persistString(mValue);
	}

	private void updateSelected() {
		String[] split = mValue.split(",");
		boolean[] selected = new boolean[mSelected.length];
		for (int i = 0; i < Math.min(split.length, mSelected.length); ++i) {//split.length; ++i)
			selected[i] = !split[i].equals("0");
		}
		for (int i = split.length; i < mSelected.length; ++i) {
			selected[i] = false;
		}
		setValue(selected);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);

		if (mEntries == null) {
			throw new IllegalStateException(
			"MultiChoicePreference requires an entries array.");
		}
		mDialogSelected = new boolean[mSelected.length];
		mDialogNumSelected = mNumSelected;
		System.arraycopy(mSelected, 0, mDialogSelected, 0, mSelected.length);

		builder.setMultiChoiceItems(mEntries, mDialogSelected, 
				new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				AlertDialog alertDialog = (AlertDialog)dialog;
				MultiChoicePreference.this.mDialogSelected[which] = isChecked;
				if (isChecked) mDialogNumSelected++;
				else mDialogNumSelected--;
				if (mDialogNumSelected < 1 || mDialogNumSelected > Common.sMaxButtons) {
					alertDialog.getButton(Dialog.BUTTON_POSITIVE).setClickable(false);
					alertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(false);
				} else {
					alertDialog.getButton(Dialog.BUTTON_POSITIVE).setClickable(true);
					alertDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(true);
				}
			}
		});
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult && mEntries != null) {
			if (callChangeListener(mDialogSelected)) {
				setValue(mDialogSelected);
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getString(index);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mValue = restoreValue ? getPersistedString(mValue) : (String) defaultValue;
		updateSelected();
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
		boolean[] value;

		public SavedState(Parcel source) {
			super(source);
			value = source.createBooleanArray();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeBooleanArray(value);
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
