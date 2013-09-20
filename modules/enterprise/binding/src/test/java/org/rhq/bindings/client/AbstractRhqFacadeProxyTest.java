/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.bindings.client;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.bindings.util.InterfaceSimplifier;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * @author Lukas Krejci
 */
@Test
public class AbstractRhqFacadeProxyTest {

    public interface TestInterface {
        void method();
    }

    public static class TestFacade extends AbstractRhqFacade {

        private Subject subject;

        @Override
        public Subject getSubject() {
            return subject;
        }

        public void setSubject(Subject subject) {
            this.subject = subject;
        }

        @Override
        public Subject login(String user, String password) throws Exception {
            return subject;
        }

        @Override
        public void logout() {
            subject = null;
        }

        @Override
        public boolean isLoggedIn() {
            return subject != null;
        }

        @Override
        public Map<RhqManager, Object> getScriptingAPI() {
            EnumMap<RhqManager, Object> ret = new EnumMap<RhqManager, Object>(RhqManager.class);

            for (RhqManager m : RhqManager.values()) {
                Class<?> iface = InterfaceSimplifier.simplify(m.remote());
                Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] { iface },
                    new TestProxy(this, m));
                ret.put(m, proxy);
            }

            return ret;
        }

        @Override
        public <T> T getProxy(Class<T> remoteApiIface) {
            RhqManager m = RhqManager.forInterface(remoteApiIface);
            if (m == null) {
                throw new IllegalArgumentException();
            }

            return remoteApiIface.cast(Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class<?>[] { remoteApiIface }, new TestProxy(this, m)));
        }

    }

    public static class InvocationRecord {
        public Method method;
        public Object[] args;
    }

    public static class TestProxy extends AbstractRhqFacadeProxy<TestFacade> {

        private static List<InvocationRecord> pastInvocations = new ArrayList<InvocationRecord>();

        /**
         * @param facade
         * @param manager
         */
        public TestProxy(TestFacade facade, RhqManager manager) {
            super(facade, manager);
        }

        @Override
        protected Object doInvoke(Object proxy, Method originalMethod, Object[] args) throws Throwable {
            InvocationRecord inv = new InvocationRecord();
            inv.method = originalMethod;
            inv.args = args;

            pastInvocations.add(inv);

            return null;
        }

        public static List<InvocationRecord> getPastInvocations() {
            return pastInvocations;
        }

        public static void clearPastInvocations() {
            pastInvocations.clear();
        }
    }

    public void testInvocationOfSimplifiedMethods() throws Exception {
        TestProxy.clearPastInvocations();

        TestFacade facade = new TestFacade();
        Subject subject = new Subject();
        
        facade.setSubject(subject);
        
        Object resourceManager = facade.getScriptingAPI().get(RhqManager.ResourceManager);
        
        Method getResource = resourceManager.getClass().getMethod("getResource", int.class);

        getResource.invoke(resourceManager, 1);

        Assert.assertEquals(TestProxy.getPastInvocations().size(), 1, "Unexpected number of proxy invocations");

        InvocationRecord inv = TestProxy.getPastInvocations().get(0);

        Assert.assertEquals(inv.method, ResourceManagerRemote.class.getMethod("getResource", Subject.class, int.class),
            "Unexpected method invoked.");

        Assert.assertEquals(subject, inv.args[0], "Unexpected subject passed to the invocation.");
        Assert.assertEquals(inv.args[1], new Integer(1), "Unexpected resource id passed to the invocation.");
    }

    public void testProxyRobustAgainstNonSimplifiedMethods() throws Exception {
        TestProxy.clearPastInvocations();

        Class<?> iface = InterfaceSimplifier.simplify(TestInterface.class);

        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { iface },
            new TestProxy(null, null));

        Method charAt = proxy.getClass().getMethod("method");

        charAt.invoke(proxy);

        Assert.assertEquals(TestProxy.getPastInvocations().size(), 1, "Unexpected number of proxy invocations");

        InvocationRecord inv = TestProxy.getPastInvocations().get(0);

        Assert.assertEquals(inv.method, TestInterface.class.getMethod("method"),
            "Unexpected method invoked.");

        Assert.assertNull(inv.args, "Unexpected number of arguments passed to the invocation.");
    }
}
