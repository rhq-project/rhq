/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.drift.wizard;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.wizard.WizardStep;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * @author Jay Shaughnessy
 */
public class DriftAddConfigWizard extends AbstractDriftAddConfigWizard {

    public DriftAddConfigWizard(EntityContext context, ResourceType type) {

        super(context, type);

        final ArrayList<WizardStep> steps = new ArrayList<WizardStep>();

        steps.add(new DriftAddConfigWizardInfoStep(DriftAddConfigWizard.this));
        steps.add(new DriftAddConfigWizardConfigStep(DriftAddConfigWizard.this));

        setSteps(steps);
    }

    public String getWindowTitle() {
        return MSG.view_drift_wizard_addConfig_windowTitle();
    }

    public String getTitle() {
        return MSG.view_drift_wizard_addConfig_title(getType().getName());
    }

    public String getSubtitle() {
        return null;
    }

    public void execute() {
        // TODO
    }

    public static void showWizard(final EntityContext context) {
        assert context != null;

        switch (context.getType()) {
        case Resource:
            ResourceCriteria rc = new ResourceCriteria();
            rc.addFilterId(context.getResourceId());
            rc.fetchResourceType(true);
            GWTServiceLookup.getResourceService().findResourcesByCriteria(rc, new AsyncCallback<PageList<Resource>>() {
                public void onSuccess(PageList<Resource> result) {
                    if (result.isEmpty()) {
                        throw new IllegalArgumentException("Entity not found [" + context + "]");
                    }

                    final Resource resource = result.get(0);

                    // bypass type cache because this is infrequent an we don't need to cache the
                    // drift config templates
                    ResourceTypeCriteria rtc = new ResourceTypeCriteria();
                    rtc.addFilterId(resource.getResourceType().getId());
                    rtc.fetchDriftConfigurationTemplates(true);
                    GWTServiceLookup.getResourceTypeGWTService().findResourceTypesByCriteria(rtc,
                        new AsyncCallback<PageList<ResourceType>>() {
                            public void onSuccess(PageList<ResourceType> result) {
                                if (result.isEmpty()) {
                                    throw new IllegalArgumentException("Resource Type not found ["
                                        + resource.getResourceType().getId() + "]");
                                }

                                DriftAddConfigWizard wizard = new DriftAddConfigWizard(context, result.get(0));
                                wizard.startWizard();
                            }

                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.widget_typeTree_loadFail(), caught);
                            }
                        });

                }

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resources_loadFailed(), caught);
                }
            });

            break;

        default:
            throw new IllegalArgumentException("Entity Context Type not supported [" + context + "]");
        }
    }

    @Override
    public void cancel() {
        super.cancel();
    }

}
