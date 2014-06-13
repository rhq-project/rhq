package org.rhq.cassandra.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.cassandra.auth.IInternodeAuthenticator;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author John Sanda
 */
public class RhqInternodeAuthenticator implements IInternodeAuthenticator, RhqInternodeAuthenticatorMBean {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String MBEAN_NAME = "org.rhq.cassandra.auth:type=" + RhqInternodeAuthenticator.class.getSimpleName();

    private final String CONF_FILE = "rhq-storage-auth.conf";

    private File authConfFile;

    private Set<InetAddress> addresses = new HashSet<InetAddress>();

    private int warned;

    public RhqInternodeAuthenticator() {
        try {
            authConfFile = new File(getClass().getResource("/" + CONF_FILE).toURI());
            if (!authConfFile.exists()) {
                throw new RuntimeException(authConfFile + " does not exist");
            }

            reloadConfiguration();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to load " + CONF_FILE, e);
        }

        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName nameObj = new ObjectName(MBEAN_NAME);
            mbs.registerMBean(this, nameObj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register MBean " + MBEAN_NAME, e);
        }
    }

    @Override
    public boolean authenticate(InetAddress address, int port) {
        boolean contains = addresses.contains(address);
        if (!contains) {
            warn("failed to authenticate " + address + "; not in " + CONF_FILE);
        }
        return contains;
    }

    private void warn(String string) {
        if (warned > 5) {
            return;
        }
        warned++;
        log.warn(string);
    }

    @Override
    public void reloadConfiguration() {
        try {
            addresses.clear();

            BufferedReader reader = new BufferedReader(new FileReader(authConfFile));
            String line = reader.readLine();

            while (line != null) {
                addresses.add(InetAddress.getByName(line));
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load addresses from " + authConfFile, e);
        }
    }

    @Override
    public void validateConfiguration() throws ConfigurationException {
        if (addresses.isEmpty())
            throw new ConfigurationException(CONF_FILE + " empty");
    }
}
