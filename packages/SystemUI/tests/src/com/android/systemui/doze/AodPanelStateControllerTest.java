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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AodPanelStateControllerTest {
    @Test
    public void repeatedStateEmitsOneEnterAndExitPerGeneration() {
        FakeSink sink = new FakeSink();
        AodPanelStateController controller = new AodPanelStateController(sink);

        controller.setAodActive(true);
        controller.setAodActive(true);
        controller.setAodActive(false);
        controller.setAodActive(false);

        assertEquals(List.of("1:ENTER", "1:EXIT"), sink.attempts);
    }

    @Test
    public void refusedEdgeRetriesWithoutInventingAnotherGeneration() {
        FakeSink sink = new FakeSink();
        sink.accept = false;
        AodPanelStateController controller = new AodPanelStateController(sink);

        controller.setAodActive(true);
        sink.accept = true;
        controller.setAodActive(true);

        assertEquals(List.of("1:ENTER", "1:ENTER"), sink.attempts);
    }

    @Test
    public void backendDeathReplaysActiveStateOnNewGeneration() {
        FakeSink first = new FakeSink();
        AodPanelStateController controller = new AodPanelStateController(first);
        controller.setAodActive(true);

        controller.onBackendDied();
        FakeSink replacement = new FakeSink();
        controller.attachBackend(replacement);
        controller.setAodActive(false);

        assertEquals(List.of("1:ENTER"), first.attempts);
        assertEquals(List.of("2:ENTER", "2:EXIT"), replacement.attempts);
    }

    @Test
    public void userChangeClosesOldGenerationBeforeReplayingActiveState() {
        FakeSink sink = new FakeSink();
        AodPanelStateController controller = new AodPanelStateController(sink);
        controller.setAodActive(true);

        controller.onUserChanged();
        controller.setAodActive(false);

        assertEquals(List.of("1:ENTER", "1:EXIT", "2:ENTER", "2:EXIT"), sink.attempts);
    }

    @Test
    public void inactiveBackendAndUserChangesEmitNothing() {
        FakeSink first = new FakeSink();
        AodPanelStateController controller = new AodPanelStateController(first);

        controller.onBackendDied();
        FakeSink replacement = new FakeSink();
        controller.attachBackend(replacement);
        controller.onUserChanged();

        assertEquals(List.of(), first.attempts);
        assertEquals(List.of(), replacement.attempts);
    }

    static final class FakeSink implements AodPanelStateController.Sink {
        final List<String> attempts = new ArrayList<>();
        boolean accept = true;

        @Override
        public boolean send(long generation, AodPanelStateController.Edge edge) {
            attempts.add(generation + ":" + edge);
            return accept;
        }
    }
}
