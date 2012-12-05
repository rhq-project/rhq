/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.jndi;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.event.EventContext;
import javax.naming.event.EventDirContext;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.jndi.context.AccessCheckingContextDecorator;
import org.rhq.jndi.context.AccessCheckingContextDecoratorSetContext;
import org.rhq.jndi.context.ContextDecorator;
import org.rhq.jndi.context.URLPreferringContextDecoratorSetContext;
import org.rhq.jndi.util.DecoratorPicker;

/**
 * This initial context factory builder is installed early on during the RHQ server startup
 * and is later on used for obtaining the {@link Context}s for all JNDI lookups in the
 * RHQ server.
 * <p>
 * We use a custom initial context factory builder to prevent the potential malicious 3rd party
 * code (like CLI alert scripts) from supplying custom environment variables to {@link InitialContext} 
 * that would modify the JNDI lookup to skip our security access checks.
 * <p>
 * By using a builder we effectively take control of the initial context creation process
 * and are free to ignore whatever the script is trying to supply.
 * <p>
 * This builder makes sure to install the RHQ server's security access checks to whatever
 * initial context that is configured by the standard environment variables 
 * ({@link Context#INITIAL_CONTEXT_FACTORY}, etc.)
 * <p>
 * This class is heavily inspired by the implementation of a similar builder in JBoss AS 7.
 * 
 * @see AllowRhqServerInternalsAccessPermission
 * 
 * @author Lukas Krejci
 */
public class AccessCheckingInitialContextFactoryBuilder implements InitialContextFactoryBuilder {
    private static final Log LOG = LogFactory.getLog(AccessCheckingInitialContextFactoryBuilder.class);

    /**
     * The list of JNDI name schemes that should be checked for security permissions
     * (in addition to the names with no scheme).
     * 
     * @see AccessCheckingContextDecorator
     */
    private static final String[] CHECKED_SCHEMES = { "java" };

    private static final Set<Class<? extends Context>> SUPPORTED_CONTEXT_INTERFACES;
    
    static {
        SUPPORTED_CONTEXT_INTERFACES = new HashSet<Class<? extends Context>>();
        SUPPORTED_CONTEXT_INTERFACES.add(Context.class);
        SUPPORTED_CONTEXT_INTERFACES.add(DirContext.class);
        SUPPORTED_CONTEXT_INTERFACES.add(EventContext.class);
        SUPPORTED_CONTEXT_INTERFACES.add(EventDirContext.class);
        SUPPORTED_CONTEXT_INTERFACES.add(LdapContext.class);
    }
    
    private static final Set<InetAddress> SERVER_BIND_IPS;
    static {
        SERVER_BIND_IPS = new HashSet<InetAddress>();

        try {
            String bindingAddressString = System.getProperty("jboss.bind.address");
            InetAddress bindingAddress = InetAddress.getByName(bindingAddressString);

            if (bindingAddress.isAnyLocalAddress()) {
                Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                while (ifaces.hasMoreElements()) {
                    NetworkInterface iface = ifaces.nextElement();
                    SERVER_BIND_IPS.addAll(Collections.list(iface.getInetAddresses()));
                }
            } else {
                SERVER_BIND_IPS.add(bindingAddress);
            }
        } catch (SocketException e) {
            LOG.error("Could not obtain the list of local IPs", e);
        } catch (UnknownHostException e) {
            LOG.error("Failed to get the binding address of the RHQ server.", e);
        }
    }

    private static final int JNP_PORT = Integer.parseInt(System.getProperty("rhq.server.startup.namingservice.port",
        "2099"));

