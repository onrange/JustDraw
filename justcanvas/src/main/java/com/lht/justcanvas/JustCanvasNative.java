package com.lht.justcanvas;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.lht.justcanvas.common.JustPaint;
import com.lht.justcanvas.common.JustV8Object;
import com.lht.justcanvas.draw.AbstractDraw;
import com.lht.justcanvas.draw.shape.DrawPath;
import com.lht.justcanvas.draw.shape.DrawRect;
import com.lht.justcanvas.draw.shape.DrawText;
import com.lht.justcanvas.view.JustView;

import java.util.ArrayList;

/**
 * Created by lht on 16/12/27.
 */

public class JustCanvasNative extends JustV8Object {

    protected JustView mJustView;
    private Handler mHandler;

    private final static String LOG_TAG = "JustCanvasNative";

    private boolean bNewStart = true;
    private JustPaint mPaintFill = new JustPaint(),
            mPaintStroke = new JustPaint();

    private float mStartX = 0, mStartY = 0;
    private Path mPath = new Path();

    private ArrayList<AbstractDraw> mShapeList = new ArrayList<>();
    public ArrayList<AbstractDraw> getShapeList() {
        return mShapeList;
    }

    public JustCanvasNative(V8 v8Runtime, JustView justView, Handler handler) {
        super(v8Runtime);

        this.mJustView = justView;
        this.mHandler = handler;

        this.initV8Object();

        mPaintFill.setStyle(Paint.Style.FILL);
        mPaintStroke.setStyle(Paint.Style.STROKE);
        mPaintFill.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintStroke.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void initV8Object() {
        // Register call() into JS
        mObject.registerJavaMethod(this, "call", "call", new Class[]{String.class, Integer.class});
        mObject.add("width", mJustView.getCanvasWidth());
        mObject.add("height", mJustView.getCanvasHeight());
    }

    private long methodTime = 0, paramTime = 0, invokeTime = 0;

    // For JavaScript to call
    public void call(String call, Integer times) {
        methodTime = paramTime = invokeTime = 0;

        String[] calls = splitBy(call, times, '&');

        for (String item: calls) {
            this.call(item);
        }

//        Log.d(LOG_TAG, "methodTime\t" + methodTime);
//        Log.d(LOG_TAG, "invokeTime\t" + invokeTime);
//        Log.d(LOG_TAG, "paramTime\t" + paramTime);

        mJustView.draw(mShapeList);
        mShapeList.clear();

        mHandler.sendEmptyMessage(0);
    }

    private void call(String call) {

        int index = call.indexOf(']');
        int length = call.length();
        String name = call.substring(1, index);
        String[] parameter = null;
        long start = System.currentTimeMillis();
        if (index != length - 1) {
            parameter = splitBy(call.substring(index + 1, length), 10, ',');
        }
        long end = System.currentTimeMillis();
        methodTime += end - start;

        switch (name) {
            case "log":
                log(parameter);
                break;
            case "beginPath":
                beginPath();
                break;
            case "closePath":
                closePath();
                break;
            case "fill":
                fill(parameter);
                break;
            case "stroke":
                stroke(parameter);
                break;
            case "moveTo":
                moveTo(parameter);
                break;
            case "lineTo":
                lineTo(parameter);
                break;
            case "arc":
                arc(parameter);
                break;
            case "rect":
                rect(parameter);
                break;
            case "clearRect":
                clearRect(parameter);
                break;
            case "fillText":
                fillText(parameter);
                break;
        }
    }

    private void log(String[] parameter) {
        Log.d(LOG_TAG, parameter[0]);
    }

    private void beginPath() {
        mPath = new Path();
        bNewStart = true;
    }

    private void closePath() {
        lineTo(mStartX, mStartY);
    }

    private void fill(String[] parameter) {
        String style = parameter[0];
        float lineWidth = getFloat(parameter[1]);
        mPaintFill.setStrokeWidth(lineWidth);
        mPaintFill.setRgbColor(style);
        mShapeList.add(new DrawPath(mPath, new JustPaint(mPaintFill)));
    }

    private void stroke(String[] parameter) {
        String style = parameter[0];
        float lineWidth = getFloat(parameter[1]);
        mPaintStroke.setStrokeWidth(lineWidth);
        mPaintStroke.setRgbColor(style);
        mShapeList.add(new DrawPath(mPath, new JustPaint(mPaintStroke)));
    }

    private void moveTo(String[] parameter) {
        long start = System.currentTimeMillis();
        float x = getFloat(parameter[0]), y = getFloat(parameter[1]);
        long end = System.currentTimeMillis();
        paramTime += end - start;

        start = System.currentTimeMillis();
        moveTo(x, y);
        end = System.currentTimeMillis();
        invokeTime += end - start;
    }

    private void moveTo(float x, float y) {
        bNewStart = false;
        mStartX = x;
        mStartY = y;
        mPath.moveTo(mStartX, mStartY);
    }

    private void lineTo(String[] parameter) {
        long start = System.currentTimeMillis();
        float x = getFloat(parameter[0]), y = getFloat(parameter[1]);
        long end = System.currentTimeMillis();
        paramTime += end - start;

        start = System.currentTimeMillis();
        lineTo(x, y);
        end = System.currentTimeMillis();
        invokeTime += end - start;
    }

    private void lineTo(float x, float y) {
        if (bNewStart) {
            moveTo(x, y);
        }
        else {
            mPath.lineTo(x, y);
        }
    }

    private void arc(String[] parameter) {
        long start = System.currentTimeMillis();
        float x = getFloat(parameter[0]), y = getFloat(parameter[1]),
                r = getFloat(parameter[2]), sAngle = getFloat(parameter[3]),
                eAngle = getFloat(parameter[4]);
        boolean counterclockwise = getBoolean(parameter[5]);
        long end = System.currentTimeMillis();
        paramTime += end - start;

        start = System.currentTimeMillis();
        RectF rectF = new RectF(x - r, y - r, x + r, y + r);

        //弧度与角度转换
        sAngle = (float)(sAngle / Math.PI * 180);
        eAngle = (float)(eAngle / Math.PI * 180);

        if (counterclockwise) {
            sAngle = -sAngle;
            eAngle = -eAngle;
        }

        mPath.addArc(rectF, sAngle, eAngle - sAngle);
        end = System.currentTimeMillis();
        invokeTime += end - start;
    }

    private void rect(String[] parameter) {
        long start = System.currentTimeMillis();
        float x = getFloat(parameter[0]), y = getFloat(parameter[1]),
                width = getFloat(parameter[2]), height = getFloat(parameter[3]);
        long end = System.currentTimeMillis();
        paramTime += end - start;

        start = System.currentTimeMillis();
        mPath.addRect(x, y, x + width, y + height, Path.Direction.CW);
        end = System.currentTimeMillis();
        invokeTime += end - start;
    }

    private void clearRect(String[] parameter) {
        float x = getFloat(parameter[0]), y = getFloat(parameter[1]),
                width = getFloat(parameter[2]), height = getFloat(parameter[3]);
        JustPaint paint = new JustPaint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        mShapeList.add(new DrawRect(x, y, width, height, paint));
    }

    private void fillText(String[] parameter) {
        String text = parameter[0], style = parameter[3];
        float x = getFloat(parameter[1]), y = getFloat(parameter[2]);
        mPaintFill.setRgbColor(style);
        mShapeList.add(new DrawText(text, x, y, new JustPaint(mPaintFill)));
    }

    private int getInt(Object param) {
        return Integer.parseInt(param.toString());
    }

    private float getFloat(Object param) {
        return Float.parseFloat(param.toString());
    }

    private boolean getBoolean(Object param) {
        return Boolean.parseBoolean(param.toString());
    }

    private static String[] splitBy(String content, int size, char symbol) {
        String[] result = new String[size];
        int index = 0, length = content.length(), start, end;
        int[] posForSymbol = new int[size];

        for (int i = 0; i < length; i++) {
            if (content.charAt(i) == symbol) {
                posForSymbol[index++] = i;
            }
        }

        start = 0;
        for (int i = 0; i < index; i++) {
            end = posForSymbol[i];
            result[i] = content.substring(start, end);
            start = end + 1;
        }

        return result;
    }
}