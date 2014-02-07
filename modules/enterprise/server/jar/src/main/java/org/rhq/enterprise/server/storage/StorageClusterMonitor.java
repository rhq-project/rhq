package org.rhq.enterprise.server.storage;

import static org.rhq.server.metrics.StorageClientConstant.REQUEST_LIMIT;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.server.metrics.StorageClientConstant;
import org.rhq.server.metrics.StorageSession;
import org.rhq.server.metrics.StorageStateListener;

/**
 * @author John Sanda
 */
public class StorageClusterMonitor implements StorageStateListener {

    private Log log = LogFactory.getLog(StorageClusterMonitor.class);

    private boolean isClusterAvailable = true;

    private File serverPropsFile;

    private StorageSession session;

    public StorageClusterMonitor(File serverPropsFile, StorageSession session) {
        this.serverPropsFile = serverPropsFile;
        this.session = session;
    }


    public boolean isClusterAvailable() {
        return isClusterAvailable;
    }

    @Override
    public void onStorageNodeUp(InetAddress address) {
        log.info("Storage node at " + address.getHostAddress() + " is up");
        isClusterAvailable = true;
        updateRequestLimit();
    }

    @Override
    public void onStorageNodeDown(InetAddress address) {
        log.info("Storage node at " + address.getHostAddress() + " is down");
        updateRequestLimit();
    }

    @Override
    public void onStorageNodeRemoved(InetAddress address) {
        log.info("Storage node at " + address.getHostAddress() + " has been removed from the cluster");
        updateRequestLimit();
    }

    @Override
    public void onStorageClusterUp() {
        log.info("Storage cluster is up");
        isClusterAvailable = true;
    }

    @Override
    public void onStorageClusterDown(NoHostAvailableException e) {
        log.info("Storage cluster is down");
        isClusterAvailable = false;
    }

    @Override
    public void onClientTimeout(NoHostAvailableException e) {
        updateRequestLimit();
    }

    private void updateRequestLimit() {
        persistStorageProperty(REQUEST_LIMIT, Double.toString(session.getRequestLimit()));
    }

    private void persistStorageProperty(StorageClientConstant constant, String value) {
        PropertiesFileUpdate updater = new PropertiesFileUpdate(serverPropsFile.getAbsolutePath());
        try {
            updater.update(constant.property(), value);
        } catch (IOException e) {
            log.warn("Failed to persist property " + constant.property() + " due to unexpected I/O error",
                ThrowableUtil.getRootCause(e));
        }
    }
}
