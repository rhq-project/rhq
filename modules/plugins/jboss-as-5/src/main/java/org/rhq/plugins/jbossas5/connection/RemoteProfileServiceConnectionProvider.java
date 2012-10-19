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
package org.rhq.plugins.jbossas5.connection;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.profileservice.spi.ProfileService;

/**
 * A connection provider for connecting to a remote Profile Service - AOP proxies are looked up from JNDI.
 *
 * @author Ian Springer
 */
public class RemoteProfileServiceConnectionProvider extends AbstractProfileServiceConnectionProvider {
    private static final String NAMING_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";

    private static final String PROFILE_SERVICE_JNDI_NAME = "ProfileService";
    private static final String MANAGEMENT_VIEW_JNDI_NAME = "ManagementView";
    private static final String DEPLOYMENT_MANAGER_JNDI_NAME = "DeploymentManager";

    private static final String JNP_TIMEOUT_JNP_INIT_PROP = "jnp.timeout";
    private static final String JNP_SOTIMEOUT_JNP_INIT_PROP = "jnp.sotimeout";
    private static final String JNP_DISABLE_DISCOVERY_JNP_INIT_PROP = "jnp.disableDiscovery";

    /**
     * This is the timeout (in milliseconds) for the initial attempt to establish the remote connection.
     */
    private static final int JNP_TIMEOUT = 60 * 1000; // 60 seconds

    /**
     * This is the timeout (in milliseconds) for methods invoked on the remote ProfileService. NOTE: This timeout comes
     * into play if the JBossAS instance has gone down since the original JNP connection was made.
     */
    private static final int JNP_SO_TIMEOUT = 60 * 1000; // 60 seconds

    /**
     * A flag indicating that the discovery process should not attempt to automatically discover (via multicast) naming
     * servers running the JBoss HA-JNDI service if it fails to connect to the specified jnp URL.
     */
    private static final boolean JNP_DISABLE_DISCOVERY = true;

    private final Log log = LogFactory.getLog(this.getClass());

    private String providerURL;
    private String principal;
    private String credentials;
    private InitialContext initialContext;

    public RemoteProfileServiceConnectionProvider(String providerURL, String principal, String credentials) {
        this.providerURL = providerURL;
        this.principal = principal;
        this.credentials = credentials;
    }

    public String getPrincipal() {
        return this.principal;
    }

    public String getCredentials() {
        return this.credentials;
    }

    protected AbstractProfileServiceConnection doConnect() {
        Properties env = new Properties();
        env.setProperty(Context.PROVIDER_URL, this.providerURL);

        // Always use the non-login context factory, since we'll use JAAS for authentication if a username/password was
        // provided.
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, NAMING_CONTEXT_FACTORY);

        env.setProperty(JNP_TIMEOUT_JNP_INIT_PROP, String.valueOf(JNP_TIMEOUT));
        env.setProperty(JNP_SOTIMEOUT_JNP_INIT_PROP, String.valueOf(JNP_SO_TIMEOUT));

        env.setProperty(JNP_DISABLE_DISCOVERY_JNP_INIT_PROP, String.valueOf(JNP_DISABLE_DISCOVERY));
        
        log.debug("Connecting to Profile Service via remote JNDI using env [" + env + "]...");
        this.initialContext = createInitialContext(env);

        ProfileService profileService = (ProfileService) lookup(this.initialContext, PROFILE_SERVICE_JNDI_NAME);
        ManagementView managementView = (ManagementView) lookup(this.initialContext, MANAGEMENT_VIEW_JNDI_NAME);
        DeploymentManager deploymentManager = (DeploymentManager) lookup(this.initialContext,
                DEPLOYMENT_MANAGER_JNDI_NAME);

        AbstractProfileServiceConnection profileServiceConnection;
        if (this.principal != null) {
            // Use a connection that will perform a JAAS login prior to all invocations.
            profileServiceConnection = new JaasAuthenticationProxyProfileServiceConnection(this, profileService,
                    managementView, deploymentManager);
        } else {
            profileServiceConnection = new BasicProfileServiceConnection(this, profileService, managementView,
                    deploymentManager);
        }
        return profileServiceConnection;
    }

    protected void doDisconnect() {
        try {
            this.initialContext.close();
        }
        catch (NamingException e) {
            log.error("Failed to close JNDI InitialContext.", e);
        }
    }
}