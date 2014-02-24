package org.rhq.cassandra.schema;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.cassandra.util.ClusterBuilder;

/**
 * @author John Sanda
 */
public class SessionManager {

    private final Log log = LogFactory.getLog(SessionManager.class);


    private Cluster cluster;

    private Session session;

    public void initSession(String username, String password, int cqlPort, String... nodes) {
        if (session == null) {
            log.debug("Initializing session for [username: " + username + ", cqlPort: " + cqlPort + ", nodes: " +
                Arrays.toString(nodes) + "]");
            cluster = new ClusterBuilder().addContactPoints(nodes).withCredentialsObfuscated(username, password)
                .withPort(cqlPort).withCompression(ProtocolOptions.Compression.NONE).build();
            session = cluster.connect("system");
        }
    }

    public void shutdownCluster() {
        log.debug("Shutting down storage cluster");
        if (cluster != null) {
            cluster.shutdown();
            cluster = null;
            session = null;
        }
    }

    public Session getSession() {
        return session;
    }

    public Set<String> getNodeAdresses() {
        Set<String> nodes = new TreeSet<String>();
        for (Host host : cluster.getMetadata().getAllHosts()) {
            nodes.add(host.getAddress().getHostName());
        }
        return nodes;
    }

}
