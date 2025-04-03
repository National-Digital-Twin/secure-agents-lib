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
package uk.gov.dbt.ndtp.secure.agent.sources.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.sources.EventSourceException;

/**
 * A helper utility that abstracts the checking of topic existence (and caching the results of those checks).  The
 * existence checks run on background threads using an {@link AdminClient}.  Once a topic is determined to exist then
 * that result is cached for the lifetime of this instance. This allows the instance to quickly answer whether any of
 * its configured topics exist, while also continuing to check and see if those known to be non-existent are created
 * later.
 * <p>
 * Primarily this is used inside {@link KafkaEventSource} as a sanity check to avoid unnecessarily polling the broker if
 * none of the topics exist.  This is done because when no topics exist Kafka's logging is very noisy and can
 * effectively flood the logs making seeing anything useful in them difficult.
 * </p>
 */
public class TopicExistenceChecker {
    private final AdminClient adminClient;
    private final Logger logger;
    private final String server;
    private final Set<String> topics;
    private final Map<String, Boolean> topicExists = new ConcurrentHashMap<>();
    private final ExecutorService service;
    private final Map<String, Future<Boolean>> inFlightChecks = new HashMap<>();
    private boolean closed = false;

    /**
     * Creates a new topic existence checker
     *
     * @param adminClient Kafka Admin Client, if none supplied then we won't be able to check topic existence but in
     *                    some scenarios (mainly testing) we know this won't be supplied so that's considered
     *                    acceptable
     * @param server      The Kafka bootstrap server(s)
     * @param topics      Topic(s) to check
     * @param logger      Logger to write any log messages to, if not supplied then a logger is created using this class
     *                    as its name
     */
    public TopicExistenceChecker(AdminClient adminClient, String server, Collection<String> topics, Logger logger) {
        this.adminClient = adminClient;
        this.logger = logger != null ? logger : LoggerFactory.getLogger(TopicExistenceChecker.class);
        Objects.requireNonNull(topics, "Topics to check cannot be null");
        if (StringUtils.isBlank(server)) {
            throw new IllegalArgumentException("server cannot be null/blank");
        }
        this.server = server;
        this.topics = new LinkedHashSet<>(topics);
        this.service = Executors.newFixedThreadPool(this.topics.size());
    }

    /**
     * Do all the configured topics exist?
     *
     * @param timeout Timeout
     * @return True if all topics exist, false otherwise
     */
    public final boolean allTopicsExist(Duration timeout) {
        boolean any = anyTopicExists(timeout);
        if (!any) {
            return false;
        }

        return allTopicsKnownToExist();
    }

    /**
     * Do any of the configured topics exist?
     * <p>
     * If this is the first time this method has been called on this instance then it will wait up to the supplied
     * timeout trying to determine if at least one of the configured topics exists.  As soon as one topic is determined
     * to exist then it will return {@code true}.  Only if none of the topics are found to exist will it return
     * {@code false}.
     * </p>
     * <p>
     * However, on any subsequent run if at least one topic is known to exist then it will return {@code true} ASAP.
     * </p>
     *
     * @param timeout Timeout
     * @return True if any of the configured topics exist
     */
    public final boolean anyTopicExists(Duration timeout) {
        if (this.closed) {
            return anyTopicKnownToExist();
        }
        resolveInFlightChecks();
        launchChecks(timeout);
        if (anyTopicKnownToExist()) {
            return true;
        }
        return waitForChecks(timeout);
    }

    /**
     * Quickly checks whether any of our configured topics are already known to exist based on our cached results
     *
     * @return True if known to exist, false otherwise
     */
    private boolean anyTopicKnownToExist() {
        return this.topicExists.values().stream().anyMatch(exists -> exists);
    }

    /**
     * Quickly checks whether all of our configured topics are already known to exist based on our cached results
     *
     * @return True if all known to exist, false otherwise
     */
    private boolean allTopicsKnownToExist() {
        return this.topicExists.size() == this.topics.size() && this.topicExists.values()
                                                                                .stream()
                                                                                .allMatch(exists -> exists);
    }

    /**
     * Resolves any in-flight checks and removes them from the in-flight registry
     * <p>
     * <strong>NB:</strong> We don't update our {@link #topicExists} map here because the checks do this directly, see
     * the {@link #doesTopicExist(String, Duration)} method.
     * </p>
     */
    protected final void resolveInFlightChecks() {
        List<Map.Entry<String, Future<Boolean>>> inFlight = new ArrayList<>(this.inFlightChecks.entrySet());
        for (Map.Entry<String, Future<Boolean>> check : inFlight) {
            if (check.getValue().isDone()) {
                this.inFlightChecks.remove(check.getKey());
            }
        }
    }

