/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.systemui.doze;

import android.annotation.Nullable;
import android.os.IBinder;
import android.os.IServiceCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;

import vendor.oplus.hardware.aodpanelstate.AodPanelEdge;
import vendor.oplus.hardware.aodpanelstate.IAodPanelStateService;

import java.util.concurrent.Executor;

import javax.inject.Inject;

/** Death-aware Binder transport for {@link AodPanelStateController}. */
@SysUISingleton
public final class AodPanelStateServiceSink implements AodPanelStateController.Sink {
    private static final String TAG = "AodPanelStateSink";
    private static final String SERVICE_NAME = IAodPanelStateService.DESCRIPTOR + "/default";

    private final Object mLock = new Object();
    private final Executor mMainExecutor;
    private final IBinder.DeathRecipient mDeathRecipient = this::onServiceDied;
    private final IServiceCallback mRegistrationCallback =
            new IServiceCallback.Stub() {
                @Override
                public void onRegistration(String name, IBinder binder) {
                    onServiceRegistered(binder);
                }
            };

    @Nullable private IAodPanelStateService mService;
    @Nullable private IBinder mBinder;
    @Nullable private AodPanelStateController mController;

    @Inject
    public AodPanelStateServiceSink(@Main Executor mainExecutor) {
        mMainExecutor = mainExecutor;
        try {
            ServiceManager.registerForNotifications(SERVICE_NAME, mRegistrationCallback);
        } catch (RemoteException e) {
            throw new IllegalStateException("servicemanager registration failed", e);
        }
    }

    void attachController(AodPanelStateController controller) {
        boolean connected;
        synchronized (mLock) {
            mController = controller;
            connected = mService != null;
        }
        if (connected) {
            controller.attachBackend(this);
        }
    }

    @Override
    public boolean send(long generation, AodPanelStateController.Edge edge) {
        IAodPanelStateService service;
        synchronized (mLock) {
            service = mService;
        }
        if (service == null) return false;
        try {
            byte binderEdge =
                    edge == AodPanelStateController.Edge.ENTER
                            ? AodPanelEdge.ENTER
                            : AodPanelEdge.EXIT;
            return service.send(generation, binderEdge);
        } catch (RemoteException e) {
            onServiceDied();
            return false;
        }
    }

    private void onServiceRegistered(IBinder binder) {
        AodPanelStateController controller;
        synchronized (mLock) {
            if (mBinder == binder) return;
            if (mBinder != null) {
                mBinder.unlinkToDeath(mDeathRecipient, 0);
            }
            try {
                binder.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                Log.w(TAG, "AOD panel-state service died during registration", e);
                return;
            }
            mBinder = binder;
            mService = IAodPanelStateService.Stub.asInterface(binder);
            controller = mController;
        }
        if (controller != null) {
            mMainExecutor.execute(() -> controller.attachBackend(this));
        }
    }

    private void onServiceDied() {
        AodPanelStateController controller;
        synchronized (mLock) {
            mBinder = null;
            mService = null;
            controller = mController;
        }
        if (controller != null) {
            mMainExecutor.execute(controller::onBackendDied);
        }
    }
}
