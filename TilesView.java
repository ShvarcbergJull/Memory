package com.example.pc.memory;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

class Card {
    Paint p = new Paint();

    public Card(float x, float y, float width, float height, int color) {
        this.color = color;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int color, backColor = Color.DKGRAY;
    boolean isOpen = false; // цвет карты
    float x, y, width, height;
    public void draw(Canvas c) {
        // нарисовать карту в виде цветного прямоугольника
        if (isOpen) {
            p.setColor(color);
        } else p.setColor(backColor);
        c.drawRect(x,y, width, height, p);
    }

    public boolean flip (float touch_x, float touch_y) {
        if (touch_x >= x && touch_x <= width && touch_y >= y && touch_y <= height) {
            isOpen = ! isOpen;
            return true;
        } else return false;
    }

}

public class TilesView extends View {
    // пауза для запоминания карт
    final int PAUSE_LENGTH = 2; // в секундах
    boolean isOnPauseNow = false;
    boolean game;

    // число открытых карт
    int openedCard = 0;
    MainActivity activity;
    Card two_card;

    ArrayList<Card> cards = new ArrayList<>();
    ArrayList<Integer> colors;

    int width, height; // ширина и высота канвы

    public TilesView(Context context) {
        super(context);
    }

    public TilesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // 1) заполнить массив tiles случайными цветами
        // сгенерировать поле 2*n карт, при этом
        // должно быть ровно n пар карт разных цветов
        game = false;
        activity = (MainActivity) context;
        Integer[] test = new Integer[] {Color.RED, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GREEN, Color.BLUE,
                                        Color.RED, Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GREEN, Color.BLUE};
        colors = new ArrayList<Integer>(Arrays.asList(test));
    }

    protected void createField() {
        float widthCard = (width - (3 * 50)) / 3;
        float heightCard = (height - (4 * 50)) / 4;
        float start_x = 25, start_y = 25, end_x = start_x + widthCard, end_y = start_y + heightCard;
        int color = Color.rgb(0, 0,0);

        ArrayList<Integer> indexs = new ArrayList<>();
        for (int i = 0;i < 12;i++) {
            int c = (int)(Math.random() * colors.size());
            Log.d("forcolors", "" + c);
            cards.add(new Card(start_x, start_y, end_x, end_y, colors.get(c)));
            colors.remove(c);
            if (end_x + 50 < width) {
                start_x = end_x + 50;
                end_x = start_x + widthCard;
            }
            else {
                start_x = 25;
                end_x = start_x + widthCard;
                start_y = end_y + 50;
                end_y = start_y + heightCard;
            }
            indexs.add(i);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        if (!game) {
            width = canvas.getWidth();
            height = canvas.getHeight();
            createField();
            game = true;
            invalidate();
        }
        else {
        // 2) отрисовка плиток
        // задать цвет можно, используя кисть
            for (Card c: cards) {
                c.draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 3) получить координаты касания
        int x = (int) event.getX();
        int y = (int) event.getY();
        // 4) определить тип события
        if (event.getAction() == MotionEvent.ACTION_DOWN && !isOnPauseNow)
        {
            // палец коснулся экрана

            for (Card c: cards) {

                if (openedCard == 0) {
                    if (c.flip(x, y)) {
                        Log.d("mytag", "card flipped: " + openedCard);
                        openedCard ++;
                        invalidate();
                        return true;
                    }
                }

                if (openedCard == 1) {


                    // перевернуть карту с задержкой
                    if (c.flip(x, y)) {
                        openedCard ++;
                        // 1) если открылис карты одинакового цвета, удалить их из списка
                        // например написать функцию, checkOpenCardsEqual

                        // 2) проверить, остались ли ещё карты
                        // иначе сообщить об окончании игры

                        // если карты открыты разного цвета - запустить задержку
                        invalidate();
                        PauseTask task = new PauseTask();
                        task.execute(PAUSE_LENGTH);
                        isOnPauseNow = true;

                        if (checkOpenCardsEqual(c)) {
                            if (cards.size() == 0) {
                                Toast toast = Toast.makeText(activity, "Game over", Toast.LENGTH_LONG);
                                toast.show();
                                return true;
                            }
                        }

                        return true;
                    }
                }

            }
        }


        // заставляет экран перерисоваться
        return true;
    }

    public void newGame() {
        // запуск новой игры
    }

    public boolean checkOpenCardsEqual(Card card) {
        for (Card c: cards) {
            if (c.isOpen && (card.x != c.x || card.y != c.y)) {
                if (card.color == c.color) {
//                    two_card = c;
                    cards.remove(c);
                    cards.remove(card);
                    return true;
                }
            }
        }

        return false;
    }

    class PauseTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... integers) {
            Log.d("mytag", "Pause started");
            try {
                Thread.sleep(integers[0] * 1000); // передаём число секунд ожидания
            } catch (InterruptedException e) {}
            Log.d("mytag", "Pause finished");
            return null;
        }

        // после паузы, перевернуть все карты обратно


        @Override
        protected void onPostExecute(Void aVoid) {
            for (Card c: cards) {
                if (c.isOpen) {
                    c.isOpen = false;
                }
            }
            openedCard = 0;
            isOnPauseNow = false;
            invalidate();
        }
    }
}
