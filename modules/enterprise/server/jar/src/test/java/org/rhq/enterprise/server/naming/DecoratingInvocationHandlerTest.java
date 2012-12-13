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

package org.rhq.enterprise.server.naming;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class DecoratingInvocationHandlerTest {
    private static final Set<String> INVOKED_METHODS = new HashSet<String>();
    
    private static final InvocationHandler NOTE_TAKING_HANDLER = new InvocationHandler() {
        
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            INVOKED_METHODS.add(method.getName());
            
            if ("hashCode".equals(method.getName())) {
                return 0;
            } else if ("equals".equals(method.getName())) {
                return false;
            }
            
            return null;
        }
    };
    
    private static Class<?>[] CONTEXT_INTERFACES;
    
    public static class Factory implements InitialContextFactory {
        public Context getInitialContext(Hashtable<?, ?> environment)
            throws NamingException {
            
            return (Context) Proxy.newProxyInstance(DecoratingInvocationHandlerTest.class.getClassLoader(), CONTEXT_INTERFACES, NOTE_TAKING_HANDLER);
        }
    }
    
    private static class DummyInitialEventContext extends InitialContext implements EventContext {

        /**
         * @param environment
         * @throws NamingException
         */
        public DummyInitialEventContext(Hashtable<?, ?> environment) throws NamingException {
            super(environment);
        }

        public void addNamingListener(Name target, int scope, NamingListener l) throws NamingException {
            ((EventContext)getURLOrDefaultInitCtx(target)).addNamingListener(target, scope, l);
        }

        public void addNamingListener(String target, int scope, NamingListener l) throws NamingException {
            ((EventContext)getURLOrDefaultInitCtx(target)).addNamingListener(target, scope, l);
        }

        public void removeNamingListener(NamingListener l) throws NamingException {
            ((EventContext)getDefaultInitCtx()).removeNamingListener(l);
        }

        public boolean targetMustExist() throws NamingException {
            return ((EventContext)getDefaultInitCtx()).targetMustExist();
        }
        
        
    }
    
    @BeforeClass
    public void setBuilder() throws Exception {
        NamingManager.setInitialContextFactoryBuilder(new AccessCheckingInitialContextFactoryBuilder(
            new org.jboss.as.naming.InitialContextFactory(), false));
    }
    
    public void testSimpleDispatch() throws Exception {
        INVOKED_METHODS.clear();
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, Factory.class.getName());
        
        CONTEXT_INTERFACES = new Class<?>[] { Context.class };
        
        InitialContext ctx = new InitialContext(env);
        
        ctx.lookup("asdf");
        
        assert INVOKED_METHODS.contains("lookup") : "The lookup doesn't seem to have propagated to the actual context to be used.";
    }      
    
    public void testMultiInterfaceDispatch() throws Exception {
        INVOKED_METHODS.clear();
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, Factory.class.getName());
        
        CONTEXT_INTERFACES = new Class<?>[] { EventContext.class, DirContext.class };
        
        InitialContext ctx = new InitialContext(env);
        
        ctx.lookup("asdf");
        
        DummyInitialEventContext ectx = new DummyInitialEventContext(env);
        
        ectx.addNamingListener("hodiny", 0, null);
        
        assert INVOKED_METHODS.contains("lookup") : "The lookup doesn't seem to have propagated to the actual context to be used.";
        assert INVOKED_METHODS.contains("addNamingListener") : "The addNamingListener doesn't seem to have propagated to the actual context to be used.";
    }
}
