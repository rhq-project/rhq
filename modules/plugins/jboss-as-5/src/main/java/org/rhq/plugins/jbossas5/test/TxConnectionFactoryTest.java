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
import java.util.UUID;

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
        final String componentName = UUID.randomUUID().toString();
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Map<String,ManagedProperty> properties = template.getProperties();
        setKnownRequiredProperties(componentName, properties);
        ManagedComponent component = createComponent(COMPONENT_TYPE, componentName, template);
        assert component.getProperty("jndi-name").getValue().equals(new SimpleValueSupport(SimpleMetaType.STRING,
                componentName));
        this.managementView.removeComponent(component);
    }

    @Test
    public void testCreateNonMandatoryPropertiesNull() throws Exception {
        final String componentName = UUID.randomUUID().toString();
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> nonMandatoryProperties = getNonMandatoryProperties(template);
        for (ManagedProperty nonMandatoryProperty : nonMandatoryProperties)
            template.getProperties().remove(nonMandatoryProperty.getName());
        setKnownRequiredProperties(componentName, template.getProperties());
        ManagedComponent component = createComponent(COMPONENT_TYPE, componentName, template);
        this.managementView.removeComponent(component);
    }

    @Test
    public void testCreateNonMandatoryPropertyValuesNull() throws Exception {
        final String componentName = UUID.randomUUID().toString();
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> nonMandatoryProperties = getNonMandatoryProperties(template);
        for (ManagedProperty nonMandatoryProperty : nonMandatoryProperties)
            nonMandatoryProperty.setValue(null);
        setKnownRequiredProperties(componentName, template.getProperties());
        ManagedComponent component = createComponent(COMPONENT_TYPE, componentName, template);
        this.managementView.removeComponent(component);
    }

    @Test
    public void testCreateNonMandatoryPropertyInnerValuesNull() throws Exception {
        final String componentName = UUID.randomUUID().toString();
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
        setKnownRequiredProperties(componentName, template.getProperties());
        ManagedComponent component = createComponent(COMPONENT_TYPE, componentName, template);
        this.managementView.removeComponent(component);
    }

    @Test
    public void testCreateMandatoryPropertiesNull() throws Exception {
        final String componentName = UUID.randomUUID().toString();
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> mandatoryProperties = getMandatoryProperties(template);
        for (ManagedProperty mandatoryProperty : mandatoryProperties)
            template.getProperties().remove(mandatoryProperty.getName());
        setKnownRequiredProperties(componentName, template.getProperties());
        createComponentWithFailureExpected(COMPONENT_TYPE, componentName, template);        
    }

    @Test
    public void testCreateMandatoryPropertyValuesNull() throws Exception {
        final String componentName = UUID.randomUUID().toString();
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> mandatoryProperties = getMandatoryProperties(template);
        for (ManagedProperty mandatoryProperty : mandatoryProperties)
            mandatoryProperty.setValue(null);
        setKnownRequiredProperties(componentName, template.getProperties());
        createComponentWithFailureExpected(COMPONENT_TYPE, componentName, template);
    }

    @Test
    public void testCreateMandatoryPropertyInnerValuesNull() throws Exception {
        final String componentName = UUID.randomUUID().toString();
        DeploymentTemplateInfo template = this.managementView.getTemplate(TEMPLATE_NAME);
        Set<ManagedProperty> mandatoryProperties = getMandatoryProperties(template);
        for (ManagedProperty mandatoryProperty : mandatoryProperties) {
            MetaValue value = mandatoryProperty.getValue();
            if (value instanceof SimpleValueSupport) {
                SimpleValueSupport simpleValueSupport = (SimpleValueSupport)value;
                simpleValueSupport.setValue(null);
            } else if (value instanceof EnumValueSupport) {
                EnumValueSupport enumValueSupport = (EnumValueSupport)value;
                enumValueSupport.setValue(null);
            }
        }
        setKnownRequiredProperties(componentName, template.getProperties());
        createComponentWithFailureExpected(COMPONENT_TYPE, componentName, template);
    }

    private void setKnownRequiredProperties(String componentName, Map<String, ManagedProperty> properties)
    {
        setSimpleStringProperty(properties, "jndi-name", componentName);
        setSimpleStringProperty(properties, "rar-name", "jms-ra.rar");
        setSimpleStringProperty(properties, "connection-definition", "org.jboss.resource.adapter.jms.JmsConnectionFactory");
    }

}
