package cz.wie.p.nback;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public final class BoardView extends View {

	public BoardView(Context context) {
		super(context);
	}

	public BoardView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BoardView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override public void draw(Canvas canvas) {
		super.draw(canvas);
		if (mBitmap == null)
			createBitmap();
		canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
	}

	private void createBitmap() {
		mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		mGridPaint = new Paint(Paint.DITHER_FLAG);
		mGridPaint.setColor(Color.WHITE);
	}

	@Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
		createBitmap();
		showItem(mItem, mSettings);
	}

	@Override protected void onMeasure(int widthSpec, int heightSpec) {
		int width = View.MeasureSpec.getSize(widthSpec),
		height = View.MeasureSpec.getSize(heightSpec);
		int size = Math.min(width, height);
		createBitmap();
		setMeasuredDimension(size, size);
		showItem(mItem, mSettings);
	}
	
	private void showGrid() {
		int fieldWidth = mWidth / 3;
		int fieldHeight = mHeight / 3;
		mCanvas.drawLine(fieldWidth, 1, fieldWidth, mHeight - 1, mGridPaint);
		mCanvas.drawLine(2 * fieldWidth, 1, 2 * fieldWidth, mHeight - 1, mGridPaint);
		mCanvas.drawLine(1, fieldHeight, mWidth - 1, fieldHeight, mGridPaint);
		mCanvas.drawLine(1, 2 * fieldHeight, mWidth - 1, 2 * fieldHeight, mGridPaint);
	}

	static int color(int i) {
		switch(i) {
		case 0: return Color.WHITE;
		case 1: return Color.GREEN;
		case 2: return Color.YELLOW;
		case 5: return Color.rgb(255, 128, 0);
		case 3: return Color.RED;
		case 4: return Color.rgb(255, 0, 255);
		case 6: return Color.GRAY;
		case 7: return Color.BLUE;
		default: return Color.CYAN;
		}
	}

	public void showItem(Item item, Settings settings) {
		if (mCanvas == null)
			createBitmap();
		
		mCanvas.drawColor(Color.BLACK);
		showGrid();
		
		if (item == null)
			return;
		mItem = item;
		mSettings = settings;
		int position = item.position(settings);
		
		Paint paint = new Paint();
		paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
		paint.setTextSize(Math.min(mWidth, mHeight) / 3);
		paint.setColor(color(item.color(settings)));
		paint.setStyle(Style.FILL);
		int fieldWidth = mWidth / 3;
		int fieldHeight = mHeight / 3;
		paint.setAntiAlias(true);
		int shape = item.shape(settings);
		String text = shape >= 0 ? Integer.toString(shape + 1) : "\u25a0";
		float x = position % 3 * fieldWidth + (fieldWidth - paint.measureText(text)) / 2;
		float y = position / 3 * fieldHeight + (fieldHeight - (paint.ascent() + paint.descent())) / 2; 
		mCanvas.drawText(text, x, y, paint);
		invalidate();
	}

	public void hideItem() {
		mItem = null;

		mCanvas.drawColor(Color.BLACK);
		showGrid();
		invalidate();
	}
	
	private Canvas mCanvas;
	private Bitmap mBitmap;
	private Paint mBitmapPaint;
	private Paint mGridPaint;
	private Item mItem;
	private Settings mSettings;
	private int mWidth = 1;
	private int mHeight = 1;
}
