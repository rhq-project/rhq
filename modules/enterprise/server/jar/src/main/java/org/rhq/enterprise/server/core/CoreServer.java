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
package org.rhq.enterprise.server.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.mx.util.MBeanServerLocator;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfig;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.util.ObjectNameFactory;

public class CoreServer extends ServiceMBeanSupport implements CoreServerMBean {
    private static final String PRODUCT_INFO_PROPERTIES_RESOURCE_PATH =
            "org/rhq/enterprise/server/core/ProductInfo.properties";

    private final Log log = LogFactory.getLog(CoreServer.class);

    /**
     * The name of the version file as found in this class's classloader
     */
    private static final String VERSION_FILE = "rhq-server-version.properties";

    /**
     * Version property whose value is the product version.
     */
    private static final String PROP_PRODUCT_VERSION = "Product-Version";

    /**
     * Version property whose value is the source code revision number used to make the build.
     */
    private static final String PROP_BUILD_NUMBER = "Build-Number";

    /**
     * Version property whose value is the date when this version of the product was built.
     */
    private static final String PROP_BUILD_DATE = "Build-Date";

    private Properties buildProps;

    private Date bootTime;

    @Override
    protected void createService() throws Exception {
        this.buildProps = loadBuildProperties();
        this.bootTime = new Date();

        // make sure our log file has an indication of the version of this server
        String version = getVersion();
        String buildNumber = getBuildNumber();
        String buildDate = this.buildProps.getProperty(PROP_BUILD_DATE, "?");
        log.info("Version=[" + version + "], Build Number=[" + buildNumber + "], Build Date=[" + buildDate + "]");
    }

    @Override
    public String getName() {
        return "RHQ Server";
    }

    public String getVersion() {
        return this.buildProps.getProperty(PROP_PRODUCT_VERSION, "?");
    }

    public String getBuildNumber() {
        return this.buildProps.getProperty(PROP_BUILD_NUMBER, "?");
    }

    public Date getBootTime() {
        return bootTime;
    }

    public File getInstallDir() {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);

        File homeDir = ((ServerConfig) mbean).getHomeDir();
        return homeDir.getParentFile(); // jboss homedir is "rhq-install-dir/jbossas", so the install dir is .. from jbossas
    }

    public File getJBossServerHomeDir() {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverHomeDir = ((ServerConfig) mbean).getServerHomeDir();
        return serverHomeDir;
    }

    public File getJBossServerDataDir() {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverDataDir = ((ServerConfig) mbean).getServerDataDir();
        return serverDataDir;
    }

    public File getJBossServerTempDir() {
        MBeanServer mbs = getMBeanServer();
        ObjectName name = ObjectNameFactory.create("jboss.system:type=ServerConfig");
        Object mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, name, ServerConfig.class, false);
        File serverTempDir = ((ServerConfig) mbean).getServerTempDir();
        return serverTempDir;
    }

    public ProductInfo getProductInfo() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(PRODUCT_INFO_PROPERTIES_RESOURCE_PATH);
        if (inputStream == null) {
            throw new IllegalStateException("Failed to find class loader resource ["
                    + PRODUCT_INFO_PROPERTIES_RESOURCE_PATH + "].");
        }
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load product info properties from class loader resource ["
                    + PRODUCT_INFO_PROPERTIES_RESOURCE_PATH + "].");
        }
        ProductInfo productInfo = new ProductInfo();
        // TODO: Using reflection below might be nicer.       
        productInfo.setBuildNumber(props.getProperty("buildNumber"));
        productInfo.setFullName(props.getProperty("fullName"));
        productInfo.setHelpDocRoot(props.getProperty("helpDocRoot"));
        productInfo.setName(props.getProperty("name"));
        productInfo.setSalesEmail(props.getProperty("salesEmail"));
        productInfo.setShortName(props.getProperty("shortName"));
        productInfo.setSupportEmail(props.getProperty("supportEmail"));
        productInfo.setUrlDomain(props.getProperty("urlDomain"));
        productInfo.setUrl(props.getProperty("url"));
        productInfo.setVersion(props.getProperty("version"));
        return productInfo;
    }

    private MBeanServer getMBeanServer() {
        return MBeanServerLocator.locateJBoss();
    }

    private Properties loadBuildProperties() {
        Properties buildProps = new Properties();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try {
            InputStream stream = classLoader.getResourceAsStream(VERSION_FILE);
            buildProps.load(stream);
            stream.close();
        } catch (Exception e) {
            log.fatal("Failed to load [" + VERSION_FILE + "] via class loader [" + classLoader + "]");
        }

        return buildProps;
    }
}