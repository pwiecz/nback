package cz.wie.p.nback;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import cz.wie.p.nback.ResultsView.Results;

public final class ResultsDb {

	private final SQLiteDatabase mDb;

	public ResultsDb(Context context) {
		mDb = context.openOrCreateDatabase("progress.db", Context.MODE_PRIVATE, null);
		mDb.execSQL(
				"CREATE TABLE IF NOT EXISTS Results ( " +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"features INTEGER NOT NULL, " +
				"back_level INTEGER NOT NULL, " +
				"date_time TEXT NOT NULL, " + 
				"result INTEGER NOT NULL," +
				"play_time INTEGER NOT NULL, " +
				"features_string TEXT NOT NULL, " +
		        "auto_set_level BOOLEAN NOT NULL);");
		alterResultsTable();
		mDb.execSQL(
				"CREATE TABLE IF NOT EXISTS ResultDetails ( " +
				"_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"feature INTEGER NOT NULL, " +
				"result INTEGER NOT NULL, " +
		        "game INTEGER NOT NULL REFERENCES Results(_id));");
		mDb.execSQL("CREATE INDEX IF NOT EXISTS ResultDateIndex ON Results (date_time DESC);");
	}

	public void close() {
		mDb.close();
	}

	public void clear() {
		mDb.execSQL("DELETE FROM Results;");
		mDb.execSQL("DELETE FROM ResultDetails;");
	}

	public int suggestNBackLevel(ArrayList<Integer> features, int minPercentToAdvance, int maxPercentToDrop, boolean byFeature) {
		String featuresString = featuresString(features);
		Cursor c = mDb.query("Results", new String[]{"_id", "back_level", "result"}, "features_string = ? AND auto_set_level = 1",
				new String[]{featuresString},
				null, null, "date_time DESC", "1");
		if (!c.moveToFirst()) {
			return 3;
		}
		int result = c.getInt(2);
		int lastLevel = c.getInt(1);
		String gameIdStr = Integer.toString(c.getInt(0));
		c.close();
		if (result <= maxPercentToDrop && lastLevel > 1)
			return lastLevel - 1;
		if (!byFeature) {
			if (result >= minPercentToAdvance && lastLevel < 15)
				return lastLevel + 1;
			return lastLevel;
		}
		Cursor c2 = mDb.query("ResultDetails", new String[]{"result"}, "game = ?", new String[]{gameIdStr}, null, null, null);
		for (c2.moveToFirst(); !c2.isAfterLast(); c2.moveToNext()) {
			if (c2.getInt(0) < minPercentToAdvance) {
				c2.close();
				return lastLevel;
			}
		}
		c2.close();
		return lastLevel + 1;
	}

	private void alterResultsTable() {
		Cursor c = mDb.rawQuery("SELECT * FROM Results LIMIT 1", null);
		String[] columnNames = c.getColumnNames();
		boolean hasPlayTime = false;
		boolean hasFeaturesString = false;
		boolean hasAutoSetLevel = false;
		for (String columnName : columnNames) {
			if (columnName.equals("play_time")) {
				hasPlayTime = true;
			} else if (columnName.equals("features_string")) {
				hasFeaturesString = true;
			} else if (columnName.equals("auto_set_level")) {
				hasAutoSetLevel = true;
			}
		}
		if (!hasPlayTime)
			mDb.execSQL("ALTER TABLE Results ADD COLUMN play_time INTEGER NOT NULL DEFAULT 0;");		
		if (!hasFeaturesString)
			mDb.execSQL("ALTER TABLE Results ADD COLUMN features_string TEXT NOT NULL DEFAULT '';");
		if (!hasAutoSetLevel)
			mDb.execSQL("ALTER TABLE Results ADD COLUMN auto_set_level BOOLEAN NOT NULL DEFAULT 0;");
		c.close();
	}

	private String featuresString(ArrayList<Integer> features) {
		StringBuilder featuresString = new StringBuilder();
		for (int i = 0, L = Common.sFeatures.length; i < L; ++i) {
			if (i > 0) featuresString.append(',');
			if (features.contains(Common.sFeatures[i])) featuresString.append('1');
			else featuresString.append('0');
		}
		return featuresString.toString();
	}

	static ArrayList<Integer> featureList(String featuresString) {
		ArrayList<Integer> features = new ArrayList<Integer>();
		if (featuresString.contains(",")) {
			String[] featuresStrs = featuresString.split(",");
			for (int i = 0; i < featuresStrs.length; ++i) {
				if (featuresStrs[i].equals("0"))
					continue;
				features.add(i);
			}
		}
		return features;
	}

	public void recordResult(int result, int[] results, long playTimeMillis, Settings settings) {
		int numFeatures = settings.features.size();
		if (numFeatures < 1 || numFeatures > 4) return;
		String dateTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
		recordResult(result, dateTime, results, numFeatures, featuresString(settings.features),
				settings.features, playTimeMillis, settings.nBackLevel, settings.autoSetLevel);
	}

