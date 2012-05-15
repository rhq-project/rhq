/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Unit Tests.
 *
 * @author Jay Shaughnessy
 */
@Test
public class StartScriptConfigurationTest {

    @Test
    public void testBasic() throws Exception {
        Configuration config = new Configuration();

        StartScriptConfiguration ssc = new StartScriptConfiguration(config);
        assert config.equals(ssc.getPluginConfig());

        File startScript = new File("run.sh");
        ssc.setStartScript(startScript);
        Assert.assertEquals(ssc.getStartScript(), startScript);

        Map<String, String> startScriptEnv = new HashMap<String, String>();
        startScriptEnv.put("JAVA_HOME", "/java");
        startScriptEnv.put("JAVA_OPTS", "-XmX1024");
        ssc.setStartScriptEnv(startScriptEnv);
        Assert.assertEquals(ssc.getStartScriptEnv(), startScriptEnv);
        Assert.assertTrue(ssc.getPluginConfig().getSimpleValue(StartScriptConfiguration.START_SCRIPT_ENV_PROP, "")
            .contains("JAVA_HOME"));

        List<String> startScriptArgs = new ArrayList<String>();
        startScriptArgs.add("-c");
        startScriptArgs.add("default");
        ssc.setStartScriptArgs(startScriptArgs);
        Assert.assertEquals(ssc.getStartScriptArgs(), startScriptArgs);
        Assert.assertTrue(ssc.getPluginConfig().getSimpleValue(StartScriptConfiguration.START_SCRIPT_ARGS_PROP, "")
            .contains("default"));
    }

    @Test
    public void testLongEnvorArgs() throws Exception {
        Configuration config = new Configuration();

        StartScriptConfiguration ssc = new StartScriptConfiguration(config);
        assert config.equals(ssc.getPluginConfig());

        Map<String, String> startScriptEnv = new HashMap<String, String>();
        char[] maxArr = new char[PropertySimple.MAX_VALUE_LENGTH];
        Arrays.fill(maxArr, 'J');
        String maxString = new String(maxArr);
        char[] longArr = new char[500];
        Arrays.fill(longArr, 'S');
        String longString = new String(longArr);

        // First ensure we can store the MAX VALUE
        String maxVal = maxString.substring(0, PropertySimple.MAX_VALUE_LENGTH - 5); // MAX= and a line terminator
        startScriptEnv.put("MAX", maxVal);
        ssc.setStartScriptEnv(startScriptEnv);
        Assert.assertNotNull(ssc.getStartScriptEnv());
        Assert.assertEquals(ssc.getStartScriptEnv().get("MAX"), maxVal);
        PropertySimple prop = ssc.getPluginConfig().getSimple(StartScriptConfiguration.START_SCRIPT_ENV_PROP);
        Assert.assertNotNull(prop);
        Assert.assertNotNull(prop.getStringValue());
        Assert.assertEquals(prop.getStringValue().length(), PropertySimple.MAX_VALUE_LENGTH);
        Assert.assertNull(prop.getErrorMessage());

        List<String> startScriptArgs = new ArrayList<String>();
        startScriptArgs.add(maxString);
        ssc.setStartScriptArgs(startScriptArgs);
        Assert.assertNotNull(ssc.getStartScriptArgs());
        Assert.assertEquals(ssc.getStartScriptArgs().size(), 1);
        Assert.assertEquals(ssc.getStartScriptArgs().get(0), maxString);
        prop = ssc.getPluginConfig().getSimple(StartScriptConfiguration.START_SCRIPT_ARGS_PROP);
        Assert.assertNotNull(prop);
        Assert.assertNotNull(prop.getStringValue());
        Assert.assertNull(prop.getErrorMessage());

        // Now, break the camel
        config = new Configuration();
        ssc = new StartScriptConfiguration(config);
        assert config.equals(ssc.getPluginConfig());

        startScriptEnv.put("LONG2", longString);
        ssc.setStartScriptEnv(startScriptEnv);
        Assert.assertTrue(ssc.getStartScriptEnv().isEmpty());
        prop = ssc.getPluginConfig().getSimple(StartScriptConfiguration.START_SCRIPT_ENV_PROP);
        Assert.assertNotNull(prop);
        Assert.assertNull(prop.getStringValue());
        Assert.assertNotNull(prop.getErrorMessage());
        assert prop.getErrorMessage().contains("long");

        startScriptArgs.add(longString);
        ssc.setStartScriptArgs(startScriptArgs);
        Assert.assertTrue(ssc.getStartScriptArgs().isEmpty());
        prop = ssc.getPluginConfig().getSimple(StartScriptConfiguration.START_SCRIPT_ARGS_PROP);
        Assert.assertNotNull(prop);
        Assert.assertNull(prop.getStringValue());
        Assert.assertNotNull(prop.getErrorMessage());
        assert prop.getErrorMessage().contains("long");
    }

}
