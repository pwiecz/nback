package cz.wie.p.nback;


import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ItemGeneratorTest {

        private static int[] countRepetitions(int seriesLength, int numFeatures, int ratio,
                                               int nBackLevel) {
            ItemGenerator generator = new ItemGenerator();
            ArrayList<Item> items = generator.createItemsDeterministic(seriesLength, numFeatures, ratio,
                    nBackLevel);
            Item[] prevItems = new Item[nBackLevel];
            int sameFeatures[] = new int[Common.sMaxFeatures + 1];
            for (Item item : items) {
                if (prevItems[nBackLevel - 1] != null) {
                    int numSame = 0;
                    for (int i = 0; i < numFeatures; ++i) {
                        if (item.feature(i) == prevItems[nBackLevel - 1].feature(i)) {
                            numSame++;
                        }
                    }
                    sameFeatures[numSame]++;
                }
                System.arraycopy(prevItems, 0, prevItems, 1, nBackLevel - 1);
                prevItems[0] = item;
            }
            return sameFeatures;
        }

    @Test
    public void testCreateItemsDeterministic() throws Exception {
        assertArrayEquals(new int[]{7, 8, 3, 1, 0}, countRepetitions(20, 3, 30, 1));
    }
}