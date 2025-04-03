// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.

/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */
package uk.gov.dbt.ndtp.secure.agent.projectors.utils;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Represents an action that you only want to run at most once within any given interval.
 * <p>
 * For example you might want to log a warning when performance drops below a certain threshold <strong>BUT</strong>
 * don't want to flood the logs with this warning and instead emit it only periodically.  Similarly you might want to
 * periodically compute some expensive statistic.  Using this class allows callers to not worry about controlling when
 * the action will run and just focus on the action itself.
 * </p>
 * <p>
 * This is effectively an implementation of a <a href="https://en.wikipedia.org/wiki/Leaky_bucket">leaky bucket rate
 * limiter</a> using the "as a meter" semantics i.e. if the action has already occurred during the period then it is
 * simply skipped.
 * </p>
 */
public class PeriodicAction {

    /**
     * The minimum configurable interval
     **/
    public static final Duration MINIMUM_INTERVAL = Duration.ofSeconds(1);

    private final Callable<Boolean> action;
    private final Duration interval;
    private long lastRan = 0, attempts = 0, successes = 0, errors = 0;
    private ExecutorService executor;
    private Future<?> autoTrigger;
    private volatile boolean runAutoTrigger = true;

    private final Object lock = new Object();

    /**
     * Creates a new periodic action
     *
     * @param action   Action, this is callable that returns a boolean indicating whether it actually performed its
     *                 action.  This is useful in that an action might need to make some calculation to decide whether
     *                 it should actually perform itself.
     * @param interval Interval, the action will be permitted to run at most once within the interval
     * @throws NullPointerException     Thrown if the action/interval is null
     * @throws IllegalArgumentException Thrown if the interval is too short (less than {@link #MINIMUM_INTERVAL} which
     *                                  is 1 second)
     */
    public PeriodicAction(Callable<Boolean> action, Duration interval) {
        Objects.requireNonNull(action, "Action cannot be null");
        Objects.requireNonNull(interval, "Interval cannot be null");
        if (interval.compareTo(MINIMUM_INTERVAL) < 0) {
            throw new IllegalArgumentException(
                    String.format("Minimum configurable interval is %s", MINIMUM_INTERVAL));
        }
        this.action = action;
        this.interval = interval;
    }

    /**
     * Creates a new periodic action
     *
     * @param action   Action
     * @param interval Interval, the action will be permitted to run at most once within the interval
     * @throws NullPointerException     Thrown if the action/interval is null
     * @throws IllegalArgumentException Thrown if the interval is too short (less than {@link #MINIMUM_INTERVAL} which *
     *                                  is 1 second)
     */
    public PeriodicAction(Runnable action, Duration interval) {
        this(wrapRunnable(action), interval);
    }

    /**
     * Wraps a runnable into a callable with the assumption that the runnable always performs the actions whose rate it
     * being limited
     *
     * @param action Action
     * @return Callable
     */
    private static Callable<Boolean> wrapRunnable(Runnable action) {
        Objects.requireNonNull(action, "Action cannot be null");
        return () -> {
            action.run();
            return true;
        };
    }

    /**
     * Runs the periodic action, if the action has already been run within the configured interval then this will be a
     * no-op
     */
    public void run() {
        Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - this.lastRan);
        if (elapsed.compareTo(this.interval) >= 1) {
            try {
                this.attempts++;
                Boolean wasPerformed = this.action.call();
                if (Boolean.TRUE.equals(wasPerformed)) {
                    this.successes++;
                    this.lastRan = System.currentTimeMillis();
                }
            } catch (Exception e) {
                // Ignored
                this.errors++;
            }
        }
    }

    /**
     * Reports when the action last successfully ran
     *
     * @return Last ran time as milliseconds since the epoch, or {@code 0} if the action has never actually run
     */
    public long lastRan() {
        return this.lastRan;
    }

    /**
     * Reports the number of attempts to run the action that have been made
     *
     * @return Attempts
     */
    public long attempts() {
        return this.attempts;
    }

    /**
     * Reports the number of successful runs of the action
     *
     * @return Successes
     */
    public long successes() {
        return this.successes;
    }

    /**
     * Reports the number of errors
     *
     * @return Errors
     */
    public long errors() {
        return this.errors;
    }

    /**
     * Sets up the periodic action to automatically trigger on a background daemon thread at the interval configured for
     * the action.
     * <p>
     * If auto-trigger is already enabled then calling this has no effect.
     * </p>
     */
    public void autoTrigger() {
        synchronized (this.lock) {
            if (this.autoTrigger != null) {
                return;
            }

            this.executor = Executors.newSingleThreadExecutor();
            this.runAutoTrigger = true;
            this.autoTrigger = this.executor.submit(() -> {
                while (this.runAutoTrigger) {
                    this.run();
                    try {
                        Thread.sleep(this.interval.toMillis());
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            });
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                this.runAutoTrigger = false;
                if (this.autoTrigger != null) {
                    this.autoTrigger.cancel(true);
                }
                if (this.executor != null) {
                    this.executor.shutdownNow();
                }
            }));
        }
    }

    /**
     * Cancels an existing auto-trigger for this action (if any).
     * <p>
     * If no auto-trigger for this action currently exists then this has no effect
     * </p>
     */
    public void cancelAutoTrigger() {
        synchronized (this.lock) {
            if (this.autoTrigger == null) {
                return;
            }

            this.runAutoTrigger = false;
            this.autoTrigger.cancel(true);
            this.autoTrigger = null;
            this.executor.shutdownNow();
            this.executor = null;
        }
    }
}
