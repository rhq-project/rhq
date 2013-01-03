package org.rhq.cassandra.ccm.cli;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.rhq.cassandra.ccm.cli.command.CCMCommand;
import org.rhq.cassandra.ccm.cli.command.Deploy;
import org.rhq.cassandra.ccm.cli.command.Shutdown;
import org.rhq.cassandra.ccm.cli.command.Start;

/**
 * @author John Sanda
 */
public class Commands {

    private Map<String, CCMCommand> commands = new TreeMap<String, CCMCommand>(new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            return s1.compareTo(s2);
        }
    });

    public Commands() {
        registerCommand(new Deploy());
        registerCommand(new Shutdown());
        registerCommand(new Start());
    }

    private void registerCommand(CCMCommand command) {
        commands.put(command.getName(), command);
    }

    public Options getOptions() {
        Options options = new Options();
        for (CCMCommand cmd : commands.values()) {
            if (cmd.getOptions().getOptions().isEmpty()) {
                options.addOption(OptionBuilder.withDescription(cmd.getDescription()).create(cmd.getName()));
            } else if (cmd.getOptions().getOptions().size() == 1) {
                options.addOption(OptionBuilder
                    .withArgName("[options]")
                    .hasOptionalArg()
                    .withDescription(cmd.getDescription())
                    .create(cmd.getName()));
            } else {
                options.addOption(OptionBuilder
                    .withArgName("[options]")
                    .hasOptionalArgs()
                    .withDescription(cmd.getDescription())
                    .create(cmd.getName()));
            }
        }
        return options;
    }

    public CCMCommand get(String name) {
        return commands.get(name);
    }

    public boolean contains(String name) {
        return commands.containsKey(name);
    }

}
