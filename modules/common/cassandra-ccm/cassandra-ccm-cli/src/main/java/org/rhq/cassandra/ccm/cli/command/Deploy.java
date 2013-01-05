package org.rhq.cassandra.ccm.cli.command;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import org.rhq.cassandra.CassandraClusterManager;
import org.rhq.cassandra.CassandraNode;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;

/**
 * @author John Sanda
 */
public class Deploy extends CCMCommand {

    private Options options;

    @Override
    public String getName() {
        return "deploy";
    }

    @Override
    public String getDescription() {
        return "Creates an embedded cluster and then starts each node.";
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options()
                .addOption("h", "help", false, "Show this message.")
                .addOption("n", "num-nodes", true, "The number of nodes to install and configure. The top level or " +
                    "base directory for each node will be nodeN where N is the node number.");
        }
        return options;
    }

    @Override
    protected void exec(CommandLine cmdLine) {
        if (cmdLine.hasOption("h")) {
            printUsage();
        } else {
            DeploymentOptions deploymentOptions = new DeploymentOptions();
            if (cmdLine.hasOption("n")) {
                int numNodes = Integer.parseInt(cmdLine.getOptionValue("n"));
                deploymentOptions.setNumNodes(numNodes);
            }

            CassandraClusterManager ccm = new CassandraClusterManager(deploymentOptions);
            List<CassandraNode> nodes = ccm.createCluster();
            ccm.startCluster();

            PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();
            try {
                serverPropertiesUpdater.update("rhq.cassandra.seeds", StringUtil.collectionToString(
                    toDelimitedString(nodes)));
            }  catch (IOException e) {
                throw new RuntimeException("An error occurred while trying to update RHQ server properties", e);
            }
        }
    }

    private List<String> toDelimitedString(List<CassandraNode> nodes) {
        List<String> list = new ArrayList<String>(nodes.size());
        for (CassandraNode node : nodes) {
            list.add(node.getHostName() + "|" + node.getThriftPort() + "|" + node.getNativeTransportPort());
        }
        return list;
    }

    private PropertiesFileUpdate getServerProperties() {
        String sysprop = System.getProperty("rhq.server.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhq.server.properties] is not defined.");
        }

        File file = new File(sysprop);
        if (!(file.exists() && file.isFile())) {
            throw new RuntimeException("System property [" + sysprop + "] points to in invalid file.");
        }

        return new PropertiesFileUpdate(file.getAbsolutePath());
    }

}
