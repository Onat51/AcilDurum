package com.example.emergencyapp.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

public class EmergencyButton extends View {

    public interface EmergencyButtonListener {
        void onEmergencyTriggered(int emergencyType);
        void onDirectionSelected(int direction);
        void onLongPressProgress(float progress);
    }

    private Paint mainButtonPaint;
    private Paint buttonTextPaint;
    private Paint directionBgPaint;
    private Paint directionTextPaint;
    private Paint directionEmojiPaint;
    private Paint progressPaint;
    private Paint glowPaint;

    private float centerX, centerY;
    private float mainRadius;
    private float currentOffsetX = 0f;
    private float currentOffsetY = 0f;

    private float touchStartX, touchStartY;
    private boolean isDragging = false;
    private boolean isLongPressing = false;
    private boolean showDirections = false;

    private Handler longPressHandler = new Handler(Looper.getMainLooper());
    private long longPressStartTime = 0;
    private float longPressProgress = 0f;

    private EmergencyButtonListener listener;
    private ValueAnimator pulseAnimator;
    private float pulseScale = 1f;
    private float glowAlpha = 0.3f;

    private Vibrator vibrator;

    // Directions: UP, LEFT, DOWN, RIGHT
    private String[][] directions = {
            {"🏃", "Kaçırılıyorum"},
            {"👁️", "Takip Ediliyorum"},
            {"📍", "Buraya Gelin"},
            {"🚑", "Ambulans"}
    };

    private int[] directionColors = {
            Color.parseColor("#FF5722"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50")
    };

    public EmergencyButton(Context context) { super(context); init(context); }
    public EmergencyButton(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(context); }
    public EmergencyButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(context);
    }

