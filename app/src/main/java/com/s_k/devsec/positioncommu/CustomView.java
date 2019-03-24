package com.s_k.devsec.positioncommu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import static java.lang.Math.sqrt;

public class CustomView extends View {
    private static final String TAG="CustomView";

    private Paint paint;
    private Path path;
    private boolean viewflg;
    private double angle = 0;

    public CustomView(Context context, AttributeSet attrs){
        super(context, attrs);
        paint = new Paint();
        path = new Path();
        viewflg = true;
    }

    public void showCanvas(boolean flg, double ang){
        Log.d(TAG, "showCanvas()");
        viewflg = flg;
        angle = ang;
        Log.d(TAG, "angle is:" + angle);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas){
        Log.d(TAG, "onDraw()");
        float customViewWidth = MainActivity.customViewWidth;
        float customViewHeight = MainActivity.customViewHeight;
        float yourXPosition = customViewWidth / 2;
        float yourYPosition = customViewHeight * (float)0.9;
        double radius = customViewHeight * 0.4;

        float targetXPosition;
        float targetYPosition;

        float rsin_theta;
        float rcos_theta;

        RectF rectF = null;

        //背景
        canvas.drawColor(Color.argb(255, 255, 255, 255));
        if(angle > 60 || angle < -60){
            canvas.drawColor(Color.argb(255, 255, 182, 191));
//            canvas.drawColor(0xffb6c1);
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(5);
            paint.setTextSize(60);
            canvas.drawText("Range Over", customViewWidth * (float)0.05, customViewHeight * (float)0.9, paint);
        }

        //自分座標マーク
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(yourXPosition, yourYPosition,10, paint);

        //自分座標文字
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        paint.setTextSize(50);
        canvas.drawText("Your Position", yourXPosition, yourYPosition + 60, paint);

        //レンジ線表示
        paint.setStrokeWidth(3);
        path.rewind();
        path.moveTo(yourXPosition, yourYPosition);
        path.lineTo(yourXPosition-(float)(radius/2*sqrt(3.0)*1.3),yourYPosition-(float)(radius/2*1.3));
        canvas.drawPath(path, paint);
        path.rewind();
        path.moveTo(yourXPosition, yourYPosition);
        path.lineTo(yourXPosition+(float)(radius/2*sqrt(3.0)*1.3),yourYPosition-(float)(radius/2*1.3));
        canvas.drawPath(path, paint);
        paint.setStrokeWidth(1);
        path.rewind();
        path.moveTo(yourXPosition, yourYPosition);
        path.lineTo(yourXPosition,yourYPosition-(float)(radius*1.3));
        canvas.drawPath(path, paint);

        //進行方向表示
        paint.setStrokeWidth(5);
        paint.setTextSize(50);
        canvas.drawText("↑Moving Direction", yourXPosition -25, yourYPosition-(float)(radius*1.3) - 25, paint);

        //円弧
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        rectF = new RectF(yourXPosition - (float)radius, yourYPosition - (float)radius, yourXPosition + (float)radius, yourYPosition + (float)radius);
        canvas.drawArc(rectF, 210, 120, false, paint); //sweepAngleは､startAngle位置から動かす角度のこと｡終端の角度ではない!

        if(viewflg){ //初回描画時は自分の真向かいにターゲット座標をマーク
            targetXPosition = yourXPosition;
            targetYPosition = yourYPosition - (float)radius;
        }else{ //angle更新毎に描画する際の処理
            Log.d(TAG, "radius:"+ radius);
            Log.d(TAG, "angle:"+ angle);
            rsin_theta = (float)(radius * Math.sin(Math.toRadians(angle)));
            Log.d(TAG, "rcos_theta:"+ rsin_theta);
            rcos_theta = (float)(radius * Math.cos(Math.toRadians(angle)));
            Log.d(TAG, "rsin_theta:"+ rcos_theta);
            targetXPosition = yourXPosition + rsin_theta;
            targetYPosition = yourYPosition - rcos_theta;
        }

        if(angle <= 60 && angle >= -60){
            //ターゲット座標マーク
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(10);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(targetXPosition, targetYPosition, 10, paint);
            //ターゲット座標文字
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(5);
            paint.setTextSize(50);
            canvas.drawText("Target", targetXPosition - 70, targetYPosition - 50, paint);

            //ターゲットまでの線表示
            paint.setStrokeWidth(1);
            paint.setStyle(Paint.Style.STROKE);
//        paint.setPathEffect(new DashPathEffect(new float[]{2, 4},0));
            path.rewind();
            path.moveTo(yourXPosition, yourYPosition);
            path.lineTo(targetXPosition, targetYPosition);
            canvas.drawPath(path, paint);

            //進行方向との角度円弧
            paint.setStrokeWidth(1);
            paint.setStyle(Paint.Style.STROKE);
            rectF = new RectF(yourXPosition - (float) radius / 2, yourYPosition - (float) radius / 2, yourXPosition + (float) radius / 2, yourYPosition + (float) radius / 2);
            canvas.drawArc(rectF, 270, (float) angle, false, paint); //sweepAngleは､startAngle位置から動かす角度のこと｡終端の角度ではない!

            //θ表示
            if (angle != 0) {
                rsin_theta = (float) (radius * Math.sin(Math.toRadians(angle / 2)));
                rcos_theta = (float) (radius * Math.cos(Math.toRadians(angle / 2)));
                targetXPosition = yourXPosition + rsin_theta;
                targetYPosition = yourYPosition - rcos_theta;
                paint.setStrokeWidth(3);
                canvas.drawText("θ", yourXPosition + (targetXPosition - yourXPosition) / 2 - 16, yourYPosition - (yourYPosition - targetYPosition) / 2, paint);
            }
        }
    }
}
