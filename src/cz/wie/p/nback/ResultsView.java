package cz.wie.p.nback;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public final class ResultsView extends View {
	private Paint mGridPaint;
	private Paint[] mChartPaints;
	private ArrayList<Results> mResults;
	int mTop;
	static final int colors[] = new int[]{Color.BLUE, Color.GREEN, Color.RED,
            Color.rgb(255, 165, 0), Color.MAGENTA};
	
	static class Results {
		ArrayList<Integer> results;
		String label;
		Results() {}		
	}
	
	public ResultsView(Context context) {
		super(context);
		
		init();
	}
	
	public ResultsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init();
	}
	
	public void setResults(ArrayList<Results> results, int top) {
		mResults = results;
		mTop = top;
	}
	
	public boolean empty() {
		for (Results r : mResults) {
			if (r.results.size() > 0)
				return false;
		}
		return true;
	}
	
	public void init() {
		mGridPaint = new Paint();
		mGridPaint.setColor(Color.BLACK);
		mGridPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
		mGridPaint.setAntiAlias(true);
		mChartPaints = new Paint[Common.sMaxButtons + 1];
		for (int i = 0; i < mChartPaints.length; ++i) {
			mChartPaints[i] = new Paint();
			mChartPaints[i].setColor(colors[i]);
			mChartPaints[i].setStrokeWidth(2);
			mChartPaints[i].setAntiAlias(true);
		}
	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		canvas.drawColor(Color.WHITE);
		int bottom = getHeight() * 9 / 10;
		int left = getWidth() / 10;
		int top = getHeight() / 10;
		int right = getWidth() * 9 / 10;
		canvas.drawLine(left, top, left, bottom, mGridPaint);
		canvas.drawLine(left, bottom, right, bottom, mGridPaint);
		
		if (mResults != null) {
			for (int i = 0; i < mResults.size(); ++i) {
				List<Integer> results = mResults.get(i).results;
				int firstResult = Math.max(0, results.size() - 100);
				drawSeries(canvas, top, bottom, left, right, results.subList(firstResult, results.size()), mChartPaints[i]);
			}

			String topLabel = Integer.toString(mTop);
			int center = getWidth() / 20;
			mGridPaint.setTextSize(getWidth() / 30);
			float x = center - mGridPaint.measureText(topLabel) / 2;
			float y = top - (mGridPaint.ascent() + mGridPaint.descent()) / 2; 
			canvas.drawText(topLabel, x, y, mGridPaint);
		}
	}

	private void drawSeries(Canvas canvas, int t, int b, int l, int r, List<Integer> series, Paint paint) {
		int prevX = -1, prevY = -1;
		int width = r - l;
		int height = b - t;
		for (int i = 0, L = series.size(); i < L; ++i) {
			int x =	l + width * i / L;
			int y = b - height * series.get(i) / mTop;
			canvas.drawCircle(x, y, 3, paint);
			if (i > 0) canvas.drawLine(prevX, prevY, x, y, paint);
			prevX = x; prevY = y;
		}
	}
}
