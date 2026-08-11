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

/** Handles the dedicated Plus Key on infiniti devices. */
final class InfinitiPlusKey implements DeviceKeyHandler {
    static final String PACKAGE_NAME = "com.oplus.pluskey";
    static final String DISPATCH_PERMISSION = "com.oplus.pluskey.permission.DISPATCH";
    static final String ACTION_CAMERA_TRIGGER_DOWN =
            "com.oplus.pluskey.CAMERA_TRIGGER_DOWN";
    static final String ACTION_CAMERA_TRIGGER_UP =
            "com.oplus.pluskey.CAMERA_TRIGGER_UP";
    static final String ACTION_SHORT_PRESS = "com.oplus.pluskey.SHORT_PRESS";
    static final String ACTION_LONG_PRESS = "com.oplus.pluskey.LONG_PRESS";

    private final Context mContext;
    private final Handler mHandler;
    private final boolean mIsInfiniti;
    private final long mLongPressTimeoutMillis;
    private final Runnable mLongPressRunnable = this::dispatchLongPress;

    private boolean mPressed;
    private boolean mLongPressDispatched;

    InfinitiPlusKey(Context context, Handler handler) {
        this(context, handler, "infiniti".equals(Build.DEVICE),
                ViewConfiguration.getLongPressTimeout());
    }

    @VisibleForTesting
    InfinitiPlusKey(Context context, Handler handler, boolean isInfiniti,
            long longPressTimeoutMillis) {
        mContext = context;
        mHandler = handler;
        mIsInfiniti = isInfiniti;
        mLongPressTimeoutMillis = longPressTimeoutMillis;
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        if (!mIsInfiniti || event.getKeyCode() != KeyEvent.KEYCODE_ASSIST) {
            return event;
        }

        synchronized (this) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!mPressed) {
                    mPressed = true;
                    mLongPressDispatched = false;
                    sendBroadcast(ACTION_CAMERA_TRIGGER_DOWN);
                    mHandler.postDelayed(mLongPressRunnable, mLongPressTimeoutMillis);
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP && mPressed) {
                mHandler.removeCallbacks(mLongPressRunnable);
                sendBroadcast(ACTION_CAMERA_TRIGGER_UP);
                if (!mLongPressDispatched) {
                    sendBroadcast(ACTION_SHORT_PRESS);
                }
                mPressed = false;
            }
        }
        return null;
    }

    private synchronized void dispatchLongPress() {
        if (!mPressed || mLongPressDispatched) {
            return;
        }
        mLongPressDispatched = true;
        sendBroadcast(ACTION_LONG_PRESS);
    }

    @SuppressLint("MissingPermission")
    private void sendBroadcast(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(PACKAGE_NAME);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, DISPATCH_PERMISSION);
    }
}
