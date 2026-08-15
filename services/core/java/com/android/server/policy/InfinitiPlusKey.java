/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.policy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.DeviceKeyHandler;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** Handles the dedicated Plus Key on infiniti devices. */
final class InfinitiPlusKey implements DeviceKeyHandler {
    static final String PACKAGE_NAME = "com.oplus.pluskey";
    static final String DISPATCH_PERMISSION = "com.oplus.pluskey.permission.DISPATCH";
    static final String ACTION_CAMERA_TRIGGER_DOWN = "com.oplus.pluskey.CAMERA_TRIGGER_DOWN";
    static final String ACTION_CAMERA_TRIGGER_UP = "com.oplus.pluskey.CAMERA_TRIGGER_UP";
    static final String ACTION_SHORT_PRESS = "com.oplus.pluskey.SHORT_PRESS";
    static final String ACTION_LONG_PRESS = "com.oplus.pluskey.LONG_PRESS";
    static final String EXTRA_SEQUENCE_ID = "com.oplus.pluskey.extra.SEQUENCE_ID";
    static final String EXTRA_USER_ID = "com.oplus.pluskey.extra.USER_ID";
    static final String EXTRA_KEYGUARD_LOCKED = "com.oplus.pluskey.extra.KEYGUARD_LOCKED";

    private final Context mContext;
    private final LongPressScheduler mLongPressScheduler;
    private final boolean mIsInfiniti;
    private final long mLongPressTimeoutMillis;
    private final IntSupplier mCurrentUserSupplier;
    private final BooleanSupplier mKeyguardLockedSupplier;

    private boolean mPressed;
    private boolean mLongPressDispatched;
    private long mNextSequenceId = 1;
    private GestureContext mGestureContext;

    InfinitiPlusKey(
            Context context,
            Handler handler,
            IntSupplier currentUserSupplier,
            BooleanSupplier keyguardLockedSupplier) {
        this(
                context,
                new HandlerLongPressScheduler(handler),
                "infiniti".equals(Build.DEVICE),
                ViewConfiguration.getLongPressTimeout(),
                currentUserSupplier,
                keyguardLockedSupplier);
    }

    @VisibleForTesting
    InfinitiPlusKey(
            Context context,
            LongPressScheduler longPressScheduler,
            boolean isInfiniti,
            long longPressTimeoutMillis,
            IntSupplier currentUserSupplier,
            BooleanSupplier keyguardLockedSupplier) {
        mContext = context;
        mLongPressScheduler = longPressScheduler;
        mIsInfiniti = isInfiniti;
        mLongPressTimeoutMillis = longPressTimeoutMillis;
        mCurrentUserSupplier = currentUserSupplier;
        mKeyguardLockedSupplier = keyguardLockedSupplier;
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        if (!mIsInfiniti || event.getKeyCode() != KeyEvent.KEYCODE_ASSIST) {
            return event;
        }

        synchronized (this) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!mPressed) {
                    beginSession(event);
                } else if (event.getRepeatCount() == 0
                        && event.getDownTime() != mGestureContext.mDownTime) {
                    finishSession(false);
                    beginSession(event);
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP
                    && mPressed
                    && event.getDownTime() == mGestureContext.mDownTime) {
                finishSession(!event.isCanceled());
            }
        }
        return null;
    }

    private void beginSession(KeyEvent event) {
        mPressed = true;
        mLongPressDispatched = false;
        mGestureContext =
                new GestureContext(
                        mNextSequenceId++,
                        event.getDownTime(),
                        mCurrentUserSupplier.getAsInt(),
                        mKeyguardLockedSupplier.getAsBoolean());
        sendBroadcast(ACTION_CAMERA_TRIGGER_DOWN, mGestureContext);
        mLongPressScheduler.postDelayed(
                mGestureContext.mLongPressRunnable, mLongPressTimeoutMillis);
    }

    private void finishSession(boolean dispatchGesture) {
        GestureContext gestureContext = mGestureContext;
        mLongPressScheduler.removeCallbacks(gestureContext.mLongPressRunnable);
        boolean dispatchShortPress = dispatchGesture && !mLongPressDispatched;
        mPressed = false;
        mLongPressDispatched = false;
        mGestureContext = null;
        sendBroadcast(ACTION_CAMERA_TRIGGER_UP, gestureContext);
        if (dispatchShortPress) {
            sendBroadcast(ACTION_SHORT_PRESS, gestureContext);
        }
    }

    private synchronized void dispatchLongPress(GestureContext gestureContext) {
        if (!mPressed || mLongPressDispatched || mGestureContext != gestureContext) {
            return;
        }
        mLongPressDispatched = true;
        sendBroadcast(ACTION_LONG_PRESS, gestureContext);
    }

    @SuppressLint("MissingPermission")
    private void sendBroadcast(String action, GestureContext gestureContext) {
        Intent intent = new Intent(action);
        intent.setPackage(PACKAGE_NAME);
        intent.putExtra(EXTRA_SEQUENCE_ID, gestureContext.mSequenceId);
        intent.putExtra(EXTRA_USER_ID, gestureContext.mUserId);
        intent.putExtra(EXTRA_KEYGUARD_LOCKED, gestureContext.mKeyguardLocked);
        mContext.sendBroadcastAsUser(
                intent, UserHandle.of(gestureContext.mUserId), DISPATCH_PERMISSION);
    }

    @VisibleForTesting
    interface LongPressScheduler {
        void postDelayed(Runnable runnable, long delayMillis);

        void removeCallbacks(Runnable runnable);
    }

    private static final class HandlerLongPressScheduler implements LongPressScheduler {
        private final Handler mHandler;

        HandlerLongPressScheduler(Handler handler) {
            mHandler = handler;
        }

        @Override
        public void postDelayed(Runnable runnable, long delayMillis) {
            mHandler.postDelayed(runnable, delayMillis);
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            mHandler.removeCallbacks(runnable);
        }
    }

    private final class GestureContext {
        final long mSequenceId;
        final long mDownTime;
        final int mUserId;
        final boolean mKeyguardLocked;
        final Runnable mLongPressRunnable = () -> dispatchLongPress(this);

        GestureContext(long sequenceId, long downTime, int userId, boolean keyguardLocked) {
            mSequenceId = sequenceId;
            mDownTime = downTime;
            mUserId = userId;
            mKeyguardLocked = keyguardLocked;
        }
    }
}
