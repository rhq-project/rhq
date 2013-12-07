/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.rest.domain.GroupRest;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.MetricSchedule;
import org.rhq.enterprise.server.rest.domain.PagingCollection;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

/**
 * Abstract base class for EJB classes that implement REST methods.
 * For the cache and its eviction policies see standalone-full.xml (in
 * the RHQ Server's AS7/standalone/configuration directory, as modified
 * by the installer.)
 *
 * @author Heiko W. Rupp
 * @author Jay Shaughnessy
 */
@Produces({MediaType.APPLICATION_JSON,MediaType.APPLICATION_XML,MediaType.TEXT_HTML,"application/vnd.rhq.wrapped+json"})
@javax.annotation.Resource(name = "ISPN", mappedName = "java:jboss/infinispan/rhq")
@SuppressWarnings("unchecked")
public class AbstractRestBean {

    protected Log log = LogFactory.getLog(getClass().getName());

    protected final MediaType wrappedCollectionJsonType = new MediaType("application","vnd.rhq.wrapped+json");
    protected final String wrappedCollectionJson = "application/vnd.rhq.wrapped+json";

    private static final CacheKey META_KEY = new CacheKey("rhq.rest.resourceMeta", 0);

    @javax.annotation.Resource( name = "ISPN")
    private CacheContainer container;
    protected Cache<CacheKey, Object> cache;

    /** Subject of the caller that gets injected via {@link SetCallerInterceptor} */
    protected Subject caller;

    @EJB
    protected ResourceManagerLocal resMgr;
    @EJB
    protected ResourceGroupManagerLocal resourceGroupManager;

    @PostConstruct
    public void start() {
        this.cache = this.container.getCache("REST-API");
    }

