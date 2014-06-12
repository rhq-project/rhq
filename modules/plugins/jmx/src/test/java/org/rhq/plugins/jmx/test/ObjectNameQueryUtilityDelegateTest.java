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
package org.rhq.plugins.jmx.test;

import static org.testng.Assert.assertEquals;

import java.util.Collections;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * This test explicitly tests the delegate for the ObjectNameQueryUtility.
 * The Utility itself got moved into the util package. To be backward compatible,
 * we need to have a delegate
 */
public class ObjectNameQueryUtilityDelegateTest {
    @Test
    public void testObjectNameQueryUtility() throws MalformedObjectNameException {
        ObjectNameQueryUtility onqu = null;
        onqu = new ObjectNameQueryUtility("java.lang:type=MemoryPool,name=Code Cache");
        assert onqu.getQueryTemplate().equals("java.lang:type=MemoryPool,name=Code Cache");

        onqu = new ObjectNameQueryUtility("java.lang:type=MemoryPool,name=%name%");
        assertEquals(onqu.getTranslatedQuery(), "java.lang:type=MemoryPool,name=*");
        assert onqu.getVariableProperties().size() == 1;
        assert onqu.getVariableProperties().get("name").equals("name");

        onqu = new ObjectNameQueryUtility("java.lang:type=Threading");

        onqu = new ObjectNameQueryUtility("java.lang:type=%foo%");
        assert onqu.getTranslatedQuery().equals("java.lang:type=*");
        assert onqu.getVariableProperties().size() == 1;
        assert onqu.getVariableProperties().get("type").equals("foo");

        onqu = new ObjectNameQueryUtility("jboss.esb.*:service=Queue,name=%name%");
        assert onqu.getTranslatedQuery().equals("jboss.esb.*:service=Queue,name=*");
        assert onqu.getVariableProperties().size() == 1;
        assert onqu.getVariableProperties().get("name").equals("name");
        ObjectName testON = new ObjectName(
            "jboss.esb.quickstart.destination:service=Queue,name=quickstart_helloworld_Request_gw");
        onqu.setMatchedKeyValues(testON.getKeyPropertyList());
        String formulatedMessageTemplate = "Name of queue: {name}";
        assert onqu.formatMessage(formulatedMessageTemplate).equals("Name of queue: quickstart_helloworld_Request_gw");

        onqu = new ObjectNameQueryUtility("java.lang:type=%MyType%,name=%MyName%,app=%MyApp%,foo=%MyFoo%");
        assertEquals(onqu.getTranslatedQuery(), "java.lang:type=*,name=*,app=*,foo=*");
        assert onqu.getVariableProperties().size() == 4;
        testON = new ObjectName("java.lang:type=A,name=B,app=C,foo=D");
        onqu.setMatchedKeyValues(testON.getKeyPropertyList());
        formulatedMessageTemplate = "Type: {MyType}, Name: {MyName}, App: {MyApp}, Foo: {MyFoo}";
        assert onqu.formatMessage(formulatedMessageTemplate).equals("Type: A, Name: B, App: C, Foo: D");

        Configuration c = new Configuration();
        c.put(new PropertySimple("e", "foo"));
        c.put(new PropertySimple("g", "bar"));
        onqu = new ObjectNameQueryUtility("a:b=c,d={e},f={g}", c);
        System.out.println("Template: " + onqu.getQueryTemplate());
        assert onqu.getQueryTemplate().equals("a:b=c,d=foo,f=bar");

        // Test some very long replacement tokens inspired by BZ 828596
        onqu = new ObjectNameQueryUtility("*:type=HttpMetricInspector,name=%name%");
        assert onqu.getQueryTemplate().equals("*:type=HttpMetricInspector,name=%name%");
        assert onqu.getVariableProperties().size() == 1;
        assert onqu.getVariableProperties().get("name").equals("name");
        testON = new ObjectName("FooBarABCDEFGHIJKLMNOPQRSTUVWXYZ:type=HttpMetricInspector,name=ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ");
        onqu.setMatchedKeyValues(testON.getKeyPropertyList());
        formulatedMessageTemplate = "Http metrics for endpoint {name}";
        String res = onqu.formatMessage(formulatedMessageTemplate);
        assert res.equals("Http metrics for endpoint ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ") : res;


    }

    @Test
    public void testObjectNameQueryUtilityFiltering() throws MalformedObjectNameException {
        ObjectNameQueryUtility onqu = null;

        onqu = new ObjectNameQueryUtility("java.lang:type=MemoryPool,name=%foo%");
        assert !onqu.isContainsExtraKeyProperties(Collections.singleton("type"));
        assert !onqu.isContainsExtraKeyProperties(Collections.singleton("name"));
        assert onqu.isContainsExtraKeyProperties(Collections.singleton("splat"));

    }

}