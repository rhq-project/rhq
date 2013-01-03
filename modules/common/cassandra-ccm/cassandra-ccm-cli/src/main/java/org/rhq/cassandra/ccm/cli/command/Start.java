package org.rhq.cassandra.ccm.cli.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.DeploymentOptions;

/**
 * @author John Sanda
 */
public class Start extends CCMCommand {

    private Options options;

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Starts the embedded cluster.";
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options()
                .addOption("h", "help", false, "Show this message")
                .addOption("n", "node", true, "A comma-delimited list of node ids that specifies nodes to start.");
        }
        return options;
    }

    @Override
    protected void exec(CommandLine commandLine) {
        if (commandLine.hasOption("h")) {
            printUsage();
        } else {
            DeploymentOptions deploymentOptions = new DeploymentOptions();
            CassandraClusterManager ccm = new CassandraClusterManager(deploymentOptions);
            // TODO handle -n option
            if (commandLine.hasOption("n")) {
                ccm.startCluster(toIntList(commandLine.getOptionValue("n")));
            } else {
                ccm.startCluster();
            }
        }
    }
}
