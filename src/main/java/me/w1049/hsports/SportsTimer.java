package me.w1049.hsports;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

public class SportsTimer {
    public SportsTimer(Button mStartButton, Button mStopButton, TextView mTimeView, TextView mAvgSpdView,
            TextView mDisView) {
        this.mStartButton = mStartButton;
        this.mStopButton = mStopButton;
        this.mTimeView = mTimeView;
        this.mAvgSpdView = mAvgSpdView;
        this.mDisView = mDisView;
        reset();
    }

    public enum TimerState {
        INIT, RUNNING, PAUSED, AUTO_PAUSED,
    }

    private TimerState state = TimerState.INIT;

    // Buttons
    private final Button mStartButton;
    private final Button mStopButton;

    // Views
    private final TextView mTimeView;
    private final TextView mAvgSpdView;
    private final TextView mDisView;

    private int mSeconds = 0;
    private double mDistance = 0;
    private final Handler timerHandler = new Handler();
    private final Runnable timerRunnable = new Runnable() {
        @SuppressLint("DefaultLocale")
        @Override
        public void run() {
            mSeconds++;
            int hours = mSeconds / 3600;
            int minutes = (mSeconds % 3600) / 60;
            int secs = mSeconds % 60;

            String time = String.format("%02d:%02d:%02d", hours, minutes, secs);
            mTimeView.setText(time);
            mAvgSpdView.setText(String.format("%.2f", mDistance / mSeconds * 3.6));
            mDisView.setText(String.format("%.2f", mDistance / 1000));

            timerHandler.postDelayed(this, 1000);
        }
    };

    public TimerState getState() {
        return state;
    }

    public int getSeconds() {
        return mSeconds;
    }

    public double getDistance() {
        return mDistance;
    }

    public void start() {
        state = TimerState.RUNNING;
        timerHandler.postDelayed(timerRunnable, 0);
        mStopButton.setActivated(false);
        mStartButton.setText("暂停");
    }

    public void pause() {
        state = TimerState.PAUSED;
        timerHandler.removeCallbacks(timerRunnable);
        mStopButton.setActivated(true);
        mStartButton.setText("继续");
    }

    public void autoPause() {
        state = TimerState.AUTO_PAUSED;
        timerHandler.removeCallbacks(timerRunnable);
        mStopButton.setActivated(true);
        mStartButton.setText("继续");
    }

    public void reset() {
        state = TimerState.INIT;
        timerHandler.removeCallbacks(timerRunnable);
        mStopButton.setActivated(false);
        mStartButton.setText("开始");
        mSeconds = 0;
        mDistance = 0;
        mTimeView.setText("00:00:00");
        mAvgSpdView.setText("0.00");
        mDisView.setText("0.00");
    }

    public void addDistance(double distance) {
        this.mDistance += distance;
    }
}
