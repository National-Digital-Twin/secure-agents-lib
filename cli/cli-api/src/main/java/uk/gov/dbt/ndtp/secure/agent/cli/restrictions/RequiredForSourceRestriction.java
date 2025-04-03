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
package uk.gov.dbt.ndtp.secure.agent.cli.restrictions;

import com.github.rvesse.airline.help.sections.HelpFormat;
import com.github.rvesse.airline.help.sections.HelpHint;
import com.github.rvesse.airline.model.OptionMetadata;
import com.github.rvesse.airline.parser.ParseState;
import com.github.rvesse.airline.parser.errors.ParseOptionMissingException;
import com.github.rvesse.airline.restrictions.OptionRestriction;
import com.github.rvesse.airline.utils.AirlineUtils;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import uk.gov.dbt.ndtp.secure.agent.configuration.Configurator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A restriction that makes an option required when a specific event source is in-use
 */
public class RequiredForSourceRestriction implements OptionRestriction, HelpHint {

    private final Set<String> envVars = new HashSet<>();
    private final String sourceName;

    /**
     * Creates a new restriction that makes this option required when a given event source is in-use
     *
     * @param name    Event source name
     * @param envVars Environment variables that can be used instead of this option
     */
    public RequiredForSourceRestriction(String name, String... envVars) {
        if (StringUtils.isBlank(name)) throw new IllegalArgumentException("Source name cannot be null/empty");
        this.sourceName = name;
        if (envVars != null) {
            CollectionUtils.addAll(this.envVars, envVars);
        }
    }

    @Override
    public String getPreamble() {
        return !this.envVars.isEmpty() ? String.format("This option is required when the %s",
                                                       this.sourceName + " event source is in use, unless one of the following environment variables is set: ") :
               null;
    }

    @Override
    public HelpFormat getFormat() {
        return !this.envVars.isEmpty() ? HelpFormat.LIST : HelpFormat.PROSE;
    }

    @Override
    public int numContentBlocks() {
        return 1;
    }

    @Override
    public String[] getContentBlock(int blockNumber) {
        if (blockNumber != 0) {
            throw new IndexOutOfBoundsException();
        }
        if (!this.envVars.isEmpty()) {
            return this.envVars.toArray(new String[0]);
        } else {
            return new String[] {
                    String.format("This option is required when the %s event source is used.", this.sourceName)
            };
        }
    }

    @Override
    public <T> void finalValidate(ParseState<T> state, OptionMetadata option) {
        int seen = state.getOptionValuesSeen(option);
        if (seen > 0) {
            return;
        }

        if (isSourceConfigured(state)) {
            validateEnvironmentVariables(option);
        }
    }

    private <T> boolean isSourceConfigured(ParseState<T> state) {
        return sourceConfiguredViaOption(state) || sourceConfiguredViaEnvironmentVariable(state);
    }

    private void validateEnvironmentVariables(OptionMetadata option) {
        if (!this.envVars.isEmpty()) {
            for (String envVar : this.envVars) {
                if (StringUtils.isNotBlank(Configurator.get(envVar))) {
                    return;
                }
            }
            throwParseOptionMissingException(option);
        } else {
            throw new ParseOptionMissingException(AirlineUtils.first(option.getOptions()), String.format(
                    "this must be specified when the %s event source is in-use", this.sourceName));
        }
    }

    private void throwParseOptionMissingException(OptionMetadata option) {
        if (this.envVars.size() > 1) {
            throw new ParseOptionMissingException(AirlineUtils.first(option.getOptions()), String.format(
                    "no default values were available from the environment variables %s.  This must be specified when the %s event source is in-use.",
                    StringUtils.join(this.envVars, ", "), this.sourceName));
        } else {
            throw new ParseOptionMissingException(AirlineUtils.first(option.getOptions()), String.format(
                    "no default values were available from the environment variable %s.  This must be specified when the %s event source is in-use.",
                    this.envVars.stream().findFirst().orElse("NeverCalledButStopsWarning"), this.sourceName));
        }
    }

    private <T> boolean sourceConfiguredViaOption(ParseState<T> state) {
        return state.getParsedOptions()
                    .stream()
                    .anyMatch(o -> o.getKey()
                                    .getRestrictions()
                                    .stream()
                                    .filter(r -> r instanceof SourceRequiredRestriction)
                                    .map(r -> (SourceRequiredRestriction) r)
                                    .anyMatch(r -> Objects.equals(this.sourceName, r.getSourceName())));
    }

    private <T> boolean sourceConfiguredViaEnvironmentVariable(ParseState<T> state) {
        return SourceRequiredRestriction.getSourceOptions(state)
                                        .stream()
                                        .flatMap(o -> o.getRestrictions().stream())
                                        .filter(r -> r instanceof SourceRequiredRestriction)
                                        .map(r -> (SourceRequiredRestriction) r)
                                        .filter(r -> Objects.equals(r.getSourceName(), this.sourceName))
                                        .anyMatch(r -> r.getUnlessEnvironmentVariables()
                                                        .stream()
                                                        .anyMatch(e -> StringUtils.isNotBlank(Configurator.get(e))));
    }

    @Override
    public <T> void postValidate(ParseState<T> state, OptionMetadata option, Object value) {
        // No post-validation needed
    }

    @Override
    public <T> void preValidate(ParseState<T> state, OptionMetadata option, String value) {
        // No pre-validation needed
    }
}
