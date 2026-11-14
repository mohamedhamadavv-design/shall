package com.example.snake;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SnakeView(this));
    }

    static class SnakeView extends View {

        private static final int COLS = 20;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Random random = new Random();
        private final ArrayList<int[]> snake = new ArrayList<>();
        private final float density;

        private int rows = 0;
        private float cell = 0f;
        private float offsetX = 0f;
        private float offsetY = 0f;

        private int dirX = 1;
        private int dirY = 0;
        private int nextDirX = 1;
        private int nextDirY = 0;
        private int foodX = 0;
        private int foodY = 0;
        private int score = 0;
        private boolean gameOver = false;
        private boolean started = false;

        private float downX = 0f;
        private float downY = 0f;

        private final Runnable ticker = new Runnable() {
            @Override
            public void run() {
                step();
                invalidate();
                handler.postDelayed(this, Math.max(70, 140 - score * 3));
            }
        };

        SnakeView(Context context) {
            super(context);
            density = context.getResources().getDisplayMetrics().density;
        }

        private void startLoop() {
            handler.removeCallbacks(ticker);
            handler.postDelayed(ticker, 140);
        }

        private void stopLoop() {
            handler.removeCallbacks(ticker);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            startLoop();
        }

        @Override
        protected void onDetachedFromWindow() {
            stopLoop();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            if (visibility == View.VISIBLE) {
                startLoop();
            } else {
                stopLoop();
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float topMargin = 64 * density;
            float available = h - topMargin - 16 * density;
            cell = (float) w / COLS;
            rows = Math.max(8, (int) (available / cell));
            offsetX = 0f;
            offsetY = topMargin + (available - rows * cell) / 2f;
            resetGame();
        }

        private void resetGame() {
            snake.clear();
            int startX = COLS / 2;
            int startY = rows / 2;
            for (int i = 0; i < 3; i++) {
                snake.add(new int[]{startX - i, startY});
            }
            dirX = 1;
            dirY = 0;
            nextDirX = 1;
            nextDirY = 0;
            score = 0;
            gameOver = false;
            started = false;
            placeFood();
        }

        private void placeFood() {
            if (snake.size() >= COLS * rows) {
                gameOver = true;
                return;
            }
            while (true) {
                int x = random.nextInt(COLS);
                int y = random.nextInt(rows);
                boolean onSnake = false;
                for (int[] p : snake) {
                    if (p[0] == x && p[1] == y) {
                        onSnake = true;
                        break;
                    }
                }
                if (!onSnake) {
                    foodX = x;
                    foodY = y;
                    return;
                }
            }
        }

        private void step() {
            if (!started || gameOver || snake.isEmpty()) {
                return;
            }
            dirX = nextDirX;
            dirY = nextDirY;

            int[] head = snake.get(0);
            int nx = head[0] + dirX;
            int ny = head[1] + dirY;

            if (nx < 0 || ny < 0 || nx >= COLS || ny >= rows) {
                gameOver = true;
                return;
            }

            boolean eating = (nx == foodX && ny == foodY);
            int limit = eating ? snake.size() : snake.size() - 1;
            for (int i = 0; i < limit; i++) {
                int[] p = snake.get(i);
                if (p[0] == nx && p[1] == ny) {
                    gameOver = true;
                    return;
                }
            }

            snake.add(0, new int[]{nx, ny});
            if (eating) {
                score++;
                placeFood();
            } else {
                snake.remove(snake.size() - 1);
            }
        }

        private void setDirection(int dx, int dy) {
            if (dx == -dirX && dy == -dirY) {
                return;
            }
            nextDirX = dx;
            nextDirY = dy;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = e.getX();
                downY = e.getY();
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                if (gameOver) {
                    resetGame();
                    invalidate();
                    return true;
                }
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;
                if (Math.abs(dx) < 40 && Math.abs(dy) < 40) {
                    return true;
                }
                if (Math.abs(dx) > Math.abs(dy)) {
                    setDirection(dx > 0 ? 1 : -1, 0);
                } else {
                    setDirection(0, dy > 0 ? 1 : -1);
                }
                started = true;
                invalidate();
                return true;
            }
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.rgb(18, 24, 38));
            if (rows == 0 || snake.isEmpty()) {
                return;
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(28, 36, 56));
            canvas.drawRect(offsetX, offsetY, offsetX + COLS * cell, offsetY + rows * cell, paint);

            float pad = cell * 0.1f;

            paint.setColor(Color.rgb(239, 83, 80));
            canvas.drawOval(
                    offsetX + foodX * cell + pad, offsetY + foodY * cell + pad,
                    offsetX + (foodX + 1) * cell - pad, offsetY + (foodY + 1) * cell - pad,
                    paint);

            for (int i = 0; i < snake.size(); i++) {
                int[] p = snake.get(i);
                paint.setColor(i == 0 ? Color.rgb(129, 199, 132) : Color.rgb(76, 175, 80));
                canvas.drawRoundRect(
                        offsetX + p[0] * cell + pad, offsetY + p[1] * cell + pad,
                        offsetX + (p[0] + 1) * cell - pad, offsetY + (p[1] + 1) * cell - pad,
                        cell * 0.25f, cell * 0.25f, paint);
            }

            paint.setColor(Color.WHITE);
            paint.setTextSize(24 * density);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Score: " + score, 16 * density, 40 * density, paint);

            paint.setTextAlign(Paint.Align.CENTER);
            float cx = getWidth() / 2f;
            float cy = offsetY + rows * cell / 2f;
            if (gameOver) {
                paint.setTextSize(32 * density);
                canvas.drawText("Game over", cx, cy, paint);
                paint.setTextSize(18 * density);
                canvas.drawText("Tap to play again", cx, cy + 36 * density, paint);
            } else if (!started) {
                paint.setTextSize(20 * density);
                canvas.drawText("Swipe to start", cx, cy + 60 * density, paint);
            }
        }
    }
}
