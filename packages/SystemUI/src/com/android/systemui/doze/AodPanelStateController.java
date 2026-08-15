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

import java.util.ArrayDeque;
import java.util.Objects;

import javax.inject.Inject;

/**
 * Orders AOD panel-state edges without depending on a device transport.
 *
 * <p>The transport owner maps these typed edges to its private panel contract. SystemUI owns only
 * the doze lifecycle and never carries a panel feature ID.
 */
public final class AodPanelStateController {
    /** A lifecycle edge delivered to the device transport. */
    public enum Edge {
        ENTER,
        EXIT
    }

    /** Maintained framework/service boundary implemented by the device transport owner. */
    public interface Sink {
        /** Returns true only when the edge was accepted for this generation. */
        boolean send(long generation, Edge edge);
    }

    private final ArrayDeque<Emission> mPendingEdges = new ArrayDeque<>();
    private Sink mSink;
    private boolean mAodActive;
    private long mGeneration;

    @Inject
    public AodPanelStateController() {}

    AodPanelStateController(Sink sink) {
        mSink = Objects.requireNonNull(sink);
    }

    /** Updates the desired AOD state. Repeated state notifications are idempotent. */
    public void setAodActive(boolean active) {
        if (active != mAodActive) {
            mAodActive = active;
            if (active) {
                mGeneration++;
                mPendingEdges.addLast(new Emission(mGeneration, Edge.ENTER));
            } else {
                mPendingEdges.addLast(new Emission(mGeneration, Edge.EXIT));
            }
        }
        flush();
    }

    /** Cancels queued work for a dead backend; active state is replayed after reconnect. */
    public void onBackendDied() {
        mSink = null;
        mPendingEdges.clear();
    }

    /** Attaches a new backend generation and replays active state exactly once. */
    public void attachBackend(Sink sink) {
        mSink = Objects.requireNonNull(sink);
        mPendingEdges.clear();
        if (mAodActive) {
            mGeneration++;
            mPendingEdges.addLast(new Emission(mGeneration, Edge.ENTER));
        }
        flush();
    }

    /** Closes the old user's generation before replaying active state for the new user. */
    public void onUserChanged() {
        if (!mAodActive) {
            mPendingEdges.clear();
            return;
        }
        mPendingEdges.addLast(new Emission(mGeneration, Edge.EXIT));
        mGeneration++;
        mPendingEdges.addLast(new Emission(mGeneration, Edge.ENTER));
        flush();
    }

    private void flush() {
        while (mSink != null && !mPendingEdges.isEmpty()) {
            Emission edge = mPendingEdges.getFirst();
            if (!mSink.send(edge.generation, edge.edge)) {
                return;
            }
            mPendingEdges.removeFirst();
        }
    }

    private static final class Emission {
        final long generation;
        final Edge edge;

        Emission(long generation, Edge edge) {
            this.generation = generation;
            this.edge = edge;
        }
    }
}
