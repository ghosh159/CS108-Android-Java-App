package com.csl.cs108ademoapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CustomDrawableView extends View {
    private Paint paint;
    private RectF rectF;
    private List<Integer> colours;
    private int activeSegment = -1;  // Default to no active segment
    private int numberOfSegments = 8;
    private int sweepAngle = 360 / numberOfSegments;

    public CustomDrawableView(Context context) {
        super(context);
        init();
    }

    public CustomDrawableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        rectF = new RectF();
        setUpColours();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectF.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int startAngle = 0;

        for (int i = 0; i < numberOfSegments; i++) {
            // If this segment is the active segment, use a distinct color
            if (i == activeSegment) {
                paint.setColor(Color.RED);  // Change this to whatever highlight color you prefer
            } else {
                paint.setColor(colours.get(i % colours.size()));
            }
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            startAngle += sweepAngle;
        }
    }

    private void setUpColours() {
        colours = new ArrayList<>();
        // Add colors you want. This is just a basic example.
        colours.add(Color.GRAY);
        colours.add(Color.BLUE);
        colours.add(Color.GREEN);
        colours.add(Color.YELLOW);
        // Add more colors if needed.
    }

    public void setActiveSegment(int segment) {
        this.activeSegment = segment;
        invalidate();
    }
}
