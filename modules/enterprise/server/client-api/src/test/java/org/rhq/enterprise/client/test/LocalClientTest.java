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

package org.rhq.enterprise.client.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.jmock.Expectations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.LocalClient;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.test.JMockTest;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class LocalClientTest extends JMockTest {

    public static class FakeContextFactory implements InitialContextFactory {
        @Override
        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            return CONTEXT_MOCK_FOR_TEST;
        }
    }
    
    public static Context CONTEXT_MOCK_FOR_TEST = null;
    
    @BeforeClass
    public void setUpNaming() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, FakeContextFactory.class.getName());
    }
    
    @Test
    public void testResilienceAgainstContextClassloaders() throws Exception {
        CONTEXT_MOCK_FOR_TEST = context.mock(Context.class);
        final AlertManagerRemote alertManagerMock = (AlertManagerRemote) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { AlertManagerRemote.class, AlertManagerLocal.class }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
            
        });
        
        context.checking(new Expectations() {{
            allowing(CONTEXT_MOCK_FOR_TEST).lookup(with(any(String.class)));
            will(returnValue(alertManagerMock));
            
            allowing(CONTEXT_MOCK_FOR_TEST).close();
        }});
        
        ClassLoader origCl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader differentCl = new URLClassLoader(new URL[0], getClass().getClassLoader());
            
            Thread.currentThread().setContextClassLoader(differentCl);

            LocalClient lc = new LocalClient(null);
            
            //this call creates the proxy and is theoretically prone to the context classloader
            Object am = lc.getScriptingAPI().get("AlertManager");

            //check that both the original and simplified methods exist on the returned object
            am.getClass().getMethod("deleteAlerts", new Class<?>[] { Subject.class, int[].class });            
            am.getClass().getMethod("deleteAlerts", new Class<?>[] { int[].class });
        } finally {
            Thread.currentThread().setContextClassLoader(origCl);
        }
    }
}
