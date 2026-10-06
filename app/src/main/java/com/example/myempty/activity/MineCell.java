package com.example.myempty.activity2;

public class MineCell {

    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPEN = 1;
    public static final int STATE_FLAG = 2;
    public static final int STATE_QUESTION = 3;

    public static final int VAL_EMPTY = 0;
    public static final int VAL_MINE = -1;

    public int state = STATE_CLOSED;
    public int value = 0;
    public boolean isMine = false;
    public boolean exploded = false;
    public boolean wrongFlag = false;

    public boolean isClosed() {
        return state == STATE_CLOSED;
    }

    public boolean isOpened() {
        return state == STATE_OPEN;
    }

    public boolean isFlagged() {
        return state == STATE_FLAG;
    }

    public void reset() {
        state = STATE_CLOSED;
        value = 0;
        isMine = false;
        exploded = false;
        wrongFlag = false;
    }
}