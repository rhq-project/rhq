/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.content.Architecture;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Jay Shaughnessy
 * @author Greg Hinkle
 */
public class ResourceFactoryInfoStep extends AbstractWizardStep {

    private LocatableDynamicForm form;
    private AbstractResourceFactoryWizard wizard;
    private Map<String, ConfigurationTemplate> templates;
    private String namePrompt;
    private String versionPrompt;
    private String architecturePrompt;
    private String templatePrompt;

    private SelectItem selectArchItem;
    private LinkedHashMap<String, Integer> selectArchValues;

    public ResourceFactoryInfoStep(AbstractResourceFactoryWizard wizard, String namePrompt, String templatePrompt,
        Map<String, ConfigurationTemplate> templates) {

        this(wizard, namePrompt, null, null, templatePrompt, templates);
    }

    public ResourceFactoryInfoStep(AbstractResourceFactoryWizard wizard, String namePrompt, String versionPrompt,
        String architecturePrompt, String templatePrompt, Map<String, ConfigurationTemplate> templates) {
        this.wizard = wizard;
        this.namePrompt = namePrompt;
        this.versionPrompt = versionPrompt;
        this.architecturePrompt = architecturePrompt;
        this.templatePrompt = templatePrompt;
        this.templates = templates;
    }

    public Canvas getCanvas(Locatable parent) {
        if (form == null) {

            if (parent != null) {
                form = new LocatableDynamicForm(parent.extendLocatorId("ResFactInfo"));
            } else {
                form = new LocatableDynamicForm("ResFactInfo");
            }
            form.setNumCols(1);
            List<FormItem> formItems = new ArrayList<FormItem>(2);

            if (null != namePrompt) {
                TextItem nameItem = new TextItem("resourceName", namePrompt);
                nameItem.setRequired(true);
                nameItem.setTitleOrientation(TitleOrientation.TOP);
                nameItem.setAlign(Alignment.LEFT);
                nameItem.setWidth(300);

                nameItem.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent event) {
                        Object value = event.getValue();
                        if (value == null) {
                            value = "";
                        }
                        wizard.setNewResourceName(value.toString());
                    }
                });
                formItems.add(nameItem);
            }

            if (null != versionPrompt) {
                TextItem versionItem = new TextItem("version", versionPrompt);
                versionItem.setRequired(true);
                versionItem.setTitleOrientation(TitleOrientation.TOP);
                versionItem.setAlign(Alignment.LEFT);
                versionItem.setWidth(300);
                versionItem.setValue("0");
                wizard.setNewResourceVersion("0");

                versionItem.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent event) {
                        Object value = event.getValue();
                        if (value == null) {
                            value = "";
                        }
                        wizard.setNewResourceVersion(value.toString());
                    }
                });
                formItems.add(versionItem);
            }

            if (null != architecturePrompt) {
                selectArchItem = new SelectItem("selectArch", architecturePrompt);
                selectArchItem.setRequired(true);
                selectArchItem.disable();
                selectArchItem.setTitleOrientation(TitleOrientation.TOP);
                selectArchItem.setAlign(Alignment.LEFT);
                selectArchItem.setWidth(300);
                setSelectArchItemValues();

                selectArchItem.addChangedHandler(new ChangedHandler() {
                    public void onChanged(ChangedEvent event) {
                        Object value = event.getValue();
                        if (value != null) {
                            wizard.setNewResourceArchitectureId((Integer) value);
                        }
                    }
                });
                formItems.add(selectArchItem);
            }

            if (null != templatePrompt) {
                SelectItem templateSelect = new SelectItem("template", templatePrompt);
                templateSelect.setTitleOrientation(TitleOrientation.TOP);
                templateSelect.setAlign(Alignment.LEFT);
                templateSelect.setWidth(300);

                if (templates != null && !templates.isEmpty()) {
                    templateSelect.setValueMap(templates.keySet().toArray(new String[templates.size()]));
                    templateSelect.setValue("default");
                    wizard.setNewResourceStartingConfiguration(templates.get("default").createConfiguration());
                    templateSelect.addChangedHandler(new ChangedHandler() {
                        public void onChanged(ChangedEvent event) {
                            Object value = event.getValue();
                            if (value == null) {
                                value = "";
                            }
                            wizard.setNewResourceStartingConfiguration(templates.get(value).createConfiguration());
                        }
                    });
                } else {
                    templateSelect.setDisabled(true);
                }

                formItems.add(templateSelect);
            }

            form.setItems(formItems.toArray(new FormItem[formItems.size()]));
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate();
    }

    public String getName() {
        return MSG.widget_resourceFactoryWizard_infoStepName();
    }

    public String getResourceName() {
        return form.getValueAsString("resourceName");
    }

    public String getVersion() {
        return form.getValueAsString("version");
    }

    public Integer getArchitectureId() {
        return (Integer) form.getValue("selectArch");
    }

    public Configuration getStartingConfiguration() {
        String template = form.getValueAsString("template");
        if (template == null) {
            template = "default";
        }
        return templates.get(template).createConfiguration();
    }

    private void setSelectArchItemValues() {

        // get all known architectures
        GWTServiceLookup.getContentService().getArchitectures(new AsyncCallback<List<Architecture>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.widget_resourceFactoryWizard_infoStep_loadFail(), caught);
            }

            public void onSuccess(List<Architecture> result) {
                selectArchValues = new LinkedHashMap<String, Integer>();

                for (Architecture arch : result) {
                    selectArchValues.put(arch.getName(), arch.getId());
                }

                selectArchItem.setValueMap(selectArchValues);
                selectArchItem.setValue("noarch");
                wizard.setNewResourceArchitectureId(selectArchValues.get("noarch"));
                selectArchItem.enable();
                selectArchItem.redraw();
            }
        });

    }
}
