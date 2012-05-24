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

package org.rhq.enterprise.gui.coregui.client.test;

import java.util.EnumSet;
import java.util.LinkedHashMap;

import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SliderItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Option;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

public class TestMessageCenterView extends LocatableVLayout {

    public TestMessageCenterView(String locatorId) {
        super(locatorId);
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
        severityMenu.setValueMap(severityChoices);
        severityMenu.setDefaultValue(Severity.Blank.name());

        final SelectItem optionMenu = new SelectItem();
        optionMenu.setTitle("Message Options");
        optionMenu.setWidth(200);
        optionMenu.setMultiple(true);
        optionMenu.setMultipleAppearance(MultipleAppearance.GRID);
        optionMenu.setValueMap(optionChoices);
        optionMenu.setAllowEmptyValue(true);

        final SliderItem exceptionItem = new SliderItem();
        exceptionItem.setTitle("Exception Depth");
        exceptionItem.setWidth(250);
        exceptionItem.setMinValue(0);
        exceptionItem.setMaxValue(10);
        exceptionItem.setDefaultValue(0);

        final TextItem conciseMessageItem = new TextItem("conciseMessage", "Message");

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
                Message msg;
                Number exceptionDepth = (Number) exceptionItem.getValue();
                if (exceptionDepth != null && exceptionDepth.intValue() > 0) {
                    Throwable t = null;
                    for (int depth = exceptionDepth.intValue(); depth > 0; depth--) {
                        if (t == null) {
                            t = new Throwable("Innermost exception here at depth #" + depth);
                        } else {
                            t = new Throwable("Exception at depth #" + depth, t);
                        }
                    }
                    msg = new Message(conciseMessage, t, severity, options);
                } else {
                    msg = new Message(conciseMessage, "When there is no exception, a detailed message can go here.",
                        severity, options);
                }
                CoreGUI.getMessageCenter().notify(msg);
            }
        });

        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("form"));
        form.setItems(severityMenu, optionMenu, exceptionItem, conciseMessageItem, button);

        addMember(form);
    }
}
