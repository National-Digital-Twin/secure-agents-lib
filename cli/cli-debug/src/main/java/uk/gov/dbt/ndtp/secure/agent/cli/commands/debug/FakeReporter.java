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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.debug;

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.ranges.DoubleRange;
import com.github.rvesse.airline.annotations.restrictions.ranges.LongRange;
import com.github.rvesse.airline.model.CommandMetadata;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import uk.gov.dbt.ndtp.secure.agent.live.IANodeLive;
import uk.gov.dbt.ndtp.secure.agent.live.model.IODescriptor;
import uk.gov.dbt.ndtp.secure.agent.live.model.LiveError;
import uk.gov.dbt.ndtp.secure.agent.projectors.utils.PeriodicAction;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.model.HealthStatus;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.options.HealthProbeServerOptions;
import uk.gov.dbt.ndtp.secure.agent.cli.options.KafkaOptions;

/**
 * A debug command that creates a fake application reporting status heartbeats to IANode Live
 */
@Command(name = "fake-reporter", description = "Creates a fake application that reports status heartbeats to IANode Live")
public class FakeReporter extends SecureAgentCommand {
    private final Random random = new Random();
    private static final Logger LOGGER = LoggerFactory.getLogger(FakeReporter.class);

    private static final Level[] ERROR_LEVELS = new Level[] { Level.ERROR, Level.WARN, Level.INFO };

    @Option(name = {
            "--app-name", "--name"
    }, title = "ApplicationName", description = "Sets the Application Name that will be reported")
    private String name;

    @Option(name = {
            "--app-id", "--id"
    }, title = "ApplicationId", description = "Sets the Application ID that will be reported")
    private String id;

    @Option(name = { "--component-type" }, title = "ComponentType", description = "Sets the component type that will be reported")
    private String componentType;

    @Option(name = {
            "--input-name", "--input"
    }, title = "InputName", description = "Sets the input name that will be reported.  Defaults to the value of --topic if not specified.")
    private String inputName;

    @Option(name = { "--input-type" }, title = "InputType", description = "Sets the input type that will be reported.  Defaults to topic if not specified.")
    private String inputType;

    @Option(name = {
            "--output-name", "--output"
    }, title = "OutputName", description = "Sets the output name that will be reported")
    private String outputName;

    @Option(name = { "--output-type" }, title = "OutputType", description = "Sets the output type that will be reported")
    private String outputType;

    @AirlineModule
    private KafkaOptions kafkaOptions = new KafkaOptions();

    @Option(name = { "--error-interval" }, title = "ErrorInterval", description = "Sets how frequently there is a chance of generating a random error, actual chance is controlled by the --error-chance option.  Defaults to 5 seconds.")
    @LongRange(min = 1, max = 30)
    private long errorInterval = 5;

    @Option(name = { "--error-chance" }, title = "ErrorChance", description = "Sets the chance of generating a random error, actual frequency of random error generation is controlled by the --error-interval option.  Defaults to 0.5 i.e. 50%")
    @DoubleRange(min = 0.0, max = 1.0)
    private double errorChance = 0.5;

    @Option(name = { "--readiness-reason" }, title = "Reason", description = "When set the Health Probe Server for the CLI will report the application is unready with this reason.")
    private String readinessReason;

    @AirlineModule
    private HealthProbeServerOptions healthProbeServerOptions = new HealthProbeServerOptions();

    @Override
    protected void setupLiveReporter(CommandMetadata metadata) {
        this.liveReporter.setupLiveReporter(this.kafkaOptions.bootstrapServers,
                                            StringUtils.isNotBlank(this.name) ? this.name : metadata.getName(),
                                            StringUtils.isNotBlank(this.id) ? this.id : metadata.getName(),
                                            StringUtils.isNotBlank(this.componentType) ? this.componentType : "mapper",
                                            new IODescriptor(StringUtils.isNotBlank(this.inputName) ? this.inputName :
                                                             StringUtils.join(this.kafkaOptions.topics, ","),
                                                             StringUtils.isNotBlank(this.inputType) ? this.inputType :
                                                             "topic"),
                                            new IODescriptor(this.outputName, this.outputType));

        this.liveReporter.setupErrorReporter(this.kafkaOptions.bootstrapServers,
                                             StringUtils.isNotBlank(this.id) ? this.id : metadata.getName());
    }

    @Override
    public int run() {
        // Run the health probe server
        this.healthProbeServerOptions.setupHealthProbeServer("Fake Reporter", () -> HealthStatus.builder()
                                                                                                .healthy(
                                                                                                        StringUtils.isBlank(
                                                                                                                this.readinessReason))
                                                                                                .reasons(
                                                                                                        StringUtils.isNotBlank(
                                                                                                                this.readinessReason) ?
                                                                                                        List.of(this.readinessReason) :
                                                                                                        Collections.emptyList())
                                                                                                .build());

        // Set up periodic random error generation
        PeriodicAction action = createPeriodicAction();
        try {
            action.autoTrigger();
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            // Ignore
            action.cancelAutoTrigger();
            this.healthProbeServerOptions.teardownHealthProbeServer();
        } finally {
            this.healthProbeServerOptions.teardownHealthProbeServer();
        }
        return 0;
    }

    private PeriodicAction createPeriodicAction() {
        AtomicInteger levelSelection = new AtomicInteger(0);
        PeriodicAction action = new PeriodicAction(() -> {
            double chance = this.random.nextDouble();
            if (chance < this.errorChance) {
                // Generated an error, we're continually cycling through the available error levels
                Level level = ERROR_LEVELS[levelSelection.getAndIncrement() % ERROR_LEVELS.length];
                LOGGER.debug("Generated a random error of level {}", level);
                IANodeLive.getErrorReporter()
                            .reportError(LiveError.create()
                                                  .message("Randomly generated error " + System.currentTimeMillis())
                                                  .level(level)
                                                  .recordCounter(System.currentTimeMillis())
                                                  .build());
            } else {
                LOGGER.debug("Random error generation didn't meet chance threshold");
            }
        }, Duration.ofSeconds(this.errorInterval));
        return action;
    }
}
