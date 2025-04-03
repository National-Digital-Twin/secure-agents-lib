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
package uk.gov.dbt.ndtp.secure.agent.cli.commands;

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.help.CommandGroupUsageGenerator;
import com.github.rvesse.airline.help.GlobalUsageGenerator;
import com.github.rvesse.airline.help.cli.CliCommandGroupUsageGenerator;
import com.github.rvesse.airline.help.cli.CliCommandUsageGenerator;
import com.github.rvesse.airline.help.cli.CliGlobalUsageSummaryGenerator;
import com.github.rvesse.airline.model.CommandGroupMetadata;
import com.github.rvesse.airline.model.CommandMetadata;
import com.github.rvesse.airline.model.GlobalMetadata;
import com.github.rvesse.airline.utils.predicates.parser.AbbreviatedCommandFinder;
import com.github.rvesse.airline.utils.predicates.parser.AbbreviatedGroupFinder;
import com.github.rvesse.airline.utils.predicates.parser.CommandFinder;
import com.github.rvesse.airline.utils.predicates.parser.GroupFinder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

/**
 * Provides help on other commands in the CLI
 */
@Command(name = "help", description = "Displays help for a command, group, or the entire CLI.")
public class HelpCommand extends SecureAgentCommand {
    static final String UNABLE_TO_SHOW_HELP = "Unable to show help";
    @AirlineModule
    private GlobalMetadata<SecureAgentCommand> global;

    @Arguments(title = "CommandOrGroupName", description = "Specifies the path to the command, which may include groups, that you wish to view help for")
    List<String> args = new ArrayList<>();

    @Option(name = "--include-hidden", description = "When set includes any hidden groups, commands and options in the help")
    boolean includeHidden = false;

    @Override
    protected void setupLiveReporter(CommandMetadata metadata) {
        // Nothing to do, Live Reporter is irrelevant for the Help Command
    }

    @Override
    public int run() {
        try {
            CommandMetadata command = findCommand();
            CommandGroupMetadata group = findGroup(0);

            if (command != null) {
                displayCommandUsage(command, group);
            } else if (group != null) {
                displayGroupUsage(group);
            } else if (this.global != null) {
                displayGlobalUsage();
            } else {
                System.err.println(UNABLE_TO_SHOW_HELP);
            }
        } catch (IOException e) {
            System.err.print("Failed to output help: ");
            System.err.println(e.getMessage());
        }
        return 2;
    }

    private CommandMetadata findCommand() {
        if (args.isEmpty() || this.global == null) {
            return null;
        }

        int argIndex = 0;
        CommandGroupMetadata group = findGroup(argIndex);
        CommandGroupMetadata subGroup = null;
        if (group != null) {
            argIndex++;
            while (argIndex < args.size() && !group.getSubGroups().isEmpty()) {
                subGroup = findSubGroup(group, argIndex);
                if (subGroup == null) {
                    break;
                }
                group = subGroup;
                argIndex++;
            }
        }

        if (argIndex < args.size()) {
            List<CommandMetadata> commands = group != null ? group.getCommands() : this.global.getDefaultGroupCommands();
            Predicate<CommandMetadata> commandFinder = this.global.getParserConfiguration().allowsAbbreviatedCommands()
                    ? new AbbreviatedCommandFinder(args.get(argIndex), commands)
                    : new CommandFinder(args.get(argIndex));

            return IterableUtils.find(commands, commandFinder);
        }

        return null;
    }

    private CommandGroupMetadata findGroup(int argIndex) {
        if (args.isEmpty() || this.global == null) {
            return null;
        }
        Predicate<CommandGroupMetadata> groupFinder = getGroupFinder(argIndex);
        return IterableUtils.find(this.global.getCommandGroups(), groupFinder);
    }

    private CommandGroupMetadata findSubGroup(CommandGroupMetadata group, int argIndex) {
        Predicate<CommandGroupMetadata> groupFinder = getGroupFinder(argIndex);
        return IterableUtils.find(group.getSubGroups(), groupFinder);
    }

    private void displayCommandUsage(CommandMetadata command, CommandGroupMetadata group) throws IOException {
        CliCommandUsageGenerator generator = new CliCommandUsageGenerator(this.includeHidden);
        generator.usage(this.global.getName(), buildGroupPathAsStrings(group), command.getName(), command,
                this.global.getParserConfiguration());
    }

    private void displayGroupUsage(CommandGroupMetadata group) throws IOException {
        CommandGroupUsageGenerator<SecureAgentCommand> generator = new CliCommandGroupUsageGenerator<>(this.includeHidden);
        generator.usage(this.global, buildGroupPath(group));
    }

    private void displayGlobalUsage() throws IOException {
        GlobalUsageGenerator<SecureAgentCommand> generator = new CliGlobalUsageSummaryGenerator<>(this.includeHidden);
        generator.usage(this.global);
    }

    private Predicate<CommandGroupMetadata> getGroupFinder(int argIndex) {
        return this.global.getParserConfiguration().allowsAbbreviatedCommands() ? new AbbreviatedGroupFinder(
                args.get(argIndex), this.global.getCommandGroups()) : new GroupFinder(args.get(argIndex));
    }

    private CommandGroupMetadata[] buildGroupPath(CommandGroupMetadata metadata) {
        if (metadata == null) {
            return new CommandGroupMetadata[0];
        }

        LinkedList<CommandGroupMetadata> path = new LinkedList<>();

        while (metadata != null) {
            path.addFirst(metadata);
            metadata = metadata.getParent();
        }

        return path.toArray(new CommandGroupMetadata[0]);
    }

    private String[] buildGroupPathAsStrings(CommandGroupMetadata metadata) {
        CommandGroupMetadata[] path = buildGroupPath(metadata);
        return Arrays.stream(path).map(CommandGroupMetadata::getName).toArray(String[]::new);
    }

    /**
     * Runs help as a standalone command, not really useful but needed for testing purposes
     *
     * @param args Arguments
     */
    public static void main(String[] args) {
        runAsSingleCommand(HelpCommand.class, args);
    }
}
