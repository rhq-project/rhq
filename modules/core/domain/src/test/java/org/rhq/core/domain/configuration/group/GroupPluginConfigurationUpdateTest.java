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

import org.rhq.core.domain.configuration.AbstractConfigurationUpdateTest;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.TestUtil;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

public class GroupPluginConfigurationUpdateTest extends AbstractConfigurationUpdateTest {

    @Test
    public void serializationShouldCopyConfigurationUpdates() throws Exception {
        GroupPluginConfigurationUpdate update = new GroupPluginConfigurationUpdate();
        update.getConfigurationUpdates().add(new PluginConfigurationUpdate(new Resource(), new Configuration(),
            "rhqadmin"));

        GroupPluginConfigurationUpdate serializedUpdate = TestUtil.serializeAndDeserialize(update);

        assertEquals(
            serializedUpdate.getConfigurationUpdates(),
            update.getConfigurationUpdates(),
            "Failed to properly serialize the 'configurationUpdate' property"
        );
    }
}
