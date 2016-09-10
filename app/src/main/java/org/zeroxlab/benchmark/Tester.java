/*
 * Copyright (C) 2010 0xlab - http://0xlab.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zeroxlab.benchmark;

import android.util.Log;

import android.os.SystemClock;

import android.app.Activity;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import android.graphics.Canvas;
import android.os.*;

public abstract class Tester extends Activity {
    private String TAG;
    public final static String PACKAGE = "org.zeroxlab.benchmark";
    // 线程里一共要循环多少次oneRound()
    int mRound;
    // 线程里还剩循环了多少次oneRound()
    int mNow;
    int mIndex;

    protected long mTesterStart = 0;
    protected long mTesterEnd   = 0;

    // 抽象方法，让子类实现，父类中调用，子类实现父类方法
    protected abstract String getTag();
    protected abstract int sleepBeforeStart();
    protected abstract int sleepBetweenRound();
    protected abstract void oneRound();

    protected String mSourceTag = "unknown";
    private boolean mNextRound = true;

    protected boolean mDropTouchEvent     = true;
    protected boolean mDropTrackballEvent = true;

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        TAG = getTag();

        // 获取传递的参数
        Intent intent = getIntent();
        if (intent != null) {
            mRound     = Case.getRound(intent);
            mSourceTag = Case.getSource(intent);
            mIndex     = Case.getIndex(intent);
        } else {
            mRound = 80;
            mIndex = -1;
        }
        mNow   = mRound;
    }

    @Override
    protected void onPause() {
        super.onPause();
        interruptTester();
    }

    /* drop the annoying event */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mDropTouchEvent) {
            return false;
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if (mDropTrackballEvent) {
            return false;
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }

    /**
     * 该方法由子类调用
     */
    protected void startTester() {
        // 传入的时间函数由子类实现
        TesterThread thread = new TesterThread(sleepBeforeStart(), sleepBetweenRound());
        thread.start();
    }

    public void interruptTester() {
        mNow = 0;
        finish();
    }

    /**
     * Call this method if you finish your testing.
     *
     * @param start The starting time of testing round
     * @param end The ending time of testing round
     */
    public void finishTester(long start, long end) {

        // 记录测试的时间
        mTesterStart = start;
        mTesterEnd   = end;

        // 设置参数
        Intent intent = new Intent();
        if (mSourceTag == null || mSourceTag.equals("")) {
            Case.putSource(intent, "unknown");
        } else {
            Case.putSource(intent, mSourceTag);
        }

        // 设置参数
        Case.putIndex(intent, mIndex);
        saveResult(intent);

        setResult(0, intent);
        finish();
    }

    /**
     * Save the benchmarking result into intent
     * If this Case and Tester has their own way to pass benchmarking result
     * just override this method
     *
     * 保存运算消耗的时间
     *
     * @param intent The intent will return to Case
     */
    protected boolean saveResult(Intent intent) {
        long elapse = mTesterEnd - mTesterStart;
        Case.putResult(intent, elapse);
        return true;
    }

    public void resetCounter() {
        mNow = mRound;
    }

    public void decreaseCounter() {
        /*
        if (mNow == mRound) {
            mTesterStart = SystemClock.uptimeMillis();
        } else if (mNow == 1) {
            mTesterEnd = SystemClock.uptimeMillis();
        }
        */

        // 下一次还剩多少次
        mNow = mNow - 1;
        // 下一次继续运行
        mNextRound = true;
    }

    /**
     * 如果mNow的次数少于等于0，则返回false表示测试结束了
     * @return
     */
    public boolean isTesterFinished() {
        return (mNow <= 0);
    }

    class TesterThread extends Thread {
        // 启动延时时间
        int mSleepingStart;
        // while循环中延时时间
        int mSleepingTime;

        TesterThread(int sleepStart, int sleepPeriod) {
            mSleepingStart = sleepStart;
            mSleepingTime  = sleepPeriod;
        }

        private void lazyLoop() throws Exception {
            // 循环设定的mRound次数后退出
            while (!isTesterFinished()) {
                if (mNextRound) {
                    mNextRound = false;
                    oneRound();
                } else {
                    sleep(mSleepingTime);
                    // TODO: 
                    // Benchmarks that calculates frequencies (e.g. fps) should be time,
                    // for example, GL cases should run for a fixed time, and calculate 
                    // # of frames rendered, instead of periodically checking if fixed 
                    // # of frames had been rendered (which can hurt performance).
                }
            }
        }

        private void nervousLoop() throws Exception {
            while (!isTesterFinished()) {
                oneRound();
            }
        }

        private void sleepLoop() throws Exception {
            while (!isTesterFinished()) {
                oneRound();
                sleep(mSleepingTime);
            }
        }

        public void run() {
            try {
                // 延时开始执行
                sleep(mSleepingStart);

                // 记录开始运行的时间
                long start = SystemClock.uptimeMillis();

                // 开始运行
                lazyLoop();

                // 记录结束运行的时间
                long end = SystemClock.uptimeMillis();

                // 测试结束
                finishTester(start, end);
            } catch (Exception e) {
                    e.printStackTrace();
            }
        }
    }
}
