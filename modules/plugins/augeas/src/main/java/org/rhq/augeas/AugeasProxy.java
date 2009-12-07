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
package org.rhq.augeas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.augeas.config.AugeasConfiguration;
import org.rhq.augeas.config.AugeasModuleConfig;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.augeas.tree.AugeasTreeBuilder;
import org.rhq.augeas.tree.AugeasTreeException;
import org.rhq.augeas.tree.impl.AugeasTreeLazy;
import org.rhq.augeas.tree.impl.AugeasTreeReal;
import org.rhq.augeas.tree.impl.DefaultAugeasTreeBuilder;
import org.rhq.augeas.util.Glob;

/**
 * 
 * @author Filip Drabek
 * @author Ian Springer
 *
 */
public class AugeasProxy {

    private AugeasConfiguration config;
    private Augeas augeas;
    private List<String> modules;
    private AugeasTreeBuilder augeasTreeBuilder;

    public AugeasProxy(AugeasConfiguration config) {
        this.config = config;
        modules = new ArrayList<String>();
        augeasTreeBuilder = new DefaultAugeasTreeBuilder();
    }

    public AugeasProxy(AugeasConfiguration config, AugeasTreeBuilder builder) {
        this.config = config;
        this.augeasTreeBuilder = builder;
        modules = new ArrayList<String>();
    }

    public void load() throws AugeasTreeException {
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
    }

    public AugeasTree getAugeasTree(String name, boolean lazy) throws AugeasTreeException {
        if (!modules.contains(name))
            throw new AugeasTreeException("Augeas Module " + name + " not found.");

        try {
            if (augeas == null)
                load();

        } catch (Exception e) {
            throw new AugeasTreeException("Loading of augeas failed");
        }

        AugeasModuleConfig module = null;

        for (AugeasModuleConfig conf : config.getModules()) {
            if (conf.getModuletName().equals(name)) {
                module = conf;
                break;
            }
        }
        AugeasTree tree;

        try {
            tree = augeasTreeBuilder.buildTree(this, config, name, lazy);
        } catch (Exception e) {
            throw new AugeasTreeException(e.getMessage());
        }

        return tree;
    }

    public String printTree(String path) {
        StringBuilder builder = new StringBuilder();
        builder.append(path + "    " + augeas.get(path) + '\n');
        List<String> list = augeas.match(path + File.separatorChar + "*");
        for (String tempStr : list) {
            builder.append(printTree(tempStr));
        }

        return builder.toString();
    }

    public Augeas getAugeas() {
        return augeas;
    }
}
