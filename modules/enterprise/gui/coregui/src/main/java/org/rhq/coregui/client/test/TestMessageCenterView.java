/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.coregui.client.test;

import java.util.EnumSet;
import java.util.LinkedHashMap;

import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SliderItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Option;
import org.rhq.coregui.client.util.message.Message.Severity;

public class TestMessageCenterView extends EnhancedVLayout {

    public TestMessageCenterView() {
        super();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        LinkedHashMap<String, String> severityChoices = new LinkedHashMap<String, String>();
        for (Severity sev : EnumSet.allOf(Severity.class)) {
            severityChoices.put(sev.name(), sev.name());
        }

        LinkedHashMap<String, String> optionChoices = new LinkedHashMap<String, String>();
        for (Option opt : EnumSet.allOf(Option.class)) {
            optionChoices.put(opt.name(), opt.name());
        }

        final SelectItem severityMenu = new SelectItem("severityItem", "Severity");
        severityMenu.setWidth(200);
        severityMenu.setValueMap(severityChoices);
        severityMenu.setDefaultValue(Severity.Blank.name());

        final SelectItem optionMenu = new SelectItem("optionMenu", "Message Options");
        optionMenu.setWidth(200);
        optionMenu.setMultiple(true);
        optionMenu.setMultipleAppearance(MultipleAppearance.GRID);
        optionMenu.setValueMap(optionChoices);
        optionMenu.setAllowEmptyValue(true);

        final SliderItem exceptionItem = new SliderItem("exceptionItem", "Exception Depth");
        exceptionItem.setWidth(200);
        exceptionItem.setMinValue(0);
        exceptionItem.setMaxValue(10);
        exceptionItem.setDefaultValue(0);

        final TextItem conciseMessageItem = new TextItem("conciseMessage", "Concise Message");
        conciseMessageItem.setWidth(200);
        conciseMessageItem.setValue("A concise message string.");

        final TextItem detailsMessageItem = new TextItem("detailsMessage", "Details or Root Cause Message");
        detailsMessageItem.setWidth(200);
        detailsMessageItem.setValue("The details or the inner-most exception message.");

        ButtonItem button = new ButtonItem("showMessage", "Show Message");
        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                EnumSet<Option> options = EnumSet.noneOf(Option.class);
                String[] optionsArray = optionMenu.getValues();
                if (optionsArray != null && optionsArray.length > 0) {
                    for (String optString : optionsArray) {
                        options.add(Option.valueOf(optString));
                    }
                }

                Severity severity = Severity.valueOf(severityMenu.getValueAsString());
                String conciseMessage = conciseMessageItem.getValueAsString();
                String detailsMessage = detailsMessageItem.getValueAsString();
                Message msg;
                Number exceptionDepth = (Number) exceptionItem.getValue();
                if (exceptionDepth != null && exceptionDepth.intValue() > 0) {
                    Throwable t = null;
                    for (int depth = exceptionDepth.intValue(); depth > 0; depth--) {
                        if (t == null) {
                            t = new Throwable(detailsMessage);
                        } else {
                            t = new Throwable("Exception at depth #" + depth, t);
                        }
                    }
                    msg = new Message(conciseMessage, t, severity, options);
                } else {
                    msg = new Message(conciseMessage, detailsMessage, severity, options);
                }
                CoreGUI.getMessageCenter().notify(msg);
            }
        });

        DynamicForm form = new DynamicForm();
        form.setWidth(500);
        form.setItems(severityMenu, optionMenu, exceptionItem, conciseMessageItem, detailsMessageItem, button);

        addMember(form);
    }
}
