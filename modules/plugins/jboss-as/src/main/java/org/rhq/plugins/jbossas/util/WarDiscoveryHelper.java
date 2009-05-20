/*
 * Jopr Management Platform
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
package org.rhq.plugins.jbossas.util;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.xml.sax.InputSource;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.plugins.jbossas.ApplicationComponent;
import org.rhq.plugins.jbossas.EmbeddedWarDiscoveryComponent;
import org.rhq.plugins.jbossas.JBossASServerComponent;
import org.rhq.plugins.jbossas.WarComponent;
import org.rhq.plugins.jbossas.WarDiscoveryComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * Provides helper methods that are used by both {@link WarDiscoveryComponent} and {@link EmbeddedWarDiscoveryComponent}
 *
 * @author Ian Springer
 * @author Heiko W. Rupp
 */
public class WarDiscoveryHelper {
    private static final Log LOG = LogFactory.getLog(WarDiscoveryHelper.class);

    public static final String JBOSS_WEB_MBEAN_NAME_TEMPLATE = "jboss.web:J2EEApplication=none,J2EEServer=none,j2eeType=WebModule,name=%name%";
    private static final String ROOT_WEBAPP_RT_LOG_FILE_NAME_BASE = "ROOT";
    private static final String RT_LOG_FILE_NAME_SUFFIX = "_rt.log";

    // regex for web-services container-generated war filenames,
    // e.g. rhq.ear-on-enterprise-server-ejb.ejb346088.war
    private static final String WEB_SERVICES_EJB_WAR_FILE_NAME_REGEX = "\\.(ejb|jar)\\d+\\.war$";

    // precompile the pattern for optimal performance
    private static final Pattern WEB_SERVICES_EJB_WAR_FILE_NAME_PATTERN = Pattern
        .compile(WEB_SERVICES_EJB_WAR_FILE_NAME_REGEX);
    private static final String ROOT_WEBAPP_RESOURCE_NAME = "ROOT.war";

    private WarDiscoveryHelper() {
    }

