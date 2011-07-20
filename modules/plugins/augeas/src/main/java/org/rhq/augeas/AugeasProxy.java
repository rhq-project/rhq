/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.augeas;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;
import net.augeas.AugeasException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.augeas.tree.impl.DefaultAugeasTreeBuilder;
import org.rhq.plugins.augeas.AugeasPluginLifecycleListener;

/**
 * This is the main entry point for interfacing with Augeas.
 * 
 * The proxy is supplied with {@link AugeasConfiguration} that configures
 * what and how Augeas should load and optionally with an {@link AugeasTreeBuilder} which
 * specifies how to build an in memory representation of the tree returned from Augeas
 * itself (see {@link AugeasTree}).
 * 
 * @author Filip Drabek
 * @author Ian Springer
 *
 */
public class AugeasProxy {
	
	private final Log log = LogFactory.getLog(this.getClass());
    private AugeasConfiguration config;
    private Augeas augeas;
    private List<String> modules;
    private AugeasTreeBuilder augeasTreeBuilder;
    
    /**
     * Instantiates new proxy with supplied configuration and
     * {@link DefaultAugeasTreeBuilder} as the tree builder.
     * 
     * @param config the augeas configuration.
     */
    public AugeasProxy(AugeasConfiguration config) {
        this(config, new DefaultAugeasTreeBuilder());
    }

    public AugeasProxy(AugeasConfiguration config, AugeasTreeBuilder builder) {
        this.config = config;
        this.augeasTreeBuilder = builder;
        modules = new ArrayList<String>();
    }

    /**
     * @return the Augeas configuration of the proxy
     */
    public AugeasConfiguration getConfiguration() {
        return config;
    }

    /**
     * @return the tree builder used to build augeas trees
     */
    public AugeasTreeBuilder getTreeBuilder() {
        return augeasTreeBuilder;
    }

    /**
     * Initializes and loads the Augeas tree.
     * 
     * @throws AugeasTreeException
     * @throws AugeasException if Augeas could not be initialized.
     */
    public void load() throws AugeasTreeException {
        try {
            config.loadFiles();
            augeas = new Augeas(config.getRootPath(), config.getLoadPath(), config.getMode());
    
            for (AugeasModuleConfig module : config.getModules()) {
    
                modules.add(module.getModuletName());
                augeas.set("/augeas/load/" + module.getModuletName() + "/lens", module.getLensPath());
    
                int idx = 1;
                for (String incl : module.getConfigFiles()) {
                    augeas.set("/augeas/load/" + module.getModuletName() + "/incl[" + (idx++) + "]", incl);
                }
            }
            augeas.load();
        } catch (NoClassDefFoundError e) {
            if (augeas != null) {
                try {
                    augeas.close();
                } catch (Exception e2) {
                }
                augeas = null;
            }
            throw new AugeasException("Failed to initialize Augeas. It is probably not installed.", e);
        } catch (Exception e) {
            if (augeas != null) {
                try {
                    augeas.close();
                } catch (Exception e2) {
                }
                augeas = null;
            }
            throw new AugeasException(e);
        }
    }
    
 

    /**
     * Produces the Augeas tree by loading it from augeas (if {@link #load()} wasn't called already)
     * and calling out to the tree builder to construct the tree.
     * 
     * @param moduleName the name of the Augeas module to use for loading
     * @param lazy true if the tree is lazily initialized, false for eager initialization
     * @return the tree
     * @throws AugeasTreeException if the specified module wasn't configured or if there was some error loading 
     * the tree.
     */
    public AugeasTree getAugeasTree(String moduleName, boolean lazy) throws AugeasTreeException {
        if (!modules.contains(moduleName))
            throw new AugeasTreeException("Augeas Module " + moduleName + " not found.");

        try {
            if (augeas == null)
                load();

        } catch (Exception e) {
            throw new AugeasTreeException("Loading of augeas failed");
        }

        AugeasTree tree;

        try {
            tree = augeasTreeBuilder.buildTree(this, config, moduleName, lazy);
        } catch (Exception e) {
            throw new AugeasTreeException("Error building Augeas tree.", e);
        }

        return tree;
    }

    /**
     * A helper method to produce the string representation of the tree.
     * 
     * @param path
     * @return
     */
    public String printTree(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append(path).append("    ").append(augeas.get(path)).append('\n');
        List<String> list = augeas.match(path + File.separatorChar + "*");
        for (String tempStr : list) {
            builder.append(printTree(tempStr));
        }

        return builder.toString();
    }

    /**
     * 
     * @return the underlying Augeas API instance.
     */
    public Augeas getAugeas() {
        return augeas;
    }
}
