package org.rhq.cassandra.ccm.cli.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * @author John Sanda
 */
public abstract class CCMCommand {

    public abstract String getName();

    public abstract String getDescription();

    public abstract Options getOptions();

    protected abstract void exec(CommandLine commandLine);

    public void exec(String[] args) {
        Options options = getOptions();
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmdLine = parser.parse(options, args);
            exec(cmdLine);
        } catch (ParseException e) {
            printUsage();
        }
    }

    public void printUsage() {
        Options options = getOptions();
        HelpFormatter helpFormatter = new HelpFormatter();
        String header = "\n" + getDescription() + "\n\n";
        String syntax;

        if (options.getOptions().isEmpty()) {
            syntax = "rhq-ccm.sh " + getName();
        } else {
            syntax = "rhq-ccm.sh " + getName() + " [options]";
        }

        helpFormatter.setNewLine("\n");
        helpFormatter.printHelp(syntax, header, options, null);
    }

}
