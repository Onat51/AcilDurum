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

    // Direction info
    private String[][] directions = {
            {"🏃", "Kaçırılıyorum"},      // UP
            {"👁️", "Takip Ediliyorum"},   // LEFT
            {"📍", "Buraya Gelin"},        // DOWN
            {"🚑", "Ambulans/Kaza"}        // RIGHT
    };

    private int[] directionColors = {
            Color.parseColor("#FF5722"),  // UP - Turuncu
            Color.parseColor("#9C27B0"),  // LEFT - Mor
            Color.parseColor("#2196F3"),  // DOWN - Mavi
            Color.parseColor("#4CAF50")   // RIGHT - Yeşil
    };

    public EmergencyButton(Context context) {
        super(context);
        init(context);
    }

    public EmergencyButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EmergencyButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        // Ana buton paint
        mainButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainButtonPaint.setStyle(Paint.Style.FILL);

        // Buton yazı paint
        buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Yön arka plan paint
        directionBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionBgPaint.setStyle(Paint.Style.FILL);

        // Yön yazı paint
        directionTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionTextPaint.setColor(Color.WHITE);
        directionTextPaint.setTextAlign(Paint.Align.CENTER);
        directionTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Emoji paint
        directionEmojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        directionEmojiPaint.setTextAlign(Paint.Align.CENTER);

        // Progress paint
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.parseColor("#FFEB3B"));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // Glow paint
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
            float value = (float) animation.getAnimatedValue();
            pulseScale = 1f + 0.05f * (float) Math.sin(value * Math.PI * 2);
            glowAlpha = 0.2f + 0.15f * (float) Math.sin(value * Math.PI * 2);
            invalidate();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;

        // Ekran boyutuna göre buton boyutu
        mainRadius = Math.min(w, h) * 0.22f;

        // Yazı boyutlarını ayarla
        buttonTextPaint.setTextSize(mainRadius * 0.28f);
        directionTextPaint.setTextSize(mainRadius * 0.22f);
        directionEmojiPaint.setTextSize(mainRadius * 0.5f);
        progressPaint.setStrokeWidth(mainRadius * 0.08f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float buttonX = centerX + currentOffsetX;
        float buttonY = centerY + currentOffsetY;
        float radius = mainRadius * pulseScale;

        // Yön göstergelerini çiz
        if (showDirections) {
            drawDirectionIndicators(canvas, buttonX, buttonY);
        }

        // Glow efekti
        glowPaint.setColor(Color.parseColor("#FF1744"));
        glowPaint.setAlpha((int) (glowAlpha * 255));
        glowPaint.setShadowLayer(radius * 0.4f, 0, 0, Color.parseColor("#FF1744"));
        canvas.drawCircle(buttonX, buttonY, radius + 15, glowPaint);

        // Long press progress
        if (isLongPressing && longPressProgress > 0) {
            RectF progressRect = new RectF(
                    buttonX - radius - 20,
                    buttonY - radius - 20,
                    buttonX + radius + 20,
                    buttonY + radius + 20
            );
            progressPaint.setColor(Color.parseColor("#FFEB3B"));
            canvas.drawArc(progressRect, -90, 360 * longPressProgress, false, progressPaint);
        }

        // Ana buton gradient
        LinearGradient gradient = new LinearGradient(
                buttonX - radius, buttonY - radius,
                buttonX + radius, buttonY + radius,
                Color.parseColor("#FF1744"),
                Color.parseColor("#D50000"),
                Shader.TileMode.CLAMP
        );
        mainButtonPaint.setShader(gradient);
        mainButtonPaint.setShadowLayer(20, 0, 10, Color.parseColor("#80000000"));
        canvas.drawCircle(buttonX, buttonY, radius, mainButtonPaint);

        // İç halka
        Paint innerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRingPaint.setStyle(Paint.Style.STROKE);
        innerRingPaint.setColor(Color.parseColor("#40FFFFFF"));
        innerRingPaint.setStrokeWidth(3);
        canvas.drawCircle(buttonX, buttonY, radius * 0.85f, innerRingPaint);

        // Buton yazısı
        canvas.drawText("ACİL", buttonX, buttonY - mainRadius * 0.08f, buttonTextPaint);
        canvas.drawText("DURUM", buttonX, buttonY + mainRadius * 0.22f, buttonTextPaint);

        // Alt bilgi
        if (!showDirections && !isLongPressing) {
            Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hintPaint.setColor(Color.parseColor("#80FFFFFF"));
            hintPaint.setTextSize(mainRadius * 0.15f);
            hintPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Kaydır veya basılı tut", buttonX, buttonY + radius + mainRadius * 0.5f, hintPaint);
        }
    }

    private void drawDirectionIndicators(Canvas canvas, float buttonX, float buttonY) {
        float offset = mainRadius * 2.2f;
        float indicatorRadius = mainRadius * 0.55f;

        // Positions: UP, LEFT, DOWN, RIGHT
        float[][] positions = {
                {centerX, centerY - offset},           // UP
                {centerX - offset, centerY},           // LEFT
                {centerX, centerY + offset},           // DOWN
                {centerX + offset, centerY}            // RIGHT
        };

        for (int i = 0; i < 4; i++) {
            float x = positions[i][0];
            float y = positions[i][1];

            // Arka plan dairesi
            directionBgPaint.setColor(directionColors[i]);
            directionBgPaint.setAlpha(220);
            directionBgPaint.setShadowLayer(15, 0, 5, Color.parseColor("#40000000"));
            canvas.drawCircle(x, y, indicatorRadius, directionBgPaint);

            // Emoji
            canvas.drawText(directions[i][0], x, y + indicatorRadius * 0.15f, directionEmojiPaint);

            // Yazı (dairenin altında)
            directionTextPaint.setColor(Color.WHITE);
            canvas.drawText(directions[i][1], x, y + indicatorRadius + mainRadius * 0.35f, directionTextPaint);
        }

        // Ortada "5 sn basılı tut" yazısı
        Paint centerHintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerHintPaint.setColor(Color.parseColor("#FFEB3B"));
        centerHintPaint.setTextSize(mainRadius * 0.18f);
        centerHintPaint.setTextAlign(Paint.Align.CENTER);
        centerHintPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("5 sn basılı tut = TAM ACİL", centerX, centerY + mainRadius * 1.4f + offset * 0.3f, centerHintPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();

                float dist = (float) Math.sqrt(
                        Math.pow(touchStartX - centerX, 2) +
                                Math.pow(touchStartY - centerY, 2)
                );

                if (dist <= mainRadius * 1.2f) {
                    isDragging = true;
                    showDirections = true;
                    vibrate(50);
                    startLongPressTimer();
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    currentOffsetX = event.getX() - touchStartX;
                    currentOffsetY = event.getY() - touchStartY;

                    float moveDist = (float) Math.sqrt(
                            currentOffsetX * currentOffsetX + currentOffsetY * currentOffsetY
                    );
                    if (moveDist > 30) {
                        stopLongPressTimer();
                    }

                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    stopLongPressTimer();

                    int direction = getDirection(currentOffsetX, currentOffsetY);
                    if (direction != -1 && listener != null) {
                        vibrate(100);
                        listener.onDirectionSelected(direction);
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

    private void vibrate(int duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }
        }
    }

    private int getDirection(float offsetX, float offsetY) {
        float threshold = mainRadius * 0.8f;

        if (Math.abs(offsetY) > Math.abs(offsetX)) {
            if (offsetY < -threshold) {
                return Constants.EMERGENCY_KIDNAPPING;
            } else if (offsetY > threshold) {
                return Constants.EMERGENCY_COME_HERE;
            }
        } else {
            if (offsetX < -threshold) {
                return Constants.EMERGENCY_BEING_FOLLOWED;
            } else if (offsetX > threshold) {
                return Constants.EMERGENCY_AMBULANCE;
            }
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
        if (listener != null) {
            listener.onLongPressProgress(0f);
        }
        invalidate();
    }

    private Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isLongPressing) {
                long elapsed = System.currentTimeMillis() - longPressStartTime;
                longPressProgress = Math.min(1f, elapsed / (float) Constants.LONG_PRESS_DURATION);

                if (listener != null) {
                    listener.onLongPressProgress(longPressProgress);
                }

                // Her %25'te titreşim
                if (longPressProgress > 0.25f && longPressProgress < 0.26f) vibrate(30);
                if (longPressProgress > 0.50f && longPressProgress < 0.51f) vibrate(30);
                if (longPressProgress > 0.75f && longPressProgress < 0.76f) vibrate(30);

                if (elapsed >= Constants.LONG_PRESS_DURATION) {
                    vibrate(200);
                    if (listener != null) {
                        listener.onEmergencyTriggered(Constants.EMERGENCY_FULL);
                    }
                    isLongPressing = false;
                    longPressProgress = 0f;
                    showDirections = false;
                } else {
                    longPressHandler.postDelayed(this, 30);
                }
                invalidate();
            }
        }
    };

    private void animateBack() {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator animX = ObjectAnimator.ofFloat(this, "offsetX", currentOffsetX, 0);
        ObjectAnimator animY = ObjectAnimator.ofFloat(this, "offsetY", currentOffsetY, 0);
        animatorSet.playTogether(animX, animY);
        animatorSet.setDuration(250);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    public void setOffsetX(float offset) {
        currentOffsetX = offset;
        invalidate();
    }

    public void setOffsetY(float offset) {
        currentOffsetY = offset;
        invalidate();
    }

    public void setListener(EmergencyButtonListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        longPressHandler.removeCallbacks(longPressRunnable);
    }
}