	synchronized public void recordResult(int result, String dateTime, int[] results, int numFeatures, String featuresString,
			ArrayList<Integer> features, long playTimeMillis, int nBackLevel, boolean autoSetLevel) {
		ContentValues gameInfo = new ContentValues();
		gameInfo.put("features", numFeatures);
		gameInfo.put("features_string", featuresString);
		gameInfo.put("back_level", nBackLevel);
		gameInfo.put("auto_set_level", autoSetLevel ? 1 : 0);
		gameInfo.put("date_time", dateTime);
		gameInfo.put("result", result);
		gameInfo.put("play_time", playTimeMillis);
		long id = mDb.insert("Results", null, gameInfo);
		if (id == -1) return;
		for (int i = 0, L = Math.min(features.size(), results.length); i < L; ++i) {
			ContentValues resultDetail = new ContentValues();
			resultDetail.put("feature", features.get(i));
			resultDetail.put("result", results[i]);
			resultDetail.put("game", id);
			mDb.insert("ResultDetails", null, resultDetail);
		}
	}
	
	public int getTimePlayedForDays(int i) {
		Date todayStart = new Date();
		Date todayEnd = (Date)todayStart.clone();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		todayStart.setHours(0);
		todayStart.setMinutes(0);
		todayStart.setSeconds(0);
		todayStart.setTime(todayStart.getTime() - (long)(i - 1) * 1000 * 3600 * 24);
		String todayStartStr = dateFormat.format(todayStart);
		todayEnd.setHours(23);
		todayEnd.setMinutes(59);
		todayEnd.setSeconds(59);
		String todayEndStr = dateFormat.format(todayEnd);
		Cursor c = mDb.query("Results", new String[]{"SUM(play_time)"}, "date_time >= ? AND date_time <= ?", new String[]{todayStartStr, todayEndStr}, null, null, null);
		int time = 0;
		for (c.moveToLast(); !c.isBeforeFirst(); c.moveToPrevious()) {
			time += c.getInt(0);
		}
		c.close();
		return time;
	}

	public ArrayList<Results> getResults(MainActivity.GameMode gameMode) {
		int numFeatures = gameMode.modesGroup.numFeatures;
		int backLevel = gameMode.modesGroup.backLevel;
		String featuresString = gameMode.featuresString;
		ArrayList<Results> results = new ArrayList<Results>();
		String queryString;
		String[] parameterArray = new String[1];
		ArrayList<String> queryParameters = new ArrayList<String>(2);
		if (featuresString == null) {
			queryString = "feature = ?";
			queryParameters.add(Integer.toString(numFeatures));
		} else {
			queryString = "features_string = ?";
			queryParameters.add(featuresString);
		}
		if (backLevel > 0) {
			queryString += " AND back_level = ?";
			queryParameters.add(Integer.toString(backLevel));
		}
		// Get 100 most recent results in reversed order.
		Cursor c = mDb.query("Results", new String[]{"result"}, queryString,
				queryParameters.toArray(parameterArray), null, null, "date_time DESC", "100");
		ArrayList<Integer> allResults = new ArrayList<Integer>(c.getCount());
		for (c.moveToLast(); !c.isBeforeFirst(); c.moveToPrevious()) {
			allResults.add(c.getInt(0));
		}
		c.close();
		{
			Results result = new Results();
			result.results = allResults;
			result.label = "All";
			results.add(result);
		}
		if (featuresString != null) {
			ArrayList<ArrayList<Integer> > detailedResults = new ArrayList<ArrayList<Integer> >(numFeatures);
			for (int i = 0; i < numFeatures; ++i) {
				detailedResults.add(new ArrayList<Integer>(allResults.size()));
			}
			queryString = "Results.features_string = ? AND Results._id = ResultDetails.game";
			if (backLevel > 0) queryString += " AND Results.back_level = ?";
			c = mDb.query(
					"Results,ResultDetails",
					new String[]{"ResultDetails.feature AS feature", "ResultDetails.result AS detailed_result"},
					queryString, queryParameters.toArray(parameterArray),
					null, null, "Results.date_time DESC, feature DESC");
			for (c.moveToLast(); !c.isBeforeFirst(); ) { 
				for (int i = 0; i < numFeatures; ++i) {
					if (c.isBeforeFirst())
						break;
					detailedResults.get(i).add(c.getInt(1));
					c.moveToPrevious();
				}
			}
			c.close();
			ArrayList<Integer> features = featureList(featuresString);
			for (int i = 0; i < numFeatures; ++i) {
				Results result = new Results();
				result.results = detailedResults.get(i);
				result.label = Common.sFeatureNames[features.get(i)];
				results.add(result);
			}
		}
		return results;
	}

	public ArrayList<Results> getLastLevels(ArrayList<Integer> features) {
		String featuresStr = featuresString(features);
		Cursor c = mDb.query("Results", new String[]{"back_level"},
				"features_string = ? AND auto_set_level = 1", new String[]{featuresStr},
				null, null, "date_time DESC", "99");
		ArrayList<Integer> levels = new ArrayList<Integer>(c.getCount());
		for (c.moveToLast(); !c.isBeforeFirst(); c.moveToPrevious()) {
			levels.add(c.getInt(0));
		}
		c.close();
		ResultsView.Results results = new ResultsView.Results();
		results.results = levels;
		results.label = "Level";
		ArrayList<Results> resultList = new ArrayList<Results>(1);
		resultList.add(results);
		return resultList;
	}
}
