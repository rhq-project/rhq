/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.report;

import java.util.EnumSet;
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.drift.DriftComplianceStatus;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.table.IconField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public class ResourceInstallReportResourceSearchView extends ResourceSearchView {

    private final int resourceTypeId;

    public ResourceInstallReportResourceSearchView(String locatorId, Criteria criteria) {
        super(locatorId, criteria);

        setInitialCriteriaFixed(true);

        this.resourceTypeId = criteria.getAttributeAsInt(ResourceDataSourceField.TYPE.propertyName());
    };

    // suppress unchecked warnings because the superclass has different generic types for the datasource
    @SuppressWarnings("unchecked")
    @Override
    protected RPCDataSource getDataSourceInstance() {
        return new DataSource();
    }

    @Override
    protected List<ListGridField> createFields() {

        List<ListGridField> fields = super.createFields();

        IconField inComplianceField = new IconField(DataSource.ATTR_IN_COMPLIANCE, MSG.common_title_in_compliance(), 95);
        fields.add(inComplianceField);

        return fields;
    }

    private class DataSource extends ResourceDatasource {

        private static final String ATTR_IN_COMPLIANCE = "inCompliance";

        private ResourceType resourceType;

        @Override
        public void executeFetch(final DSRequest request, final DSResponse response, final ResourceCriteria criteria) {

            if (null == resourceType) {

                ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceTypeId,
                    EnumSet.of(ResourceTypeRepository.MetadataType.driftDefinitionTemplates),
                    new ResourceTypeRepository.TypeLoadedCallback() {

                        public void onTypesLoaded(ResourceType type) {
                            resourceType = type;
                            DataSource.super.executeFetch(request, response, criteria);
                        }
                    });

            } else {
                super.executeFetch(request, response, criteria);
            }
        }

        @Override
        protected ResourceCriteria getFetchCriteria(DSRequest request) {
            ResourceCriteria criteria = super.getFetchCriteria(request);
            criteria.fetchDriftDefinitions(true);
            return criteria;
        }

        @Override
        public ListGridRecord copyValues(Resource from) {
            ListGridRecord record = super.copyValues(from);

            if (!resourceType.getDriftDefinitionTemplates().isEmpty()) {
                record.setAttribute(ATTR_IN_COMPLIANCE, ImageManager.getAvailabilityIcon(!isOutOfCompliance(from)));
            }
            return record;
        }

        private boolean isOutOfCompliance(Resource resource) {
            for (DriftDefinition def : resource.getDriftDefinitions()) {
                if (def.getComplianceStatus() != DriftComplianceStatus.IN_COMPLIANCE) {
                    return true;
                }
            }
            return false;
        }
    }

}
