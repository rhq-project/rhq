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
package org.jboss.on.common.jbossas;

import java.io.File;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author Ian Springer
 */
public class JBossASDiscoveryUtils {
    private static final Log LOG = LogFactory.getLog(JBossASDiscoveryUtils.class);

    @Nullable
    public static UserInfo getJmxInvokerUserInfo(File configDir) {
        String securityDomain = getJmxInvokerSecurityDomain(configDir);
        if (securityDomain == null) {
            LOG.debug("The JMX invoker service is not configured to require authentication.");
            return null;
        }

        LOG.debug("The JMX invoker service is configured to use the '" + securityDomain
                + "' security domain for authentication.");
        File usersPropsFile = new File(configDir, "conf/props/" + securityDomain + "-users.properties");
        if (!usersPropsFile.exists()) {
            LOG.debug("Could not find users configuration for security domain '" + securityDomain
                    + "' - " + usersPropsFile + " does not exist.");
        }
        File rolesPropsFile = new File(configDir, "conf/props/" + securityDomain + "-roles.properties");
        if (!rolesPropsFile.exists()) {
            LOG.debug("Could not find roles configuration for security domain '" + securityDomain
                    + "' - " + rolesPropsFile + " does not exist.");
        }
        if (usersPropsFile.exists() && rolesPropsFile.exists()) {
            try {
                SecurityDomainInfo securityDomainInfo = new SecurityDomainInfo(usersPropsFile, rolesPropsFile);
                Set<String> adminUsers = securityDomainInfo.getUsers("JBossAdmin");
                if (!adminUsers.isEmpty()) {
                    // Use the first one - it's as good as any.
                    String adminUser = adminUsers.iterator().next();
                    String adminPassword = securityDomainInfo.getPassword(adminUser);
                    LOG.debug("Discovered principal (" + adminUser
                            + ") and credentials for connecting to the JMX invoker service.");
                    return new UserInfo(adminUser, adminPassword);
                }
            }
            catch (Exception e) {
                LOG.error("Could not determine username and password of admin user - failed to parse users and/or roles configuration file.");
            }
        }
        return null;
    }

    @Nullable
    private static String getJmxInvokerSecurityDomain(File configDir) {
        File deployDir = new File(configDir, "deploy");
        File jmxInvokerServiceXmlFile = new File(deployDir, "jmx-invoker-service.xml");
        String securityDomain = null;
        try {
            JmxInvokerServiceConfiguration jmxInvokerConfig = new JmxInvokerServiceConfiguration(jmxInvokerServiceXmlFile);
            securityDomain = jmxInvokerConfig.getSecurityDomain();
        }
        catch (Exception e) {
            LOG.debug("Failed to read " + jmxInvokerServiceXmlFile
                    + " - unable to determine if authentication is enabled on the JMX invoker. Cause: "
                    + ThrowableUtil.getAllMessages(e));
        }
        return securityDomain;
    }

    public static class UserInfo {
        private final String username;
        private final String password;

        public UserInfo(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    private JBossASDiscoveryUtils() {
    }
}
