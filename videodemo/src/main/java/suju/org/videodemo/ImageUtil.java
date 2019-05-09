package suju.org.videodemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

/**
 * Created by suju on 16/12/25.
 */

public class ImageUtil {

    private static Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale); //长和宽放大缩小的比例
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();
        return resizeBmp;
    }

    private static Bitmap maskImageForSmallSDK17(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Bitmap scaleBitmap = scaleBitmap(bitmap, 0.05f);
        scaleBitmap = scaleBitmap(scaleBitmap, 20.0f);
        Bitmap canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(canvasBitmap);
        Paint paint = new Paint();
        paint.setMaskFilter(new BlurMaskFilter(100f, BlurMaskFilter.Blur.SOLID));

        canvas.drawBitmap(scaleBitmap, 0, 0, paint);
        canvas.drawColor(Color.parseColor("#3f000000"));
        return canvasBitmap;
    }

    public static Bitmap maskImage(Context context, Bitmap bitmap) {
        Bitmap scaleBitmap = scaleBitmap(bitmap, 0.7f);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return maskImageForSmallSDK17(scaleBitmap);
        }
        RenderScript rs = RenderScript.create(context);
        Allocation allocation = Allocation.createFromBitmap(rs, scaleBitmap);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, allocation.getElement());
        blurScript.setInput(allocation);
        blurScript.setRadius(20f);
        blurScript.forEach(allocation);
        allocation.copyTo(scaleBitmap);
        rs.destroy();
        return scaleBitmap;
    }
}
