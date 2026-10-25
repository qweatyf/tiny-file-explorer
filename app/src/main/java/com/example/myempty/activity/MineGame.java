package com.example.myempty.activity2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MineGame {

    public static final int RESULT_CONTINUE = 0;
    public static final int RESULT_WIN = 1;
    public static final int RESULT_LOSE = 2;

    public int width;
    public int height;
    public int totalMines;

    private MineCell[][] board;
    private boolean firstClick = true;
    private boolean gameOver = false;
    private boolean win = false;
    private int openedCount = 0;
    private int flagCount = 0;
    private long startTime = 0;
    private long endTime = 0;
    private boolean timing = false;

    private final Random rng = new Random();

    public MineGame(int width, int height, int mines) {
        this.width = width;
        this.height = height;

        int max = width * height - 9;
        if (mines < 1) mines = 1;
        if (mines > max) mines = max;
        this.totalMines = mines;

        board = new MineCell[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                board[y][x] = new MineCell();
            }
        }
    }

    public MineCell get(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return null;
        return board[y][x];
    }

    public boolean isGameOver() { return gameOver; }
    public boolean isWin() { return win; }
    public int getOpenedCount() { return openedCount; }
    public int getFlagCount() { return flagCount; }
    public int getRemainMines() { return totalMines - flagCount; }

    public long getElapsedMs() {
        if (!timing) return endTime - startTime;
        return System.currentTimeMillis() - startTime;
    }

    public boolean isTiming() { return timing; }

    private void placeMines(int safeX, int safeY) {
        List<int[]> spots = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (Math.abs(x - safeX) <= 1 && Math.abs(y - safeY) <= 1) continue;
                spots.add(new int[]{x, y});
            }
        }
        Collections.shuffle(spots, rng);

        int place = Math.min(totalMines, spots.size());
        for (int i = 0; i < place; i++) {
            int[] p = spots.get(i);
            board[p[1]][p[0]].isMine = true;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (board[y][x].isMine) {
                    board[y][x].value = MineCell.VAL_MINE;
                } else {
                    board[y][x].value = countAround(x, y);
                }
            }
        }
    }

    private int countAround(int cx, int cy) {
        int n = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int x = cx + dx;
                int y = cy + dy;
                if (x < 0 || x >= width || y < 0 || y >= height) continue;
                if (board[y][x].isMine) n++;
            }
        }
        return n;
    }

    public int open(int x, int y) {
        if (gameOver) return RESULT_CONTINUE;
        MineCell c = get(x, y);
        if (c == null) return RESULT_CONTINUE;
        if (c.isOpened() || c.isFlagged()) return RESULT_CONTINUE;
        if (c.state == MineCell.STATE_QUESTION) return RESULT_CONTINUE;

        if (firstClick) {
            firstClick = false;
            placeMines(x, y);
            startTime = System.currentTimeMillis();
            timing = true;
        }

        if (c.isMine) {
            c.exploded = true;
            c.state = MineCell.STATE_OPEN;
            gameOver = true;
            win = false;
            timing = false;
            endTime = System.currentTimeMillis();
            revealAllMines();
            return RESULT_LOSE;
        }

        c.state = MineCell.STATE_OPEN;
        openedCount++;

        if (checkWin()) {
            gameOver = true;
            win = true;
            timing = false;
            endTime = System.currentTimeMillis();
            autoFlagAllMines();
            return RESULT_WIN;
        }

        return RESULT_CONTINUE;
    }

    public void toggleFlag(int x, int y) {
        if (gameOver) return;
        MineCell c = get(x, y);
        if (c == null || c.isOpened()) return;

        if (c.state == MineCell.STATE_CLOSED) {
            c.state = MineCell.STATE_FLAG;
            flagCount++;
        } else if (c.state == MineCell.STATE_FLAG) {
            c.state = MineCell.STATE_QUESTION;
            flagCount--;
        } else if (c.state == MineCell.STATE_QUESTION) {
            c.state = MineCell.STATE_CLOSED;
        }
    }

    public boolean chord(int x, int y) {
        return false;
    }

    private boolean checkWin() {
        return openedCount == width * height - totalMines;
    }

    private void revealAllMines() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MineCell c = board[y][x];
                if (c.isMine && !c.isFlagged()) {
                    c.state = MineCell.STATE_OPEN;
                }
                if (!c.isMine && c.isFlagged()) {
                    c.wrongFlag = true;
                }
            }
        }
    }

    private void autoFlagAllMines() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MineCell c = board[y][x];
                if (c.isMine) {
                    c.state = MineCell.STATE_FLAG;
                }
            }
        }
        flagCount = totalMines;
    }
}