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
package uk.gov.dbt.ndtp.secure.agent.projectors.sinks;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.lib.CacheSet;
import org.apache.jena.atlas.logging.FmtLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.dbt.ndtp.secure.agent.observability.AttributeNames;
import uk.gov.dbt.ndtp.secure.agent.observability.MetricNames;
import uk.gov.dbt.ndtp.secure.agent.observability.IANodeMetrics;
import uk.gov.dbt.ndtp.secure.agent.projectors.Library;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.builder.AbstractForwardingSinkBuilder;

/**
 * A sink that performs duplicate item suppression by using an LRU Cache to avoid forwarding items that have recently
 * been seen
 *
 * @param <T> Item type
 */
public class SuppressDuplicatesSink<T> extends AbstractTransformingSink<T, T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuppressDuplicatesSink.class);

    private final CacheSet<T> cache;
    private long suppressed = 0;
    private final LongCounter suppressedMetric;
    private final Attributes metricAttributes;

    private long lastCacheOperationAt = -1;
    private final long expireCacheAfter;
    private final Supplier<Boolean> invalidateWholeCache;
    private final Function<T, Boolean> invalidateCache;

    /**
     * Creates a new sink
     *
     * @param destination     Destination sink
     * @param cacheSize       Cache size
     * @param invalidateCache Whether to invalidate cache
     */
    SuppressDuplicatesSink(Sink<T> destination, int cacheSize, String metricsLabel,
                           Function<T, Boolean> invalidateCache, Supplier<Boolean> invalidateWholeCache,
                           Duration expireCacheAfter) {
        super(destination);
        this.invalidateCache = invalidateCache != null ? invalidateCache : x -> false;
        this.invalidateWholeCache = invalidateWholeCache != null ? invalidateWholeCache : () -> false;
        if (expireCacheAfter != null && expireCacheAfter.compareTo(Duration.ZERO) < 1) {
            throw new IllegalArgumentException("Expire cache after duration must be a duration greater than zero");
        }
        this.expireCacheAfter = expireCacheAfter != null ? expireCacheAfter.toMillis() : -1;

        if (cacheSize < 1) {
            throw new IllegalArgumentException("Cache Size must be >= 1");
        }

        this.cache = CacheFactory.createCacheSet(cacheSize);
        if (StringUtils.isNotBlank(metricsLabel)) {
            Meter meter = IANodeMetrics.getMeter(Library.NAME);
            //@formatter:off
            this.suppressedMetric
                    = meter.counterBuilder(MetricNames.DUPLICATES_SUPPRESSED)
                           .setDescription(MetricNames.DUPLICATES_SUPPRESSED_DESCRIPTION)
                           .build();
            //@formatter:on
            this.metricAttributes = Attributes.of(AttributeKey.stringKey(AttributeNames.ITEMS_TYPE), metricsLabel,
                                                  AttributeKey.stringKey(AttributeNames.INSTANCE_ID),
                                                  UUID.randomUUID().toString());
        } else {
            this.suppressedMetric = null;
            this.metricAttributes = null;
        }
    }

    @Override
    protected boolean shouldForward(T item) {
        // Check for whole cache invalidation
        if (this.invalidateWholeCache.get()) {
            LOGGER.info("Invalidated duplicate suppression cache");
            this.cache.clear();
        } else if (this.expireCacheAfter != -1 && this.lastCacheOperationAt > -1) {
            long timeSinceLastCacheOperation = System.currentTimeMillis() - this.lastCacheOperationAt;
            if (timeSinceLastCacheOperation > this.expireCacheAfter) {
                LOGGER.info("Invalidated duplicate suppression cache due to prolonged inactivity");
                this.cache.clear();
            }
        }
        this.lastCacheOperationAt = System.currentTimeMillis();

        // Check for cache entry invalidation
        if (this.invalidateCache.apply(item)) {
            this.cache.remove(item);
        } else {
            if (this.cache.contains(item)) {
                if (this.suppressedMetric != null) {
                    this.suppressedMetric.add(1, this.metricAttributes);
                }
                this.suppressed++;
                return false;
            }
            this.cache.add(item);
        }
        return true;
    }

    @Override
    protected T transform(T input) {
        return input;
    }

    @Override
    public void close() {
        super.close();

        if (this.suppressed > 0) {
            FmtLog.info(LOGGER, "Suppressed %,d duplicates", this.suppressed);
        }

        this.cache.clear();
    }

    /**
     * Gets how many duplicates were suppressed
     *
     * @return Suppressed duplicates count
     */
    public long getSuppressed() {
        return this.suppressed;
    }

    /**
     * Creates a builder for a duplicate suppressing sink
     *
     * @param <TItem> Item type
     * @return Duplicate suppressing sink builder
     */
    public static <TItem> Builder<TItem> create() {
        return new Builder<>();
    }

    /**
     * A builder for a duplicate suppressing sink
     *
     * @param <TItem> Item type
     */
    public static class Builder<TItem>
            extends AbstractForwardingSinkBuilder<TItem, TItem, SuppressDuplicatesSink<TItem>, Builder<TItem>> {

        private int cacheSize;
        private String metricsLabel;
        private Duration expireCacheAfter;
        private Supplier<Boolean> invalidateWholeCache;
        private Function<TItem, Boolean> invalidateCache;

        /**
         * Sets a metrics label to use for collecting metrics on the number of suppressed items
         *
         * @param metricsLabel Metrics label
         * @return Builder
         */
        public Builder<TItem> withMetrics(String metricsLabel) {
            this.metricsLabel = metricsLabel;
            return this;
        }

        /**
         * Sets the cache size used for suppressing duplicates.
         * <p>
         * A higher cache size increases memory usage but may do better at suppressing duplicates.
         * </p>
         *
         * @param size Size
         * @return Builder
         */
        public Builder<TItem> cacheSize(int size) {
            this.cacheSize = size;
            return this;
        }

        /**
         * Sets whether to invalidate the cache entry for a specific item
         *
         * @param invalidate Cache invalidation function
         * @return Builder
         */
        public Builder<TItem> invalidateWhen(Function<TItem, Boolean> invalidate) {
            this.invalidateCache = invalidate;
            return this;
        }

        /**
         * Sets a function that calculates when to invalidate the whole cache.
         * <p>
         * If not specified the whole cache is never invalidated.
         * </p>
         *
         * @param invalidateWholeCache Cache invalidation function
         * @return Builder
         */
        public Builder<TItem> invalidateWholeWhen(Supplier<Boolean> invalidateWholeCache) {
            this.invalidateWholeCache = invalidateWholeCache;
            return this;
        }

        /**
         * Sets the whole cache to expire after no activity for the given period.
         * <p>
         * If not specified the cache does not expire.
         * </p>
         *
         * @param expiryPeriod Expiry period
         * @return Builder
         */
        public Builder<TItem> expireCacheAfter(Duration expiryPeriod) {
            this.expireCacheAfter = expiryPeriod;
            return this;
        }

        /**
         * Builds a duplicate suppressing sink
         *
         * @return Duplicate suppressing sink
         */
        @Override
        public SuppressDuplicatesSink<TItem> build() {
            return new SuppressDuplicatesSink<>(this.getDestination(), this.cacheSize, this.metricsLabel,
                                                this.invalidateCache, this.invalidateWholeCache, this.expireCacheAfter);
        }
    }
}