    /**
     * This is the default initial context factory that is returned when no other is 
     * configured using the environment variables.
     * <p>
     * It uses {@link NamingContextFactory} as the underlying mechanism - the same
     * as the default configuration in JBoss 4.
     */
    private static final InitialContextFactory DEFAULT_FACTORY = new InitialContextFactory() {
        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            return null; // TODO: how to do this in AS7? createSecureWrapper(new NamingContextFactory(), environment).getInitialContext(environment);
        }
    };

    /**
     * Create a InitialContext factory.  If the environment does not override the factory class it will use the
     * default context factory.
     * 
     * @param environment The environment
     * @return An initial context factory
     * @throws NamingException If an error occurs loading the factory class.
     */
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        final String factoryClassName = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);        
        if (factoryClassName == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No " + Context.INITIAL_CONTEXT_FACTORY + " set. Using the default factory.");
            }
            return DEFAULT_FACTORY;
        }
        final ClassLoader classLoader = getContextClassLoader();
        try {
            final Class<?> factoryClass = Class.forName(factoryClassName, true, classLoader);
            InitialContextFactory configuredFactory = (InitialContextFactory) factoryClass.newInstance();            
            return createSecureWrapper(configuredFactory, environment);
        } catch (Exception e) {
            throw new NamingException("Failed instantiate InitialContextFactory " + factoryClassName
                + " from classloader " + classLoader);
        }
    }

    private ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private static InitialContextFactory
        createSecureWrapper(InitialContextFactory factory, Hashtable<?, ?> environment) {
        String providerUrl = (String) environment.get(Context.PROVIDER_URL);

        if (providerUrl == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Wrapping " + factory + " of class " + factory.getClass() + " in an access checking wrapper. No provider URL detected.");
            }
            return getAccessCheckingFactory(factory);
        } else {
            try {
                URI uri = new URI(providerUrl);
                InetAddress providerHost = InetAddress.getByName(uri.getHost());

                //check if we are accessing the RHQ server through some remoting
                //interface.
                if (uri.getPort() == JNP_PORT && SERVER_BIND_IPS.contains(providerHost)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Wrapping " + factory + " of class " + factory.getClass() + " in an access checking wrapper. The provider URL points to this server.");
                    }
                    return getAccessCheckingFactory(factory);
                } else {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Wrapping " + factory + " of class " + factory.getClass() + " in an URL preferring wrapper to enable remote connections.");
                    }
                    return getURLPreferringFactory(factory);
                }
            } catch (URISyntaxException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The " + Context.PROVIDER_URL
                        + " is not a valid URI. Falling back to using the access checking wrapper for the factory "
                        + factory + " of class " + factory.getClass() + ".", e);
                }
                return getAccessCheckingFactory(factory);
            } catch (UnknownHostException e) {
                //let the factory deal with the unknown host...
                //this most probably shouldn't be secured because localhost addresses
                //should be resolvable.
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The " + Context.PROVIDER_URL
                        + " is not resolvable. Falling back to using the URL preferring wrapper for the factory "
                        + factory + " of class " + factory.getClass() + ".", e);
                }
                return getURLPreferringFactory(factory);
            }
        }
    }

    private static InitialContextFactory getAccessCheckingFactory(InitialContextFactory original) {
        ArrayList<DecoratorPicker<Context, ContextDecorator>> pickers = new ArrayList<DecoratorPicker<Context,ContextDecorator>>();
        pickers.add(getURLPreferringDecoratorPicker());
        pickers.add(getAccessCheckingDecoratorPicker());
        
        return new DecoratingInitialContextFactory(original, pickers);
    }

    private static InitialContextFactory getURLPreferringFactory(InitialContextFactory original) {
        ArrayList<DecoratorPicker<Context, ContextDecorator>> pickers = new ArrayList<DecoratorPicker<Context,ContextDecorator>>();
        pickers.add(getURLPreferringDecoratorPicker());

        return new DecoratingInitialContextFactory(original, pickers);
    }

    private static DecoratorPicker<Context, ContextDecorator> getAccessCheckingDecoratorPicker() {
        DecoratorPicker<Context, ContextDecorator> ret = new DecoratorPicker<Context, ContextDecorator>();
        ret.setContext(new AccessCheckingContextDecoratorSetContext(SUPPORTED_CONTEXT_INTERFACES, CHECKED_SCHEMES));
        
        return ret;
    }

    private static DecoratorPicker<Context, ContextDecorator> getURLPreferringDecoratorPicker() {
        DecoratorPicker<Context, ContextDecorator> ret = new DecoratorPicker<Context, ContextDecorator>();
        ret.setContext(new URLPreferringContextDecoratorSetContext(SUPPORTED_CONTEXT_INTERFACES));
        
        return ret;
    }
}
