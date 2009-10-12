/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.jboss.on.common.jbossas.test;

import java.io.InputStream;

import org.testng.annotations.Test;

import org.jboss.on.common.jbossas.JmxInvokerServiceConfiguration;

/**
 * @author Ian Springer
 */
public class JmxInvokerServiceConfigurationTest {
    private static final String EAP4_SECURITY_ENABLED_RESOURCE_PATH = "configFiles/jmx-invoker-service-eap4-securityEnabled.xml";
    private static final String EAP4_SECURITY_DISABLED_RESOURCE_PATH = "configFiles/jmx-invoker-service-eap4-securityDisabled.xml";
    private static final String EAP5_SECURITY_ENABLED_RESOURCE_PATH = "configFiles/jmx-invoker-service-eap5-securityEnabled.xml";
    private static final String EAP5_SECURITY_DISABLED_RESOURCE_PATH = "configFiles/jmx-invoker-service-eap5-securityDisabled.xml";

    @Test
    public void testEap4SecurityEnabled() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(EAP4_SECURITY_ENABLED_RESOURCE_PATH);
        JmxInvokerServiceConfiguration config = new JmxInvokerServiceConfiguration(inputStream);
        assert "jmx-console".equals(config.getSecurityDomain());
    }

    @Test
    public void testEap4SecurityDisabled() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(EAP4_SECURITY_DISABLED_RESOURCE_PATH);
        JmxInvokerServiceConfiguration config = new JmxInvokerServiceConfiguration(inputStream);
        assert config.getSecurityDomain() == null : "Security domain = '" + config.getSecurityDomain() + "'";        
    }

    @Test
    public void testEap5SecurityEnabled() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(EAP5_SECURITY_ENABLED_RESOURCE_PATH);
        JmxInvokerServiceConfiguration config = new JmxInvokerServiceConfiguration(inputStream);
        assert "jmx-console".equals(config.getSecurityDomain());
    }

    @Test
    public void testEap5SecurityDisabled() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(EAP5_SECURITY_DISABLED_RESOURCE_PATH);
        JmxInvokerServiceConfiguration config = new JmxInvokerServiceConfiguration(inputStream);
        assert config.getSecurityDomain() == null : "Security domain = '" + config.getSecurityDomain() + "'";        
    }
}
