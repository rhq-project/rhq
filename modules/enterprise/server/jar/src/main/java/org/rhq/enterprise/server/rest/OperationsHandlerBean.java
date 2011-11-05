package org.rhq.enterprise.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
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

        CacheControl cc = new CacheControl();
        cc.setMaxAge(1200); // Definitions are very stable, so caching for 20mins is ok
        cc.setPrivate(false);
        builder.tag(eTag);
        builder.cacheControl(cc);

        return builder.build();

    }

    @Override
    // TODO find out why a return type of Response lets this fail for XML (even with @Wrapped annotation
    public List<OperationDefinitionRest> getOperationDefinitions(Integer resourceId, UriInfo uriInfo, Request request) {

        if (resourceId == null)
            throw new ParameterMissingException("resourceId");

        Resource res =resourceManager.getResource(caller,resourceId);
        if(res==null)
            throw new StuffNotFoundException("resource with id " + resourceId);

        ResourceType resourceType = res.getResourceType();

/*
        EntityTag eTag = new EntityTag(Integer.toHexString(resourceType.hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
        if (builder==null) {
*/

            Set<OperationDefinition> opDefList = resourceType.getOperationDefinitions();
            List<OperationDefinitionRest> resultList = new ArrayList<OperationDefinitionRest>(opDefList.size());

            for (OperationDefinition def : opDefList) {
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
        return resultList;

/*

            builder = Response.ok(resultList);
        }

        CacheControl cc = new CacheControl();
        cc.setMaxAge(1200); // Definitions are very stable, so caching for 20mins is ok
        cc.setPrivate(false);
        builder.tag(eTag);
        builder.cacheControl(cc);

        return builder.build();
*/

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
            Link link = new Link("submit","TODO");
            operation.addLink(link);

        }
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/operation/{id}");
        URI uri = uriBuilder.build(operation.getId());
        Link editLink = new Link("edit",uri.toString());
        operation.addLink(editLink);

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

}
