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
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

import bsh.EvalError;
import bsh.Interpreter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.plugins.jmx.ObjectNameQueryUtility;

/**
 * Accesses the MainDeployer mbean to find the deployment files behind services.
 *
 * @author Greg Hinkle
 */
public class DeploymentUtility {
    private static Log log = LogFactory.getLog(DeploymentUtility.class);

    /**
     * The object name of the JBoss main deployer MBean.
     */
    protected static final String MAIN_DEPLOYER = "jboss.system:service=MainDeployer";

    /**
     * The name of the main deployer operation that is to be used to get the list of the modules are deployed - this is
     * the Main Deployer operation name for JBossAS 4.x.
     */
    private static final String LIST_DEPLOYED_MODULES_OP_NAME = "listDeployedModules";

    /**
     * The name of the main deployer operation that is to be used to get the list of the modules are deployed - this is
     * the Main Deployer operation name for JBossAS 3.x.
     */
    private static final String LIST_DEPLOYED_OP_NAME = "listDeployed";

    protected static EmsOperation getListDeployedOperation(EmsConnection connection) {
        EmsOperation retOperation;
        EmsBean bean = connection.getBean(MAIN_DEPLOYER);

        // first try the new operation name, used by JBossAS 3.2.8 and 4.x.
        retOperation = bean.getOperation(LIST_DEPLOYED_MODULES_OP_NAME);

        // if that doesn't exist, we are probably connected to a JBossAS 3.2.7 or earlier version
        if (retOperation == null) {
            retOperation = bean.getOperation(LIST_DEPLOYED_OP_NAME);
        }

        // if we still did not manage to find the operation name, let the caller handle this error condition
        return retOperation;
    }

    /**
     * This will attempt to find the deployment descriptor file where the ObjectName MBean was deployed.
     *
     * @param  connection the connection to the JBoss instance
     * @param objectName The objectname to look for
     * @return the path to the file where the MBean was deployed, or <code>null</code> if it could not be found
     */
    public static File getDescriptorFile(EmsConnection connection, String objectName) {
        File retDescriptorFile = null;

        // will contain information on all deployments
        Collection deployed;
        try {
            deployed = getDeploymentInformations(connection);
        }
        catch (Exception e) {
            return null;
        }

        try {
            EmsOperation operation = getListDeployedOperation(connection);

            if (operation == null) {
                throw new UnsupportedOperationException(
                    "The JBossAS instance is unsupported; it doesn't have a listDeployed operation");
            }

            deployed = (Collection) operation.invoke(new Object[0]);
        } catch (Exception e) {
            log.warn("Cannot determine the descriptor file name for service [" + objectName + "]. Cause: " + e);

            return null;
        }

        // ask our connection object to create an ObjectName for us as a java.lang.Object - we don't want to import ObjectName ourselves
        Object ourObjectName = connection.buildObjectName(objectName);

        // used by our BSH script to see if the current deployment's list of object names contains our object name
        String testScriptObjectNameVariable = "ourObjectName";
        String testScriptContainsOurObjectName = "sdi.mbeans.contains(" + testScriptObjectNameVariable + ")";

        // find out which deployment was responsible for deploying our MBean (identified by objectName)
        Interpreter i = new Interpreter();
        for (Iterator it = deployed.iterator(); it.hasNext() && (retDescriptorFile == null);) {
            Object sdi = it.next();
            try {
                i.set("sdi", sdi);

                // this is the deployment descriptor file that we are currently examining;
                // this is what will be returned if the MBean was configured in this file.
                String file = i.eval("sdi.url").toString();

                if (file.startsWith("file:/")) {
                    file = file.substring(5);
                }

                // get the collection of MBeans that were deployed from the current deployment and
                // see if our MBean object name is among them
                i.set(testScriptObjectNameVariable, ourObjectName);
                Boolean b = (Boolean) i.eval(testScriptContainsOurObjectName);
                if (b) {
                    retDescriptorFile = new File(file); // found it! this is the file where the MBean was configured/deployed
                    break;
                }
            } catch (EvalError evalError) {
                log.warn("Failed to determine if a deployment contains our mbean", evalError);
            }
        }

        log.debug("Descriptor file for [" + objectName + "] is [" + retDescriptorFile + " ].");

        return retDescriptorFile;
    }

