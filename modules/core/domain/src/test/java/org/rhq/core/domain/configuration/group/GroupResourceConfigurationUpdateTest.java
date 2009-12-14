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

package org.rhq.core.domain.configuration.group;

import static org.testng.Assert.*;

import org.rhq.core.domain.configuration.AbstractConfigurationUpdateTest;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.TestUtil;
import org.rhq.core.domain.resource.Resource;
import org.testng.annotations.Test;

public class GroupResourceConfigurationUpdateTest extends AbstractConfigurationUpdateTest {

    @Test
    public void serializationShouldCopyConfigurationUpdates() throws Exception {
        GroupResourceConfigurationUpdate update = new GroupResourceConfigurationUpdate();
        update.addConfigurationUpdate(new ResourceConfigurationUpdate(new Resource(), new Configuration(), "rhqadmin"));

        GroupResourceConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        assertEquals(
            serializedUpdate.getConfigurationUpdates(),
            update.getConfigurationUpdates(),
            "Failed to properly serialize the 'configurationUpdate' property"
        );
    }

}
