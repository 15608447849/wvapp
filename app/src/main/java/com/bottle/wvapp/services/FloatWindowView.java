package com.bottle.wvapp.services;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import lee.bottle.lib.toolset.log.LLog;

public class FloatWindowView extends View {

    private boolean isConnect = false;

    private Paint mPaint = null;

    public FloatWindowView(Context context) {
        super(context);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(10);

        LLog.print("创建 "+ this);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //处理 wrap_content 和 padding

    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 实现效果
        if (isConnect){
            mPaint.setColor(Color.GREEN);
        }else{
            mPaint.setColor(Color.GREEN);
        }
        //画圆环
        canvas.drawCircle(15,15,3,mPaint);
        LLog.print("绘制 "+ this);

    }

    public void setConnect(boolean connect) {
        isConnect = connect;
        postInvalidate();
    }

}
