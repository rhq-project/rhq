/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.util.updater;

import java.io.File;

import org.testng.annotations.Test;

@Test
public class DeploymentPropertiesTest {
    public void testSaveLoadFile() throws Exception {
        DeploymentProperties props = new DeploymentProperties();
        props.setDeploymentId(12345);
        props.setBundleName("test-bundle-name");
        props.setBundleVersion("1.0");
        props.setDescription("This is a description\nof a bundle");
        File tmpFile = File.createTempFile("deploymentPropertiesTest", ".properties");
        try {
            props.saveToFile(tmpFile);
            DeploymentProperties propsDup = DeploymentProperties.loadFromFile(tmpFile);
            assertSameProperties(props, propsDup);
            assert propsDup.getDescription().equals("This is a description\nof a bundle"); // sanity check, make sure newline is OK
        } finally {
            tmpFile.delete();
        }
    }

    public void testValidate() throws Exception {
        DeploymentProperties props = new DeploymentProperties();

        try {
            props.getDeploymentId();
            assert false : "Should have thrown an exception since there is no deployment ID";
        } catch (Exception ok) {
            // this is expected and ok
        }

        try {
            props.getBundleName();
            assert false : "Should have thrown an exception since there is no bundle name";
        } catch (Exception ok) {
            // this is expected and ok
        }

        try {
            props.getBundleVersion();
            assert false : "Should have thrown an exception since there is no bundle version";
        } catch (Exception ok) {
            // this is expected and ok
        }

        try {
            props.setBundleName(null);
            assert false : "Should have thrown an exception due to null value";
        } catch (Exception ok) {
            // this is expected and ok
        }

        try {
            props.setBundleVersion(null);
            assert false : "Should have thrown an exception due to null value";
        } catch (Exception ok) {
            // this is expected and ok
        }

        File tmpFile = File.createTempFile("deploymentPropertiesTest", ".properties");
        try {
            try {
                props.saveToFile(tmpFile);
                assert false : "Should have thrown an exception since it was not valid";
            } catch (Exception ok) {
                // this is expected and ok
            }

            props.setDeploymentId(12345);
            try {
                props.saveToFile(tmpFile);
                assert false : "Should have thrown an exception since it was not valid";
            } catch (Exception ok) {
                // this is expected and ok
            }

            props.setBundleName("a");
            try {
                props.saveToFile(tmpFile);
                assert false : "Should have thrown an exception since it was not valid";
            } catch (Exception ok) {
                // this is expected and ok
            }

            props.setBundleVersion("1");

            // we set all properties we need, it should be valid and we should be able to save it now
            props.saveToFile(tmpFile);
            DeploymentProperties propsDup = DeploymentProperties.loadFromFile(tmpFile);
            assert propsDup.equals(props) : props + "!=" + propsDup;
        } finally {
            tmpFile.delete();
        }
    }

    private void assertSameProperties(DeploymentProperties props1, DeploymentProperties props2) {
        assert props2.equals(props1) : props1 + "!=" + props2;
        assert props2.getDeploymentId() == props1.getDeploymentId() : props1 + "!=" + props2;
        assert props2.getBundleName().equals(props1.getBundleName()) : props1 + "!=" + props2;
        assert props2.getBundleVersion().equals(props1.getBundleVersion()) : props1 + "!=" + props2;
        assert props2.getDescription().equals(props1.getDescription()) : props1 + "!=" + props2;
        assert props2.size() == props1.size() : props1 + " is not same size as " + props2;
    }

}
