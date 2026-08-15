/*
 * Copyright (C) 2025-2026 AxionOS Project
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
package com.android.server.wm;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.app.IGameSpaceCallback;

import lineageos.health.HealthInterface;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

class GameStateDispatcher {

    private static final String TAG = "GameStateDispatcher";
    private static final String KEY_GAMING_MODE_ACTIVE = "ax_gaming_mode_active";
    private static final String KEY_CHARGE_HOLD_ENABLED = "charge_hold_enabled";
    private static final String KEY_POWER_MODE_PERF = "persist.sys.power_mode_perf";
    private static final String KEY_POWER_MODE_PERF_BY_USER = "persist.sys.power_mode_perf_by_user";

    private final Context mContext;
    private final List<IGameSpaceCallback> mCallbacks;
    private final ChargeHoldSession mChargeHoldSession;

    private boolean mGameActive;

    GameStateDispatcher(Context context, List<IGameSpaceCallback> callbacks) {
        mContext = context;
        mCallbacks = callbacks;
        HealthInterface health = HealthInterface.getInstance(context);
        mChargeHoldSession =
                new ChargeHoldSession(
                        new ChargeHoldPolicy() {
                            @Override
                            public boolean acquire(IBinder token, boolean active) {
                                return health.acquireChargeHoldSession(token, active);
                            }

                            @Override
                            public boolean update(IBinder token, boolean active) {
                                return health.updateChargeHoldSession(token, active);
                            }

                            @Override
                            public boolean release(IBinder token) {
                                return health.releaseChargeHoldSession(token);
                            }
                        },
                        Binder::new);

        ContentObserver chargeHoldObserver =
                new ContentObserver(new Handler(context.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        updateChargeHoldSession();
                    }
                };
        context.getContentResolver()
                .registerContentObserver(
                        Settings.System.getUriFor(KEY_CHARGE_HOLD_ENABLED),
                        false,
                        chargeHoldObserver,
                        UserHandle.USER_ALL);
    }

    void dispatchGameState(boolean active, String packageName) {
        Settings.Secure.putIntForUser(
                mContext.getContentResolver(),
                KEY_GAMING_MODE_ACTIVE,
                active ? 1 : 0,
                UserHandle.USER_CURRENT);

        for (IGameSpaceCallback callback : mCallbacks) {
            try {
                if (active && packageName != null) {
                    callback.onGameStart(packageName);
                } else {
                    callback.onGameLeave();
                }
            } catch (Exception e) {
                Slog.w(TAG, "Removing dead callback", e);
                mCallbacks.remove(callback);
            }
        }

        synchronized (this) {
            mGameActive = active;
            if (active) {
                if (!mChargeHoldSession.update(chargeHoldEnabled())) {
                    Slog.w(TAG, "Unable to acquire game charge-hold session");
                }
            } else {
                mChargeHoldSession.finish();
            }
        }
    }

    void boostGame(boolean enable) {
        int perfByUser =
                Settings.System.getIntForUser(
                        mContext.getContentResolver(),
                        KEY_POWER_MODE_PERF_BY_USER,
                        0,
                        UserHandle.USER_CURRENT);
        if (perfByUser == 1) return;

        Settings.System.putIntForUser(
                mContext.getContentResolver(),
                KEY_POWER_MODE_PERF,
                enable ? 1 : 0,
                UserHandle.USER_CURRENT);
        SystemProperties.set(KEY_POWER_MODE_PERF, enable ? "1" : "0");
    }

    private synchronized void updateChargeHoldSession() {
        if (mGameActive && !mChargeHoldSession.update(chargeHoldEnabled())) {
            Slog.w(TAG, "Unable to update game charge-hold session");
        }
    }

    private boolean chargeHoldEnabled() {
        return Settings.System.getIntForUser(
                        mContext.getContentResolver(),
                        KEY_CHARGE_HOLD_ENABLED,
                        0,
                        UserHandle.USER_CURRENT)
                == 1;
    }

    interface ChargeHoldPolicy {
        boolean acquire(IBinder token, boolean active);

        boolean update(IBinder token, boolean active);

        boolean release(IBinder token);
    }

    static final class ChargeHoldSession {
        private final ChargeHoldPolicy mPolicy;
        private final Supplier<IBinder> mTokenFactory;

        private IBinder mToken;
        private boolean mAcquired;

        ChargeHoldSession(ChargeHoldPolicy policy, Supplier<IBinder> tokenFactory) {
            mPolicy = Objects.requireNonNull(policy);
            mTokenFactory = Objects.requireNonNull(tokenFactory);
        }

        synchronized boolean update(boolean active) {
            if (mToken == null) {
                mToken = Objects.requireNonNull(mTokenFactory.get());
            }
            if (!mAcquired) {
                mAcquired = mPolicy.acquire(mToken, active);
            } else if (!mPolicy.update(mToken, active)) {
                mAcquired = mPolicy.acquire(mToken, active);
            }
            return mAcquired;
        }

        synchronized void finish() {
            if (mToken == null) {
                return;
            }
            mPolicy.release(mToken);
            mToken = null;
            mAcquired = false;
        }
    }
}
