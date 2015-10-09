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
package org.rhq.coregui.client.bundle.deploy;

import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Jay Shaughnessy
 *
 */
public class SelectBundleVersionStep extends AbstractWizardStep {

    static private final String LATEST_VERSION = "latest";
    static private final String LIVE_VERSION = "live";
    static private final String SELECT_VERSION = "select";

    private final BundleDeployWizard wizard;
    private DynamicForm form;

    private RadioGroupItem radioGroupItem = new RadioGroupItem("options", "Deploy Options");
    private SelectItem selectVersionItem = new SortedSelectItem("selectVersion", "Deployment Version");
    private LinkedHashMap<String, String> radioGroupValues = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> selectVersionValues = new LinkedHashMap<String, String>();
    private PageList<BundleVersion> bundleVersions = null;
    private BundleVersion latestVersion;
    private BundleVersion liveVersion;

    public SelectBundleVersionStep(BundleDeployWizard wizard) {
        this.wizard = wizard;
    }

    public String getName() {
        return MSG.view_bundle_deployWizard_selectVersionStep();
    }

    public Canvas getCanvas() {
        if (form == null) {
            form = new DynamicForm();
            form.setWidth100();
            form.setNumCols(2);
            form.setColWidths("50%", "*");

            setItemValues();

            radioGroupItem.setRequired(true);
            radioGroupItem.setDisabled(true);
            radioGroupItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    boolean isLatestVersion = LATEST_VERSION.equals(event.getValue());
                    if (isLatestVersion) {
                        wizard.setBundleVersion(latestVersion);
                    }
                    boolean isLiveVersion = LIVE_VERSION.equals(event.getValue());
                    if (isLiveVersion) {
                        wizard.setBundleVersion(liveVersion);
                    }
                    selectVersionItem.setDisabled(isLatestVersion || isLiveVersion);
                    selectVersionItem.setRequired(!(isLatestVersion || isLiveVersion));
                    selectVersionItem.redraw();
                    form.markForRedraw();
                }
            });

            selectVersionItem.setDisabled(true);
            selectVersionItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    for (BundleVersion bundleVersion : bundleVersions) {
                        if (bundleVersion.getVersion().equals(event.getValue())) {
                            wizard.setBundleVersion(bundleVersion);
                            break;
                        }
                    }
                }
            });

            form.setItems(radioGroupItem, selectVersionItem);
        }

        return form;
    }

    private void setItemValues() {
        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.addFilterBundleId(wizard.getBundleId());
        criteria.fetchConfigurationDefinition(true);
        GWTServiceLookup.getBundleService().findBundleVersionsByCriteria(criteria, //
            new AsyncCallback<PageList<BundleVersion>>() {

                public void onSuccess(PageList<BundleVersion> result) {
                    bundleVersions = result;
                    if (null == bundleVersions || bundleVersions.isEmpty()) {
                        onFailure(new IllegalArgumentException("No bundle versions defined for bundle."));
                    }

                    int highVersionOrder = -1;
                    for (BundleVersion bundleVersion : result) {
                        int versionOrder = bundleVersion.getVersionOrder();
                        if (versionOrder > highVersionOrder) {
                            highVersionOrder = versionOrder;
                            latestVersion = bundleVersion;
                        }
                        selectVersionValues.put(bundleVersion.getVersion(), bundleVersion.getVersion());
                    }

                    BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
                    criteria.addFilterDestinationId(wizard.getDestination().getId());
                    criteria.addFilterIsLive(true);
                    criteria.fetchBundleVersion(true);
                    criteria.fetchConfiguration(true);
                    GWTServiceLookup.getBundleService().findBundleDeploymentsByCriteria(criteria, //
                        new AsyncCallback<PageList<BundleDeployment>>() {

                            public void onSuccess(PageList<BundleDeployment> result) {
                                radioGroupValues.put(LATEST_VERSION,
                                    MSG.view_bundle_deployWizard_selectVersion_latest(latestVersion.getVersion()));

                                if (!result.isEmpty()) {
                                    BundleDeployment liveDeployment = result.get(0);
                                    // make sure the liveDeployment record has a bundleversion with configdef loaded
                                    BundleVersion liveBundleVersion = liveDeployment.getBundleVersion();
                                    int i = bundleVersions.indexOf(liveBundleVersion);
                                    liveDeployment.setBundleVersion(bundleVersions.get(i));
                                    wizard.setLiveDeployment(liveDeployment);
                                    liveVersion = liveDeployment.getBundleVersion();
                                    radioGroupValues.put(LIVE_VERSION,
                                        MSG.view_bundle_deployWizard_selectVersion_live(liveVersion.getVersion()));
                                }

                                radioGroupValues.put(SELECT_VERSION,
                                    MSG.view_bundle_deployWizard_selectVersion_select());
                                selectVersionItem.setValueMap(selectVersionValues);
                                selectVersionItem.setValue(latestVersion.getVersion());
                                selectVersionItem.redraw();

                                radioGroupItem.setValueMap(radioGroupValues);
                                radioGroupItem.setValue(LATEST_VERSION);
                                wizard.setBundleVersion(latestVersion);
                                radioGroupItem.setDisabled(false);
                                radioGroupItem.redraw();
                                form.markForRedraw();
                            }

                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deployWizard_error_11(), caught);
                            }
                        });
                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deployWizard_error_12(), caught);
                }
            });
    }

    public boolean nextPage() {
        return form.validate() && (null != this.wizard.getBundleVersion());
    }
}