    /**
     * Waits for any in-flight topic existence checks to complete, returning {@code true} as soon as any topic is known
     * to exist or {@code false} as soon as all checks complete unsuccessfully.
     *
     * @param timeout Timeout
     * @return True if any topic is known to exist, false otherwise
     */
    protected final boolean waitForChecks(Duration timeout) {
        long maxWaitMs = timeout.toMillis();
        long start = System.currentTimeMillis();
        while (true) {
            if (hasTimeoutElapsed(start, maxWaitMs) || this.inFlightChecks.isEmpty()) {
                return anyTopicKnownToExist();
            }

            boolean foundInflightCheck = processInFlightChecks();
            if(foundInflightCheck){
                return true;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    private boolean hasTimeoutElapsed(long start, long maxWaitMs) {
        long elapsed = System.currentTimeMillis() - start;
        return elapsed >= maxWaitMs;
    }

    private boolean processInFlightChecks() {
        List<Map.Entry<String, Future<Boolean>>> checks = new ArrayList<>(this.inFlightChecks.entrySet());
        for (Map.Entry<String, Future<Boolean>> check : checks) {
            if (check.getValue().isDone()) {
                this.inFlightChecks.remove(check.getKey());
                try {
                    if (check.getValue().get()) {
                        // A check succeeded, so we know at least one topic exists, outstanding checks will be
                        // resolved later
                        return true;
                    }
                } catch (Throwable e) {
                    // This check failed in some way, ignore it, we'll re-issue it next time around
                }
            }
        }
        return false;
    }

    /**
     * Launches the topic existence checks
     * <p>
     * Checks are only launched for topics that are not yet known to exist.  If a topic is already known to exist we
     * don't launch any further checks for it.
     * </p>
     *
     * @param timeout Timeout
     */
    protected final void launchChecks(Duration timeout) {
        for (String topic : this.topics) {
            // Only launch if we've not yet checked for this topic, or we've checked but it was previously known to not
            // exist
            if (!this.topicExists.containsKey(topic) || (this.topicExists.containsKey(topic) && !this.topicExists.get(
                    topic))) {
                if (this.inFlightChecks.containsKey(topic)) {
                    // Possibly could already be an in-flight check for this topic.  This is most likely to occur if
                    // we get asked to make a check with a long timeout and then later with a short timeout before the
                    // original timeout has expired. Regardless of how it happens we don't need to launch another one,
                    // rather we'll wait for the existing one to complete.  As realistically if a check is still
                    // in-flight it means that topic really doesn't exist at the moment and starting a new shorter time
                    // out check in the meantime isn't going to change that.
                    continue;
                }
                this.inFlightChecks.put(topic, this.service.submit(() -> doesTopicExist(topic, timeout)));
            }
        }
    }

    /**
     * The actual check whether a given topic actually exists within the Kafka cluster.  This gets run in the background
     * via our executor {@link #service} and are kicked off by {@link #launchChecks(Duration)}.
     * <p>
     * A positive result is cached, and once that is {@code true} calling this method for that topic is effectively a
     * no-op and does not involve any further communication with Kafka.  We also won't bother launching future checks
     * for topics we know to exist.
     * </p>
     * <p>
     * This method blocks up to the given timeout if the topic does not exist to wait to see if something outside our
     * control is going to create the topic.  Each time it gets a negative result from the Kafka cluster it sleeps a
     * brief amount of time and then asks the cluster again.  As soon as it gets a positive result it will return that.
     * </p>
     * <p>
     * The only exception to this behaviour is that the check will fail fast if it receives a security error from Kafka
     * indicating we aren't able to make that check, or are talking to a secured Kafka cluster without sufficient
     * credentials.
     * </p>
     * <p>
     * The check will actively update the shared {@link #topicExists} cache with the results of the checks as they
     * proceed.
     * </p>
     *
     * @param topic   Specific topic to check
     * @param timeout Timeout for the check
     * @return True if the topic exists, false otherwise
     */
    protected final boolean doesTopicExist(String topic, Duration timeout) {
        if (isTopicKnownToExist(topic)) {
            return true;
        }

        boolean firstCheck = !this.topicExists.containsKey(topic);

        if (this.adminClient == null) {
            handleNoAdminClient(firstCheck, topic);
            return true;
        }

        long remainingTimeout = timeout.toMillis();
        while (remainingTimeout > 0) {
            long start = System.currentTimeMillis();
            if (checkTopicExistence(topic, remainingTimeout)) {
                break;
            }
            remainingTimeout = updateRemainingTimeout(remainingTimeout, start);
        }

        logIfFirstCheckFailed(firstCheck, topic);

        return isTopicKnownToExist(topic);
    }

    private boolean isTopicKnownToExist(String topic) {
        return this.topicExists.containsKey(topic) && this.topicExists.get(topic);
    }

    private void handleNoAdminClient(boolean firstCheck, String topic) {
        if (firstCheck) {
            logger.debug("Unable to perform a topic existence check as this event source instance does not have a Kafka AdminClient available");
            this.topicExists.put(topic, true);
        }
    }

    private boolean checkTopicExistence(String topic, long remainingTimeout) {
        try {
            DescribeTopicsResult topics = this.adminClient.describeTopics(List.of(topic));
            Map<String, TopicDescription> descriptions = topics.allTopicNames().get(remainingTimeout, TimeUnit.MILLISECONDS);
            this.topicExists.put(topic, descriptions.containsKey(topic));
            return true;
        } catch (UnknownTopicOrPartitionException e) {
            this.topicExists.put(topic, false);
        } catch (AuthenticationException | AuthorizationException e) {
            logger.error("Kafka Security Error: ", e);
            throw new EventSourceException("Kafka Security rejected the request", e);
        } catch (Throwable e) {
            this.topicExists.put(topic, false);
        }
        return false;
    }

    private long updateRemainingTimeout(long remainingTimeout, long start) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            return 0;
        }
        return remainingTimeout - (System.currentTimeMillis() - start);
    }

    private void logIfFirstCheckFailed(boolean firstCheck, String topic) {
        if (firstCheck && !this.topicExists.get(topic)) {
            logger.warn("Kafka topic {} does not currently exist on the Kafka server {}", topic, this.server);
        }
    }

    /**
     * Closes the checker, aborting any remaining in-flight checks
     */
    public synchronized void close() {
        if (!this.closed) {
            this.service.shutdownNow();
            try {
                this.service.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Ignored, just trying to give the in-flight checks time to complete
            }
            if (this.adminClient != null) {
                this.adminClient.close();
            }
            this.closed = true;
        }
    }
}