    public static Set<DiscoveredResourceDetails> initPluginConfigurations(
            JBossASServerComponent parentJBossASComponent, Set<DiscoveredResourceDetails> warResources,
            ApplicationComponent parentEARComponent) {

        EmsConnection jmxConnection = parentJBossASComponent.getEmsConnection();
        File configPath = parentJBossASComponent.getConfigurationPath();
        File logDir = new File(configPath, "log");
        File rtLogDir = new File(logDir, "rt");

        List<String> objectNames = new ArrayList<String>();

        // Create a List of objectNames that will be passed to DeploymentUtility.
        for (DiscoveredResourceDetails resourceDetails : warResources) {
            Configuration warConfig = resourceDetails.getPluginConfiguration();
            PropertySimple objectNameProperty = warConfig.getSimple(MBeanResourceComponent.OBJECT_NAME_PROP);
            objectNames.add(objectNameProperty.getStringValue());
        }

        // Get the list of deployed modules
        Map<String, List<WarDeploymentInformation>> deploymentInformations = DeploymentUtility
            .getWarDeploymentInformation(jmxConnection, objectNames);

        Set<DiscoveredResourceDetails> resultingResources = new HashSet<DiscoveredResourceDetails>();

        // Loop over the discovered war files and try to fill in missing information
        for (Iterator<DiscoveredResourceDetails> warResourcesIterator = warResources.iterator(); warResourcesIterator
            .hasNext();) {
            DiscoveredResourceDetails discoResDetail = warResourcesIterator.next();
            Configuration warConfig = discoResDetail.getPluginConfiguration();
            PropertySimple objectNameProperty = warConfig.getSimple(MBeanResourceComponent.OBJECT_NAME_PROP);
            List<WarDeploymentInformation> deploymentInfoList = null;

            if (deploymentInformations != null) {
                deploymentInfoList = deploymentInformations.get(objectNameProperty.getStringValue());
            }

            if (deploymentInfoList != null) {
                /*
                 * Loop over the list elements and create new resources for all the vhosts
                 * that are not localhost. This is needed so that the majority of installed
                 * webapps will just be found with their existing key in case of an update.
                 */
                for (WarDeploymentInformation info : deploymentInfoList) {
                    String vhost = info.getVHost();
                    if ("localhost".equals(vhost)) {
                        initPluginConfiguration(info, rtLogDir, warResourcesIterator, discoResDetail);
                        resultingResources.add(discoResDetail);
                    } else {
                        DiscoveredResourceDetails myDetail;
                        String key = discoResDetail.getResourceKey();
                        key += ",vhost=" + vhost;

                        Configuration configClone;
                        try {
                            configClone = discoResDetail.getPluginConfiguration().clone();
                        } catch (CloneNotSupportedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            configClone = new Configuration();
                        }

                        myDetail = new DiscoveredResourceDetails(discoResDetail.getResourceType(), key, discoResDetail
                            .getResourceName(), discoResDetail.getResourceVersion(), discoResDetail
                            .getResourceDescription()
                            + " on (" + vhost + ")", configClone, discoResDetail.getProcessInfo());

                        initPluginConfiguration(info, rtLogDir, warResourcesIterator, myDetail);
                        resultingResources.add(myDetail); // the cloned one
                    }
                }
            } else { // Our war was not in the list, the main deployer's listDeployedModules()
                if (parentEARComponent==null) {
                    // WAR has no associated context root, so remove it from the list of discovered resources...
                    // @todo Might not want to call remove, because, as an example, in EC, the EC war file would end up being removed because this happens before it is completely deployed
                    LOG.warn("D--##--  Would remove " + discoResDetail); // TODO remove for production
                   //  warResourcesIterator.remove();

                    if (!discoResDetail.getResourceName().equals(ROOT_WEBAPP_RESOURCE_NAME)) {
                        LOG
                            .debug("The deployed WAR '"
                                + discoResDetail.getResourceName()
                                + "' does not have a jboss.web MBean (i.e. context root) associated with it; it will not be added to inventory.");
                    }
                } else { // WAR within an EAR and distributable flag set in the war

                    // Search for the parent ear and its deployment-descritptor(s) and read the contextroot from there
                    String appName = parentEARComponent.getApplicationName();
                    String warName = objectNameProperty.getStringValue();
                    Pattern pat = Pattern.compile(".*,name=([\\w-]+\\.war).*");
                    Matcher m = pat.matcher(warName);
                    if (m.find()) {
                        warName = m.group(1);
                    }
                    String contextRoot = getContextRootFromEar(parentEARComponent,parentJBossASComponent,warName);
                    warConfig.put(new PropertySimple(WarComponent.CONTEXT_ROOT_CONFIG_PROP,contextRoot));

                    String webName = getJbossWebNameFromContextRoot(contextRoot,parentJBossASComponent);
                    warConfig.put(new PropertySimple(WarComponent.JBOSS_WEB_NAME,webName));

                    warConfig.put(new PropertySimple(WarComponent.VHOST_CONFIG_PROP,"localhost"));
                    setRtLogInPluginConfig(rtLogDir,warConfig,contextRoot,"localhost"); // TODO check if vhost can be != localhost

                    resultingResources.add(discoResDetail);
                }
            }
        }
        return resultingResources;
    }



    /**
     * Try to get the context root from the ear file this war is embedded in
     * @param parentEARComponent the ApplicationComponent representing the EAR
     * @param parentJBossASComponent The JBossAS instance the ear is deployed in
     * @param warName THe name of the war file we are looking for
     * @return context root of the war file
     */
    private static String getContextRootFromEar(ApplicationComponent parentEARComponent,
                                                JBossASServerComponent parentJBossASComponent,
                                                String warName) {

        String contextRoot =" - invalid - ";

        // Get the ear and then the deploymentDescriptor attribute from it - this directly contains the one and
        // only context-root
        String objectNameTemplate = "jboss.management.local:J2EEServer=Local,j2eeType=J2EEApplication,name=" + parentEARComponent.getApplicationName();
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(objectNameTemplate);
        List<EmsBean> mBeans = parentJBossASComponent.getEmsConnection().queryBeans(queryUtility.getTranslatedQuery());
        if (mBeans.size() ==1) {
            EmsBean theBean = mBeans.get(0);
            EmsAttribute ddAttr = theBean.getAttribute("deploymentDescriptor");
            ddAttr.refresh();
            ddAttr.getValue();
            String dd = (String) ddAttr.getValue();
            // dd contains application.xml

            InputSource is = new InputSource(new StringReader(dd));

            XPath xp = XPathFactory.newInstance().newXPath();

            try {
                contextRoot = xp.evaluate("/application/module/web/web-uri['" + warName +"']/../context-root",is);
            }
            catch (XPathException xpe) {
                xpe.printStackTrace(); // TODO fix this

            }

        }


        return contextRoot;
    }


