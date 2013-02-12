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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.JobId;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.OperationDefinitionRest;
import org.rhq.enterprise.server.rest.domain.OperationHistoryRest;
import org.rhq.enterprise.server.rest.domain.OperationRest;
import org.rhq.enterprise.server.rest.domain.SimplePropDef;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

/**
 * Deal with operations
 * @author Heiko W. Rupp
 */
@Path("/operation")
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class OperationsHandlerBean extends AbstractRestBean  {

    @EJB
    OperationManagerLocal opsManager;

    @EJB
    ResourceManagerLocal resourceManager;

    @GET
    @Path("definition/{id}")
    @Cache(maxAge = 1200)
    @ApiOperation("Retrieve a single operation definition by its id")
    public Response getOperationDefinition(
            @ApiParam("Id of the definition to retrieve") @PathParam("id") int definitionId,
            @ApiParam("Id of a resource that supports this operation") @QueryParam("resourceId") Integer resourceId,
               @Context UriInfo uriInfo,
               @Context Request request,
               @Context HttpHeaders httpHeaders) {



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

    @GZIP
    @GET
    @Path("definitions")
    @Cache(maxAge = 1200)
    @ApiOperation("List all operation definitions for a resource")
    public Response getOperationDefinitions(
            @ApiParam(value = "Id of the resource",required = true) @QueryParam("resourceId") Integer resourceId,
                                            @Context UriInfo uriInfo,
                                            @Context Request request,
                                            @Context HttpHeaders httpHeaders) {

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

    @POST
    @Path("definition/{id}")
    @ApiOperation("Create a new (draft) operation from the passed definition id for the passed resource")
    public Response createOperation(
            @ApiParam("Id of the definition") @PathParam("id") int definitionId,
            @ApiParam(value = "Id of the resource", required = true) @QueryParam("resourceId") Integer resourceId,
                                    @Context UriInfo uriInfo) {

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

    @GET
    @Path("{id}")
    @ApiOperation("Return a (draft) operation")
    public Response getOperation(@ApiParam("Id of the operation to retrieve") @PathParam("id") int operationId) {
        OperationRest op = getFromCache(operationId,OperationRest.class);
        if (op==null)
            throw new StuffNotFoundException("Operation with id " + operationId);

        return Response.ok(op).build();
    }

    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML})
    @ApiOperation("Update a (draft) operation. If the state is set to 'ready', the operation will be scheduled")
    public Response updateOperation(@ApiParam("Id of the operation to update") @PathParam("id") int operationId,
                OperationRest operation, @Context UriInfo uriInfo) {

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

    @DELETE
    @Path("{id}")
    @ApiOperation("Delete a (draft) operation")
    public Response cancelOperation(@ApiParam("Id of the operation to remove") @PathParam("id") int operationId) {

        log.info("Cancel called");

        removeFromCache(operationId,OperationRest.class);

        return null;  // TODO: Customise this generated block
    }

    @GZIP
    @GET
    @Path("history/{id}")
    @ApiOperation("Return the outcome of the scheduled operation")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    public Response outcome(
            @ApiParam("Name of the submitted job.") @PathParam("id") String jobName,
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders httpHeaders) {

        MediaType mediaType = httpHeaders.getAcceptableMediaTypes().get(0);

        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
        JobId jobId = new JobId(jobName);
        criteria.addFilterJobId(jobId);

        ResourceOperationHistory history ;//= opsManager.getOperationHistoryByJobId(caller,jobName);
        List<ResourceOperationHistory> list = opsManager.findResourceOperationHistoriesByCriteria(caller,criteria);
        if (list==null || list.isEmpty()) {
            log.info("No history with id " + jobId + " found");
            throw new StuffNotFoundException("OperationHistory with id " + jobId);
        }

        history = list.get(0);
        OperationHistoryRest hist = historyToHistoryRest(history, uriInfo);
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("operationHistory.ftl",hist));
        } else {
            builder = Response.ok(hist);
        }
        if (history.getStatus()== OperationRequestStatus.SUCCESS) {
            // add a long time cache header
            CacheControl cc = new CacheControl();
            cc.setMaxAge(1200);
            builder.cacheControl(cc);
        }

        return builder.build();

    }

    @GZIP
    @GET
    @Path("history")
    @ApiOperation("Return the outcome of the executed operations for a resource")
    @Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML})
    public Response listHistory(
            @ApiParam("Id of a resource to limit to") @QueryParam("resourceId") int resourceId,
            @Context UriInfo uriInfo,
            @Context Request request,
            @Context HttpHeaders httpHeaders) {

        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
        if (resourceId>0)
            criteria.addFilterResourceIds(resourceId);

        criteria.addSortEndTime(PageOrdering.DESC);

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<ResourceOperationHistory, ResourceOperationHistoryCriteria> queryExecutor = new CriteriaQueryExecutor<ResourceOperationHistory, ResourceOperationHistoryCriteria>() {
            @Override
            public PageList<ResourceOperationHistory> execute(ResourceOperationHistoryCriteria criteria) {
                return opsManager.findResourceOperationHistoriesByCriteria(caller, criteria);
            }
        };

        CriteriaQuery<ResourceOperationHistory, ResourceOperationHistoryCriteria> list = new CriteriaQuery<ResourceOperationHistory, ResourceOperationHistoryCriteria>(
            criteria, queryExecutor);

        List<OperationHistoryRest> result = new ArrayList<OperationHistoryRest>();
        for (ResourceOperationHistory roh : list) {
            OperationHistoryRest historyRest = historyToHistoryRest(roh,uriInfo);
            result.add(historyRest);
        }

        MediaType mediaType = httpHeaders.getAcceptableMediaTypes().get(0);
        Response.ResponseBuilder builder;
        if (mediaType.equals(MediaType.TEXT_HTML_TYPE)) {
            builder = Response.ok(renderTemplate("listOperationHistory.ftl", result));
        } else {
            GenericEntity<List<OperationHistoryRest>> res = new GenericEntity<List<OperationHistoryRest>>(result) {};
            builder = Response.ok(res);
        }
        return builder.build();
    }

    @DELETE
    @Path("history/{id}")
    @ApiOperation(value = "Delete the operation history item with the passed id")
    public Response deleteOperationHistoryItem(@ApiParam("Name fo the submitted job") @PathParam("id") String jobName) {

        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
        criteria.addFilterJobId(new JobId(jobName));
        criteria.clearPaging();//disable paging as the code assumes all the results will be returned.

        List<ResourceOperationHistory> list = opsManager.findResourceOperationHistoriesByCriteria(caller,criteria);
        if ((list != null && !list.isEmpty())) {

            ResourceOperationHistory history = list.get(0);
            opsManager.deleteOperationHistory(caller,history.getId(),false);
        }
        return Response.noContent().build();

    }


    private OperationHistoryRest historyToHistoryRest(ResourceOperationHistory history, UriInfo uriInfo) {
        String status;
        if (history.getStatus()==null)
            status = " - no information yet -";
        else
            status = history.getStatus().getDisplayName();

        OperationHistoryRest hist = new OperationHistoryRest();
        hist.setStatus(status);
        if (history.getResource()!=null)
            hist.setResourceName(history.getResource().getName());
        hist.setOperationName(history.getOperationDefinition().getName());
        hist.lastModified(history.getModifiedTime());
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

        String jobName = history.getJobName();
        String jobGroup = history.getJobGroup();
        JobId jobId = new JobId(jobName, jobGroup);
        hist.setJobId(jobId.toString());

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/operation/history/{id}");
        URI url = uriBuilder.build(jobId);
        Link self = new Link("self",url.toString());
        hist.getLinks().add(self);
        return hist;
    }

}