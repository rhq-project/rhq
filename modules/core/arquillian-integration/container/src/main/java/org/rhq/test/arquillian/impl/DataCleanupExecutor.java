/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.test.arquillian.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;

import org.rhq.core.util.file.FileUtil;
import org.rhq.test.arquillian.ClearPersistedData;
import org.rhq.test.arquillian.When;
import org.rhq.test.arquillian.spi.events.PluginContainerDiscovered;

/**
 * @author Lukas Krejci
 */
public class DataCleanupExecutor {

    private static final String INVENTORY_DAT = "inventory.dat";
    private static final Log LOG = LogFactory.getLog(DataCleanupExecutor.class);

    @Inject
    private Instance<RhqAgentPluginContainer> pcContainer;

    public void process(@Observes PluginContainerDiscovered pcDiscovered) {
        doCleanup(pcDiscovered.getTestMethod(), true);
    }

    public void process(@Observes After test) {
        doCleanup(test.getTestMethod(), false);
    }

    private void doCleanup(Method testMethod, boolean isBefore) {
        ClearPersistedData clearData = testMethod.getAnnotation(ClearPersistedData.class);
        if (clearData != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Clean up for: " + testMethod);
            }

            //b a x y
            //0 0 0 nogo
            //0 0 1 nogo
            //0 1 0 go
            //0 1 1 nogo
            //1 0 0 nogo
            //1 0 1 go
            //1 1 0 go
            //1 1 1 go
            EnumSet<When> when = toEnumSet(When.class, clearData.when());
            boolean hasBefore = when.contains(When.BEFORE_TEST);
            boolean hasAfter = when.contains(When.AFTER_TEST);

            if ((!hasBefore && !hasAfter) ||
                (!hasBefore && isBefore) ||
                (hasBefore && !hasAfter && !isBefore)) {

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping clean up. Currently " + (isBefore ? "before" : "after") + " test but scheduled to run:" + when);
                }
                return;
            }

            LOG.info("Stopping Plugin Container to clean up data");
            pcContainer.get().stopPc();

            File dataDir = pcContainer.get().getConfiguration().getDataDirectory();
            File tmpDir = pcContainer.get().getConfiguration().getTemporaryDirectory();

            FileUtil.purge(tmpDir, false);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Purged temp dir");
            }

            if (clearData.ofInventory()) {
                File inventoryDat = new File(dataDir, INVENTORY_DAT);
                if (inventoryDat.exists()) {
                    inventoryDat.delete();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Purged inventory dat");
                }
            }

            if (clearData.ofDrift()) {
                FileUtil.purge(new File(dataDir, "changesets"), false);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Purged drift changesets");
                }
            }

            List<String> plugins = Arrays.asList(clearData.ofPlugins());
            if (plugins.contains(ClearPersistedData.ALL_PLUGINS)) {
                removeAllDataBut(dataDir, new String[] {"inventory.dat", "changesets"});
            } else {
                for(String n : plugins) {
                    FileUtil.purge(new File(dataDir, n), true);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Purged plugin dir: " + n);
                    }
                }
            }

            LOG.info("Starting Plugin Container after data cleanup.");
            pcContainer.get().startPc();
        }
    }

    private void removeAllDataBut(File dataDir, final String[] excludedNames) {
        File[] files = dataDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                for(String n : excludedNames) {
                    if (name.equals(n)) {
                        return false;
                    }
                }

                return true;
            }
        });

        for(File f : files) {
            FileUtil.purge(f, true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Purged dir: " + f.getName());
            }
        }
    }

    private <T extends Enum<T>> EnumSet<T> toEnumSet(Class<T> c, T... values) {
        EnumSet<T> ret = EnumSet.noneOf(c);
        for(T e : values) {
            ret.add(e);
        }

        return ret;
    }
}
