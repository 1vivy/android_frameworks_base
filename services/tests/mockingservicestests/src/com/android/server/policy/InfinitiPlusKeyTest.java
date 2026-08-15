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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.UserHandle;
import android.view.KeyEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class InfinitiPlusKeyTest {
    private static final long LONG_PRESS_TIMEOUT_MS = 500;

    private RecordingContext mContext;
    private RecordingScheduler mScheduler;
    private AtomicInteger mCurrentUser;
    private AtomicBoolean mKeyguardLocked;
    private InfinitiPlusKey mPlusKey;

    @Before
    public void setUp() {
        mContext =
                new RecordingContext(
                        InstrumentationRegistry.getInstrumentation().getTargetContext());
        mCurrentUser = new AtomicInteger(10);
        mKeyguardLocked = new AtomicBoolean(false);
        mScheduler = new RecordingScheduler();
        mPlusKey =
                new InfinitiPlusKey(
                        mContext,
                        mScheduler,
                        true,
                        LONG_PRESS_TIMEOUT_MS,
                        mCurrentUser::get,
                        mKeyguardLocked::get);
    }

    @Test
    public void shortPressSendsPairedCameraTriggersAndExactlyOneShortPress() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_UP)));

        assertBroadcastContract(
                List.of(
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_SHORT_PRESS));
    }

    @Test
    public void longPressSendsPairedCameraTriggersAndExactlyOneLongPress() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_DOWN)));
        mScheduler.runPendingCallback();
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_UP)));

        assertBroadcastContract(
                List.of(
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_LONG_PRESS,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP));
    }

    @Test
    public void gestureContextIsImmutableAcrossUserAndKeyguardChanges() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(100, KeyEvent.ACTION_DOWN)));
        mCurrentUser.set(11);
        mKeyguardLocked.set(true);
        assertNull(mPlusKey.handleKeyEvent(assistEvent(100, KeyEvent.ACTION_UP)));

        assertBroadcastContext(0, 1, 10, false);
        assertBroadcastContext(1, 1, 10, false);
        assertBroadcastContext(2, 1, 10, false);

        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_UP)));

        assertBroadcastContext(3, 2, 11, true);
        assertBroadcastContext(4, 2, 11, true);
        assertBroadcastContext(5, 2, 11, true);
    }

    @Test
    public void duplicateEventsDispatchExactlyOnce() {
        KeyEvent down = assistEvent(100, KeyEvent.ACTION_DOWN);
        KeyEvent up = assistEvent(100, KeyEvent.ACTION_UP);

        assertNull(mPlusKey.handleKeyEvent(down));
        assertNull(mPlusKey.handleKeyEvent(down));
        assertNull(mPlusKey.handleKeyEvent(up));
        assertNull(mPlusKey.handleKeyEvent(up));

        assertBroadcastContract(
                List.of(
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_SHORT_PRESS));
    }

    @Test
    public void canceledSessionRestoresWithoutDispatchingGesture() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(100, KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(canceledAssistUp(100)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_UP)));

        assertEquals(
                List.of(
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_SHORT_PRESS),
                actions());
        assertBroadcastContext(0, 1, 10, false);
        assertBroadcastContext(1, 1, 10, false);
        assertBroadcastContext(2, 2, 10, false);
        assertBroadcastContext(3, 2, 10, false);
        assertBroadcastContext(4, 2, 10, false);
    }

    @Test
    public void newDownRestoresSessionWhoseUpWasLost() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(100, KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_UP)));

        assertEquals(
                List.of(
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_SHORT_PRESS),
                actions());
        assertBroadcastContext(0, 1, 10, false);
        assertBroadcastContext(1, 1, 10, false);
        assertBroadcastContext(2, 2, 10, false);
        assertBroadcastContext(3, 2, 10, false);
        assertBroadcastContext(4, 2, 10, false);
    }

    @Test
    public void staleTimeoutFromReplacedSessionCannotDispatchForNewSession() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(100, KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_DOWN)));
        mScheduler.runLastRemovedCallback();
        assertNull(mPlusKey.handleKeyEvent(assistEvent(200, KeyEvent.ACTION_UP)));

        assertEquals(
                List.of(
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                        InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                        InfinitiPlusKey.ACTION_SHORT_PRESS),
                actions());
    }

    @Test
    public void otherDevicesDoNotConsumeAssist() {
        InfinitiPlusKey plusKey =
                new InfinitiPlusKey(
                        mContext,
                        mScheduler,
                        false,
                        LONG_PRESS_TIMEOUT_MS,
                        mCurrentUser::get,
                        mKeyguardLocked::get);
        KeyEvent event = assistEvent(KeyEvent.ACTION_DOWN);

        assertSame(event, plusKey.handleKeyEvent(event));
        assertEquals(0, mContext.broadcasts.size());
    }

    private void assertBroadcastContract(List<String> expectedActions) {
        assertEquals(expectedActions, actions());
        for (int i = 0; i < expectedActions.size(); i++) {
            Broadcast broadcast = mContext.broadcasts.get(i);
            assertEquals(InfinitiPlusKey.PACKAGE_NAME, broadcast.intent.getPackage());
            assertEquals(InfinitiPlusKey.DISPATCH_PERMISSION, broadcast.permission);
            assertBroadcastContext(i, 1, 10, false);
        }
        assertEquals(
                expectedActions.contains(InfinitiPlusKey.ACTION_SHORT_PRESS) ? 1 : 0,
                countAction(InfinitiPlusKey.ACTION_SHORT_PRESS));
        assertEquals(
                expectedActions.contains(InfinitiPlusKey.ACTION_LONG_PRESS) ? 1 : 0,
                countAction(InfinitiPlusKey.ACTION_LONG_PRESS));
    }

    private void assertBroadcastContext(
            int index, long sequenceId, int userId, boolean keyguardLocked) {
        Broadcast broadcast = mContext.broadcasts.get(index);
        assertEquals(UserHandle.of(userId), broadcast.user);
        assertEquals(
                sequenceId, broadcast.intent.getLongExtra(InfinitiPlusKey.EXTRA_SEQUENCE_ID, 0));
        assertEquals(
                userId,
                broadcast.intent.getIntExtra(InfinitiPlusKey.EXTRA_USER_ID, UserHandle.USER_NULL));
        if (keyguardLocked) {
            assertTrue(
                    broadcast.intent.getBooleanExtra(InfinitiPlusKey.EXTRA_KEYGUARD_LOCKED, false));
        } else {
            assertFalse(
                    broadcast.intent.getBooleanExtra(InfinitiPlusKey.EXTRA_KEYGUARD_LOCKED, true));
        }
    }

    private List<String> actions() {
        List<String> actions = new ArrayList<>();
        for (Broadcast broadcast : mContext.broadcasts) {
            actions.add(broadcast.intent.getAction());
        }
        return actions;
    }

    private int countAction(String action) {
        int count = 0;
        for (Broadcast broadcast : mContext.broadcasts) {
            if (action.equals(broadcast.intent.getAction())) {
                count++;
            }
        }
        return count;
    }

    private static KeyEvent assistEvent(int action) {
        return assistEvent(0, action);
    }

    private static KeyEvent assistEvent(long downTime, int action) {
        return new KeyEvent(downTime, downTime, action, KeyEvent.KEYCODE_ASSIST, 0);
    }

    private static KeyEvent canceledAssistUp(long downTime) {
        return new KeyEvent(
                downTime,
                downTime,
                KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_ASSIST,
                0,
                0,
                0,
                0,
                KeyEvent.FLAG_CANCELED,
                0);
    }

    private static final class Broadcast {
        final Intent intent;
        final UserHandle user;
        final String permission;

        Broadcast(Intent intent, UserHandle user, String permission) {
            this.intent = new Intent(intent);
            this.user = user;
            this.permission = permission;
        }
    }

    private static final class RecordingScheduler implements InfinitiPlusKey.LongPressScheduler {
        private Runnable mPendingCallback;
        private Runnable mLastRemovedCallback;

        @Override
        public void postDelayed(Runnable runnable, long delayMillis) {
            assertEquals(LONG_PRESS_TIMEOUT_MS, delayMillis);
            mPendingCallback = runnable;
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            if (mPendingCallback == runnable) {
                mLastRemovedCallback = mPendingCallback;
                mPendingCallback = null;
            }
        }

        void runPendingCallback() {
            Runnable callback = mPendingCallback;
            mPendingCallback = null;
            callback.run();
        }

        void runLastRemovedCallback() {
            Runnable callback = mLastRemovedCallback;
            mLastRemovedCallback = null;
            callback.run();
        }
    }

    private static final class RecordingContext extends ContextWrapper {
        final List<Broadcast> broadcasts = new ArrayList<>();

        RecordingContext(Context base) {
            super(base);
        }

        @Override
        public void sendBroadcastAsUser(Intent intent, UserHandle user, String receiverPermission) {
            broadcasts.add(new Broadcast(intent, user, receiverPermission));
        }
    }
}
