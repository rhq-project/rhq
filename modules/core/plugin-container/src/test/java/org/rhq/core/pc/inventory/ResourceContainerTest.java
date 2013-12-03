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
    }

    @AfterClass
    public void afterClass() {
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
            FacetLockType.WRITE, SECONDS.toMillis(1), true, false, true);
        try {
            proxy.invokeOperation("op", new Configuration());
            fail("Expected invokeOperation to throw a TimeoutException");
        } catch (TimeoutException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Caught expected TimeoutException: " + e.getMessage());
            }
            MockResourceComponent component = (MockResourceComponent) resourceContainer.getResourceComponent();
            for (int i = 0; i < 5 && !component.caughtInterruptedComponentInvocation(); i++) {
                Thread.sleep(SECONDS.toMillis(2));
            }
            assertTrue(component.caughtInterruptedComponentInvocation());
        }
    }

    public void testUninterruptedComponentInvocationContext() throws Exception {
        ResourceContainer resourceContainer = getResourceContainer();
        OperationFacet proxy = resourceContainer.createResourceComponentProxy(OperationFacet.class,
            FacetLockType.WRITE, SECONDS.toMillis(10), true, false, true);
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
        volatile boolean caughtInterruptedComponentInvocation;
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
            caughtInterruptedComponentInvocation = false;
            sleep(SECONDS.toMillis(5));
            if (resourceContext.getComponentInvocationContext().isInterrupted()) {
                caughtInterruptedComponentInvocation = true;
                throw new InterruptedException();
            }
            return new OperationResult(OPERATION_RESULT);
        }

        private void sleep(long millis) {
            // Do not return before 'millis' have elapsed, even if the thread is interrupted
            long start = System.currentTimeMillis();
            for (long stop = System.currentTimeMillis(); stop - start < millis; stop = System.currentTimeMillis()) {
                try {
                    Thread.sleep(millis - (stop - start));
                } catch (InterruptedException e) {
                }
            }
        }

        boolean caughtInterruptedComponentInvocation() {
            return caughtInterruptedComponentInvocation;
        }
    }

    class MockRuntimeException extends RuntimeException {
    }
}