    /**
     * Retrieves all the discovery information for a War resources. We are retrieving all the information
     * so that there is only ever one call to the MBeanServer to get the deployed mbeans, therefore saving
     * some performance if it did this for each and every war resource one at a time.
     *
     * @param connection EmsConnection to get the mbean information
     * @param objectNames Name of the main jboss.management mbeans for a collection of wars.
     * @return map holds all the war deployment information for the objects passed in the objectNames collection
     */
    public static Map<String, List<WarDeploymentInformation>> getWarDeploymentInformation(EmsConnection connection,
        List<String> objectNames) {
        // We need a list of informations, as one jsr77 deployment can end up in multiple web apps in different vhosts
        HashMap<String, List<WarDeploymentInformation>> retDeploymentInformationMap = new HashMap<String, List<WarDeploymentInformation>>();

        // will contain information on all deployments
        Collection deployed;
        try {
            deployed = getDeploymentInformations(connection);
        }
        catch (Exception e) {
            return null;
        }

        String separator = System.getProperty("file.separator");
        boolean isOnWin = separator.equals("\\");

        // Loop through the deployments, get the name information and compare it to the collection
        // of strings passed into this method
        //        Interpreter i = new Interpreter();
        for (Object sdi : deployed) {
            try {
                ObjectName jbossWebObjectName = getFieldValue(sdi, "deployedObject", javax.management.ObjectName.class);
                if (jbossWebObjectName != null) {
                    String shortName = getFieldValue(sdi, "shortName", String.class);

                    for (String objectNameString : objectNames) {
                        String nameFromObject = getMBeanNameAttribute(objectNameString);

                        if (shortName.equals(nameFromObject)) {

                            String jbossWebmBeanName = jbossWebObjectName.getCanonicalName();
                            String contextRoot = getMBeanNameAttribute(jbossWebmBeanName);
                            /*
                            * Lets find out the real context root. The one passed is //<vhost>/<context>
                            * If it ends in a slash, it's the root context (context root -> "/"). Otherwise, the
                            * context root will be everything after the last slash (e.g. "jmx-console").
                            * BUT: We need to be careful with slashes inside the context root, as jmx/console
                            * is a valid context root as well.
                            */
                            if (contextRoot.startsWith("//")) {
                                contextRoot = contextRoot.substring(2);
                                int pos = contextRoot.indexOf("/");
                                if (pos > -1)
                                    contextRoot = contextRoot.substring(pos); // Skip vhost
                                if (contextRoot.length()>1)
                                    contextRoot = contextRoot.substring(1); // Skip leading / if cR is othern than "/"
                            }
                            else { // Fallback just in case
                                int lastSlashIndex = contextRoot.lastIndexOf("/");
                                if (lastSlashIndex > 0) {
                                    int lastIndex = contextRoot.length() - 1;
                                    contextRoot = (lastSlashIndex == lastIndex) ? "/" : contextRoot.substring(lastSlashIndex + 1);
                                }
                            }

                            String file = getFieldValue(sdi, "url", URL.class).toString();
                            if (file.startsWith("file:/")) {
                               if (isOnWin) {
                                  file = file.substring(6);
                                  // listDeployed() always delivers / as path separator, so we need to correct this.
                                  File tmp = new File(file);
                                  file = tmp.getCanonicalPath();
                               }
                               else
                                  file = file.substring(5);
                            }

                            /*
                             * We now have a valid deployment. Go back to the MBeanServer and get *all*
                             * applicable virtual hosts for this web app. Return deployment info for all
                             * of them.
                             * Most of the time this will only be one virtual host called 'localhost', as
                             * this is the default if no vhost is set.
                             */
                            List<EmsBean> vhosts = getVHosts(contextRoot, connection);
                            List<WarDeploymentInformation> infos = new ArrayList<WarDeploymentInformation>(vhosts
                                .size());

                            for (EmsBean vhost : vhosts) {
                                WarDeploymentInformation deploymentInformation = new WarDeploymentInformation();
                                String vhostname = vhost.getBeanName().getKeyProperty("host");
                                deploymentInformation.setVHost(vhostname);
                                deploymentInformation.setFileName(file);
                                deploymentInformation.setContextRoot(contextRoot);
                                // jboss.web:J2EEApplication=none,J2EEServer=none,j2eeType=WebModule,name=//bsd.de/test
                                // TODO to we have a better tool to exchange a value of a key-value pair in an ObjectName ?
                                int index = jbossWebmBeanName.indexOf("name=");
                                jbossWebmBeanName = jbossWebmBeanName.substring(0, index + 5);
                                jbossWebmBeanName += "//" + vhostname + WarDiscoveryHelper.getContextPath(contextRoot);

                                deploymentInformation.setJbossWebModuleMBeanObjectName(jbossWebmBeanName);
                                infos.add(deploymentInformation);
                            }
                            retDeploymentInformationMap.put(objectNameString, infos);
                        }
                    }
                }
            } catch (Exception evalError) {
                log.warn("Failed to determine if a deployment contains our mbean", evalError);
            }
        }
        return retDeploymentInformationMap;
    }

