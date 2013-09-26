/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.inventory;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.component.ComponentInvocationContextImpl;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Unit test for {@link ResourceContainer}.
 *
 * @author Ian Springer
 */
@Test
public class ResourceContainerTest {
    private static final Log LOG = LogFactory.getLog(ResourceContainerTest.class);

    @BeforeClass
    public void beforeClass() {
        PluginContainerConfiguration config = new PluginContainerConfiguration();
        File dataDir = new File("target/PluginContainerTest");
        dataDir.mkdirs();
        config.setDataDirectory(dataDir);
        PluginContainer pc = PluginContainer.getInstance();
        pc.setConfiguration(config);
        pc.initialize();
        ResourceContainer.initialize();
    }

    @AfterClass
    public void afterClass() {
        ResourceContainer.shutdown();
        PluginContainer.getInstance().shutdown();
    }

    public void testCreateResourceComponentProxy() throws Exception {
        ResourceContainer resourceContainer = getResourceContainer();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Testing proxy call that should timeout...");
        }
        AvailabilityFacet resourceComponentProxy = resourceContainer.createResourceComponentProxy(
            AvailabilityFacet.class, FacetLockType.NONE, 50, true, false, true);
        try {
            resourceComponentProxy.getAvailability();
            assert (false);
        } catch (RuntimeException e) {
            assert (e instanceof TimeoutException) : "[" + e + "] is not a org.rhq.core.pc.inventory.TimeoutException.";
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("SUCCESS!");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Testing proxy call that should complete successfully...");
        }
        resourceComponentProxy = resourceContainer.createResourceComponentProxy(AvailabilityFacet.class,
            FacetLockType.NONE, 150, true, false, true);
        AvailabilityType avail = resourceComponentProxy.getAvailability();
        assert (avail == AvailabilityType.UP);
        if (LOG.isDebugEnabled()) {
            LOG.debug("SUCCESS!");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Testing proxy call that should fail...");
        }
        ResourceComponent naughtyResourceComponent = new MockResourceComponent(true);
        resourceContainer.setResourceComponent(naughtyResourceComponent);
        resourceComponentProxy = resourceContainer.createResourceComponentProxy(AvailabilityFacet.class,
            FacetLockType.NONE, Long.MAX_VALUE, true, false, true);
        try {
            resourceComponentProxy.getAvailability();
            assert (false);
        } catch (RuntimeException e) {
            assert (e instanceof MockRuntimeException);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("SUCCESS!");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Testing proxy call should not timeout; its not to a declared method in proxied interface");
        }
        resourceComponentProxy = resourceContainer.createResourceComponentProxy(AvailabilityFacet.class,
            FacetLockType.NONE, 50, true, false, true);
        String string = resourceComponentProxy.toString();
        assert (string.equals(MockResourceComponent.class.toString()));
        if (LOG.isDebugEnabled()) {
            LOG.debug("SUCCESS!");
        }
        // TODO: Test proxy locking.
    }

    public void testInterruptedComponentInvocationContext() throws Exception {
        ResourceContainer resourceContainer = getResourceContainer();
        OperationFacet proxy = resourceContainer.createResourceComponentProxy(OperationFacet.class,
                FacetLockType.WRITE, 50, true, false, true);
        try {
            proxy.invokeOperation("op", new Configuration());
            fail("Expected invokeOperation to throw a TimeoutException");
        } catch (TimeoutException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Caught expected TimeoutException: " + e.getMessage());
            }
            assertTrue(((MockResourceComponent) resourceContainer.getResourceComponent())
                    .caughtInterruptedComponentInvocation());
        }
    }

    public void testUninterruptedComponentInvocationContext() throws Exception {
        ResourceContainer resourceContainer = getResourceContainer();
        OperationFacet proxy = resourceContainer.createResourceComponentProxy(OperationFacet.class,
                FacetLockType.WRITE, SECONDS.toMillis(2), true, false, true);
        try {
            OperationResult op = proxy.invokeOperation("op", new Configuration());
            assertTrue(op.getSimpleResult().equals(MockResourceComponent.OPERATION_RESULT));
        } catch (Exception e) {
            fail("Caught unexpected Exception: " + e.getClass().getName());
            assertFalse(((MockResourceComponent) resourceContainer.getResourceComponent())
                    .caughtInterruptedComponentInvocation());
        }
    }

    private ResourceContainer getResourceContainer() throws Exception {
        Resource resource = new Resource("TestPlatformKey", "MyTestPlatform", PluginMetadataManager.TEST_PLATFORM_TYPE);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ResourceContainer resourceContainer = new ResourceContainer(resource, contextClassLoader);
        ResourceContext resourceContext = new ResourceContext(resource, null, null, null, null, null, null, null, null,
                null, null, null, null, null, new ComponentInvocationContextImpl());
        resourceContainer.setResourceContext(resourceContext);
        ResourceComponent resourceComponent = new MockResourceComponent(false);
        resourceContainer.setResourceComponent(resourceComponent);
        resourceComponent.start(resourceContext);
        return resourceContainer;
    }

    private class MockResourceComponent implements ResourceComponent, OperationFacet {
        static final String OPERATION_RESULT = "uninterrupted";
        boolean naughty;
        boolean caughtInterruptedComponentInvocation;
        ResourceContext resourceContext;

        MockResourceComponent(boolean naughty) {
            this.naughty = naughty;
        }

        @Override
        public void start(ResourceContext resourceContext) throws Exception {
            this.resourceContext = resourceContext;
        }

        @Override
        public void stop() {
            this.resourceContext = null;
        }

        @Override
        public AvailabilityType getAvailability() {
            if (this.naughty) {
                throw new MockRuntimeException();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                return AvailabilityType.DOWN;
            }
            return AvailabilityType.UP;
        }

        @Override
        public String toString() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return this.getClass().toString();
        }

        @Override
        public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
            long start = System.nanoTime();
            while (!resourceContext.getComponentInvocationContext().isInterrupted()) {
                // Return after 1s
                if ((System.nanoTime() - start) > SECONDS.toNanos(1)) {
                    caughtInterruptedComponentInvocation = false;
                    return new OperationResult(OPERATION_RESULT);
                }
            }
            caughtInterruptedComponentInvocation = true;
            throw new InterruptedException();
        }

        boolean caughtInterruptedComponentInvocation() {
            return caughtInterruptedComponentInvocation;
        }
    }

    class MockRuntimeException extends RuntimeException {
    }
}
