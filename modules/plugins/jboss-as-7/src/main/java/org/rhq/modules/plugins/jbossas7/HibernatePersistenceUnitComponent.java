/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import java.util.List;
import java.util.Map;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenNames;
import org.rhq.modules.plugins.jbossas7.json.ReadResource;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Thomas Segismont
 */
public class HibernatePersistenceUnitComponent extends BaseComponent {
    private static final String[] QUERY_CACHE_ATTRIBUTE_NAMES = new String[] { //
    "query-name", //
        "query-execution-count", //
        "query-execution-row-count", //
        "query-execution-min-time", //
        "query-execution-max-time", //
        "query-execution-average-time", //
        "query-cache-hit-count", //
        "query-cache-miss-count", //
        "query-cache-put-count" //
    };
    private static final int BATCH_SIZE = 10;
    private static final int MANAGEMENT_QUERY_TIMEOUT = 60;

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("viewQueries".equals(name)) {
            return invokeViewQueriesOperation(parameters);
        }
        return super.invokeOperation(name, parameters);
    }

    private OperationResult invokeViewQueriesOperation(Configuration parameters) throws InterruptedException {
        OperationResult operationResult = new OperationResult();

        int batchSize;
        PropertySimple batchSizeProp = parameters.getSimple("batchSize");
        if (batchSizeProp == null || batchSizeProp.getStringValue() == null) {
            batchSize = BATCH_SIZE;
        } else {
            try {
                batchSize = Integer.parseInt(batchSizeProp.getStringValue());
                if (batchSize < 1) {
                    operationResult.setErrorMessage("batchSize property is less than 1");
                    return operationResult;
                }
            } catch (NumberFormatException e) {
                operationResult.setErrorMessage("batchSize property is not an integer");
                return operationResult;
            }
        }

        int timeoutSec;
        PropertySimple managementQueryTimeoutProp = parameters.getSimple("managementQueryTimeout");
        if (managementQueryTimeoutProp == null || managementQueryTimeoutProp.getStringValue() == null) {
            timeoutSec = MANAGEMENT_QUERY_TIMEOUT;
        } else {
            try {
                timeoutSec = Integer.parseInt(managementQueryTimeoutProp.getStringValue());
                if (timeoutSec < 1) {
                    operationResult.setErrorMessage("managementQueryTimeout property is less than 1");
                    return operationResult;
                }
            } catch (NumberFormatException e) {
                operationResult.setErrorMessage("managementQueryTimeout property is not an integer");
                return operationResult;
            }
        }

        PropertyList queriesPropertyList = new PropertyList("queries");
        operationResult.getComplexResults().put(queriesPropertyList);

        ReadChildrenNames readQueryCacheChildrenNames = new ReadChildrenNames(getAddress(), "query-cache");
        Result readQueryCacheChildrenNamesResult = getASConnection().execute(readQueryCacheChildrenNames, timeoutSec);
        if (!readQueryCacheChildrenNamesResult.isSuccess()) {
            operationResult.setErrorMessage("Could not read query-cache children names: "
                + readQueryCacheChildrenNamesResult.getFailureDescription());
            return operationResult;
        }
        @SuppressWarnings("unchecked")
        List<String> childrenNames = (List<String>) readQueryCacheChildrenNamesResult.getResult();

        // Process children in batches to avoid sending too many queries if the PU has many query-cache nodes
        while (!childrenNames.isEmpty()) {
            if (context.getComponentInvocationContext().isInterrupted()) {
                // Operation canceled or timed out
                throw new InterruptedException();
            }

            List<String> childrenNamesSubList = childrenNames.subList(0, Math.min(batchSize, childrenNames.size()));

            // Create batch operation to read N query-cache nodes
            CompositeOperation batchOperation = new CompositeOperation();
            for (String childrenName : childrenNamesSubList) {
                Address address = new Address(getAddress());
                address.add("query-cache", childrenName);
                ReadResource readQueryCacheResource = new ReadResource(address);
                readQueryCacheResource.includeRuntime(true);
                batchOperation.addStep(readQueryCacheResource);
            }

            // Execute batch
            Result batchResult = getASConnection().execute(batchOperation, timeoutSec);
            if (!batchResult.isSuccess()) {
                operationResult.setErrorMessage("Could not read query-cache attributes: "
                    + batchResult.getFailureDescription());
                return operationResult;
            }

            // Iterate over batch results
            @SuppressWarnings("unchecked")
            Map<String, Map> mapResult = (Map<String, Map>) batchResult.getResult();
            for (Map stepResult : mapResult.values()) {
                PropertyMap queryPropertyMap = new PropertyMap("query");
                @SuppressWarnings("unchecked")
                Map<String, String> queryCacheAttributes = (Map<String, String>) stepResult.get("result");
                for (String queryCacheAttributeName : QUERY_CACHE_ATTRIBUTE_NAMES) {
                    addAttributeToPropertyMap(queryCacheAttributeName, queryPropertyMap, queryCacheAttributes);
                }
                queriesPropertyList.add(queryPropertyMap);
            }

            childrenNamesSubList.clear();
        }

        return operationResult;
    }

    private void addAttributeToPropertyMap(String attributeName, PropertyMap propertyMap,
        Map<String, String> queryCacheAttributes) {
        propertyMap.put(new PropertySimple(attributeName, queryCacheAttributes.get(attributeName)));
    }
}
