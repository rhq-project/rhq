/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.HistoryJobId;
import org.rhq.core.domain.operation.JobId;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.OperationDefinitionRest;
import org.rhq.enterprise.server.rest.domain.OperationHistoryRest;
import org.rhq.enterprise.server.rest.domain.OperationRest;
import org.rhq.enterprise.server.rest.domain.SimplePropDef;

/**
 * Deal with operations
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class OperationsHandlerBean extends AbstractRestBean implements OperationsHandlerLocal {

    @EJB
    OperationManagerLocal opsManager;

    @EJB
    ResourceManagerLocal resourceManager;

    @Override
    public Response getOperationDefinition(int definitionId,
                                           Integer resourceId,
                                           UriInfo uriInfo,
                                           Request request, HttpHeaders httpHeaders) {



        OperationDefinition def;
        def = getFromCache(definitionId, OperationDefinition.class);
        if (def==null) {
            def = opsManager.getOperationDefinition(caller,definitionId);
            if (def==null)
                throw new StuffNotFoundException("OperationDefinition with id " + definitionId);
            else
                putToCache(definitionId,OperationDefinition.class,def);
        }

        EntityTag eTag = new EntityTag(Integer.toHexString(def.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {

            OperationDefinitionRest odr = new OperationDefinitionRest();
            odr.setId(def.getId());
            odr.setName(def.getName());

            copyParamsForDefinition(def, odr);

            builder=Response.ok(odr);

            // Add some links
            if (resourceId!=null) {
                UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
                uriBuilder.path("/operation/definition/{id}");
                uriBuilder.queryParam("resourceId",resourceId);
                Link createLink = new Link("create",uriBuilder.build(definitionId).toString());
                odr.addLink(createLink);
            }

        }

        builder.tag(eTag);

        return builder.build();

    }

    private void copyParamsForDefinition(OperationDefinition def, OperationDefinitionRest odr) {
        ConfigurationDefinition cd = def.getParametersConfigurationDefinition();
        if (cd==null)
            return;

        for (Map.Entry<String,PropertyDefinition> entry : cd.getPropertyDefinitions().entrySet()) {
            PropertyDefinition pd = entry.getValue();
            if (pd instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple pds = (PropertyDefinitionSimple) pd;
                SimplePropDef prop = new SimplePropDef();
                prop.setName(pds.getName());
                prop.setRequired(pds.isRequired());
                prop.setType(pds.getType());
                prop.setDefaultValue(pds.getDefaultValue());
                odr.addParam(prop);
            }
            log.debug("copyParams: " + pd.getName() + " not yet supported");
        }
    }

    @Override
    public Response getOperationDefinitions(Integer resourceId, UriInfo uriInfo, Request request) {

        if (resourceId == null)
            throw new ParameterMissingException("resourceId");

        Resource res =resourceManager.getResource(caller,resourceId);
        if(res==null)
            throw new StuffNotFoundException("resource with id " + resourceId);

        ResourceType resourceType = res.getResourceType();

        EntityTag eTag = new EntityTag(Integer.toHexString(resourceType.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {

            Set<OperationDefinition> opDefList = resourceType.getOperationDefinitions();
            List<OperationDefinitionRest> resultList = new ArrayList<OperationDefinitionRest>(opDefList.size());

            for (OperationDefinition def : opDefList) {
                putToCache(def.getId(),OperationDefinition.class,def);
                OperationDefinitionRest odr = new OperationDefinitionRest();
                odr.setId(def.getId());
                odr.setName(def.getName());

                copyParamsForDefinition(def,odr);

                UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
                uriBuilder.path("/operation/definition/{id}");
                uriBuilder.queryParam("resourceId",resourceId);
                Link createLink = new Link("create",uriBuilder.build(def.getId()).toString());
                odr.addLink(createLink);

                resultList.add(odr);
            }

            GenericEntity<List<OperationDefinitionRest>> entity = new GenericEntity<List<OperationDefinitionRest>>(resultList){};
            builder = Response.ok(entity);
        }

        builder.tag(eTag);
        return builder.build();

    }

    @Override
    public Response createOperation(int definitionId, Integer resourceId, UriInfo uriInfo) {

        if (resourceId == null)
            throw new ParameterMissingException("resourceId");

        Resource res =resourceManager.getResource(caller,resourceId);
        if(res==null)
            throw new StuffNotFoundException("resource with id " + resourceId);


        OperationDefinition opDef = opsManager.getOperationDefinition(caller,definitionId);
        if (opDef==null) {
            throw new StuffNotFoundException("Operation definition with id " + definitionId);
        }
        OperationRest operationRest = new OperationRest(resourceId,definitionId);
        operationRest.setId((int)System.currentTimeMillis()); // TODO better id (?)(we need one for pUT later on)
        operationRest.setReadyToSubmit(false);
        operationRest.setName(opDef.getName());
        ConfigurationDefinition paramDefinition = opDef.getParametersConfigurationDefinition();
        if (paramDefinition != null) {
            for (PropertyDefinition propDefs : paramDefinition.getNonGroupedProperties()) { // TODO extend to all properties ?
                operationRest.getParams().put(propDefs.getName(),"TODO"); // TODO type and value of the value
            }
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/operation/{id}");
        URI uri = uriBuilder.build(operationRest.getId());
        Link editLink = new Link("edit",uri.toString());
        operationRest.addLink(editLink);
        Response.ResponseBuilder builder = Response.ok(operationRest);

        putToCache(operationRest.getId(),OperationRest.class,operationRest);

        return builder.build();

    }

    @Override
    public Response getOperation(int operationId) {
        OperationRest op = getFromCache(operationId,OperationRest.class);
        if (op==null)
            throw new StuffNotFoundException("Operation with id " + operationId);

        return Response.ok(op).build();
    }

    @Override
    public Response updateOperation(int operationId, OperationRest operation, UriInfo uriInfo) {

        if (!operation.isReadyToSubmit() && operation.getDefinitionId()>0 && !operation.getName().isEmpty()) {
            // TODO check all the required parameters for presence before allowing to submit
            operation.setReadyToSubmit(true);
        }
        if (operation.isReadyToSubmit()) {
            // todo check params

            // submit

            Configuration parameters = new Configuration();
            for (Map.Entry<String,Object> entry : operation.getParams().entrySet()) {
                parameters.put(new PropertySimple(entry.getKey(),entry.getValue())); // TODO honor more types
            }
            ResourceOperationSchedule sched = opsManager.scheduleResourceOperation(caller,operation.getResourceId(),operation.getName(),0,0,0,-1,
                    parameters,"Test");
            JobId jobId = new JobId(sched.getJobName(),sched.getJobGroup());
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/operation/history/{id}");
            URI uri = uriBuilder.build(jobId);
            Link histLink = new Link("history",uri.toString());
            operation.addLink(histLink);

        }
        else {
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/operation/{id}");
            URI uri = uriBuilder.build(operation.getId());
            Link editLink = new Link("edit",uri.toString());
            operation.addLink(editLink);
        }
        // Update item in cache
        putToCache(operation.getId(),OperationRest.class,operation);
        Response.ResponseBuilder builder = Response.ok(operation);
        return builder.build();
    }

    @Override
    public Response cancelOperation(int operationId) {

        log.info("Cancel called");

        removeFromCache(operationId,OperationRest.class);

        return null;  // TODO: Customise this generated block
    }

    @Override
    public Response outcome(String jobName, UriInfo uriInfo) {


        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
        criteria.addFilterJobId(new JobId(jobName));

        ResourceOperationHistory history ;//= opsManager.getOperationHistoryByJobId(caller,jobName);
        List<ResourceOperationHistory> list = opsManager.findResourceOperationHistoriesByCriteria(caller,criteria);
        if (list==null || list.isEmpty()) {
            log.info("No history with id " + new HistoryJobId(jobName) + " found");
            throw new StuffNotFoundException("OperationHistory with id " + new HistoryJobId(jobName));
        }

        history = list.get(0);
        String status;
        if (history.getStatus()==null)
            status = " - no information yet -";
        else
            status = history.getStatus().getDisplayName();

        OperationHistoryRest hist = new OperationHistoryRest();
        hist.setStatus(status);
        if (history.getErrorMessage()!=null)
            hist.setErrorMessage(history.getErrorMessage());
        if (history.getResults()!=null) {
            Configuration results = history.getResults();
            for (Property p : results.getProperties()) {
                String val;
                if (p instanceof PropertySimple)
                    val = ((PropertySimple)p).getStringValue();
                else
                    val = p.toString();
                hist.getResult().put(p.getName(),val);
            }
        }

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/operation/history/{id}");
        URI url = uriBuilder.build(new JobId(jobName));
        Link self = new Link("self",url.toString());
        hist.getLinks().add(self);


        return Response.ok(hist).build();

    }
}
