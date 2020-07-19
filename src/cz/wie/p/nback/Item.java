package cz.wie.p.nback;


import android.os.Parcel;
import android.os.Parcelable;

public final class Item implements Parcelable {
	final int[] features;
	
	public Item(int feature1, int feature2, int feature3, int feature4) {
		features = new int[4];
		features[0] = feature1;
		features[1] = feature2;
		features[2] = feature3;
		features[3] = feature4;
	}
	
	public int feature(int i) {
		return features[i];
	}
	
	public boolean sameFeature(int feature, Item that) {
		return features[feature] == that.features[feature];
	}
	
	private int getFeature(int feature, Settings settings) {
		for (int i = 0; i < settings.features.size(); ++i) {
			if (settings.features.get(i) == feature)
				return features[i];
		}
		return -1;
	}
	
	public int sound(Settings settings) {
		return getFeature(Common.sSound, settings);
	}
	
	public int position(Settings settings) {
		int	position = getFeature(Common.sPosition, settings);
		if (position >= 0) {
			if (position < 4) return position;
			else return position + 1;
		}
		return 4;
	}
	
	public int shape(Settings settings) {
		int shape = getFeature(Common.sShape, settings);
		if (shape >= 0) return shape;
		return -1;
	}
	
	public int color(Settings settings) {
		int color = getFeature(Common.sColor, settings);
		if (color >= 0) return color;
		return 0;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int feature : features) {
            builder.append(feature).append(", ");
        }
        return builder.toString();
    }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeIntArray(features);
	}

	public static final Parcelable.Creator<Item> CREATOR
			= new Parcelable.Creator<Item>() {
		public Item createFromParcel(Parcel in) {
			int[] features = new int[4];
			in.readIntArray(features);
			return new Item(features[0], features[1], features[2], features[3]);
		}

		public Item[] newArray(int size) {
			return new Item[size];
		}
	};
}
