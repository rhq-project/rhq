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
package org.rhq.enterprise.server.core.concurrency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.enterprise.server.core.concurrency.LatchedServiceController.LatchedService;

@Test
public class LatchedServiceDependenciesTest {

    public void testDependencies() throws Exception {
        // 1 is a plugin that is a dependency of 2
        // 2 can't start because 1 takes up the entire thread pool
        // Make sure this doesn't deadlock - 1 needs to be able to inform 2 that it is done,
        // even though 2 isn't yet started in a thread
        Collection<DummyLatchedService> dummies = getDummiesFromDependencyGraph("1,2-1");
        LatchedServiceController controller = new LatchedServiceController(dummies);
        controller.setThreadPoolSize(1);
        controller.executeServices();
    }

    public void testDependencies2() throws Exception {
        Collection<DummyLatchedService> dummies = getDummiesFromDependencyGraph("1,6-1,2,7-2,3,8-3,4,9-4,5,10-5");
        LatchedServiceController controller = new LatchedServiceController(dummies);
        controller.setThreadPoolSize(1);
        controller.executeServices();
    }

    public void testDependencies3() throws Exception {
        Collection<DummyLatchedService> dummies = getDummiesFromDependencyGraph("1,6-1,2,7-2,3,8-3,4,9-4,5,10-5");
        LatchedServiceController controller = new LatchedServiceController(dummies);
        controller.setThreadPoolSize(5);
        controller.executeServices();
    }

    private class DummyLatchedService extends LatchedService {
        public DummyLatchedService(String name) {
            super(name);
        }

        @Override
        public void executeService() throws LatchedServiceException {
            return;
        }
    }

    private Collection<DummyLatchedService> getDummiesFromDependencyGraph(String dependencyGraph) {
        Map<String, DummyLatchedService> knownServices = new HashMap<String, DummyLatchedService>();
        List<DummyLatchedService> orderedServices = new ArrayList<DummyLatchedService>();

        String[] deps = dependencyGraph.replaceAll(" ", "").split(",");
        for (String dep : deps) {
            String[] parts = dep.split("-");

            DummyLatchedService service = getDummyServiceByName(parts[0], knownServices);
            if (parts.length > 1) {
                DummyLatchedService dependency = getDummyServiceByName(parts[1], knownServices);
                service.addDependency(dependency);
            }
            orderedServices.add(service);
        }

        return orderedServices;
    }

    private DummyLatchedService getDummyServiceByName(String name, Map<String, DummyLatchedService> dummies) {
        DummyLatchedService result = dummies.get(name);
        if (result == null) {
            result = new DummyLatchedService(name);
            dummies.put(name, result);
        }
        return result;
    }
}
