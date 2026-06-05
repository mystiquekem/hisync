package com.example.hisync;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicNoteView extends View {

    private static final int[] COLORS = {
            0x33EF4B3C, // red
            0x2264B5C3, // cyan
            0x22EF9F27, // amber
            0x18FDF1E3, // white
            0x22C4382C, // dark red
            0x1A2D5A61, // teal dark
    };

    private static final int TYPE_QUARTER   = 0;
    private static final int TYPE_EIGHTH    = 1;
    private static final int TYPE_BEAMED    = 2;
    private static final int TYPE_TREBLE    = 3;
    private static final int TYPE_HALF      = 4;
    private static final int TYPE_SIXTEENTH = 5;
    private static final int TYPE_WHOLE     = 6;

    private static class Note {
        float x, y, size, dx, dy, rot, drot, alpha, pulse, dPulse;
        int type, color;
    }

    private final List<Note> notes = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private boolean initialized = false;
    private int noteCount = 40;

    public MusicNoteView(Context context) {
        super(context);
    }

    public MusicNoteView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setNoteCount(int count) {
        this.noteCount = count;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (!initialized && w > 0 && h > 0) {
            initNotes(w, h);
            initialized = true;
        }
    }

    private void initNotes(int w, int h) {
        notes.clear();
        int[] types = {TYPE_QUARTER, TYPE_EIGHTH, TYPE_BEAMED,
                TYPE_TREBLE,  TYPE_HALF,   TYPE_SIXTEENTH, TYPE_WHOLE};
        for (int i = 0; i < noteCount; i++) {
            Note n = new Note();
            n.x     = rnd.nextFloat() * w;
            n.y     = rnd.nextFloat() * h;
            n.size  = rnd.nextFloat() * 28 + 14;
            n.type  = types[rnd.nextInt(types.length)];
            n.color = COLORS[rnd.nextInt(COLORS.length)];
            n.dx    = (rnd.nextFloat() - 0.5f) * 0.6f;
            n.dy    = -(rnd.nextFloat() * 1.2f + 0.5f);
            n.rot   = rnd.nextFloat() * (float)(Math.PI * 2);
            n.drot  = (rnd.nextFloat() - 0.5f) * 0.03f;
            n.alpha = rnd.nextFloat() * 0.55f + 0.2f;
            n.pulse = rnd.nextFloat() * (float)(Math.PI * 2);
            n.dPulse= rnd.nextFloat() * 0.04f + 0.02f;
            notes.add(n);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        for (Note n : notes) {
            // update position
            n.x    += n.dx;
            n.y    += n.dy;
            n.rot  += n.drot;
            n.pulse += n.dPulse;

            // wrap
            if (n.y < -80)   { n.y = h + 40; n.x = rnd.nextFloat() * w; }
            if (n.x < -40)   n.x = w + 20;
            if (n.x > w + 40) n.x = -20;

            float alpha = n.alpha * (0.65f + 0.35f * (float)Math.sin(n.pulse));
            int a = (int)(alpha * 255);
            int baseColor = n.color & 0x00FFFFFF;
            paint.setColor((a << 24) | baseColor);

            canvas.save();
            canvas.translate(n.x, n.y);
            canvas.rotate((float) Math.toDegrees(n.rot));
            drawNote(canvas, n.type, n.size);
            canvas.restore();
        }

        postInvalidateOnAnimation();
    }

    private void drawNote(Canvas canvas, int type, float s) {
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(s * 0.13f);

        switch (type) {

            case TYPE_QUARTER: {
                // filled oval head
                canvas.save();
                canvas.rotate(-23);
                canvas.drawOval(-s*.55f,-s*.42f, s*.55f,s*.42f, paint);
                canvas.restore();
                // stem
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(s*.45f,0, s*.45f,-s*1.8f, paint);
                break;
            }

            case TYPE_EIGHTH: {
                canvas.save(); canvas.rotate(-23);
                canvas.drawOval(-s*.55f,-s*.42f, s*.55f,s*.42f, paint);
                canvas.restore();
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(s*.45f,0, s*.45f,-s*1.8f, paint);
                // flag
                Path flag = new Path();
                flag.moveTo(s*.45f, -s*1.8f);
                flag.cubicTo(s*.45f+s*.9f, -s*1.8f+s*.3f,
                        s*.45f+s*.9f, -s*1.8f+s*.9f,
                        s*.45f+s*.3f, -s*1.8f+s*1.1f);
                canvas.drawPath(flag, paint);
                break;
            }

            case TYPE_SIXTEENTH: {
                canvas.save(); canvas.rotate(-23);
                canvas.drawOval(-s*.55f,-s*.42f, s*.55f,s*.42f, paint);
                canvas.restore();
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(s*.45f,0, s*.45f,-s*1.8f, paint);
                Path f1 = new Path();
                f1.moveTo(s*.45f,-s*1.8f);
                f1.cubicTo(s*.45f+s*.9f,-s*1.8f+s*.3f,
                        s*.45f+s*.9f,-s*1.8f+s*.9f,
                        s*.45f+s*.3f,-s*1.8f+s*1.1f);
                canvas.drawPath(f1, paint);
                Path f2 = new Path();
                f2.moveTo(s*.45f,-s*1.4f);
                f2.cubicTo(s*.45f+s*.9f,-s*1.4f+s*.3f,
                        s*.45f+s*.9f,-s*1.4f+s*.9f,
                        s*.45f+s*.3f,-s*1.4f+s*1.1f);
                canvas.drawPath(f2, paint);
                break;
            }

            case TYPE_BEAMED: {
                float g = s * 1.1f;
                // head 1
                canvas.save(); canvas.rotate(-23);
                canvas.drawOval(-s*.55f,-s*.42f, s*.55f,s*.42f, paint);
                canvas.restore();
                // head 2
                canvas.save(); canvas.translate(g,0); canvas.rotate(-23);
                canvas.drawOval(-s*.55f,-s*.42f, s*.55f,s*.42f, paint);
                canvas.restore();
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(s*.45f,0,    s*.45f,-s*1.8f, paint);
                canvas.drawLine(s*.45f+g,0,  s*.45f+g,-s*1.8f, paint);
                paint.setStrokeWidth(s * 0.28f);
                canvas.drawLine(s*.45f,-s*1.8f, s*.45f+g,-s*1.8f, paint);
                break;
            }

            case TYPE_HALF: {
                paint.setStyle(Paint.Style.STROKE);
                canvas.save(); canvas.rotate(-23);
                canvas.drawOval(-s*.55f,-s*.42f, s*.55f,s*.42f, paint);
                canvas.restore();
                canvas.drawLine(s*.45f,0, s*.45f,-s*1.8f, paint);
                break;
            }

            case TYPE_WHOLE: {
                paint.setStyle(Paint.Style.STROKE);
                canvas.save(); canvas.rotate(-11);
                canvas.drawOval(-s*.7f,-s*.5f, s*.7f,s*.5f, paint);
                canvas.restore();
                // inner cutout hint
                paint.setStyle(Paint.Style.FILL);
                int savedColor = paint.getColor();
                paint.setColor(0xFF0D0F12);
                canvas.save(); canvas.rotate(-11);
                canvas.drawOval(-s*.3f,-s*.18f, s*.3f,s*.18f, paint);
                canvas.restore();
                paint.setColor(savedColor);
                break;
            }

            case TYPE_TREBLE: {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(s * 0.14f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                Path clef = new Path();
                clef.moveTo(0, s*1.2f);
                clef.cubicTo(-s*1.1f,s*.6f,  -s*1.1f,-s*.4f,  0,-s*.6f);
                clef.cubicTo( s*1.0f,-s*.8f,  s*1.0f, s*.2f,  0, s*.5f);
                clef.cubicTo(-s*.8f, s*.8f,  -s*.3f,  s*1.4f, 0, s*1.5f);
                canvas.drawPath(clef, paint);
                canvas.drawCircle(0, s*1.2f, s*.28f, paint);
                break;
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }
}