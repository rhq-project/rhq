/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.core.domain.resource.InventoryStatus.COMMITTED;
import static org.rhq.core.domain.resource.ResourceCategory.PLATFORM;

/**
 * @author jsanda
 */
@Interceptors(SetCallerInterceptor.class)
@Stateless
public class PlatformUtilizationHandler extends AbstractRestBean implements PlatformUtilizationLocal {

    @EJB
    private ResourceManagerLocal resourceMgr;

    @EJB
    private MeasurementDataManagerLocal measurementDataMgr;

    @Override
    public StreamingOutput generateReport(UriInfo uriInfo, Request request, HttpHeaders headers) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                ResourceCriteria criteria = new ResourceCriteria();
                criteria.addFilterResourceCategories(PLATFORM);
                criteria.addFilterInventoryStatus(COMMITTED);
                criteria.fetchResourceType(true);

                CriteriaQueryExecutor<Resource, ResourceCriteria> queryExecutor =
                    new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
                        @Override
                        public PageList<Resource> execute(ResourceCriteria criteria) {
                            return resourceMgr.findResourcesByCriteria(caller, criteria);
                        }
                    };

                CriteriaQuery<Resource, ResourceCriteria> query =
                    new CriteriaQuery<Resource, ResourceCriteria>(criteria, queryExecutor);
                output.write((getHeader() + "\n").getBytes());
                for (Resource platform : query ) {

                }
            }
        };
    }

    private String getHeader() {
        return "Name,Version,CPU,Memory,Swap";
    }
}
