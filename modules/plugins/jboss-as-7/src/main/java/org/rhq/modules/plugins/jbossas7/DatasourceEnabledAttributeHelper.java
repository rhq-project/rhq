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

import static java.lang.Boolean.TRUE;

import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Datasources resource types 'enabled' attribute is read/write in the plugin descriptor, but not in EAP management
 * interface. This helper queries the current attribute value and, as needed, will invoke whether the 'enable' or
 * 'disable' operation on EAP.
 *
 * @author Thomas Segismont
 */
final class DatasourceEnabledAttributeHelper {

    Address datasourceAddress;
    ASConnection asConnection;

    DatasourceEnabledAttributeHelper(Address datasourceAddress) {
        this.datasourceAddress = datasourceAddress;
    }

    static DatasourceEnabledAttributeHelper on(Address datasourceAddress) {
        return new DatasourceEnabledAttributeHelper(datasourceAddress);
    }

    DatasourceEnabledAttributeHelper with(ASConnection asConnection) {
        this.asConnection = asConnection;
        return this;
    }

    void setAttributeValue(Boolean attributeValue, EnabledAttributeHelperCallbacks callbacks) {
        if (asConnection == null) {
            throw new IllegalStateException("No ASConnection instance provided");
        }
        if (attributeValue == null) {
            throw new IllegalArgumentException("Argument attributeValue is null");
        }
        ReadAttribute readAttribute = new ReadAttribute(datasourceAddress, DatasourceComponent.ENABLED_ATTRIBUTE);
        Result readAttributeResult = asConnection.execute(readAttribute);
        if (!readAttributeResult.isSuccess()) {
            if (callbacks != null) {
                callbacks.onReadAttributeFailure(readAttributeResult);
            }
            return;
        }
        Boolean currentAttributeValue = (Boolean) readAttributeResult.getResult();
        if (currentAttributeValue != attributeValue) {
            if (attributeValue == TRUE) {
                Operation operation = new Operation(DatasourceComponent.ENABLE_OPERATION, datasourceAddress);
                Result res = asConnection.execute(operation);
                if (!res.isSuccess() && callbacks != null) {
                    callbacks.onEnableOperationFailure(res);
                }
            } else {
                Operation operation = new Operation(DatasourceComponent.DISABLE_OPERATION, datasourceAddress);
                Result res = asConnection.execute(operation);
                if (!res.isSuccess() && callbacks != null) {
                    callbacks.onDisableOperationFailure(res);
                }
            }
        }
    }

    static interface EnabledAttributeHelperCallbacks {
        void onReadAttributeFailure(Result opResult);

        void onEnableOperationFailure(Result opResult);

        void onDisableOperationFailure(Result opResult);
    }
}
