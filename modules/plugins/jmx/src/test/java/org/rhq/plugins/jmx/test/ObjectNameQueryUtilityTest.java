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
package org.rhq.plugins.jmx.test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.testng.annotations.Test;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

public class ObjectNameQueryUtilityTest {
    @Test
    public void testObjectNameQueryUtility() throws MalformedObjectNameException {
        ObjectNameQueryUtility onqu = null;
        onqu = new ObjectNameQueryUtility("java.lang:type=MemoryPool,name=Code Cache");
        assert onqu.getQueryTemplate().equals("java.lang:type=MemoryPool,name=Code Cache");

        onqu = new ObjectNameQueryUtility("java.lang:type=MemoryPool,name=%name%");
        assert onqu.getTranslatedQuery().equals("java.lang:type=MemoryPool,*");
        assert onqu.getVariableProperties().size() == 1;
        assert onqu.getVariableProperties().get("name").equals("name");

        onqu = new ObjectNameQueryUtility("java.lang:type=Threading");

        onqu = new ObjectNameQueryUtility("java.lang:type=%foo%");
        assert onqu.getTranslatedQuery().equals("java.lang:*");
        assert onqu.getVariableProperties().size() == 1;
        assert onqu.getVariableProperties().get("type").equals("foo");

        onqu = new ObjectNameQueryUtility("java.lang:type=%MyType%,name=%MyName%,app=%MyApp%,foo=%MyFoo%");
        assert onqu.getTranslatedQuery().equals("java.lang:*");
        assert onqu.getVariableProperties().size() == 4;
        ObjectName testON = new ObjectName("java.lang:type=A,name=B,app=C,foo=D");
        onqu.setMatchedKeyValues(testON.getKeyPropertyList());
        String formulatedMessageTemplate = "Type: {MyType}, Name: {MyName}, App: {MyApp}, Foo: {MyFoo}";
        assert onqu.formatMessage(formulatedMessageTemplate).equals("Type: A, Name: B, App: C, Foo: D");

        Configuration c = new Configuration();
        c.put(new PropertySimple("e","foo"));
        c.put(new PropertySimple("g","bar"));
        onqu = new ObjectNameQueryUtility("a:b=c,d={e},f={g}",c);
        System.out.println("Template: " + onqu.getQueryTemplate());
        assert onqu.getQueryTemplate().equals("a:b=c,d=foo,f=bar");
    }
}