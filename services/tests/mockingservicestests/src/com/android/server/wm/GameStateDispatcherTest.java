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
package com.android.server.wm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import android.os.Binder;
import android.os.IBinder;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(JUnit4.class)
public class GameStateDispatcherTest {

    @Test
    public void chargeHoldSession_usesStableTokenUntilRelease() {
        RecordingPolicy policy = new RecordingPolicy();
        GameStateDispatcher.ChargeHoldSession session =
                new GameStateDispatcher.ChargeHoldSession(policy, Binder::new);

        session.update(true);
        IBinder token = policy.mTokens.get(0);
        session.update(false);
        session.finish();
        session.finish();

        assertEquals(List.of("acquire:true", "update:false", "release"), policy.mEvents);
        assertSame(token, policy.mTokens.get(1));
        assertSame(token, policy.mTokens.get(2));
    }

    @Test
    public void chargeHoldSession_failedAcquireRetriesWithSameToken() {
        RecordingPolicy policy = new RecordingPolicy();
        policy.mAcquireResult = false;
        GameStateDispatcher.ChargeHoldSession session =
                new GameStateDispatcher.ChargeHoldSession(policy, Binder::new);

        session.update(true);
        IBinder token = policy.mTokens.get(0);
        policy.mAcquireResult = true;
        session.update(true);
        session.finish();

        assertEquals(List.of("acquire:true", "acquire:true", "release"), policy.mEvents);
        assertSame(token, policy.mTokens.get(1));
        assertSame(token, policy.mTokens.get(2));
    }

    @Test
    public void chargeHoldSession_failedAcquireStillReleasesToken() {
        RecordingPolicy policy = new RecordingPolicy();
        policy.mAcquireResult = false;
        GameStateDispatcher.ChargeHoldSession session =
                new GameStateDispatcher.ChargeHoldSession(policy, Binder::new);

        session.update(true);
        IBinder token = policy.mTokens.get(0);
        session.finish();

        assertEquals(List.of("acquire:true", "release"), policy.mEvents);
        assertSame(token, policy.mTokens.get(1));
    }

    @Test
    public void chargeHoldSession_failedUpdateReacquiresWithSameToken() {
        RecordingPolicy policy = new RecordingPolicy();
        GameStateDispatcher.ChargeHoldSession session =
                new GameStateDispatcher.ChargeHoldSession(policy, Binder::new);

        session.update(true);
        IBinder token = policy.mTokens.get(0);
        policy.mUpdateResult = false;
        session.update(false);

        assertEquals(List.of("acquire:true", "update:false", "acquire:false"), policy.mEvents);
        assertSame(token, policy.mTokens.get(1));
        assertSame(token, policy.mTokens.get(2));
    }

    @Test
    public void chargeHoldSession_nextGameGetsNewToken() {
        RecordingPolicy policy = new RecordingPolicy();
        GameStateDispatcher.ChargeHoldSession session =
                new GameStateDispatcher.ChargeHoldSession(policy, Binder::new);

        session.update(true);
        IBinder firstToken = policy.mTokens.get(0);
        session.finish();
        session.update(true);
        IBinder secondToken = policy.mTokens.get(2);

        assertNotSame(firstToken, secondToken);
    }

    private static final class RecordingPolicy implements GameStateDispatcher.ChargeHoldPolicy {
        final List<String> mEvents = new ArrayList<>();
        final List<IBinder> mTokens = new ArrayList<>();
        boolean mAcquireResult = true;
        boolean mUpdateResult = true;

        @Override
        public boolean acquire(IBinder token, boolean active) {
            mEvents.add("acquire:" + active);
            mTokens.add(token);
            return mAcquireResult;
        }

        @Override
        public boolean update(IBinder token, boolean active) {
            mEvents.add("update:" + active);
            mTokens.add(token);
            return mUpdateResult;
        }

        @Override
        public boolean release(IBinder token) {
            mEvents.add("release");
            mTokens.add(token);
            return true;
        }
    }
}
