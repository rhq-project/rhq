/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.arquillian;

import org.mockito.Mockito;

import org.rhq.core.clientapi.server.bundle.BundleServerService;
import org.rhq.core.clientapi.server.configuration.ConfigurationServerService;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.core.CoreServerService;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.drift.DriftServerService;
import org.rhq.core.clientapi.server.event.EventServerService;
import org.rhq.core.clientapi.server.inventory.ResourceFactoryServerService;
import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.clientapi.server.operation.OperationServerService;
import org.rhq.core.pc.ServerServices;

/**
 * An example server service extension that sets up the various server services
 * using mockito mocks.
 *
 * @author Lukas Krejci
 */
public class MockingServerServices extends ServerServices {

    public MockingServerServices() {
        setBundleServerService(Mockito.mock(BundleServerService.class));
        setConfigurationServerService(Mockito.mock(ConfigurationServerService.class));
        setContentServerService(Mockito.mock(ContentServerService.class));
        setCoreServerService(Mockito.mock(CoreServerService.class));
        setDiscoveryServerService(Mockito.mock(DiscoveryServerService.class));
        setDriftServerService(Mockito.mock(DriftServerService.class));
        setEventServerService(Mockito.mock(EventServerService.class));
        setMeasurementServerService(Mockito.mock(MeasurementServerService.class));
        setOperationServerService(Mockito.mock(OperationServerService.class));
        setResourceFactoryServerService(Mockito.mock(ResourceFactoryServerService.class));
    }
}