    private void init(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        mainButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainButtonPaint.setStyle(Paint.Style.FILL);

        buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        directionBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionBgPaint.setStyle(Paint.Style.FILL);

        directionTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionTextPaint.setColor(Color.WHITE);
        directionTextPaint.setTextAlign(Paint.Align.CENTER);
        directionTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        directionEmojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionEmojiPaint.setTextAlign(Paint.Align.CENTER);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.parseColor("#FFEB3B"));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
        startPulseAnimation();
    }

    private void startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            pulseScale = 1f + 0.04f * (float) Math.sin(v * Math.PI * 2);
            glowAlpha = 0.15f + 0.12f * (float) Math.sin(v * Math.PI * 2);
            invalidate();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;

        // Main button: 28% of smaller dimension — keeps it from being huge
        mainRadius = Math.min(w, h) * 0.28f;

        buttonTextPaint.setTextSize(mainRadius * 0.26f);
        // Smaller direction labels
        directionTextPaint.setTextSize(mainRadius * 0.18f);
        directionEmojiPaint.setTextSize(mainRadius * 0.38f);
        progressPaint.setStrokeWidth(mainRadius * 0.07f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float buttonX = centerX + currentOffsetX;
        float buttonY = centerY + currentOffsetY;
        float radius = mainRadius * pulseScale;

        if (showDirections) {
            drawDirectionIndicators(canvas, buttonX, buttonY);
        }

        // Subtle glow
        glowPaint.setColor(Color.parseColor("#FF1744"));
        glowPaint.setAlpha((int) (glowAlpha * 255));
        glowPaint.setShadowLayer(radius * 0.35f, 0, 0, Color.parseColor("#FF1744"));
        canvas.drawCircle(buttonX, buttonY, radius + 10, glowPaint);

        // Long press progress arc
        if (isLongPressing && longPressProgress > 0) {
            RectF rect = new RectF(buttonX - radius - 16, buttonY - radius - 16,
                    buttonX + radius + 16, buttonY + radius + 16);
            progressPaint.setColor(Color.parseColor("#FFEB3B"));
            canvas.drawArc(rect, -90, 360 * longPressProgress, false, progressPaint);
        }

        // Main button
        LinearGradient gradient = new LinearGradient(
                buttonX - radius, buttonY - radius,
                buttonX + radius, buttonY + radius,
                Color.parseColor("#FF1744"), Color.parseColor("#D50000"),
                Shader.TileMode.CLAMP);
        mainButtonPaint.setShader(gradient);
        mainButtonPaint.setShadowLayer(18, 0, 8, Color.parseColor("#80000000"));
        canvas.drawCircle(buttonX, buttonY, radius, mainButtonPaint);

        // Inner ring
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(Color.parseColor("#35FFFFFF"));
        ring.setStrokeWidth(2);
        canvas.drawCircle(buttonX, buttonY, radius * 0.83f, ring);

        // Label
        canvas.drawText("ACİL", buttonX, buttonY - mainRadius * 0.07f, buttonTextPaint);
        canvas.drawText("DURUM", buttonX, buttonY + mainRadius * 0.21f, buttonTextPaint);

        // Hint below button
        if (!showDirections && !isLongPressing) {
            Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hint.setColor(Color.parseColor("#60FFFFFF"));
            hint.setTextSize(mainRadius * 0.14f);
            hint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Kaydır veya 5s bas", buttonX, buttonY + radius + mainRadius * 0.45f, hint);
        }
    }

    private void drawDirectionIndicators(Canvas canvas, float buttonX, float buttonY) {
        // Offset scaled to view size — tighter spacing
        float offset = mainRadius * 1.9f;
        // Smaller indicator circles
        float indicatorRadius = mainRadius * 0.45f;

        float[][] positions = {
                {centerX,          centerY - offset},   // UP
                {centerX - offset, centerY},            // LEFT
                {centerX,          centerY + offset},   // DOWN
                {centerX + offset, centerY}             // RIGHT
        };

        for (int i = 0; i < 4; i++) {
            float x = positions[i][0];
            float y = positions[i][1];

            directionBgPaint.setColor(directionColors[i]);
            directionBgPaint.setAlpha(210);
            directionBgPaint.setShadowLayer(12, 0, 4, Color.parseColor("#40000000"));
            canvas.drawCircle(x, y, indicatorRadius, directionBgPaint);

            directionEmojiPaint.setTextSize(indicatorRadius * 0.75f);
            canvas.drawText(directions[i][0], x, y + indicatorRadius * 0.15f, directionEmojiPaint);

            directionTextPaint.setTextSize(indicatorRadius * 0.38f);
            directionTextPaint.setColor(Color.WHITE);
            canvas.drawText(directions[i][1], x, y + indicatorRadius + mainRadius * 0.28f, directionTextPaint);
        }

        // Center hint for full emergency
        Paint centerHint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerHint.setColor(Color.parseColor("#FFEB3B"));
        centerHint.setTextSize(mainRadius * 0.15f);
        centerHint.setTextAlign(Paint.Align.CENTER);
        centerHint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("5 sn bas = TAM ACİL", centerX, centerY + mainRadius * 1.1f + offset * 0.3f, centerHint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                float dist = (float) Math.sqrt(
                        Math.pow(touchStartX - centerX, 2) +
                                Math.pow(touchStartY - centerY, 2));
                if (dist <= mainRadius * 1.2f) {
                    isDragging = true;
                    showDirections = true;
                    vibrate(40);
                    startLongPressTimer();
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    currentOffsetX = event.getX() - touchStartX;
                    currentOffsetY = event.getY() - touchStartY;
                    float moveDist = (float) Math.sqrt(currentOffsetX * currentOffsetX + currentOffsetY * currentOffsetY);
                    if (moveDist > 25) stopLongPressTimer();
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    stopLongPressTimer();
                    int direction = getDirection(currentOffsetX, currentOffsetY);
                    if (direction != -1 && listener != null) {
                        vibrate(80);
                        listener.onDirectionSelected(direction);
                    } else if (listener != null) {
                        listener.onDirectionSelected(-1);
                    }
                    animateBack();
                }
                isDragging = false;
                showDirections = false;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void vibrate(int ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }

    private int getDirection(float offsetX, float offsetY) {
        float threshold = mainRadius * 0.7f;
        if (Math.abs(offsetY) > Math.abs(offsetX)) {
            if (offsetY < -threshold) return Constants.EMERGENCY_KIDNAPPING;
            if (offsetY >  threshold) return Constants.EMERGENCY_COME_HERE;
        } else {
            if (offsetX < -threshold) return Constants.EMERGENCY_BEING_FOLLOWED;
            if (offsetX >  threshold) return Constants.EMERGENCY_AMBULANCE;
        }
        return -1;
    }

    private void startLongPressTimer() {
        isLongPressing = true;
        longPressStartTime = System.currentTimeMillis();
        longPressHandler.post(longPressRunnable);
    }

    private void stopLongPressTimer() {
        isLongPressing = false;
        longPressProgress = 0f;
        longPressHandler.removeCallbacks(longPressRunnable);
        if (listener != null) listener.onLongPressProgress(0f);
        invalidate();
    }

    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (!isLongPressing) return;
            long elapsed = System.currentTimeMillis() - longPressStartTime;
            longPressProgress = Math.min(1f, elapsed / (float) Constants.LONG_PRESS_DURATION);
            if (listener != null) listener.onLongPressProgress(longPressProgress);
            if (longPressProgress > 0.25f && longPressProgress < 0.26f) vibrate(25);
            if (longPressProgress > 0.50f && longPressProgress < 0.51f) vibrate(25);
            if (longPressProgress > 0.75f && longPressProgress < 0.76f) vibrate(25);
            if (elapsed >= Constants.LONG_PRESS_DURATION) {
                vibrate(180);
                if (listener != null) listener.onEmergencyTriggered(Constants.EMERGENCY_FULL);
                isLongPressing = false;
                longPressProgress = 0f;
                showDirections = false;
            } else {
                longPressHandler.postDelayed(this, 30);
            }
            invalidate();
        }
    };

    private void animateBack() {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator animX = ObjectAnimator.ofFloat(this, "offsetX", currentOffsetX, 0);
        ObjectAnimator animY = ObjectAnimator.ofFloat(this, "offsetY", currentOffsetY, 0);
        set.playTogether(animX, animY);
        set.setDuration(220);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }

    public void setOffsetX(float v) { currentOffsetX = v; invalidate(); }
    public void setOffsetY(float v) { currentOffsetY = v; invalidate(); }
    public void setListener(EmergencyButtonListener l) { this.listener = l; }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) pulseAnimator.cancel();
        longPressHandler.removeCallbacks(longPressRunnable);
    }
}