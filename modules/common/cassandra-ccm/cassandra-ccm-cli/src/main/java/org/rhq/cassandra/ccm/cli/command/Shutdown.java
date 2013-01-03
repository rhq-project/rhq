package org.rhq.cassandra.ccm.cli.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.DeploymentOptions;

/**
 * @author John Sanda
 */
public class Shutdown extends CCMCommand {

    private Options options;

    @Override
    public String getName() {
        return "shutdown";
    }

    @Override
    public String getDescription() {
        return "Shutdown an embedded cluster. Note that if a cassandra.pid file is not found, no attempt will be " +
            "made to shutdown the node.";
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options()
                .addOption("h", "help", false, "Show this message")
                .addOption("n", "node", true, "A comma-delimited list of node ids that specifies nodes to shut down.");
        }
        return options;
    }

    @Override
    protected void exec(CommandLine cmdLine) {
        if (cmdLine.hasOption("h")) {
            printUsage();
        } else {
            DeploymentOptions deploymentOptions = new DeploymentOptions();
            CassandraClusterManager ccm = new CassandraClusterManager(deploymentOptions);

            if (cmdLine.hasOption("n")) {
                ccm.shutdown(toIntList(cmdLine.getOptionValue("n")));
            } else {
                ccm.shutdownCluster();
            }
        }
    }
}
