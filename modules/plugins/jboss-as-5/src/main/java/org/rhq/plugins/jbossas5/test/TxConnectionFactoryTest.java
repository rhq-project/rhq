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
package org.rhq.plugins.jbossas5.test;

import java.util.Set;
import java.util.Map;

import org.testng.annotations.Test;

import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.metatype.api.values.EnumValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.deployers.spi.management.KnownComponentTypes;

/**
 * @author Ian Springer
 */
public class TxConnectionFactoryTest extends AbstractManagedComponentTest
{
    private static final String TEMPLATE_NAME = "TxConnectionFactoryTemplate";
    private static final ComponentType COMPONENT_TYPE = KnownComponentTypes.ConnectionFactoryTypes.XA.getType();

    @Test
    public void testCreateValid() throws Exception {
        final String componentName = "foobar";
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Map<String,ManagedProperty> properties = template.getProperties();
        properties.get("jndi-name").setValue(new SimpleValueSupport(SimpleMetaType.STRING, componentName));
        properties.get("rar-name").setValue(new SimpleValueSupport(SimpleMetaType.STRING, "jms-ra.rar"));
        properties.get("connection-definition").setValue(new SimpleValueSupport(SimpleMetaType.STRING,
                "org.jboss.resource.adapter.jms.JmsConnectionFactory"));
        this.managementView.applyTemplate(componentName, template);
        this.managementView.process();
        ManagedComponent component = this.managementView.getComponent(componentName, COMPONENT_TYPE);
        assert component != null;
        assert component.getName().equals(componentName);
        assert component.getProperty("jndi-name").getValue().equals(new SimpleValueSupport(SimpleMetaType.STRING,
                componentName));
    }

    //@Test
    public void testCreateNonMandatoryPropertyPropertyNull() throws Exception {
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> nonMandatoryProperties = getNonMandatoryProperties(template);
        for (ManagedProperty nonMandatoryProperty : nonMandatoryProperties)
            template.getProperties().remove(nonMandatoryProperty.getName());
        this.managementView.applyTemplate("foobar", template);
        this.managementView.process();
    }

    //@Test
    public void testCreateNonMandatoryPropertyValueNull() throws Exception {
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> nonMandatoryProperties = getNonMandatoryProperties(template);
        for (ManagedProperty nonMandatoryProperty : nonMandatoryProperties)
            nonMandatoryProperty.setValue(null);        
        this.managementView.applyTemplate("foobar", template);
        this.managementView.process();
    }

    //@Test
    public void testCreateNonMandatoryPropertyInnerValueNull() throws Exception {
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> nonMandatoryProperties = getNonMandatoryProperties(template);
        for (ManagedProperty nonMandatoryProperty : nonMandatoryProperties) {
            MetaValue value = nonMandatoryProperty.getValue();
            if (value instanceof SimpleValueSupport) {
                SimpleValueSupport simpleValueSupport = (SimpleValueSupport)value;
                simpleValueSupport.setValue(null);
            } else if (value instanceof EnumValueSupport) {
                EnumValueSupport enumValueSupport = (EnumValueSupport)value;
                enumValueSupport.setValue(null);
            }
        }
        this.managementView.applyTemplate("foobar", template);
        this.managementView.process();
    }
}
