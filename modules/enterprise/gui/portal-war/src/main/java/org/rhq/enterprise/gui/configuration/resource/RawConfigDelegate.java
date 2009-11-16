/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.configuration.resource;

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeMap;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;

public class RawConfigDelegate implements Serializable {

    void populateRaws() {
        Collection<RawConfiguration> rawConfigurations = LookupUtil.getConfigurationManager()
            .getLatestResourceConfigurationUpdate(EnterpriseFacesContextUtility.getSubject(), resourceId)
            .getConfiguration().getRawConfigurations();

        for (RawConfiguration raw : rawConfigurations) {
            raws.put(raw.getPath(), raw);
        }
    }

    public RawConfigDelegate(int resourceId) {
        super();
        this.resourceId = resourceId;
    }

    public void setRaws(TreeMap<String, RawConfiguration> raws) {
        this.raws = raws;
    }

    public TreeMap<String, RawConfiguration> getRaws() {

        if (null == raws) {
            raws = new TreeMap<String, RawConfiguration>();
            populateRaws();
        }

        return raws;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        if (null == configuration) {

            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int resourceId = EnterpriseFacesContextUtility.getResource().getId();
            AbstractResourceConfigurationUpdate configurationUpdate = LookupUtil.getConfigurationManager()
                .getLatestResourceConfigurationUpdate(subject, resourceId);
            Configuration configuration = (configurationUpdate != null) ? configurationUpdate.getConfiguration() : null;
            if (configuration != null) {
                //ConfigurationMaskingUtility.maskConfiguration(configuration, getConfigurationDefinition());
            }

            return configuration;

        }
        return configuration;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -9058700205958371765L;

    int resourceId = 0;
    String selectedPath;
    private TreeMap<String, RawConfiguration> raws;
    TreeMap<String, RawConfiguration> modified = new TreeMap<String, RawConfiguration>();
    RawConfiguration current = null;
    ConfigurationDefinition configurationDefinition = null;
    private Configuration configuration;

}