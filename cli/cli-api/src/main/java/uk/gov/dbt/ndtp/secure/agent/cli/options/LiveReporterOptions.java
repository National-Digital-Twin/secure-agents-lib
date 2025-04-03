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
package uk.gov.dbt.ndtp.secure.agent.cli.options;

import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.NotBlank;
import com.github.rvesse.airline.annotations.restrictions.ranges.IntegerRange;
import java.time.Duration;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.live.IANodeLive;
import uk.gov.dbt.ndtp.secure.agent.live.LiveErrorReporter;
import uk.gov.dbt.ndtp.secure.agent.live.LiveReporter;
import uk.gov.dbt.ndtp.secure.agent.live.LiveReporterBuilder;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveError;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveStatus;
import uk.gov.dbt.ndtp.secure.agent.live.serializers.LiveErrorSerializer;
import uk.gov.dbt.ndtp.secure.agent.projectors.Sink;
import uk.gov.dbt.ndtp.secure.agent.projectors.sinks.NullSink;
import uk.gov.dbt.ndtp.secure.agent.sources.Event;
import uk.gov.dbt.ndtp.secure.agent.sources.kafka.sinks.KafkaSink;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Options for IANode Live heartbeat reporting
 */
public class LiveReporterOptions extends KafkaConfigurationOptions {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveReporterOptions.class);

    private LiveReporter reporter;

    @Option(name = {
            "--live-reporter", "--no-live-reporter"
    }, arity = 0, description = "Sets whether IANode Live heartbeat reporting is enabled/disabled.")
    private boolean enableLiveReporter = true;

    @Option(name = "--live-reporter-topic", title = "LiveTopic", arity = 1, description = "Sets the Kafka topic to which IANode Live heartbeat reports are sent.  Only used if Kafka connection has been suitably configured.")
    @NotBlank
    private String liveReportTopic = LiveReporter.DEFAULT_LIVE_TOPIC;

    @Option(name = "--live-error-topic", title = "LiveErrorTopic", arity = 1, description = "Sets the Kafka topic to which IANode Live errors are sent.  Only used if Kafka connection has been suitably configured.")
    private String liveErrorTopic = LiveErrorReporter.DEFAULT_LIVE_TOPIC;

    @Option(name = {
            "--live-report-interval", "--live-reporter-interval"
    }, title = "LiveReportInterval", arity = 1, description = "Sets the IANode Live heartbeat reporting interval in seconds i.e. how frequently the application will send a Heartbeat.  Defaults to 15 seconds.")
    @IntegerRange(min = 1, max = 300)
    private int liveReportPeriod = LiveReporter.DEFAULT_REPORTING_PERIOD_SECONDS;

    @Option(name = {
            "--live-bootstrap-server", "--live-bootstrap-servers"
    }, title = "LiveBootstrapServers", description = "Provides a comma separated list of bootstrap servers to use for creating the initial connection to Kafka.  For commands that connect to Kafka anyway this option is unnecessary provided the Kafka source is configured via the --bootstrap-servers option, however for commands that don't require a Kafka connection normally this option is required for the Live Heartbeats to be reported correctly.")
    private String liveBootstrapServers = Configurator.get(KafkaOptions.BOOTSTRAP_SERVERS);

    /**
     * Sets up and starts the IANode {@link LiveReporter} if appropriately configured
     *
     * @param bootstrapServers Kafka Bootstrap Servers, if {@code null}/blank then Live Reporting will not be sent to
     *                         Kafka unless the {@code --live-reporter-bootstrap-servers} option was provided
     * @param name             Application name
     * @param id               Application ID
     * @param componentType    Component Type
     * @param input            Input descriptor
     * @param output           Output descriptor
     */
    public void setupLiveReporter(String bootstrapServers, String name, String id, String componentType,
                                  IODescriptor input, IODescriptor output) {
        if (!this.enableLiveReporter) {
            warnLiveReportingDisabled();
            return;
        }

        //@formatter:off
        LiveReporterBuilder builder = LiveReporter.create()
                                                  .name(name)
                                                  .id(id)
                                                  .componentType(componentType)
                                                  .reportingPeriod(Duration.ofSeconds(this.liveReportPeriod))
                                                  .input(input)
                                                  .output(output);
        //@formatter:on
        if (StringUtils.isNotBlank(this.liveBootstrapServers)) {
            logLiveReportingLocation(this.liveBootstrapServers);
            builder = builder.toKafka(k -> k.bootstrapServers(this.liveBootstrapServers)
                                            .topic(this.liveReportTopic)
                                            .producerConfig(this.getAdditionalProperties()));
        } else if (StringUtils.isNotBlank(bootstrapServers)) {
            logLiveReportingLocation(bootstrapServers);
            builder = builder.toKafka(k -> k.bootstrapServers(bootstrapServers)
                                            .topic(this.liveReportTopic)
                                            .producerConfig(this.getAdditionalProperties()));
        }

        this.reporter = builder.build();
        reporter.start();
    }

    private void logLiveReportingLocation(String bootstrapServers) {
        LOGGER.info("IANode Live Heartbeat Reporting going to Kafka topic {} @ {}", this.liveReportTopic,
                    bootstrapServers);
    }

    /**
     * Tears down the IANode {@link LiveReporter} that was previously created (via
     * {@link #setupLiveReporter(String, String, String, String, IODescriptor, IODescriptor)}) stopping it with the
     * given stop status
     * <p>
     * Generally it may be better to call {@link #teardown(LiveStatus)} as that will handle any unexpected errors that
     * occur during teardown.
     * </p>
     *
     * @param stopStatus Stop Status
     * @deprecated Avoid direct usage, call {@link #teardown(LiveStatus)} instead
     */
    @Deprecated
    public void teardownLiveReporter(LiveStatus stopStatus) {
        if (this.reporter != null) {
            this.reporter.stop(stopStatus);
        }
    }

    /**
     * Sets up the IANode {@link LiveErrorReporter} if appropriately configured
     *
     * @param bootstrapServers Kafka bootstrap servers, if {@code null}/blank then Live Reporting will not be sent to
     *                         Kafka unless the {@code --live-reporter-bootstrap-servers} option was provided
     * @param id               Application ID
     */
    public void setupErrorReporter(String bootstrapServers, String id) {
        if (!this.enableLiveReporter) {
            warnLiveReportingDisabled();
            return;
        }

        Sink<Event<Bytes, LiveError>> sink;
        if (StringUtils.isAllBlank(this.liveBootstrapServers, bootstrapServers)) {
            sink = NullSink.of();
        } else {
            String kafkaServers =
                    StringUtils.isNotBlank(this.liveBootstrapServers) ? this.liveBootstrapServers : bootstrapServers;
            LOGGER.info("IANode Live Error Reporting going to Kafka topic {} @ {}", this.liveErrorTopic,
                        kafkaServers);
            //@formatter:off
            sink = KafkaSink.<Bytes, LiveError>create()
                            .bootstrapServers(kafkaServers)
                            .topic(this.liveErrorTopic)
                            .keySerializer(BytesSerializer.class)
                            .valueSerializer(LiveErrorSerializer.class)
                            .producerConfig(this.getAdditionalProperties())
                            .build();
            //@formatter:on
        }

        LiveErrorReporter errorReporter = LiveErrorReporter.create().id(id).destination(sink).build();
        IANodeLive.setErrorReporter(errorReporter);
    }

    private static void warnLiveReportingDisabled() {
        LOGGER.warn("IANode Live Reporting explicitly disabled by user");
    }

    /**
     * Tears down the {@link LiveErrorReporter} that was previously created via a call to
     * {@link #setupErrorReporter(String, String)}
     * <p>
     * Generally it may be better to call {@link #teardown(LiveStatus)} as that will handle any unexpected errors that
     * occur during teardown.
     * </p>
     * @deprecated Avoid direct usage, call {@link #teardown(LiveStatus)} instead
     */
    @Deprecated
    public void teardownErrorReporter() {
        if (IANodeLive.getErrorReporter() != null) {
            IANodeLive.getErrorReporter().close();
        }
    }

    /**
     * Tears down the configured reporters (if any)
     * <p>
     * This method intentionally suppresses any errors that might occur while trying to tear things down as those errors
     * could have the effect of hiding the real cause of the shutdown and preven correct troubleshooting of the issue.
     * </p>
     *
     * @param stopStatus Stop status for the final Live Heartbeat
     */
    public void teardown(LiveStatus stopStatus) {
        try {
            this.teardownLiveReporter(stopStatus);
        } catch (Throwable e) {
            // Log and ignore
            LOGGER.warn("Unexpected error tearing down IANode Live Heartbeat reporting: {}", e.getMessage());
        }
        try {
            this.teardownErrorReporter();
        } catch (Throwable e) {
            // Log and ignore
            LOGGER.warn("Unexpected error tearing down IANode Live Error reporting: {}", e.getMessage());
        }
    }
}
