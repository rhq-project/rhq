/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.gui.startup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyDynamicType;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.gui.configuration.helper.PropertyRenderingUtility;
import org.rhq.enterprise.gui.configuration.DatabaseDynamicPropertyRetriever;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is a servlet context listener defined in web.xml that loads several files and puts data in the servlet context:
 *
 * <ul>
 *   <li>the <code>/WEB-INF/rhq-server-internals.properties</code> properties which configures things like how to
 *     connect to EJBs</li>
 *   <li>configures a user's default preferences, for those users that have no persisted preferences already</li>
 *   <li>the build version number</li>
 *   <li>taglib properties</li>
 * </ul>
 */
public final class Configurator implements ServletContextListener {
    private static final Log LOG = LogFactory.getLog(Configurator.class.getName());

    private static final String SERVER_INTERNALS_FILE = "/WEB-INF/rhq-server-internals.properties";
    private static final String DEFAULT_USER_PREFERENCES_FILE = "/WEB-INF/DefaultUserPreferences.properties";
    private static final String TAGLIB_PROPERTIES_FILE = "/WEB-INF/taglib.properties";
    private static final String LOGGING_PROPERTIES_RESOURCE = "logging.properties";

    // If any name in the server internals properties file starts with this, it defines a system property.
    // Therefore, the name/value will not go into the servlet context, but will be a system property.
    private static final String SYSPROP_KEY = "org.jboss.on.sysprop";

    /**
     * Creates a new {@link Configurator} object.
     */
    public Configurator() {
    }

    /**
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        loadConfig(ctx);
        loadPreferences(ctx);
        loadTablePreferences(ctx);
        loadBuildVersion(ctx);
        loadJavaLoggingConfiguration();
        registerDynamicPropertyRetrievers();
    }

    /**
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    /**
     * Load the internals properties file and configures the web application. All properties in the file are exposed as
     * servlet context attributes.
     */
    private void loadConfig(ServletContext ctx) {
        Properties props;

        try {
            props = loadProperties(ctx, SERVER_INTERNALS_FILE);
        } catch (Exception e) {
            LOG.error("unable to load server internals file [" + SERVER_INTERNALS_FILE + "]", e);
            return;
        }

        if (props == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("server internals file [" + SERVER_INTERNALS_FILE + "] does not exist");
            }
            return;
        }

        Enumeration names = props.propertyNames();

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            // this is not currently used, but is interesting, it lets you define system properties in rhq-server-internals.properties
            if (name.startsWith(SYSPROP_KEY)) {
                System.setProperty(name, props.getProperty(name));
            } else {
                ctx.setAttribute(name, props.getProperty(name));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("loaded server internals [" + SERVER_INTERNALS_FILE + "]");
        }
    }

    /**
     * There is a single properties file in the web application that defines the default user properties. That
     * properties file is loaded in and stored in the servlet context so a user that logs in but doesn't have
     * preferences set yet can obtain these default settings.
     */
    private void loadPreferences(ServletContext ctx) {
        try {
            Configuration userPrefs = new Configuration();
            Properties userProps = loadProperties(ctx, DEFAULT_USER_PREFERENCES_FILE);
            Enumeration keys = userProps.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                userPrefs.put(new PropertySimple(key, userProps.getProperty(key)));
            }

            ctx.setAttribute(Constants.DEF_USER_PREFS, userPrefs);
        } catch (Exception e) {
            LOG.error("failed to load user preferences at [" + DEFAULT_USER_PREFERENCES_FILE + "]: ", e);
        }
    }

    /**
     * The table tag GUI component needs the taglib definitions. This loads those definitions.
     */
    private void loadTablePreferences(ServletContext ctx) {
        try {
            Properties tableProps = loadProperties(ctx, TAGLIB_PROPERTIES_FILE);
            ctx.setAttribute(Constants.PROPS_TAGLIB_NAME, tableProps);
        } catch (Exception e) {
            LOG.error("failed to load the taglib properties at [" + TAGLIB_PROPERTIES_FILE + "]: ", e);
        }
    }

    /**
     * Determines the internal build/version number and puts it in a context attribute.
     */
    private void loadBuildVersion(ServletContext servletContext) {
        String version = LookupUtil.getCoreServer().getVersion();
        servletContext.setAttribute(AttrConstants.RHQ_VERSION_ATTR, version);
    }

    /**
     * Load the specified properties file and return the properties.
     *
     * @param  context  the <code>ServletContext</code>
     * @param  filename the fully qualifed name of the properties file
     *
     * @return the contents of the properties file
     *
     * @throws Exception if a problem occurs while loading the file
     */
    private Properties loadProperties(ServletContext context, String filename) throws Exception {
        Properties props = new Properties();
        InputStream is = context.getResourceAsStream(filename);
        if (is != null) {
            props.load(is);
            is.close();
        }

        return props;
    }

    private void loadJavaLoggingConfiguration() {
        InputStream loggingPropertiesStream = Configurator.class.getClassLoader().getResourceAsStream(
            LOGGING_PROPERTIES_RESOURCE);
        if (loggingPropertiesStream != null) {
            try {
                LogManager.getLogManager().readConfiguration(loggingPropertiesStream);
            } catch (IOException e) {
                LOG.error("Failed to load '" + LOGGING_PROPERTIES_RESOURCE + "' from webapp classloader.");
            }
        }
    }

    private void registerDynamicPropertyRetrievers() {
        // Configures the configuration rendering to be able to support database backed dynamic configuration properties
        PropertyRenderingUtility.putDynamicPropertyRetriever(PropertyDynamicType.DATABASE,
            new DatabaseDynamicPropertyRetriever());
    }
}
