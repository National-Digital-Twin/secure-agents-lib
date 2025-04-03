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
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestPeriodicAction {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Action.*cannot be null")
    public void periodic_action_bad_01() {
        new PeriodicAction((Callable<Boolean>) null, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Action.*cannot be null")
    public void periodic_action_bad_02() {
        new PeriodicAction((Runnable) null, null);
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Interval.*cannot be null")
    public void periodic_action_bad_03() {
        new PeriodicAction(() -> true, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Minimum configurable interval.*")
    public void periodic_action_bad_04() {
        new PeriodicAction(() -> true, Duration.ofMillis(1));
    }

    @Test
    public void periodic_action_01() {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, PeriodicAction.MINIMUM_INTERVAL);

        // Calling run a bunch of times should only cause the actual action to run once
        for (int i = 0; i < 10_000; i++) {
            action.run();
        }

        verifyCounter(action, counter, 1);
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_02() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, PeriodicAction.MINIMUM_INTERVAL);

        // Calling run a bunch of times should only cause the actual action to run twice
        long start = System.currentTimeMillis();
        while (counter.get() < 2) {
            action.run();
            Thread.sleep(25);
        }
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertTrue(elapsed < 2500, "Approximately 2 seconds should have elapsed");

        verifyCounter(action, counter, 2);
    }

    @Test
    public void periodic_action_03() {
        PeriodicAction action =
                new PeriodicAction(() -> {throw new RuntimeException();}, PeriodicAction.MINIMUM_INTERVAL);

        // Errors from actions are suppressed
        for (int i = 0; i < 10_000; i++) {
            action.run();
        }

        Assert.assertEquals(action.lastRan(), 0);
        Assert.assertEquals(action.successes(), 0);
        Assert.assertEquals(action.attempts(), 10_000);
        Assert.assertEquals(action.errors(), 10_000);
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_04() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Callable<Boolean> callable = () -> {
            int x = counter.getAndIncrement();
            return x % 2 == 0;
        };
        PeriodicAction action = new PeriodicAction(callable, PeriodicAction.MINIMUM_INTERVAL);

        // Calling run a bunch of times should only cause the actual action to run a few times
        long start = System.currentTimeMillis();
        while (action.attempts() < 3) {
            action.run();
            Thread.sleep(25);
        }
        long elapsed = System.currentTimeMillis() - start;
        Assert.assertTrue(elapsed < 2500, "Approximately two seconds should have elapsed");

        Assert.assertEquals(action.attempts(), 3);
        Assert.assertEquals(action.successes(), 2);
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_auto_trigger_01() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, PeriodicAction.MINIMUM_INTERVAL);

        try {
            action.autoTrigger();
            waitForCounter(counter, 1);

            verifyCounter(action, counter, 1);
        } finally {
            action.cancelAutoTrigger();
        }
    }

    /**
     * Verifies that counter has been incremented as expected
     *
     * @param action   Action that increments the counter whose own internal counters will be verified
     * @param counter  Counter that should have been incremented
     * @param expected Expected value of the counters
     */
    private void verifyCounter(PeriodicAction action, AtomicInteger counter, int expected) {
        Assert.assertEquals(counter.get(), expected, "Wrong counter value");
        Assert.assertEquals(action.attempts(), expected, "Wrong attempt count");
        Assert.assertEquals(action.successes(), expected, "Wrong success count");
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_auto_trigger_01b() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, PeriodicAction.MINIMUM_INTERVAL);

        try {
            // Multiple autoTrigger() calls have no effect
            action.autoTrigger();
            action.autoTrigger();
            action.autoTrigger();
            waitForCounter(counter, 1);

            verifyCounter(action, counter, 1);
        } finally {
            action.cancelAutoTrigger();
        }
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_auto_trigger_02() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, PeriodicAction.MINIMUM_INTERVAL);

        try {
            // Wait long enough for the action to auto-trigger multiple times
            action.autoTrigger();
            waitForCounter(counter, 2);

            verifyCounter(action, counter, 2);
        } finally {
            action.cancelAutoTrigger();
        }
    }

    /**
     * Waits until the counter has reached the desired value
     *
     * @param counter      Counter
     * @param desiredValue Desired value
     * @throws InterruptedException if the sleep is somehow interrupted
     */
    private void waitForCounter(AtomicInteger counter, int desiredValue) throws InterruptedException {
        while (counter.get() < desiredValue) {
            Thread.sleep(25);
        }
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_auto_trigger_03() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, PeriodicAction.MINIMUM_INTERVAL);

        try {
            action.autoTrigger();
            waitForCounter(counter, 1);
            verifyCounter(action, counter, 1);

            // Make sure that the action doesn't fire again after we've cancelled it
            // Also that multiple cancelAutoTrigger() calls are safe
            action.cancelAutoTrigger();
            action.cancelAutoTrigger();
            action.cancelAutoTrigger();
            Thread.sleep(1000);
            verifyCounter(action, counter, 1);
        } finally {
            action.cancelAutoTrigger();
        }
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_auto_trigger_04() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, PeriodicAction.MINIMUM_INTERVAL);

        try {
            action.autoTrigger();
            waitForCounter(counter, 1);
            verifyCounter(action, counter, 1);

            // Make sure that the action doesn't fire again after we've cancelled it
            action.cancelAutoTrigger();
            Thread.sleep(1000);
            verifyCounter(action, counter, 1);

            // Note we can still manually run the action
            Thread.sleep(500);
            action.run();
            verifyCounter(action, counter, 2);
        } finally {
            action.cancelAutoTrigger();
        }
    }

    @Test(retryAnalyzer = RetryAnalyzer.class)
    public void periodic_action_auto_trigger_05() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(counter::incrementAndGet, Duration.ofMinutes(15));

        // Intentionally never cancelled so that we maybe test the shutdown hook firing
        // Whether shutdown hooks fire is of course subject to the vagaries of JVM termination, hence why the interval
        // for this action is set high so it's unlikely to ever fire multiple times during the lifetime of the JVM and
        // impact on the performance or behaviour of other tests
        action.autoTrigger();
        waitForCounter(counter, 1);

        verifyCounter(action, counter, 1);
    }
}