    /**
     * Renders the passed object with the help of a freemarker template into a string. Freemarket templates
     * are searched in the class path in a directory called "/rest_templates". In the usual Maven tree structure,
     * this is below src/main/resources/.
     *
     * @param templateName Template to use for rendering. If the template name does not end in .ftl, .ftl is appended.
     * @param objectToRender Object to render via template
     * @return Template filled with data from objectToRender
     */
    protected String renderTemplate(String templateName, Object objectToRender) {
        try {
            freemarker.template.Configuration config = new freemarker.template.Configuration();

            // XXX fall-over to ClassTL after failure in FTL seems not to work
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/rest_templates/");
            TemplateLoader[] loaders = new TemplateLoader[] { ctl };
            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);

            config.setTemplateLoader(mtl);

            if (!templateName.endsWith(".ftl")) {
                templateName = templateName + ".ftl";
            }
            Template template = config.getTemplate(templateName);

            StringWriter out = new StringWriter();
            try {
                Map<String, Object> root = new HashMap<String, Object>();
                root.put("var", objectToRender);
                template.process(root, out);
                return out.toString();
            } finally {
                out.close();
            }
        } catch (IOException ioe) {
            log.error(ioe);
        } catch (TemplateException te) {
            log.error(te.getMessage());
        }
        return null;
    }

    /**
     * Retrieve an object from the cache. This is identified by its class and id
     * @param id Id of the object to load.
     * @param clazz Wanted return type
     * @return Object if found and the caller has access to it.
     * @see #getFromCache(int, Class)
     */
    protected <T> T getFromCache(int id, Class<T> clazz) {
        CacheKey key = new CacheKey(clazz, id);
        return getFromCache(key, clazz);
    }

    /**
     * Retrieve an object from the cache if present or null otherwise.
     * We need to be careful here as we must not return objects the current
     * caller has no access to. We do this by checking the "readers" attribute
     * of the selected node to see if the caller has put the object there
     * @param key FullyQualified name (=path in cache) of the object to retrieve
     * @param clazz Return type
     * @return The desired object if found and valid for the current caller. Null otherwise.
     * @see #putToCache(CacheKey, Object)
     */
    protected <T> T getFromCache(CacheKey key, Class<T> clazz) {
        Object o = null;

        CacheValue value = (CacheValue) cache.get(key);

        boolean debugEnabled = log.isDebugEnabled();
        if (null != value) {
            if (debugEnabled) {
                log.debug("Cache Hit for " + key);
            }

            if (value.getReaders().contains(caller.getId())) {
                o = value.getValue();

            } else {
                if (debugEnabled) {
                    log.debug("Cache Hit ignored, caller " + caller.toString() + " not found");
                }
            }
        } else {
            if (debugEnabled) {
                log.debug("Cache Miss for " + key);
            }
        }

        return (T) o;
    }

    /**
     * Put an object into the cache identified by its type and id
     * @param id Id of the object to put
     * @param clazz Type to put in
     * @param o Object to put
     * @return true if put was successful
     * @see #putToCache(CacheKey, Object)
     */
    protected <T> boolean putToCache(int id, Class<T> clazz, T o) {
        CacheKey key = new CacheKey(clazz, id);
        return putToCache(key, o);
    }

    /**
     * Put an object into the cache. We need to record the caller so that we can later
     * check if the caller can access that object or not.
     * @param key Fully qualified name (=path to object)
     * @param o Object to put
     * @return true if put was successful
     * @see #getFromCache(CacheKey, Class)
     */
    @SuppressWarnings("unchecked")
    protected <T> boolean putToCache(CacheKey key, T o) {
        boolean result = false;

        CacheValue value = (CacheValue) cache.get(key);
        if (null != value) {
            value.getReaders().add(caller.getId());
            value.setValue(o);
        } else {
            value = new CacheValue(o, caller.getId());
        }
        try {
            cache.put(key, value);

            if (log.isDebugEnabled()) {
                log.debug("Cache Put " + key);
            }

            result = true;
        }

        catch (Exception e) {
            log.warn(e.getMessage());
        }

        return result;
    }




    /**
     * Remove an item from the cache
     * @param id Id of the item
     * @param clazz Type of object for that node
     * @return true if object is no longer in cache
     */
    protected <T> boolean removeFromCache(int id, Class<T> clazz) {
        CacheKey key = new CacheKey(clazz, id);
        Object cacheValue = cache.remove(key);
        if (null != cacheValue) {
            log.debug("Cache Remove " + key);
        }

        return true;
    }

    public ResourceWithType fillRWT(Resource res, UriInfo uriInfo) {
        ResourceType resourceType = res.getResourceType();
        ResourceWithType rwt = new ResourceWithType(res.getName(), res.getId());
        rwt.setTypeName(resourceType.getName());
        rwt.setTypeId(resourceType.getId());
        rwt.setPluginName(resourceType.getPlugin());
        rwt.setStatus(res.getInventoryStatus().name());
        rwt.setLocation(res.getLocation());
        rwt.setDescription(res.getDescription());
        rwt.setAvailability(res.getCurrentAvailability().getAvailabilityType().toString());
        Resource parent = res.getParentResource();
        if (parent != null) {
            rwt.setParentId(parent.getId());
        } else {
            rwt.setParentId(0);
        }

        rwt.setAncestry(res.getAncestry());

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/operation/definitions");
        uriBuilder.queryParam("resourceId", res.getId());
        URI uri = uriBuilder.build();
        Link link = new Link("operationDefinitions", uri.toString());
        rwt.addLink(link);

        link = getLinkToResource(res, uriInfo, "self");
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/schedules");
        uri = uriBuilder.build(res.getId());
        link = new Link("schedules", uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/availability");
        uri = uriBuilder.build(res.getId());
        link = new Link("availability", uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/children");
        uri = uriBuilder.build(res.getId());
        link = new Link("children", uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/alerts");
        uri = uriBuilder.build(res.getId());
        link = new Link("alerts", uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/alert/definitions");
        uriBuilder.queryParam("resourceId",res.getId());
        uri = uriBuilder.build(res.getId());
        link = new Link("alertDefinitions", uri.toString());
        rwt.addLink(link);
        if (parent != null) {
            uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}/");
            uri = uriBuilder.build(parent.getId());
            link = new Link("parent", uri.toString());
            rwt.addLink(link);
        }
        rwt.addLink(createUILink(uriInfo,UILinkTemplate.RESOURCE,res.getId()));

        return rwt;
    }

    protected Link getLinkToResource(Resource res, UriInfo uriInfo, String rel) {
        UriBuilder uriBuilder;URI uri;
        Link link;
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}");
        uri = uriBuilder.build(res.getId());
        link = new Link(rel, uri.toString());
        return link;
    }

    protected Link getLinkToResourceType(ResourceType type, UriInfo uriInfo, String rel) {
        UriBuilder uriBuilder;URI uri;
        Link link;
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/type/{id}");
        uri = uriBuilder.build(type.getId());
        link = new Link(rel, uri.toString());
        return link;
    }

    protected Link getLinkToGroup(ResourceGroup group, UriInfo uriInfo, String rel) {
        UriBuilder uriBuilder;
        URI uri;
        Link link;
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}");
        uri = uriBuilder.build(group.getId());
        link = new Link(rel, uri.toString());
        return link;
    }

    protected Resource fetchResource(int resourceId) {
        Resource res;
        res = resMgr.getResource(caller, resourceId);
        if (res == null)
            throw new StuffNotFoundException("Resource with id " + resourceId);
        /*
                res = getFromCache(resourceId, Resource.class);
                if (res == null) {
                    res = resMgr.getResource(caller, resourceId);
                    if (res != null)
                        putToCache(resourceId, Resource.class, res);
                    else
                        throw new StuffNotFoundException("Resource with id " + resourceId);
                }
        */
        return res;
    }

    /**
     * Create the paging headers for collections and attach them to the passed builder. Those are represented as
     * <i>Link:</i> http headers that carry the URL for the pages and the respective relation.
     * <br/>In addition a <i>X-total-size</i> header is created that contains the whole collection size.
     * @param builder The ResponseBuilder that receives the headers
     * @param uriInfo The uriInfo of the incoming request to build the urls
     * @param resultList The collection with its paging information
     */
    protected void createPagingHeader(final Response.ResponseBuilder builder, final UriInfo uriInfo, final PageList<?> resultList) {

        UriBuilder uriBuilder;

        PageControl pc = resultList.getPageControl();
        int page = pc.getPageNumber();

        if (resultList.getTotalSize()> (pc.getPageNumber() +1 ) * pc.getPageSize()) {
            int nextPage = page+1;
            uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
            uriBuilder.replaceQueryParam("page",nextPage);

            builder.header("Link",new Link("next",uriBuilder.build().toString()).rfc5988String());
        }

        if (page>0) {
            int prevPage = page -1;
            uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
            uriBuilder.replaceQueryParam("page",prevPage);
            builder.header("Link", new Link("prev",uriBuilder.build().toString()).rfc5988String());
        }

        // A link to the last page
        if (!pc.isUnlimited()) {
            int lastPage = (resultList.getTotalSize() / pc.getPageSize() ) -1;
            uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
            uriBuilder.replaceQueryParam("page",lastPage);
            builder.header("Link", new Link("last",uriBuilder.build().toString()).rfc5988String());
        }

        // A link to the current page
        uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
        builder.header("Link", new Link("current",uriBuilder.build().toString()).rfc5988String());


        // Create a total size header
        builder.header("X-collection-size",resultList.getTotalSize());
    }

    /**
     * Wrap the passed collection #resultList in an object with paging information
     * @param builder ResonseBuilder to add the entity to
     * @param uriInfo UriInfo to construct paging links
     * @param originalList The original list to obtain the paging info from
     * @param resultList The list of result items
     */
    protected void wrapForPaging(Response.ResponseBuilder builder, UriInfo uriInfo, final PageList<?> originalList, final Collection resultList) {

        PagingCollection pColl = new PagingCollection(resultList);
        pColl.setTotalSize(originalList.getTotalSize());
        PageControl pageControl = originalList.getPageControl();
        pColl.setPageSize(pageControl.getPageSize());
        int page = pageControl.getPageNumber();
        pColl.setCurrentPage(page);
        int lastPage = (originalList.getTotalSize() / pageControl.getPageSize()) -1 ; // -1 as page # is 0 based
        pColl.setLastPage(lastPage);

        UriBuilder uriBuilder;
        if (originalList.getTotalSize() > (page +1 ) * pageControl.getPageSize()) {
            int nextPage = page +1;
            uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
            uriBuilder.replaceQueryParam("page",nextPage);
            pColl.addLink(new Link("next",uriBuilder.build().toString()));
        }
        if (page > 0) {
            int prevPage = page -1;
            uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
            uriBuilder.replaceQueryParam("page",prevPage);
            pColl.addLink(new Link("prev",uriBuilder.build().toString()));
        }

        // A link to the last page
        if (!pageControl.isUnlimited()) {
            uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
            uriBuilder.replaceQueryParam("page",lastPage);
            pColl.addLink( new Link("last",uriBuilder.build().toString()));
        }

        // A link to the current page
        uriBuilder = uriInfo.getRequestUriBuilder(); // adds ?q, ?ps and ?category if needed
        pColl.addLink(new Link("current",uriBuilder.build().toString()));

        builder.entity(pColl);
    }

    /**
     * Fetch the group with the passed id
     *
     * @param groupId id of the resource group
     * @param requireCompatible Does the group have to be a compatible group?
     * @return the group object if found
     * @throws org.rhq.enterprise.server.rest.StuffNotFoundException if the group is not found (or not accessible by the caller)
     * @throws BadArgumentException if a compatible group is required, but the found one is not a compatible one
     */
    protected ResourceGroup fetchGroup(int groupId, boolean requireCompatible) {
        ResourceGroup resourceGroup;
        resourceGroup = resourceGroupManager.getResourceGroup(caller, groupId);
        if (resourceGroup == null) {
            throw new StuffNotFoundException("Group with id " + groupId);
        }
        if (requireCompatible) {
            if (resourceGroup.getGroupCategory() != GroupCategory.COMPATIBLE) {
                throw new BadArgumentException("Group with id " + groupId,"it is no compatible group");
            }
        }
        return resourceGroup;
    }

    protected GroupRest fillGroup(ResourceGroup group, UriInfo uriInfo) {

        GroupRest gr = new GroupRest(group.getName());
        gr.setId(group.getId());
        gr.setCategory(group.getGroupCategory());
        gr.setRecursive(group.isRecursive());
        if (group.getGroupDefinition()!=null) {
            gr.setDynaGroupDefinitionId(group.getGroupDefinition().getId());
        }
        gr.setExplicitCount(group.getExplicitResources().size());
        gr.setImplicitCount(group.getImplicitResources().size());
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}");
        URI uri = uriBuilder.build(group.getId());
        Link link = new Link("edit",uri.toASCIIString());
        gr.getLinks().add(link);
        gr.getLinks().add(getLinkToGroup(group,uriInfo, "self"));

        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/group/{id}/metricDefinitions");
        uri = uriBuilder.build(group.getId());
        link = new Link("metricDefinitions",uri.toASCIIString());
        gr.getLinks().add(link);

        gr.getLinks().add(createUILink(uriInfo,UILinkTemplate.GROUP,group.getId()));

        return gr;
    }

    /**
     * Creates a link to the respective entry in coregui
     * @param uriInfo The uriInfo object to build the final url from
     * @param template Template to use
     * @param entityId Ids of the various entities used in the template
     * @return A Link object
     */
    protected Link createUILink(UriInfo uriInfo, UILinkTemplate template, Integer... entityId) {

        String urlBase = template.getUrl();
        String replaced = String.format(urlBase,entityId);

        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.fragment(replaced);
        uriBuilder.replacePath("coregui/"); // trailing / is needed

        URI uri = uriBuilder.build();

        String href = uri.toString();
        href = href.replaceAll("%2F","/");
        Link link = new Link("coregui", href);

        return link;

    }

    protected MetricSchedule getMetricScheduleInternal(UriInfo uriInfo, MeasurementSchedule schedule,
                                                       MeasurementDefinition definition) {
        MetricSchedule ms = new MetricSchedule(schedule.getId(), definition.getName(),
            definition.getDisplayName(), schedule.isEnabled(), schedule.getInterval(), definition
                .getUnits().toString(), definition.getDataType().toString());
        ms.setDefinitionId(definition.getId());

        if (schedule.getMtime()!=null)
            ms.setMtime(schedule.getMtime());


        UriBuilder uriBuilder;
        URI uri;
        if (definition.getDataType() == DataType.MEASUREMENT) {
            uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/metric/data/{id}");
            uri = uriBuilder.build(schedule.getId());
            Link metricLink = new Link("metric", uri.toString());
            ms.addLink(metricLink);
            uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/metric/data/{id}/raw");
            uri = uriBuilder.build(schedule.getId());
            metricLink = new Link("metric-raw", uri.toString());
            ms.addLink(metricLink);
        }
        // create link to the resource
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("resource/" + schedule.getResource().getId());
        uri = uriBuilder.build();
        Link link = new Link("resource", uri.toString());
        ms.addLink(link);
        return ms;
    }

    /**
     * Set the caching header on the response
     * @param builder Response builder to put the caching header on
     * @param maxAgeSecs Max retention time on the client. Only set if the value is > 0
     */
    protected void setCachingHeader(Response.ResponseBuilder builder, int maxAgeSecs) {
        CacheControl cc = new CacheControl();
        cc.setPrivate(false);
        cc.setNoCache(false);
        cc.setNoStore(false);
        if (maxAgeSecs>-1)
            cc.setMaxAge(maxAgeSecs);
        builder.cacheControl(cc);
    }

    protected static class CacheKey {
        private String namespace;
        private int id;

        /**
         * @param clazz The class name will be used as the namespace for the id.
         * @param id
         */
        public CacheKey(Class<?> clazz, int id) {
            this(clazz.getName(), id);
        }

        public CacheKey(String namespace, int id) {
            this.namespace = namespace;
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
            result = prime * result + id;

            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            if (namespace == null) {
                if (other.namespace != null) {
                    return false;
                }
            } else if (!namespace.equals(other.namespace)) {
                return false;
            }
            if (id != other.id) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "CacheKey [namespace=" + namespace + ", id=" + id + "]";
        }
    }

    private static class CacheValue {
        private Object value;
        private Set<Integer> readers;

        public CacheValue(Object value, int readerId) {
            this.readers = new HashSet<Integer>();
            this.readers.add(readerId);
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Set<Integer> getReaders() {
            return readers;
        }
    }
}
