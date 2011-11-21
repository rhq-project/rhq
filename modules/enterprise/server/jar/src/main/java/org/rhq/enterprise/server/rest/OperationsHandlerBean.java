package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.criteria.OperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.HistoryJobId;
import org.rhq.core.domain.operation.JobId;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.OperationDefinitionRest;
import org.rhq.enterprise.server.rest.domain.OperationRest;

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
    public Response createOperation(@PathParam("id") int definitionId,
                                    @QueryParam("resourceId") Integer resourceId,
                                    UriInfo uriInfo) {

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
        operationRest.setState("creating");
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
    public Response getOperation(@PathParam("id") int operationId) {
        OperationRest op = getFromCache(operationId,OperationRest.class);
        if (op==null)
            throw new StuffNotFoundException("Operation with id " + operationId);

        return Response.ok(op).build();
    }

    @Override
    public Response updateOperation(@PathParam("id") int operationId, OperationRest operation, UriInfo uriInfo) {

        if (operation.getState().equals("creating") && operation.getDefinitionId()>0 && !operation.getName().isEmpty()) {
            // TODO check all the required parameters for persence before allowing to submit
            operation.setState("ready");
        }
        if (operation.getState().equals("ready")) {
            // todo check params

            // submit
            ResourceOperationSchedule sched = opsManager.scheduleResourceOperation(caller,operation.getResourceId(),operation.getName(),0,0,0,-1,new Configuration(),"TEst");
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
    @DELETE
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response cancelOperation(@PathParam("id") int operationId) {

        log.info("Cancel called");

        removeFromCache(operationId,OperationRest.class);

        return null;  // TODO: Customise this generated block
    }

    @Override
    @GET
    @Path("history/{id}")
    public Response outcome(@PathParam("id") String jobName) {

/*
        ResourceOperationSchedule schedule = opsManager.getResourceOperationSchedule(caller,operationHistoryId);
        if (schedule==null)
            throw new StuffNotFoundException("OperationSchedule with id " + operationHistoryId);
*/

        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
        criteria.addFilterJobId(new JobId(jobName));

        OperationHistory history ;//= opsManager.getOperationHistoryByJobId(caller,jobName);
        List<ResourceOperationHistory> list = opsManager.findResourceOperationHistoriesByCriteria(caller,criteria);
        if (list==null || list.isEmpty())
            throw new StuffNotFoundException("OperationHistory with id " + new HistoryJobId(jobName));

        history = list.get(0);
        String ret = history.getStatus().getDisplayName();
        // TODO get history.results with the help of the property definition

        return Response.ok(ret).build();

    }
}
