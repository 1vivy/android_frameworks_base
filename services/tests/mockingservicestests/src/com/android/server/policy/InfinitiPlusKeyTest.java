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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.testing.TestableLooper;
import android.view.KeyEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InfinitiPlusKeyTest {
    private static final long LONG_PRESS_TIMEOUT_MS = 500;

    private RecordingContext mContext;
    private TestableLooper mTestLooper;
    private InfinitiPlusKey mPlusKey;

    @Before
    public void setUp() throws Exception {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mTestLooper = new TestableLooper(Looper.myLooper());
        mContext = new RecordingContext(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        mPlusKey = new InfinitiPlusKey(mContext, new Handler(Looper.myLooper()),
                true, LONG_PRESS_TIMEOUT_MS);
    }

    @After
    public void tearDown() throws Exception {
        mTestLooper.destroy();
    }

    @Test
    public void shortPressSendsPairedCameraTriggersAndExactlyOneShortPress() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_DOWN)));
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_UP)));
        mTestLooper.moveTimeForward(LONG_PRESS_TIMEOUT_MS);
        mTestLooper.processAllMessages();

        assertBroadcastContract(List.of(
                InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP,
                InfinitiPlusKey.ACTION_SHORT_PRESS));
    }

    @Test
    public void longPressSendsPairedCameraTriggersAndExactlyOneLongPress() {
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_DOWN)));
        mTestLooper.moveTimeForward(LONG_PRESS_TIMEOUT_MS);
        mTestLooper.processAllMessages();
        assertNull(mPlusKey.handleKeyEvent(assistEvent(KeyEvent.ACTION_UP)));

        assertBroadcastContract(List.of(
                InfinitiPlusKey.ACTION_CAMERA_TRIGGER_DOWN,
                InfinitiPlusKey.ACTION_LONG_PRESS,
                InfinitiPlusKey.ACTION_CAMERA_TRIGGER_UP));
    }

    @Test
    public void otherDevicesDoNotConsumeAssist() {
        InfinitiPlusKey plusKey = new InfinitiPlusKey(mContext, new Handler(Looper.myLooper()),
                false, LONG_PRESS_TIMEOUT_MS);
        KeyEvent event = assistEvent(KeyEvent.ACTION_DOWN);

        assertSame(event, plusKey.handleKeyEvent(event));
        assertEquals(0, mContext.broadcasts.size());
    }

    private void assertBroadcastContract(List<String> expectedActions) {
        assertEquals(expectedActions.size(), mContext.broadcasts.size());
        for (int i = 0; i < expectedActions.size(); i++) {
            Broadcast broadcast = mContext.broadcasts.get(i);
            assertEquals(expectedActions.get(i), broadcast.intent.getAction());
            assertEquals(InfinitiPlusKey.PACKAGE_NAME, broadcast.intent.getPackage());
            assertSame(UserHandle.CURRENT, broadcast.user);
            assertEquals(InfinitiPlusKey.DISPATCH_PERMISSION, broadcast.permission);
        }
        assertEquals(expectedActions.contains(InfinitiPlusKey.ACTION_SHORT_PRESS) ? 1 : 0,
                countAction(InfinitiPlusKey.ACTION_SHORT_PRESS));
        assertEquals(expectedActions.contains(InfinitiPlusKey.ACTION_LONG_PRESS) ? 1 : 0,
                countAction(InfinitiPlusKey.ACTION_LONG_PRESS));
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
        return new KeyEvent(0, 0, action, KeyEvent.KEYCODE_ASSIST, 0);
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

    private static final class RecordingContext extends ContextWrapper {
        final List<Broadcast> broadcasts = new ArrayList<>();

        RecordingContext(Context base) {
            super(base);
        }

        @Override
        public void sendBroadcastAsUser(Intent intent, UserHandle user,
                String receiverPermission) {
            broadcasts.add(new Broadcast(intent, user, receiverPermission));
        }
    }
}