    /**
     * Return the path where the passed objectnames are deployed
     * @param connection 
     * @param fileNames The objectNames of the EAR files we are interested in
     * @return a Map with objectname as key and path as value. This map may be empty on error.
     */
    public static Map<String,String> getEarDeploymentPath(EmsConnection connection, List<String> fileNames) {

        Collection deploymentInfos =null;
        String separator = System.getProperty("file.separator");
        boolean isOnWin = separator.equals("\\");
        Map<String,String> results = new HashMap<String,String>(fileNames.size());

        try {
            // Get the list of deployed modules
            deploymentInfos = getDeploymentInformations(connection);
            for (Object sdi : deploymentInfos) {
                String file = getFieldValue(sdi, "url", URL.class).toString();

                // loop over the input, find the matchin entry and add to the results.
                for (String earName : fileNames) {

                    if (!file.endsWith(earName))
                        continue;

                    if (file.startsWith("file:/")) {
                       if (isOnWin) {
                          file = file.substring(6);
                          // listDeployed() always delivers / as path separator, so we need to correct this.
                          File tmp = new File(file);
                          file = tmp.getCanonicalPath();
                       }
                       else
                          file = file.substring(5);
                    }
                    results.put(earName,file);
                }
            }
        }
        catch (Exception e) {
            return new HashMap<String,String>();
        }

        return results;
    }

    public static boolean isDuplicateJndiName(EmsConnection connection, String mbeanType, String jndiName) {
        if ((jndiName != null) && (mbeanType != null)) {
            String name = mbeanType + ",name=" + jndiName;

            EmsBean bean = connection.getBean(name);
            if (bean == null) {
                return false;
            } else {
                return bean.isRegistered();
            }
        }

        return false;
    }

    public static String getJndiNameBinding(EmsBean bean) {
        String jndiNameBinding = "";
        if (bean != null) {
            EmsAttribute attribute = bean.getAttribute("JNDIName");
            attribute.refresh();
            jndiNameBinding = String.valueOf(attribute.getValue());
        }

        return jndiNameBinding;
    }

    private static Collection getDeploymentInformations(EmsConnection connection) throws Exception {
        Collection deploymentInfos = null;
        EmsOperation operation = null;
        try {
            operation = getListDeployedOperation(connection);
            if (operation == null) {
                throw new UnsupportedOperationException(
                    "This JBossAS instance is unsupported; its MainDeployer MBean doesn't have a listDeployedModules or listDeployed operation.");
            }
            deploymentInfos = (Collection) operation.invoke(new Object[0]);
        } catch (RuntimeException re) {
            // Make a last ditch effort in case the call to listDeployedModules() failed due to
            // https://jira.jboss.org/jira/browse/JBAS-5983.
            if (operation != null && operation.getName().equals(LIST_DEPLOYED_MODULES_OP_NAME)) {
                EmsBean mainDeployerMBean = connection.getBean(MAIN_DEPLOYER);
                operation = mainDeployerMBean.getOperation(LIST_DEPLOYED_OP_NAME);
                try {
                    deploymentInfos = (Collection) operation.invoke(new Object[0]);
                }
                catch (RuntimeException re2) {
                    deploymentInfos = null;
                }
            }
            if (deploymentInfos == null) {
                log.warn("Cannot determine deployed modules - cause: " + re);
                throw new Exception(re);
            }
        }
        return deploymentInfos;
    }

    private static String getMBeanNameAttribute(String objectBeanName) {
        Object[] splitName = objectBeanName.split("name=");
        String retNameAttribute = "";
        if (splitName.length > 1) {
            String afterName = (String) splitName[1];
            String[] removeAnythingAfter = afterName.split(",");
            if (removeAnythingAfter.length > 0) {
                retNameAttribute = removeAnythingAfter[0];
            } else {
                retNameAttribute = afterName;
            }
        }
        return retNameAttribute;
    }

    private static <T> T getFieldValue(Object target, String name, Class<T> T) {

        if (target == null)
            return null;

        Field field;
        T ret;
        try {
            field = target.getClass().getField(name);
            ret = (T) field.get(target);
            if (T == ObjectName.class && ret != null) {
                ret = (T) new ObjectName(ret.toString());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    /**
     * Retrieves the virtual host MBeans for the webapp with the specified context root.
     * VHost MBeans have the pattern "jboss.web:host=*,path=/<ctx_root>,type=Manager".
     *
     * @param contextRoot the context root
     *
     * @return the list of VHost MBeans for this webapp
     */
    public static List<EmsBean> getVHosts(String contextRoot, EmsConnection emsConnection) {
        String pattern = "jboss.web:host=%host%,path=" + WarDiscoveryHelper.getContextPath(contextRoot)
                + ",type=Manager";
        ObjectNameQueryUtility queryUtil = new ObjectNameQueryUtility(pattern);
        List<EmsBean> mBeans = emsConnection.queryBeans(queryUtil.getTranslatedQuery());
        return mBeans;
    }
}