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

    private static final String DEPLOYMENT_ID = "deployment.id";
    private static final String BUNDLE_NAME = "bundle.name";
    private static final String BUNDLE_VERSION = "bundle.version";

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
        } catch (Exception e) {
            throw new Exception("Deployment properties are invalid: " + e.getMessage());
        }
    }

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
}
