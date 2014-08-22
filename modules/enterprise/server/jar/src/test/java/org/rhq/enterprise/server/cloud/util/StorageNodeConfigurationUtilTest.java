/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.cloud.util;

import static org.testng.Assert.*;

import org.mockito.Mockito;
import org.testng.annotations.Test;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;

/**
 * @author Michael Burman
 */
public class StorageNodeConfigurationUtilTest {

    @Test
    public void testConfigurationSync() throws Exception {
        StorageNode mockStorageNode = Mockito.mock(StorageNode.class);
        StorageNodeConfigurationComposite oldConfig = new StorageNodeConfigurationComposite(mockStorageNode);
        StorageNodeConfigurationComposite newConfig = new StorageNodeConfigurationComposite(mockStorageNode);

        // These should be copied to newConfig
        String oldLogLocationValue = "oldLogLocation";
//        int oldJmxPort = 123;
        oldConfig.setCommitLogLocation(oldLogLocationValue); // Test object copy
//        oldConfig.setJmxPort(oldJmxPort); // Test primitive copy, but actually don't test it.

        // This should stay null in oldConfig and be left alone in newConfig
        String heapNewSize = "384m";
        newConfig.setHeapNewSize(heapNewSize);

        // These should not be touched
        String newCacheLocationValue = "newCacheLocation";
        String oldCacheLocationValue = "oldCacheLocation";
        newConfig.setSavedCachesLocation(newCacheLocationValue);
        oldConfig.setSavedCachesLocation(oldCacheLocationValue);

        StorageNodeConfigurationUtil.syncConfigs(newConfig, oldConfig);

        assertEquals(newConfig.getCommitLogLocation(), oldLogLocationValue);
//        assertEquals(newConfig.getJmxPort(), oldJmxPort);

        assertEquals(newConfig.getHeapNewSize(), heapNewSize);
        assertNull(oldConfig.getHeapNewSize());

        assertEquals(newConfig.getSavedCachesLocation(), newCacheLocationValue);
        assertEquals(oldConfig.getSavedCachesLocation(), oldCacheLocationValue);

        // TODO: Add test to check against StorageNodeReplacement
    }
}
