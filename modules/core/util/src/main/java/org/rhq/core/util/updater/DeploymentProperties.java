/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.util.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Provides properties that define metadata about a deployment.
 * Use the factory method {@link #loadFromFile(File)} to load deployment properties
 * files. Use {@link #saveToFile(File)} to store deployment properties.
 * 
 * @author John Mazzitelli
 */
public class DeploymentProperties extends Properties {
    private static final long serialVersionUID = 1L;

    // required properties that must always exist
    private static final String DEPLOYMENT_ID = "deployment.id";
    private static final String BUNDLE_NAME = "bundle.name";
    private static final String BUNDLE_VERSION = "bundle.version";
    private static final String BUNDLE_DESCRIPTION = "bundle.description";

    // optional properties
    private static final String MANAGE_ROOT_DIR = "manage.root.dir";

    public static DeploymentProperties loadFromFile(File file) throws Exception {
        DeploymentProperties props = new DeploymentProperties();
        FileInputStream is = new FileInputStream(file);
        try {
            props.load(is);
        } finally {
            is.close();
        }
        props.validate();
        return props;
    }

    /**
     * Creates an empty set of deployment properties. The caller must ensure valid
     * deployment properties are set later.
     */
    public DeploymentProperties() {
        super();
    }

    /**
     * Convenience constructor whose parameters are all the required values that
     * this object needs.
     * 
     * @param deploymentId see {@link #getDeploymentId()}
     * @param bundleName see {@link #getBundleName()}
     * @param bundleVersion see {@link #getBundleVersion()}
     * @param description see {@link #getDescription()}
     */
    public DeploymentProperties(int deploymentId, String bundleName, String bundleVersion, String description) {
        super();
        setDeploymentId(deploymentId);
        setBundleName(bundleName);
        setBundleVersion(bundleVersion);
        setDescription(description);
        try {
            validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns <code>true</code> if this object has everything required to define a valid deployment.
     * 
     * @return true if this is valid
     */
    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void saveToFile(File file) throws Exception {
        validate(); // makes sure we never save invaild properties

        FileOutputStream os = new FileOutputStream(file);
        try {
            store(os, "This file is auto-generated - DO NOT MODIFY!!!");
        } finally {
            os.close();
        }
        return;
    }

    private void validate() throws Exception {
        // all the getters thrown runtime exceptions if the values are not valid, so
        // just call them all and catch exceptions if they throw them
        try {
            getDeploymentId();
            getBundleName();
            getBundleVersion();
            getDescription();
        } catch (Exception e) {
            throw new Exception("Deployment properties are invalid: " + e.getMessage());
        }
    }

    /**
     * This returns a deployment ID that identifies a known deployment.
     * If the deployment is not yet known (that is, its going to be a new
     * deployment added to the system), this will typically return 0.
     *
     * @return an identifier that uniquely identifies this particular deployment.
     */
    public int getDeploymentId() {
        String str = getProperty(DEPLOYMENT_ID);
        if (str == null) {
            throw new IllegalStateException("There is no deployment ID");
        }

        try {
            int id = Integer.parseInt(str);
            return id;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid deployment ID: " + str);
        }
    }

    public void setDeploymentId(int id) {
        setProperty(DEPLOYMENT_ID, Integer.toString(id));
    }

    /**
     * @return the name of the bundle for this deployment
     */
    public String getBundleName() {
        String str = getProperty(BUNDLE_NAME);
        if (str == null) {
            throw new IllegalStateException("There is no bundle name");
        }
        return str;
    }

    public void setBundleName(String name) {
        setProperty(BUNDLE_NAME, name);
    }

    /**
     * @return the version of the bundle for this deployment
     */
    public String getBundleVersion() {
        String str = getProperty(BUNDLE_VERSION);
        if (str == null) {
            throw new IllegalStateException("There is no bundle version");
        }
        return str;
    }

    public void setBundleVersion(String version) {
        setProperty(BUNDLE_VERSION, version);
    }

    /**
     * @return the description of this deployment
     */
    public String getDescription() {
        String str = getProperty(BUNDLE_DESCRIPTION);
        return str;
    }

    public void setDescription(String description) {
        if (description == null) {
            remove(BUNDLE_DESCRIPTION);
        } else {
            setProperty(BUNDLE_DESCRIPTION, description);
        }
    }

    /**
     * @return the flag to indicate if the entire root directory content is to be managed.
     *         If there is no property, this method returns a default of <code>true</code>
     */
    public boolean getManageRootDir() {
        String str = getProperty(MANAGE_ROOT_DIR);
        if (str == null) {
            return true;
        }
        return Boolean.parseBoolean(str);
    }

    public void setManageRootDir(boolean willManageRootDir) {
        setProperty(MANAGE_ROOT_DIR, Boolean.toString(willManageRootDir));
    }
}
