/*
 * RHQ Management Platform
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

package org.rhq.rhqtransform.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.augeas.Augeas;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.util.Glob;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.rhqtransform.AugeasRhqException;


/**
 * This class implements the augeas configuration interface by reading the settings
 * from the plugin descriptor.
 * 
 * @author Filip Drabek
 * @author Ian Springer
 *
 */

public class PluginDescriptorBasedAugeasConfiguration implements AugeasConfiguration {

    public static final String INCLUDE_GLOBS_PROP = "configurationFilesInclusionPatterns";
    public static final String EXCLUDE_GLOBS_PROP = "configurationFilesExclusionPatterns";
    public static final String AUGEAS_MODULE_NAME_PROP = "augeasModuleName";
    public static final String AUGEAS_LOAD_PATH = "augeasLoadPath";

    public static final String DEFAULT_AUGEAS_ROOT_PATH = File.listRoots()[0].getPath();
    
    private final Log log = LogFactory.getLog(this.getClass());
    protected List<AugeasModuleConfig> modules;
    protected String loadPath;

    /**
     * Instantiates new augeas configuration based on the data in the provided
     * plugin configuration.
     * See the constants in this class for the expected properties.
     * 
     * @param pluginConfiguration
     * @throws AugeasRhqException
     */
    public PluginDescriptorBasedAugeasConfiguration(String path,Configuration pluginConfiguration) throws AugeasRhqException {
        List<String> includes = determineGlobs(pluginConfiguration, INCLUDE_GLOBS_PROP);
        List<String> excludes = determineGlobs(pluginConfiguration, EXCLUDE_GLOBS_PROP);
        modules = new ArrayList<AugeasModuleConfig>();
        if (includes.isEmpty())
            throw new AugeasRhqException("At least one Include glob must be defined.");
              
        try {
          loadPath = pluginConfiguration.getSimpleValue(AUGEAS_LOAD_PATH, "");
           if (loadPath.equals("")){
        	   loadPath = path; 
             }
       
        AugeasModuleConfig config = new AugeasModuleConfig();
          config.setIncludedGlobs(includes);
          config.setExcludedGlobs(excludes);          
          config.setLensPath(getAugeasModuleName(pluginConfiguration) + ".lns");
          config.setModuletName(getAugeasModuleName(pluginConfiguration));
        modules.add(config);
        
        }catch(Exception e){
        	log.error("Creation of temporary Directory for augeas lens failed.");
        	throw new AugeasRhqException("Creation of temporary Directory for augeas lens failed.",e);
        }
    }

    protected List<String> determineGlobs(Configuration configuration, String name) {
        PropertySimple includeGlobsProp = configuration.getSimple(name);
        if (includeGlobsProp == null)
            return null;

        List<String> ret = new ArrayList<String>();
        ret.addAll(getGlobList(includeGlobsProp));

        return ret;
    }

    protected String getAugeasModuleName(Configuration configuration) {
        return (configuration.getSimpleValue(AUGEAS_MODULE_NAME_PROP, null));
    }

    public static PropertySimple getGlobList(String name, List<String> simples) {
        StringBuilder bld = new StringBuilder();
        if (simples != null) {
            for (String s : simples) {
                bld.append(s).append("|");
            }
        }
        if (bld.length() > 0) {
            bld.deleteCharAt(bld.length() - 1);
        }
        return new PropertySimple(name, bld);
    }

    public static List<String> getGlobList(PropertySimple list) {
        if (list != null && list.getStringValue() != null) {
            return Arrays.asList(list.getStringValue().split("\\s*\\|\\s*"));
        } else {
            return Collections.emptyList();
        }
    }

    public Configuration updateConfiguration(Configuration configuration) throws AugeasRhqException {
        if (modules.isEmpty())
            throw new AugeasRhqException("Error in augeas Configuration.");
        AugeasModuleConfig tempModule = modules.get(0);

        PropertySimple includeProps = getGlobList(INCLUDE_GLOBS_PROP, tempModule.getIncludedGlobs());
        PropertySimple excludeProps = getGlobList(EXCLUDE_GLOBS_PROP, tempModule.getExcludedGlobs());
        configuration.put(includeProps);
        configuration.put(excludeProps);

        return configuration;
    }

    public String getLoadPath() {
        return loadPath;
    }

    public int getMode() {
        return Augeas.NO_MODL_AUTOLOAD;
    }

    public List<AugeasModuleConfig> getModules() {
        return modules;
    }

    public String getRootPath() {
        return DEFAULT_AUGEAS_ROOT_PATH;
    }

    public AugeasModuleConfig getModuleByName(String name) {
        for (AugeasModuleConfig module : modules) {
            if (module.getModuletName().equals(name))
                return module;
        }
        return null;
    }
    
	public void loadFiles() {
		  File root = new File(getRootPath());

		  for (AugeasModuleConfig module : modules){
	        List<String> includeGlobs = module.getIncludedGlobs();

	        if (includeGlobs.size() <= 0) {
	            throw new IllegalStateException("Expecting at least once inclusion pattern for configuration files.");
	        }

	        List<File> files = Glob.matchAll(root, includeGlobs, Glob.ALPHABETICAL_COMPARATOR);

	        if (module.getExcludedGlobs() != null) {
	            List<String> excludeGlobs = module.getExcludedGlobs();
	            Glob.excludeAll(files, excludeGlobs);
	        }

	        for (File configFile : files) {
	            if (!configFile.isAbsolute()) {
	                throw new IllegalStateException("Configuration files inclusion patterns contain a non-absolute file.");
	            }
	            if (!configFile.exists()) {
	                throw new IllegalStateException("Configuration files inclusion patterns refer to a non-existent file.");
	            }
	            if (configFile.isDirectory()) {
	                throw new IllegalStateException("Configuration files inclusion patterns refer to a directory.");
	            }
	            if (!module.getConfigFiles().contains(configFile.getAbsolutePath()))
	                module.addConfigFile(configFile.getAbsolutePath());
	        }
		  }
	}

}
