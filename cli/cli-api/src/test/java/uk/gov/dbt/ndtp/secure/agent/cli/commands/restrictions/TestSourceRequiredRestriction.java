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
package uk.gov.dbt.ndtp.secure.agent.cli.commands.restrictions;

import com.github.rvesse.airline.model.CommandMetadata;
import com.github.rvesse.airline.model.MetadataLoader;
import com.github.rvesse.airline.model.OptionMetadata;
import com.github.rvesse.airline.parser.ParseState;
import com.github.rvesse.airline.parser.errors.ParseOptionGroupException;
import com.github.rvesse.airline.parser.errors.ParseOptionMissingException;
import java.util.Properties;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.ConfigurationSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.NullSource;
import uk.gov.dbt.ndtp.secure.agent.configuration.sources.PropertiesSource;
import uk.gov.dbt.ndtp.secure.agent.cli.commands.SecureAgentCommand;
import uk.gov.dbt.ndtp.secure.agent.cli.options.KafkaOptions;
import uk.gov.dbt.ndtp.secure.agent.cli.restrictions.RequiredForSourceRestriction;
import uk.gov.dbt.ndtp.secure.agent.cli.restrictions.SourceRequiredRestriction;

public class TestSourceRequiredRestriction {

    private static final CommandMetadata COMMAND_METADATA = MetadataLoader.loadCommand(FakeCommand.class,
                                                                                       MetadataLoader.loadParser(
                                                                                               FakeCommand.class));
    private static final OptionMetadata BOOTSTRAP_SERVERS = COMMAND_METADATA.getAllOptions()
                                                                            .stream()
                                                                            .filter(o -> o.getOptions()
                                                                                          .contains(
                                                                                                  "--bootstrap-servers"))
                                                                            .findFirst()
                                                                            .orElse(null);

    private static final OptionMetadata FAKE_SOURCE = COMMAND_METADATA.getAllOptions()
                                                                      .stream()
                                                                      .filter(o -> o.getOptions()
                                                                                    .contains(
                                                                                            "--fake-source"))
                                                                      .findFirst()
                                                                      .orElse(null);

    private static final OptionMetadata DIRECTORY_SOURCE = COMMAND_METADATA.getAllOptions()
                                                                           .stream()
                                                                           .filter(o -> o.getOptions()
                                                                                         .contains("--source-dir"))
                                                                           .findFirst()
                                                                           .orElse(null);

    private static final OptionMetadata FILE_SOURCE = COMMAND_METADATA.getAllOptions()
                                                                      .stream()
                                                                      .filter(o -> o.getOptions()
                                                                                    .contains("--source-file"))
                                                                      .findFirst()
                                                                      .orElse(null);

    private static final OptionMetadata TOPIC = COMMAND_METADATA.getAllOptions()
                                                                .stream()
                                                                .filter(o -> o.getOptions().contains("--topic"))
                                                                .findFirst()
                                                                .orElse(null);

    @BeforeMethod
    public void testSetup() {
        Configurator.reset();
    }

    @AfterClass
    public void cleanup() {
        Configurator.reset();
    }

