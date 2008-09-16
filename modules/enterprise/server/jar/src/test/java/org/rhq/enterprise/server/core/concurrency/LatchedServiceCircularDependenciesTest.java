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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.enterprise.server.core.concurrency.LatchedServiceController.LatchedService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

/**
 * @author Joseph Marques
 */
@Test
public class LatchedServiceCircularDependenciesTest extends AbstractEJB3Test {

    private class DummyLatchedService extends LatchedService {

        public DummyLatchedService(String name) {
            super(name);
        }

        @Override
        public void executeService() throws LatchedServiceException {
            return; // no-op, we only care about testing the circular dependency checker 
        }

    }

    private Collection<DummyLatchedService> getDummiesFromDependencyGraph(String dependencyGraph) {
        Map<String, DummyLatchedService> knownServices = new HashMap<String, DummyLatchedService>();

        String[] deps = dependencyGraph.replaceAll(" ", "").split(",");
        for (String dep : deps) {
            String[] parts = dep.split("-");

            DummyLatchedService service = getDummyServiceByName(parts[0], knownServices);
            DummyLatchedService dependency = getDummyServiceByName(parts[1], knownServices);

            service.addDependency(dependency);
        }

        Collection<DummyLatchedService> dummies = knownServices.values();
        return dummies;
    }

    private DummyLatchedService getDummyServiceByName(String name, Map<String, DummyLatchedService> dummies) {
        DummyLatchedService result = dummies.get(name);
        if (result == null) {
            result = new DummyLatchedService(name);
            dummies.put(name, result);
        }
        return result;
    }

    @Test
    public void testLongCircularDependencyGraph() {
        testCircularDependency("1-2, 2-3, 3-4, 4-5, 5-1");
    }

    @Test
    public void testShortCircularDependencyGraph() {
        testCircularDependency("1-2, 2-3, 2-4, 2-5, 5-1");
    }

    @Test
    public void testTwoElementCircularDependencyGraph() {
        testCircularDependency("1-2, 2-1");
    }

    @Test
    public void testSelfCircularDependencyGraph() {
        testCircularDependency("1-1");
    }

    private void testCircularDependency(String dependencyGraph) {
        Collection<DummyLatchedService> dummies = getDummiesFromDependencyGraph(dependencyGraph);
        LatchedServiceController controller = new LatchedServiceController(dummies);
        try {
            controller.executeServices();
            assert false : "Should have recieved a LatchedServiceCircularityException, but didn't";
        } catch (LatchedServiceCircularityException lsce) {
            assert true;
        }
    }

}