    private static String getJbossWebNameFromContextRoot(String contextRoot,
                                                       JBossASServerComponent parentJBossASComponent) {

        if (contextRoot.startsWith("/"))
            contextRoot = contextRoot.substring(1);
        String objectNameTemplate = "jboss.web:j2eeType=WebModule,name=//localhost/"+ contextRoot + ",J2EEApplication=none,J2EEServer=none";
        ObjectNameQueryUtility queryUtility = new ObjectNameQueryUtility(objectNameTemplate);
        List<EmsBean> mBeans = parentJBossASComponent.getEmsConnection().queryBeans(queryUtility.getTranslatedQuery());
        if (mBeans.size() ==1) {
            EmsBean theMBean = mBeans.get(0);
            return theMBean.getBeanName().getCanonicalName();
        }

        return null;
    }

    public static void setDeploymentInformation(Configuration pluginConfig,
        WarDeploymentInformation deploymentInformation) {
        // JBNADM-3420 - These are being set incorrectly. Specifically, the incorrect file name is causing invalid
        // file names on windows and thus breaking WAR updates.
        pluginConfig.put(new PropertySimple(WarComponent.FILE_NAME, deploymentInformation.getFileName()));
        pluginConfig.put(new PropertySimple(WarComponent.JBOSS_WEB_NAME, deploymentInformation
            .getJbossWebModuleMBeanObjectName()));
        pluginConfig.put(new PropertySimple(WarComponent.CONTEXT_ROOT_CONFIG_PROP, deploymentInformation
            .getContextRoot()));
        pluginConfig.put(new PropertySimple(WarComponent.VHOST_CONFIG_PROP, deploymentInformation.getVHost()));
    }

    public static String getContextPath(String contextRoot) {
        return ((contextRoot.equals(WarComponent.ROOT_WEBAPP_CONTEXT_ROOT)) ? "/" : "/" + contextRoot);
    }

    private static void initPluginConfiguration(WarDeploymentInformation deploymentInformation, File rtLogDir,
        Iterator<DiscoveredResourceDetails> warResourcesIterator, DiscoveredResourceDetails resource) {

        Configuration pluginConfig = resource.getPluginConfiguration();
        String warFileName = resource.getResourceName();
        String contextRoot = deploymentInformation.getContextRoot();
        int length = contextRoot.length();
        if (length > 0 && (contextRoot.charAt(length - 1) == '.') // e.g. "//localhost/rhq-rhq-enterprise-server-ejb."
            && WEB_SERVICES_EJB_WAR_FILE_NAME_PATTERN.matcher(warFileName).find()) {
            // It's a web-services container-generated war - we don't want to auto-discover
            // these, since they're not explicitly deployed by the user and their names change
            // on every redeploy of the parent ear (see http://jira.jboss.com/jira/browse/JBNADM-2728).
            // So remove it from the list of discovered resources and move on...
            warResourcesIterator.remove();
            return;
        }

        WarDiscoveryHelper.setDeploymentInformation(pluginConfig, deploymentInformation);

        // Set the default value for the 'responseTimeLogFile' plugin config prop.
        // We do it here because the filename is derived from the context root.
        // first check if the context root is a multi level one and replace the / with an underscore
        String vHost = deploymentInformation.getVHost();
        setRtLogInPluginConfig(rtLogDir, pluginConfig, contextRoot, vHost);
    }

    private static void setRtLogInPluginConfig(File rtLogDir, Configuration pluginConfig, String contextRoot,
                                               String vHost) {
        if (!contextRoot.equals("/")) {
            contextRoot = contextRoot.replace('/', '_');
        }
        String rtLogFileNameBase = (contextRoot.equals(WarComponent.ROOT_WEBAPP_CONTEXT_ROOT)) ? ROOT_WEBAPP_RT_LOG_FILE_NAME_BASE
            : contextRoot;
        if ("localhost".equals(vHost))
            vHost = "";
        else
            vHost = vHost + "_";
        String rtLogFileName = vHost + rtLogFileNameBase + RT_LOG_FILE_NAME_SUFFIX;
        File rtLogFile = new File(rtLogDir, rtLogFileName);
        pluginConfig.put(new PropertySimple(WarComponent.RESPONSE_TIME_LOG_FILE_CONFIG_PROP, rtLogFile));
    }
}