    @Test(expectedExceptions = ParseOptionGroupException.class, expectedExceptionsMessageRegExp = "At least one.*--bootstrap-server.*")
    public void no_source_provided_01() {
        Configurator.addSource(NullSource.INSTANCE);
        Configurator.setUseAllSources(false);

        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA);
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);
    }

    @Test
    public void no_source_provided_02() {
        requireFakeSourceEnvVar();
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("fake", "FAKE_SOURCE");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA);
        // No error as pom.xml sets the FAKE_SOURCE environment variable for tests
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);
    }

    private static void requireFakeSourceEnvVar() {
        Properties properties = new Properties();
        properties.put(ConfigurationSource.asSystemPropertyKey("FAKE_SOURCE"), "fake");
        PropertiesSource propertiesSource = new PropertiesSource(properties);
        Configurator.addSource(propertiesSource);
        Configurator.setUseAllSources(false);
    }

    @Test
    public void source_provided_01() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(BOOTSTRAP_SERVERS)
                          .withOptionValue(BOOTSTRAP_SERVERS, "localhost:9092");
        // No error as --bootstrap-servers option was set
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);
    }

    @Test
    public void source_provided_02() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(BOOTSTRAP_SERVERS)
                          .withOptionValue(BOOTSTRAP_SERVERS, "localhost:9092")
                          .withOption(BOOTSTRAP_SERVERS)
                          .withOptionValue(BOOTSTRAP_SERVERS, "localhost:19092");
        // No error as --bootstrap-servers option was set, even though it was set multiple times that's acceptable as
        // only one source was set
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);
    }

    @Test(expectedExceptions = ParseOptionGroupException.class, expectedExceptionsMessageRegExp = "Only one.*2 were found.*")
    public void source_provided_03() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(BOOTSTRAP_SERVERS)
                          .withOptionValue(BOOTSTRAP_SERVERS, "localhost:9092")
                          .withOption(FAKE_SOURCE)
                          .withOptionValue(FAKE_SOURCE, "bar");
        // Error as multiple sources are specified
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);
    }

    @Test
    public void source_provided_04() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(FAKE_SOURCE)
                          .withOptionValue(FAKE_SOURCE, "foo");
        // No error as --fake-source option was set
        restriction.finalValidate(state, FAKE_SOURCE);
    }

    @Test
    public void source_provided_05() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(DIRECTORY_SOURCE)
                          .withOptionValue(DIRECTORY_SOURCE, "target");
        // No error as --source-dir option was set
        restriction.finalValidate(state, DIRECTORY_SOURCE);
    }

    @Test
    public void source_provided_06() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(FILE_SOURCE)
                          .withOptionValue(FILE_SOURCE, "pom.xml");
        // No error as --source-file option was set
        restriction.finalValidate(state, FILE_SOURCE);
    }

    @Test(expectedExceptions = ParseOptionMissingException.class, expectedExceptionsMessageRegExp = ".*-t.*Kafka event source.*")
    public void required_for_source_01() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(BOOTSTRAP_SERVERS)
                          .withOptionValue(BOOTSTRAP_SERVERS, "localhost:9092");
        // No error as --bootstrap-servers option was set
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);

        // Should error as topic is required when
        RequiredForSourceRestriction requiredForSourceRestriction = new RequiredForSourceRestriction("Kafka");
        requiredForSourceRestriction.finalValidate(state, TOPIC);
    }

    @Test
    public void required_for_source_01b() {
        requireFakeSourceEnvVar();
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA);

        // Should not error as we've faked the restriction, so it thinks topic is effectively specified
        RequiredForSourceRestriction requiredForSourceRestriction =
                new RequiredForSourceRestriction("Kafka", "FAKE_SOURCE");
        requiredForSourceRestriction.finalValidate(state, TOPIC);
    }

    @Test
    public void required_for_source_02() {
        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(BOOTSTRAP_SERVERS)
                          .withOptionValue(BOOTSTRAP_SERVERS, "localhost:9092")
                          .withOption(TOPIC).withOptionValue(TOPIC, "test");
        // No error as --bootstrap-servers option was set
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);

        // Should not error as topic was specified
        RequiredForSourceRestriction requiredForSourceRestriction = new RequiredForSourceRestriction("Kafka");
        requiredForSourceRestriction.finalValidate(state, TOPIC);
    }

    @Test
    public void required_for_source_03() {
        requireFakeSourceEnvVar();

        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(BOOTSTRAP_SERVERS)
                          .withOptionValue(BOOTSTRAP_SERVERS, "localhost:9092");
        // No error as --bootstrap-servers option was set
        restriction.finalValidate(state, BOOTSTRAP_SERVERS);

        // Should not error as although topic was not specified as an option the FAKE_SOURCE environment variable is set
        RequiredForSourceRestriction requiredForSourceRestriction =
                new RequiredForSourceRestriction("Kafka", "FAKE_SOURCE");
        requiredForSourceRestriction.finalValidate(state, TOPIC);
    }

    @Test
    public void required_for_source_04() {
        requireFakeSourceEnvVar();

        SourceRequiredRestriction restriction = new SourceRequiredRestriction("test");
        ParseState<SecureAgentCommand> state =
                ParseState.<SecureAgentCommand>newInstance()
                          .withCommand(COMMAND_METADATA)
                          .withOption(FAKE_SOURCE)
                          .withOptionValue(FAKE_SOURCE, "test");
        // No error as --fake source was set

        // Should not error as although topic was not specified as an option the --fake source is configured
        RequiredForSourceRestriction requiredForSourceRestriction =
                new RequiredForSourceRestriction("Kafka", KafkaOptions.BOOTSTRAP_SERVERS);
        requiredForSourceRestriction.finalValidate(state, TOPIC);
    }
}

