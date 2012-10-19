/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.plugins.cassandra;

import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.FAILURE;
import static org.rhq.core.domain.configuration.ConfigurationUpdateStatus.SUCCESS;
import static org.rhq.plugins.cassandra.CassandraUtil.getCluster;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.exceptions.HectorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * @author John Sanda
 */
public class ColumnFamilyComponent extends MBeanResourceComponent<JMXComponent<?>> {

    private Log log = LogFactory.getLog(ColumnFamilyComponent.class);

    @Override
    public Configuration loadResourceConfiguration() {
        Configuration config = super.loadResourceConfiguration();

        if (log.isDebugEnabled()) {
            ResourceContext<?> context = getResourceContext();
            log.debug("Loading resource context for column family " + context.getResourceKey());
        }

        ColumnFamilyDefinition cfDef = getColumnFamilyDefinition();
        config.put(new PropertySimple("gc_grace_seconds", cfDef.getGcGraceSeconds()));

        return config;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("repair")) {
            String columnFamilyName = this.getResourceContext().getPluginConfiguration().getSimpleValue("name");
            return this.getParentKeyspace().repairKeyspace(columnFamilyName);
        } else if (name.equals("compact")) {
            String columnFamilyName = this.getResourceContext().getPluginConfiguration().getSimpleValue("name");
            return this.getParentKeyspace().compactKeyspace(columnFamilyName);
        } else if (name.equals("takeSnapshot")) {
            String columnFamilyName = this.getResourceContext().getPluginConfiguration().getSimpleValue("name");
            return this.getParentKeyspace().takeSnapshot(parameters, columnFamilyName);
        }

        return super.invokeOperation(name, parameters);
    };

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        ResourceContext<?> context = getResourceContext();

        if (log.isDebugEnabled()) {
            log.debug("Updating resource configuration for column family " + context.getResourceKey());
        }

        Cluster cluster = getCluster();
        ColumnFamilyDefinition cfDef = getColumnFamilyDefinition();
        Configuration updatedConfig = report.getConfiguration();

        String gcGraceSeconds = updatedConfig.getSimpleValue("gc_grace_seconds", "864000");
        cfDef.setGcGraceSeconds(Integer.parseInt(gcGraceSeconds));

        try {
            cluster.updateColumnFamily(cfDef, true);
            report.setStatus(SUCCESS);
        } catch (HectorException e) {
            String msg = "Failed to update resource configuration for column family " + context.getResourceKey();
            if (log.isDebugEnabled()) {
                log.debug(msg, e);
            } else if (log.isWarnEnabled()) {
                log.warn(msg);
            }
            report.setErrorMessageFromThrowable(e);
            report.setStatus(FAILURE);
        }

        report.getConfiguration().remove("gc_grace_seconds");
        super.updateResourceConfiguration(report);
    }

    private ColumnFamilyDefinition getColumnFamilyDefinition() {
        Configuration pluginConfig = this.getResourceContext().getPluginConfiguration();
        String cfName = pluginConfig.getSimpleValue("name");
        KeyspaceComponent<?> keyspaceComponent = this.getParentKeyspace();

        for (ColumnFamilyDefinition cfDef : keyspaceComponent.getKeyspaceDefinition().getCfDefs()) {
            if (cfName.equals(cfDef.getName())) {
                return cfDef;
            }
        }

        return null;
    }

    private KeyspaceComponent<?> getParentKeyspace() {
        return (KeyspaceComponent<?>) this.getResourceContext().getParentResourceComponent();
    }
}
