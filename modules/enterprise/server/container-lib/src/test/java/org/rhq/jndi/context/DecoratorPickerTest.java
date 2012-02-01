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

package org.rhq.jndi.context;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.event.EventContext;
import javax.naming.ldap.LdapContext;

import org.testng.annotations.Test;

import org.rhq.jndi.util.DecoratorPicker;
import org.rhq.jndi.util.DecoratorSetContext;

/**
 * @author Lukas Krejci
 */
@Test
public class DecoratorPickerTest {

    private static final InvocationHandler DUMMY_HANDLER = new InvocationHandler() {

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("hashCode".equals(method.getName())) {
                return 0;
            } else if ("equals".equals(method.getName())) {
                return false;
            }

            return null;
        }
    };

    private static final Class<?> TEST_OBJECT_CLASS1 = createProxyClass(Context.class);
    private static final Class<?> TEST_OBJECT_CLASS2 = createProxyClass(DirContext.class);
    private static final Class<?> TEST_OBJECT_CLASS3 = createProxyClass(LdapContext.class);
    private static final Class<?> TEST_OBJECT_CLASS4 = createProxyClass(LdapContext.class, EventContext.class);
    private static final Class<?> DECORATOR_CLASS1 = createProxyClass(Context.class);
    private static final Class<?> DECORATOR_CLASS2 = createProxyClass(EventContext.class);
    private static final Class<?> DECORATOR_CLASS3 = createProxyClass(LdapContext.class);
    private static final Class<?> DECORATOR_CLASS4 = createProxyClass(DirContext.class);

    public void testSimpleDecoratorIdentifiedByClass() throws Exception {
        DecoratorPicker<Object, Object> picker = createTestPicker();

        Set<Object> contextDecorators = picker.getDecoratorsForClass(TEST_OBJECT_CLASS1);
        assertEquals(contextDecorators.size(), 1, "Expected exactly one decorator for Context class");
        assertEquals(contextDecorators.iterator().next().getClass().getInterfaces()[0], Context.class);
    }

    public void testSuperClassDecoratorHasPrecedenceOverSubClassDecorator() throws Exception {
        DecoratorPicker<Object, Object> picker = createTestPicker();

        //this tests that the LdapContext isn't returned even though it subclasses the DirContext
        Set<Object> contextDecorators = picker.getDecoratorsForClass(TEST_OBJECT_CLASS2);
        assertEquals(contextDecorators.size(), 1, "Expected exactly one decorator for DirContext class");
        assertEquals(contextDecorators.iterator().next().getClass().getInterfaces()[0], DirContext.class);
    }

    public void testSubClassDecoratorCorrectlyIdentified() throws Exception {
        DecoratorPicker<Object, Object> picker = createTestPicker();

        Set<Object> contextDecorators = picker.getDecoratorsForClass(TEST_OBJECT_CLASS3);
        assertEquals(contextDecorators.size(), 1, "Expected exactly one decorator for LdapContext class");
        assertEquals(contextDecorators.iterator().next().getClass().getInterfaces()[0], LdapContext.class);
    }

    public void testMultipleDecoratorsDetectable() throws Exception {
        DecoratorPicker<Object, Object> picker = createTestPicker();

        Set<Object> decorators = picker.getDecoratorsForClass(TEST_OBJECT_CLASS4);
        assertEquals(decorators.size(), 2,
            "Exactly 2 decorators should have been found for a class implementing 2 interfaces.");

        boolean ldapContextDecoratorFound = false;
        boolean eventContextDecoratorFound = false;

        for (Object d : decorators) {
            if (LdapContext.class.isAssignableFrom(d.getClass())) {
                ldapContextDecoratorFound = true;
                continue; //just to make sure that somehow the decorator doesn't implement both
            }
            if (EventContext.class.isAssignableFrom(d.getClass())) {
                eventContextDecoratorFound = true;
            }
        }

        assertTrue(ldapContextDecoratorFound && eventContextDecoratorFound,
            "The found decorators don't implement the desired interfaces.");
    }

    public void testDecoratorsIdentifiedByMethod() throws Exception {
        DecoratorPicker<Object, Object> picker = createTestPicker();

        Set<Object> decorators =
            picker.getDecoratorsForMethod(LdapContext.class.getMethod("getConnectControls", (Class<?>[]) null));
        assertEquals(decorators.size(), 1,
            "Expected exactly one decorator for method 'getConnectControls()' from LdapContext class");
        assertEquals(decorators.iterator().next().getClass().getInterfaces()[0], LdapContext.class);
    }

    public void testMethodFromSubclassMatchesSubclassDecorator() throws Exception {
        DecoratorPicker<Object, Object> picker = createTestPicker();

        //this is a method from the DirContext but we're asking for it from a class
        //that implements also an LdapContext
        //The LdapContext decorator also inherits from the DirContext decorator
        //(by the virtue of LdapContext interface inheriting from the DirContext)
        //The picker should therefore match the LdapContext decorator because its
        //the "closest" one to the actual class.
        Set<Object> decorators =
            picker.getDecoratorsForMethod(TEST_OBJECT_CLASS3.getMethod("getSchemaClassDefinition",
                new Class<?>[] { String.class }));
        assertEquals(decorators.size(), 1,
            "Expected exactly one decorator for method 'getSchemaClassDefinition(String)' from LdapContext class");
        assertEquals(decorators.iterator().next().getClass().getInterfaces()[0], LdapContext.class);
    }

    private static Class<?> createProxyClass(Class<?>... ifaces) {
        return Proxy.getProxyClass(DecoratorPickerTest.class.getClassLoader(), ifaces);
    }

    private static DecoratorPicker<Object, Object> createTestPicker() {
        DecoratorPicker<Object, Object> picker = new DecoratorPicker<Object, Object>();

        DecoratorSetContext<Object, Object> decSet = new DecoratorSetContext<Object, Object>() {

            public Object instantiate(Class<? extends Object> decoratorClass) throws Exception {
                Constructor<? extends Object> ctor = decoratorClass.getConstructor(InvocationHandler.class);                
                return ctor.newInstance(DUMMY_HANDLER);
            }

            public void init(Object decorator, Object object) throws Exception {
            }

            @SuppressWarnings("unchecked")
            public Set<Class<? extends Object>> getSupportedInterfaces() {
                return new HashSet<Class<? extends Object>>(Arrays.asList(Context.class, EventContext.class,
                    LdapContext.class, DirContext.class));
            }

            @SuppressWarnings("unchecked")
            public Set<Class<? extends Object>> getDecoratorClasses() {
                return new HashSet<Class<? extends Object>>(Arrays.asList(DECORATOR_CLASS1, DECORATOR_CLASS2, DECORATOR_CLASS3, DECORATOR_CLASS4));
            }
        };

        picker.setContext(decSet);
        
        return picker;
    }
}
