/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.util;

import java.util.Properties;

import org.testng.annotations.Test;

@Test
public class StringPropertyReplacerTest {
    public void testReplacer() {
        Properties props = new Properties();
        props.setProperty("hello", "world");
        props.setProperty("wot", "gorilla?");
        String str = StringPropertyReplacer.replaceProperties("${hello}", props);
        assert str.equals("world") : str;
        str = StringPropertyReplacer.replaceProperties("AAA${hello}BBB", props);
        assert str.equals("AAAworldBBB") : str;
        str = StringPropertyReplacer.replaceProperties("AAA${wot}BBB${hello}CCC${foo:the Default}", props);
        assert str.equals("AAAgorilla?BBBworldCCCthe Default") : str;
    }

    public void testReplacerSystemProps() {
        String sysprop = System.getProperty("os.name");
        String str = StringPropertyReplacer.replaceProperties("${os.name}");
        assert str.equals(sysprop) : str;
        str = StringPropertyReplacer.replaceProperties("AAA${os.name}BBB");
        assert str.equals("AAA" + sysprop + "BBB") : str;
        str = StringPropertyReplacer.replaceProperties("${foo:the Default}");
        assert str.equals("the Default") : str;
        System.setProperty("foo", "foo value here");
        str = StringPropertyReplacer.replaceProperties("${foo:the Default}");
        assert str.equals("foo value here") : str;
    }
}
