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
        ResourceComponent resourceComponent = new MockResourceComponent(false);
        resourceContainer.setResourceComponent(resourceComponent);
        System.out.println("Testing proxy call that should time out...");
        ResourceComponent resourceComponentProxy = resourceContainer.createResourceComponentProxy(ResourceComponent.class, FacetLockType.NONE, 50, true, false);
        try {
            resourceComponentProxy.getAvailability();
            assert (false);
        }
        catch (RuntimeException e) {
            assert (e instanceof TimeoutException);
        }
        System.out.println("SUCCESS!");
        System.out.println("Testing proxy call that should complete successfully...");
        resourceComponentProxy = resourceContainer.createResourceComponentProxy(ResourceComponent.class, FacetLockType.NONE, 150, true, false);
        AvailabilityType avail = resourceComponentProxy.getAvailability();
        assert (avail == AvailabilityType.UP);
        System.out.println("SUCCESS!");
        System.out.println("Testing proxy call that should fail...");
        ResourceComponent naughtyResourceComponent = new MockResourceComponent(true);
        resourceContainer.setResourceComponent(naughtyResourceComponent);        
        resourceComponentProxy = resourceContainer.createResourceComponentProxy(ResourceComponent.class, FacetLockType.NONE, 0, true, false);
        try {
            resourceComponentProxy.getAvailability();
            assert (false);
        }
        catch (RuntimeException e) {
            assert (e instanceof MockRuntimeException);
        }
        System.out.println("SUCCESS!");
        // TODO: Test proxy locking.
    }

    class MockResourceComponent implements ResourceComponent
    {
        private boolean naughty;

        MockResourceComponent(boolean naughty) {
            this.naughty = naughty;
        }

        public void start(ResourceContext resourceContext) throws Exception {
        }

        public void stop() {
        }

        public AvailabilityType getAvailability() {
            if (this.naughty) {
                throw new MockRuntimeException();
            }
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                return AvailabilityType.DOWN;
            }
            return AvailabilityType.UP;
        }
    }

    class MockRuntimeException extends RuntimeException {
    }
}
