package com.shiweinan.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Weinan on 2017/12/26.
 */

public class CandidateView extends View {
    private List<String> mSuggestions;      // 存放候选列表
    private static final int X_GAP = 10;    // 每个候选之间的间隔
    private Paint mPaint;                   // 用于绘制候选
    private int mCandidateVPadding;         // 候选文字上下边距
    private int highLightIndex; // 高亮候选词的Index
    private IMEService service;
    public CandidateView(Context context) {
        super(context);
        init(context);
    }
    public CandidateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public CandidateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }
    private void init(Context context) {
        Resources r = context.getResources();
        mCandidateVPadding = r.getDimensionPixelSize(R.dimen.candidateVerticalPadding);
        highLightIndex = 0;
        setBackgroundColor(r.getColor(R.color.candidateBackground, null)); // 设置背景色
        mPaint = new Paint();
        mPaint.setColor(r.getColor(R.color.candidate, null));               // 设置前景色
        mPaint.setAntiAlias(true);      // 设置字体
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidateFontHeight));   // 设置字号
        mPaint.setStrokeWidth(1);
        setWillNotDraw(false);  // 覆盖了onDraw函数应清除该标记
    }

    private static int GetHighlightColor() {
        return Color.parseColor("#FF9912");
    }

    private static int GetNormalColor() {
        return Color.parseColor("#FFFFFF");
    }

    public void setHighLightCandidateIndex(int index) {
        highLightIndex = index;
        invalidate();
        requestLayout();
    }

    public void setService(IMEService s) {
        service = s;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.d(this.getClass().toString(), "onMeasure: ");
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int measuredWidth = resolveSize(50, widthMeasureSpec);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mCandidateVPadding;
        // 系统会根据返回值确定窗体的大小
        //Log.d(this.getClass().toString(), "onMeasure: "+measuredWidth+","+desiredHeight);
        setMeasuredDimension(measuredWidth, resolveSize(desiredHeight, heightMeasureSpec));
    }
    HashMap<String, Pair<Integer, Integer>> candidateList = new HashMap<>();
    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(this.getClass().toString(), "onDraw: ");
        super.onDraw(canvas);
        if (mSuggestions == null)
            return;
        // 依次绘制每组候选字串
        int x = 0;
        final int count = mSuggestions.size();
        final int height = getHeight();
        int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());
        candidateList.clear();
        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            float textWidth = mPaint.measureText(suggestion);
            if (i == highLightIndex) {
                mPaint.setColor(GetHighlightColor());
            }
            else {
                mPaint.setColor(GetNormalColor());
            }
            final int wordWidth = (int) textWidth + X_GAP * 2;
            if (x + wordWidth > 1080) {
                y += (int)(mPaint.getTextSize() - mPaint.ascent());
                x = 0;
                break;
            }
            candidateList.put(suggestion, new Pair<>(x, x+wordWidth));
            canvas.drawText(suggestion, x + X_GAP, y, mPaint);
            x += wordWidth;
        }
        int morePadding = 50;
        Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_arrow_drop_down_24dp, null);
        d.setBounds(1080 - (int)mPaint.getTextSize() - morePadding, 0, 1080 - morePadding, (int)mPaint.getTextSize());
        d.draw(canvas);
    }
    public void setSuggestions(List<String> suggestions) {
        //Log.d(this.getClass().toString(), "setSuggestions: ");
        // 设置候选字串列表
        if (suggestions != null) {
            mSuggestions = new ArrayList<>(suggestions);
        }
        invalidate();
        requestLayout();
    }
    @Override
    public boolean onTouchEvent(MotionEvent me) {
        //System.out.println("candidate touch:" + me.getX() + "," + me.getY());
        if ((me.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            int x = Math.round(me.getX());
            for (String str: candidateList.keySet()) {
                Pair<Integer, Integer> pos = candidateList.get(str);
                if (x>= pos.first && x<=pos.second) {
                    service.select(str + " ");
                    return true;
                }
            }
            if (me.getX() > 2000) {
                CandidatePage candidatePage = new CandidatePage(this.getContext());
                //CandidateView candidatePage = new CandidateView(this.getContext());
                //candidatePage.setBackgroundColor(Color.BLACK);
                //candidatePage.showAtLocation(this, Gravity.BOTTOM, 0, 0);
                candidatePage.setWidth(this.getWidth());
                candidatePage.setHeight(service.getKeyboardHeight());
                System.out.println("KBD:height" + service.getKeyboardHeight());
                candidatePage.showAsDropDown(this, 0, 0);
                candidatePage.setPinyin(mSuggestions);
            }
        }

        return true;
    }
}
