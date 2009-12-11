/*
 * RHQ Management Platform
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

package org.rhq.core.domain.configuration;

import org.testng.annotations.Test;

public abstract class AbstractConfigurationUpdateTest {


    @Test
    public void serializationShouldCopyConfiguration() throws Exception {
        Configuration config = new Configuration();
        config.put(new PropertySimple("x", "1"));

        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate();
        update.setConfiguration(config);

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        org.testng.Assert.assertEquals(
            serializedUpdate.getConfiguration(),
            update.getConfiguration(),
            "Failed to properly serialize the 'configuration' property"
        );
    }

    @Test
    public void serializationShouldCopyErrorMessage() throws Exception {
        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate();
        update.setErrorMessage("update error");

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        org.testng.Assert.assertEquals(
            serializedUpdate.getErrorMessage(),
            update.getErrorMessage(),
            "Failed to properly serialize the 'errorMessage' property"
        );
    }

    @Test
    public void serializationShouldCopyStatus() throws Exception {
        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate();
        update.setStatus(ConfigurationUpdateStatus.SUCCESS);

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        org.testng.Assert.assertEquals(
            serializedUpdate.getStatus(),
            update.getStatus(),
            "Failed to properly serialize the 'status' property"
        );
    }

    @Test
    public void serializationShouldCopySubjectName() throws Exception {
        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate(null, new Configuration(), "rhqadmin");

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        org.testng.Assert.assertEquals(
            serializedUpdate.getSubjectName(),
            update.getSubjectName(),
            "Failed to properly serialize the 'subjectName' property"
        );
    }

    @Test
    public void serializationShouldCopyCreatedTime() throws Exception {
        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate();

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        org.testng.Assert.assertEquals(
            serializedUpdate.getCreatedTime(),
            update.getCreatedTime(),
            "Failed to properly serialize the 'createdTime' property"
        );
    }

    @Test
    public void serializationShouldCopyModifiedTime() throws Exception {
        ResourceConfigurationUpdate update = new ResourceConfigurationUpdate();

        ResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        org.testng.Assert.assertEquals(
            serializedUpdate.getModifiedTime(),
            update.getModifiedTime(),
            "Failed to properly serialize the 'modifiedTime' property"
        );
    }
}
