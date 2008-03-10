/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc.inventory;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pc.util.FacetLockType;

/**
 * Unit test for {@link ResourceContainer}.
 *
 * @author Ian Springer
 */
@Test
public class ResourceContainerTest {
    public void testCreateResourceComponentProxy() throws Exception {
        Resource resource = new Resource();
        ResourceContainer resourceContainer = new ResourceContainer(resource);
        ResourceComponent resourceComponent = new MockResourceComponent();
        resourceContainer.setResourceComponent(resourceComponent);
        ResourceComponent resourceComponentProxy = resourceContainer.createResourceComponentProxy(ResourceComponent.class, FacetLockType.NONE, 50, false);
        assert resourceComponentProxy != null;
        try {
            resourceComponentProxy.getAvailability();
            assert (false);
        }
        catch (RuntimeException e) {
            assert (e instanceof TimeoutException);
        }
        resourceComponentProxy = resourceContainer.createResourceComponentProxy(ResourceComponent.class, FacetLockType.NONE, 150, false);
        AvailabilityType avail = resourceComponentProxy.getAvailability();
        assert (avail == AvailabilityType.UP);                         
    }

    class MockResourceComponent implements ResourceComponent
    {
        public void start(ResourceContext resourceContext) throws Exception {
        }

        public void stop() {
        }

        public AvailabilityType getAvailability() {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                return AvailabilityType.DOWN;
            }
            return AvailabilityType.UP;
        }
    }
}
