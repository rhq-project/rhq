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

import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlOutputText;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.gui.configuration.ConfigRenderer;
import org.rhq.core.gui.configuration.CssStyleClasses;
import org.rhq.core.gui.configuration.propset.ConfigurationSet;
import org.rhq.core.gui.configuration.propset.ConfigurationSetComponent;
import org.rhq.core.gui.configuration.propset.ConfigurationSetMember;
import org.rhq.core.gui.util.FacesExpressionUtility;
import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.enterprise.gui.common.Outcomes;
import org.ajax4jsf.component.html.HtmlAjaxCommandLink;

/**
 * @author Ian Springer
 */
public abstract class AbstractTestConfigurationUIBean {
    public static final String [] LABELS = new String[] { "AAA", "ZZZ", "BBB", "YYY","AAA", "AAA", "ZZZ", "ZZZ", "YYY", "BBB"};

    private ConfigurationDefinition configurationDefinition;
    private Configuration configuration;
    private List<Property> properties;
    private ConfigurationSet configurationSet;
    private ConfigurationSetComponent configurationSetComponent;
    private HtmlPanelGroup panelGroup;

    protected AbstractTestConfigurationUIBean()
    {
        this.configurationDefinition = TestConfigurationFactory.createConfigurationDefinition();
        this.configuration = TestConfigurationFactory.createConfiguration();
        List<ConfigurationSetMember> members = new ArrayList(10);
        for (int i = 0; i < 10; i++) {
            Configuration configuration = this.configuration.deepCopy(true);
            configuration.setId(i + 1);
            configuration.getSimple("String1").setStringValue(UUID.randomUUID().toString());
            configuration.getSimple("Integer").setStringValue(String.valueOf(i + 1));
            configuration.getSimple("Boolean").setStringValue(String.valueOf(i % 2 == 0));
            if (i == 0)
                configuration.getMap("OpenMapOfSimples").put(new PropertySimple("PROCESSOR_CORES", "4"));
            ConfigurationSetMember memberInfo =
                    new ConfigurationSetMember(LABELS[i], configuration);
            members.add(memberInfo);
        }
        this.configurationSet = new ConfigurationSet(this.configurationDefinition, members);
        // Unwrap the Hibernate proxy objects, which Facelets appears not to be able to handle.
        this.properties = new ArrayList<Property>(this.configuration.getProperties());
    }

    public String finish() {
        return Outcomes.SUCCESS;
    }

    @Nullable
    public ConfigurationDefinition getConfigurationDefinition() {
        return this.configurationDefinition;
    }

    public void setConfigurationDefinition(@NotNull
    ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    @Nullable
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(@NotNull
    Configuration configuration) {
        this.configuration = configuration;
    }

    public ConfigurationSet getConfigurationSet()
    {
        return configurationSet;
    }

    public void setConfigurationSet(ConfigurationSet configurationSet)
    {
        this.configurationSet = configurationSet;
    }

    public List<Property> getProperties()
    {
        return this.properties;
    }

    public String getNullConfigurationDefinitionMessage() {
        return "Test config def is null (should never happen).";
    }

    public String getNullConfigurationMessage() {
        return "Test config is null (should never happen).";
    }

    public ConfigurationSetComponent getConfigurationSetComponent() {        
        return this.configurationSetComponent;
    }

    public void setConfigurationSetComponent(ConfigurationSetComponent configurationSetComponent) {
        this.configurationSetComponent = configurationSetComponent;
        this.configurationSetComponent.setValueExpression("configurationSet",
                FacesExpressionUtility.createValueExpression("#{EditTestConfigurationUIBean.configurationSet}",
                        ConfigurationSet.class));
        ConfigRenderer renderer = new ConfigRenderer();
        this.configurationSetComponent.getChildren().clear();
        renderer.addChildComponents(configurationSetComponent);
    }

    public HtmlPanelGroup getPanelGroup() {
        return this.panelGroup;
    }

    public void setPanelGroup(HtmlPanelGroup panelGroup) {
        this.panelGroup = panelGroup;
        this.panelGroup.getChildren().clear();
        HtmlOutputText outputText = FacesComponentUtility.createComponent(HtmlOutputText.class);
        outputText.setValue("ABCDE" + UUID.randomUUID());
        this.panelGroup.getChildren().add(outputText);
        HtmlAjaxCommandLink ajaxCommandLink = FacesComponentUtility.createComponent(HtmlAjaxCommandLink.class);
        this.panelGroup.getChildren().add(ajaxCommandLink);
             //ajaxCommandLink.setOncomplete("Richfaces.showModalPanel('" +
             //        this.memberValuesModalPanel.getClientId(FacesContext.getCurrentInstance()) + "');");
             //String propertySetFormId = this.config.getId() + "PropertySetForm";
             //ajaxCommandLink.setReRender(":" + propertySetFormId + ":rhq_propSet");
             //ajaxCommandLink.setReRender("rhq_configSet");
        ajaxCommandLink.setTitle("Refresh");
        //FacesComponentUtility.addParameter(ajaxCommandLink, null, "propertyExpressionString",
        //        input.getValueExpression("value").getExpressionString());
        FacesComponentUtility.addParameter(ajaxCommandLink, null, "reload", "true");
        FacesComponentUtility.addButton(ajaxCommandLink, "Refresh", CssStyleClasses.BUTTON_SMALL);
    }
}