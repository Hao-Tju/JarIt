
package net.jarit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class MaskedImageView extends ImageView {
    private static final String TAG = MaskedImageView.class.getSimpleName();

    private Bitmap maskBitmap;
    private Bitmap preparedBmp;

    public MaskedImageView(Context context) {
        super(context);
    }

    public MaskedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaskedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas onDrawCanvas) {
        Bitmap background = ((BitmapDrawable) getDrawable()).getBitmap();
        Matrix matrix = new Matrix();
        RectF srcRect = new RectF(0, 0, background.getWidth(), background.getHeight());
        RectF dstRect = new RectF(0, 0, getWidth(), getHeight());

        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL);
        background = Bitmap.createBitmap(background, 0, 0, background.getWidth(),
                background.getHeight(), matrix, true);
        if (background != null) {
            if (maskBitmap != null
                    && (preparedBmp == null || (preparedBmp.getWidth() != getWidth() || preparedBmp
                            .getHeight() != getHeight()))) {
                preparedBmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(preparedBmp);
                canvas.drawARGB(0, 0, 0, 0);
                canvas.drawBitmap(maskBitmap, (getWidth() - maskBitmap.getWidth()) / 2,
                        (getHeight() - maskBitmap.getHeight()) / 2, null);
            }

            Bitmap result = Bitmap.createBitmap(background.getWidth(), background.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);

            final int color = Color.RED;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, background.getWidth(), background.getWidth());
            final RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            if (preparedBmp != null) {
                canvas.drawBitmap(preparedBmp, 0, 0, paint);
            }

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
            canvas.drawBitmap(background, 0, 0, paint);

            onDrawCanvas.drawBitmap(result, 0, 0, null);

        }
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
        invalidate();
    }

    public void setMaskBitmap(Bitmap d) {
        maskBitmap = d;
        requestLayout();
        invalidate();
    }
}
