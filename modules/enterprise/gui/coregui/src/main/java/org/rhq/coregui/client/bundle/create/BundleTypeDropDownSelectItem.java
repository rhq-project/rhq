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
package org.rhq.coregui.client.bundle.create;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.bundle.BundleType;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * Provides a drop down menu that allows one to select a bundle type.
 */
public class BundleTypeDropDownSelectItem extends SortedSelectItem {

    private final HashMap<String, BundleType> knownBundleTypes = new HashMap<String, BundleType>();
    private BundleType selected = null;

    public BundleTypeDropDownSelectItem(String name) {
        super(name, CoreGUI.getMessages().view_bundle_bundleType());
        buildDropDownMenu();
    }

    public BundleType getSelected() {
        return selected;
    }

    private void setSelected(BundleType selected) {
        this.selected = selected;
    }

    private void buildDropDownMenu() {
        setVisible(false);
        setDisabled(true);
        setTitleAlign(Alignment.LEFT);
        setAllowEmptyValue(false);
        setMultiple(false);

        addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                BundleType bundleType = knownBundleTypes.get(event.getValue());
                setSelected(bundleType);
            }
        });

        GWTServiceLookup.getBundleService().getAllBundleTypes(new AsyncCallback<ArrayList<BundleType>>() {
            public void onSuccess(ArrayList<BundleType> result) {
                if (result == null || result.size() == 0) {
                    setSelected(null);
                    CoreGUI.getMessageCenter().notify(
                        new Message(CoreGUI.getMessages().view_bundle_createWizard_noBundleTypesSupported(),
                            Severity.Error));
                    return;
                }

                for (BundleType bundleType : result) {
                    knownBundleTypes.put(bundleType.getName(), bundleType);
                    if (getSelected() == null) {
                        setSelected(bundleType);
                        setDefaultValue(bundleType.getName());
                        setValue(bundleType.getName());
                    }
                }
                setValueMap(knownBundleTypes.keySet().toArray(new String[0]));
                setDisabled(false);
                // don't bother showing the menu if there is only one item
                if (knownBundleTypes.size() > 1) {
                    setVisible(true);
                    show(); // in case we've already been rendered
                }
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    CoreGUI.getMessages().view_bundle_createWizard_noBundleTypesAvail(), caught);
            }
        });
    }
}
