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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.ReadChildrenResources;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * @author Thomas Segismont
 */
class Redeployer {

    private final String name;
    private final String runtimeName;
    private final String hash;
    private final ASConnection connection;

    Redeployer(String name, String runtimeName,String hash, ASConnection connection) {
        this.name =  name;
        this.hash = hash;
        this.connection = connection;
        this.runtimeName = runtimeName;
    }

    boolean deploymentExists() {
        Result deploymentResources = connection.execute(new ReadChildrenResources(new Address(), "deployment"));
        for (Map.Entry<?, ?> deploymentResource : ((Map<?, ?>) deploymentResources.getResult()).entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> deploymentResourceDetails = (Map<String, Object>) deploymentResource.getValue();
            if (name.equals(deploymentResourceDetails.get("name"))) {
                return true;
            }
        }
        return false;
    }

    Result redeployOnServer() {
        Operation op = new Operation("full-replace-deployment", new Address());
        op.addAdditionalProperty("name", name);
        if (runtimeName != null) {
            op.addAdditionalProperty("runtime-name", runtimeName);
        }
        List<Object> content = new ArrayList<Object>(1);
        Map<String, Object> contentValues = new HashMap<String, Object>();
        contentValues.put("hash", new PROPERTY_VALUE("BYTES_VALUE", hash));
        content.add(contentValues);
        op.addAdditionalProperty("content", content);
        return connection.execute(op);
    }

}
