package cz.wie.p.nback;

public final class Common {
	static final int sPosition = 0;
	static final int sShape = 1;
	static final int sColor = 2;
	static final int sSound = 3;
	static final int sMaxFeatures = 4;
	static final int sMaxButtons = 4;
	static final String[] sFeatureNames = new String[] { "Position", "Shape", "Color", "Sound" };
	static final int[] sFeatures = new int[] { sPosition, sShape, sColor, sSound };
	static final int TODAY = 0;
	static final int LAST_7_DAYS = 1;
	static final int LAST_30_DAYS = 2;
	static final int TOTAL = 3;

	public static String numberOfFeaturesAdj(int numberOfFeatures) {
		switch (numberOfFeatures) {
			case 1:
				return "Single";
			case 2:
				return "Dual";
			case 3:
				return "Triple";
			case 4:
				return "Quad";
			default:
				return "???";
		}
	}

	public static String gameModeToString(int numberOfFeatures, int backLevel) {
		return numberOfFeaturesAdj(numberOfFeatures) + " " + backLevel + "-Back";
	}
}
