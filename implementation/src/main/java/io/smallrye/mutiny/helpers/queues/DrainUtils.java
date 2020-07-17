/*
 * Copyright (c) 2019-2020 Red Hat
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.mutiny.helpers.queues;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.helpers.Subscriptions;

/**
 * Copy from Project Reactor.
 */
public class DrainUtils {

    /**
     * Indicates the source completed and the value field is ready to be emitted.
     * <p>
     * The AtomicLong (this) holds the requested amount in bits 0..62 so there is room
     * for one signal bit. This also means the standard request accounting helper method doesn't work.
     */
    private static final long COMPLETED_MASK = 0x8000_0000_0000_0000L;
    private static final long REQUESTED_MASK = 0x7FFF_FFFF_FFFF_FFFFL;

    private DrainUtils() {
        // avoid direct instantiation.
    }

    /**
     * Perform a potential post-completion request accounting.
     *
     * @param <T> the output value type
     * @param n the requested amount
     * @param downstream the downstream consumer
     * @param queue the queue holding the available values
     * @param requested the requested atomic long
     * @param isCancelled callback to detect cancellation
     * @return true if the state indicates a completion state.
     */
    public static <T> boolean postCompleteRequest(long n,
            Subscriber<? super T> downstream,
            Queue<T> queue,
            AtomicLong requested,
            BooleanSupplier isCancelled) {

        for (;;) {
            long r = requested.get();

            // extract the current request amount
            long r0 = r & REQUESTED_MASK;

            // preserve COMPLETED_MASK and calculate new requested amount
            long u = (r & COMPLETED_MASK) | Subscriptions.add(r0, n);

            if (requested.compareAndSet(r, u)) {
                // (complete, 0) -> (complete, n) transition then replay
                if (r == COMPLETED_MASK) {

                    postCompleteDrain(n | COMPLETED_MASK, downstream, queue, requested, isCancelled);

                    return true;
                }
                // (active, r) -> (active, r + n) transition then continue with requesting from upstream
                return false;
            }
        }

    }

    /**
     * Drains the queue either in a pre- or post-complete state.
     *
     * @param n the requested amount
     * @param downstream the downstream consumer
     * @param queue the queue holding available values
     * @param requested the atomic long keeping track of requests
     * @param isCancelled callback to detect cancellation
     * @return true if the queue was completely drained or the drain process was cancelled
     */
    private static <T> boolean postCompleteDrain(long n,
            Subscriber<? super T> downstream,
            Queue<T> queue,
            AtomicLong requested,
            BooleanSupplier isCancelled) {

        long e = n & COMPLETED_MASK;

        for (;;) {

            while (e != n) {
                if (isCancelled.getAsBoolean()) {
                    return true;
                }

                T t = queue.poll();

                if (t == null) {
                    downstream.onComplete();
                    return true;
                }

                downstream.onNext(t);
                e++;
            }

            if (isCancelled.getAsBoolean()) {
                return true;
            }

            if (queue.isEmpty()) {
                downstream.onComplete();
                return true;
            }

            n = requested.get();

            if (n == e) {

                n = requested.addAndGet(-(e & REQUESTED_MASK));

                if ((n & REQUESTED_MASK) == 0L) {
                    return false;
                }

                e = n & COMPLETED_MASK;
            }
        }

    }

    /**
     * Tries draining the queue if the source just completed.
     *
     * @param <T> the output value type
     * @param downstream the downstream consumer
     * @param queue the queue holding available values
     * @param requested the atomic long keeping track of requests
     * @param isCancelled callback to detect cancellation
     */
    public static <T> void postComplete(Subscriber<? super T> downstream,
            Queue<T> queue,
            AtomicLong requested,
            BooleanSupplier isCancelled) {

        if (queue.isEmpty()) {
            downstream.onComplete();
            return;
        }

        if (postCompleteDrain(requested.get(), downstream, queue, requested, isCancelled)) {
            return;
        }

        for (;;) {
            long r = requested.get();

            if ((r & COMPLETED_MASK) != 0L) {
                return;
            }

            long u = r | COMPLETED_MASK;
            // (active, r) -> (complete, r) transition
            if (requested.compareAndSet(r, u)) {
                // if the requested amount was non-zero, drain the queue
                if (r != 0L) {
                    postCompleteDrain(u, downstream, queue, requested, isCancelled);
                }

                return;
            }
        }
    }
}
