package cz.wie.p.nback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ItemGenerator {
	final Random mRandom;
	
	public ItemGenerator() { 
		mRandom = new Random();
	}

	private static final boolean[][][] sPatterns = new boolean[][][] {
		new boolean[][] {
				new boolean[] {false, false, false, false},
		},
		new boolean[][] {
				new boolean[] {true, false, false, false},
				new boolean[] {false, true, false, false},
				new boolean[] {false, false, true, false},
				new boolean[] {false, false, false, true},
		},
		new boolean[][] {
				new boolean[] {true, true, false, false},
				new boolean[] {true, false, true, false},
                new boolean[] {false, true, true, false},
                new boolean[] {true, false, false, true},
                new boolean[] {false, true, false, true},
                new boolean[] {false, false, true, true},
		},
		new boolean[][] {
				new boolean[] {true, true, true, false},
                new boolean[] {true, true, false, true},
                new boolean[] {true, false, true, true},
                new boolean[] {false, true, true, true},
            },
        new boolean[][]{
                new boolean[]{true, true, true, true},
        }
    };

	private int binomial(int n, int k) {
		int[] coeffs = new int[k + 1];
		Arrays.fill(coeffs, 0);
		coeffs[0] = 1;
		for (int i = 0; i < n; ++i) {
			for (int j = k; j > 0; --j) {
				coeffs[j] += coeffs[j - 1];
			}
		}
		return coeffs[k];
	}
	
	
	private ArrayList<boolean[]> generatePatterns(int numPatterns, int numFeatures, int ratio) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>(numPatterns);
		double prevDiffInExpected = 0; 
		for (int i = numFeatures; i >= 1; i--) {
			double probability = Math.pow(ratio / 100.0, i) * Math.pow(1 - ratio / 100.0, numFeatures - i);
			int cN_I = binomial(numFeatures, i);
			double expectedSame = Math.max(numPatterns * probability, 0) * cN_I + prevDiffInExpected;
			int expectedSameRounded = (int)Math.round(expectedSame);
			prevDiffInExpected = expectedSame - expectedSameRounded;
			for (int s = 0; s < expectedSameRounded; s++) {
				patterns.add(sPatterns[i][s % cN_I]);
			}
		}
		for (int i = patterns.size(); i < numPatterns; ++i)
			patterns.add(sPatterns[0][0]);
		for (int i = patterns.size() - 1; i >= 1; --i) {
			int newPos = mRandom.nextInt(i + 1);
			if (i != newPos) {
				boolean[] tmp = patterns.get(i);
				patterns.set(i, patterns.get(newPos));
				patterns.set(newPos, tmp);
			}
		}
		return patterns;
	}
	
	public ArrayList<Item> createItemsDeterministic(int numItems, int numFeatures, int ratio, int backLevel) {
		ArrayList<boolean[]> patterns = generatePatterns(numItems - backLevel, numFeatures, ratio);
		ArrayList<Item> items = new ArrayList<Item>(numItems);
		for (int i = 0; i < numItems; ++i) {
			if (i < backLevel) {
				items.add(new Item(
						mRandom.nextInt(8),
						mRandom.nextInt(8),
						mRandom.nextInt(8),
                        mRandom.nextInt(8)));
			} else {
				int prevIndex = i - backLevel;
				Item prevItem = items.get(prevIndex);
				boolean[] pattern = patterns.get(prevIndex);
				items.add(new Item(
						(pattern[0] ? prevItem.feature(0) : differentThan(prevItem.feature(0))),
						(pattern[1] ? prevItem.feature(1) : differentThan(prevItem.feature(1))),
						(pattern[2] ? prevItem.feature(2) : differentThan(prevItem.feature(2))),
                        (pattern[3] ? prevItem.feature(3) : differentThan(prevItem.feature(3)))));
			}
		}
		return items;
	}

	public ArrayList<Item> createItemsRandom(int numItems, int repetitionPercentage, int backLevel) {
		ArrayList<Item> items = new ArrayList<Item>(numItems);
		for (int i = 0; i < numItems; ++i) {
			if (i < backLevel) {
				items.add(new Item(
						mRandom.nextInt(8),
						mRandom.nextInt(8),
						mRandom.nextInt(8),
                        mRandom.nextInt(8)));
			} else {
				Item prevItem = items.get(i - backLevel);
				items.add(new Item(
						(mRandom.nextInt(100) < repetitionPercentage ? prevItem.feature(0) : differentThan(prevItem.feature(0))),
						(mRandom.nextInt(100) < repetitionPercentage ? prevItem.feature(1) : differentThan(prevItem.feature(1))),
						(mRandom.nextInt(100) < repetitionPercentage ? prevItem.feature(2) : differentThan(prevItem.feature(2))),
                        (mRandom.nextInt(100) < repetitionPercentage ? prevItem.feature(3) : differentThan(prevItem.feature(3)))));
			}
		}
		return items;
	}

	private int differentThan(int i) {
		int result;
		do {
			result = mRandom.nextInt(8);
		} while (result == i);
		return result;
	}
}
