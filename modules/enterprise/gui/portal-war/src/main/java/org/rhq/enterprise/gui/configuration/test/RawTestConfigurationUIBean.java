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
package org.rhq.enterprise.gui.configuration.test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.definition.ConfigurationFormat;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.gui.configuration.propset.ConfigurationSetMember;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.Outcomes;

/**
 * @author Ian Springer
 */
public class RawTestConfigurationUIBean extends AbstractTestConfigurationUIBean {
    public RawTestConfigurationUIBean() {
        setConfigurationDefinition(TestConfigurationFactory.createConfigurationDefinition());
        setConfiguration(TestConfigurationFactory.createConfiguration());
        List<ConfigurationSetMember> members = new ArrayList(GROUP_SIZE);
        for (int i = 0; i < GROUP_SIZE; i++) {
            Configuration configuration = getConfiguration().deepCopy(true);
            configuration.setId(i + 1);
            configuration.getSimple("String1").setStringValue(UUID.randomUUID().toString());
            configuration.getSimple("Integer").setStringValue(String.valueOf(i + 1));
            configuration.getSimple("Boolean").setStringValue(String.valueOf(i % 2 == 0));
            if (i == 0)
                configuration.getMap("OpenMapOfSimples").put(new PropertySimple("PROCESSOR_CORES", "4"));
            ConfigurationSetMember memberInfo = new ConfigurationSetMember(LABELS[GROUP_SIZE % LABELS.length],
                configuration);
            members.add(memberInfo);
        }
        setConfigurationSet(new ConfigurationSet(getConfigurationDefinition(), members));
        // Unwrap the Hibernate proxy objects, which Facelets appears not to be able to handle.
        setProperties(new ArrayList<Property>(this.getConfiguration().getProperties()));

        getConfigurationDefinition().setConfigurationFormat(ConfigurationFormat.STRUCTURED_AND_RAW);

        RawConfiguration rawConfiguration = new RawConfiguration();
        rawConfiguration.setPath("/tmp/nowhere.txt");
        rawConfiguration.setContents("A=1".getBytes());
        getConfiguration().addRawConfiguration(rawConfiguration);

    }

    public static final String MANAGED_BEAN_NAME = RawTestConfigurationUIBean.class.getSimpleName();
    protected static final String SUCCESS_OUTCOME = "success";
    protected static final String FAILURE_OUTCOME = "failure";

    public String updateConfiguration() {
        // Any values changed in the group config (i.e. via the inputs on the main page) need to be
        // applied to all member configs before persisting them.
        //getConfigurationSet().applyGroupConfiguration();
        // TODO (low priority): Persist the test config somewhere.
        FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Configuration updated.");
        return Outcomes.SUCCESS;
    }
}