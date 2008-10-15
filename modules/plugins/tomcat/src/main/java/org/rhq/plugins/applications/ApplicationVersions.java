 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.applications;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Access to the persistent storage of application (EARs, WARs) package versions. This class provides the ability to
 * save and load the versions to disk, along with the basic retrieval and set calls for the versions themselves.
 *
 * @author Jason Dobies
 */
public class ApplicationVersions {

    /**
     * Name of the file used to persist the version data.
     */
    public static final String FILENAME = "application-versions.dat";
    
    /**
     * Version data storage. This is loaded through the {@link #loadFromDisk()} method which must be called prior
     * to any version manipulation calls.
     */
    private static ApplicationVersionData data;
    private static ReentrantReadWriteLock dataLock = new ReentrantReadWriteLock(true);

    /**
     * Plugin in which this instance lives. This will be used in the naming of the persistent store, thus allowing
     * plugins that share application component code to not run into issues about file conflicts.
     */
    private String pluginName;

    /**
     * Data directory of the agent. This is where the versions will be persisted and is provided by the plugin
     * container.
     */
    private String dataDirectory;

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Creates a new entry point to the persisted version data. Multiple instances of this class may be instantiated;
     * the storage of the data itself does not live in any particular instance of this class. This call will not
     * load the data from disk, it must explicitly be done by the user through a call to {@link #loadFromDisk()}.
     *
     * @param pluginName    plugin loading the version data
     * @param dataDirectory directory into which to persist the versions
     */
    public ApplicationVersions(String pluginName, String dataDirectory) {
        this.pluginName = pluginName;
        this.dataDirectory = dataDirectory;
    }

    /**
     * Loads the last known application (package) versions for the plugin this instance is scoped to. This method will
     * absorb and log any errors that occur while saving; this call represents a best-effort to save the values. If
     * a data store for the versions has already been found, this method will not reload it from disk. Thus it is
     * safe to make this call multiple times not only per instance, but per class loader, as the data is stored as
     * a static variable across all instances of this class. 
     */
    public void loadFromDisk() {
        dataLock.readLock().lock();

        ObjectInputStream ois = null;
        try {

            // All instances of this class will use the same store. If one loaded it already, there's nothing to do.
            if (data != null)
                return;

            log.debug("Loading application versions from storage for plugin [" + pluginName + "]");

            File file = new File(dataDirectory, FILENAME);

            // There will be no data file after a clean or on the first run, so create an empty one 
            if (!file.exists()) {
                log.debug("No application versions found for plugin [" + pluginName +
                    "]. This will be the case if the Agent was cleaned or on the first run.");
                data = new ApplicationVersionData();
            } else {
                FileInputStream fis = new FileInputStream(file);
                ois = new ObjectInputStream(fis);
                data = (ApplicationVersionData) ois.readObject();
            }
        } catch (Exception e) {
            log.error("Could not load persistent version data from disk for plugin [" + pluginName +
                "]. Application version values will be reset.", e);
            data = new ApplicationVersionData();
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    log.error("Error closing input stream for persistent version data for plugin [" + pluginName + "]",
                        e);
                }
            }
            dataLock.readLock().unlock();
        }
    }

    /**
     * Saves the current state of the application (package) versions to disk. The values will be saved scoped to the
     * plugin specified in the constructor. This method will absorb and log any errors that occur while saving;
     * this call represents a best-effort to save the values.
     */
    public void saveToDisk() {
        dataLock.writeLock().lock();

        if (data == null)
            throw new IllegalStateException("Data has not been loaded prior to saving for plugin [" + pluginName + "]");

        ObjectOutputStream oos = null;
        try {
            File file = new File(dataDirectory, FILENAME);
            FileOutputStream fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);

            oos.writeObject(data);
        } catch (Exception e) {
            log.error("Error saving persistent version data for plugin [" + pluginName +
                "]. Application versions may not be maintained between agent restarts.", e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    log.error("Error closing output stream for plugin [" + pluginName + "].", e);
                }
            }

            dataLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the version for the package identified by the supplied package key. This method will synchronize
     * against the data store, thus callers need not worry about handling calls across multiple discovery threads.
     *
     * @param packageKey identifies the package
     * 
     * @return version of the package if it is known; <code>null</code> otherwise
     */
    public String getVersion(String packageKey) {
        if (packageKey == null)
            throw new IllegalArgumentException("packageKey cannot be null");

        checkLoaded();

        String version;
        synchronized (data) {
            version = data.getVersion(packageKey);
        }

        return version;
    }

    /**
     * For internal use only - Removes the static data object to simulate the first load in the system. This should
     * not be called by production code and is only for testing purposes.
     */
    public void unload() {
        data = null;
    }

    /**
     * Updates the store with a new version for the package identified by the specified key. If there is an existing
     * version in the store, it will be overwritten. This method will synchronize on against the data store, thus
     * callers need not worry about handling calls across multiple deploy threads.
     *
     * @param packageKey identifies the package
     * @param version    version of the package
     */
    public void putVersion(String packageKey, String version) {
        if (packageKey == null)
            throw new IllegalArgumentException("packageKey cannot be null");

        checkLoaded();

        synchronized (data) {
            data.setVersion(packageKey, version);
        }
    }

    /**
     * Called by the get/set version methods to ensure the caller has properly initialized the data storage.
     */
    private void checkLoaded() {
        if (data == null)
            throw new IllegalStateException("Attempt to access package versions without loading the data. " +
                "Call loadFromDisk() before attempting these operations.");
    }
}
