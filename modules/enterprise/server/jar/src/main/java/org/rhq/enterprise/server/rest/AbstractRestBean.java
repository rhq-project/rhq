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
package org.rhq.enterprise.server.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.TreeCacheMBean;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.rest.domain.Link;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

/**
 * Abstract base class for EJB classes that implement REST methods.
 * For the cache and its evicion policies see rhq-cache-service.xml
 * @author Heiko W. Rupp
 */
@javax.annotation.Resource(name="cache",type= TreeCacheMBean.class,mappedName = "RhqCache")
public class AbstractRestBean {

    Log log = LogFactory.getLog(getClass().getName());

    /** Subject of the caller that gets injected via {@link SetCallerInterceptor} */
    Subject caller;

    /** The cache to use */
    @javax.annotation.Resource(name="cache")
    TreeCacheMBean treeCache;

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
            freemarker.template.Configuration config = new Configuration();

            // XXX fall-over to ClassTL after failure in FTL seems not to work
            // FileTemplateLoader ftl = new FileTemplateLoader(new File("src/main/resources"));
            ClassTemplateLoader ctl = new ClassTemplateLoader(getClass(), "/rest_templates/");
            TemplateLoader[] loaders = new TemplateLoader[] { ctl };
            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);

            config.setTemplateLoader(mtl);

            if (!templateName.endsWith(".ftl"))
                templateName = templateName + ".ftl";
            Template template = config.getTemplate(templateName);

            StringWriter out = new StringWriter();
            try {
                Map<String, Object> root = new HashMap<String, Object>();
                root.put("var", objectToRender);
                template.process(root, out);
                return out.toString();
            }
            finally {
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
     * @see #getFqn(int, Class)
     */
    protected <T>T getFromCache(int id,Class<T> clazz) {
        Fqn fqn = getFqn(id, clazz);
        return getFromCache(fqn,clazz);
    }


    /**
     * Retrieve an object from the cache if present or null otherwise.
     * We need to be careful here as we must not return objects the current
     * caller has no access to. We do this by checking the "readers" attribute
     * of the selected node to see if the caller has put the object there
     * @param fqn FullyQualified name (=path in cache) ot the object to retrieve
     * @param clazz Return type
     * @return The desired object if found and valid for the current caller. Null otherwise.
     * @see #putToCache(org.jboss.cache.Fqn, Object)
     */
    @SuppressWarnings("unchecked")
    protected <T>T getFromCache(Fqn fqn,Class<T> clazz) {
        Object o=null;
        if (treeCache.exists(fqn)) {
            log.debug("Hit for " + fqn.toString());
            try{
                Node n = treeCache.get(fqn);
                Set<Integer> readers= (Set<Integer>) n.get("readers");
                if (readers.contains(caller.getId())) {
                    o = n.get("item");
                }
                else {
                    log.debug("No object for caller " +caller.toString() + " found");
                }
            } catch (CacheException e) {
                log.debug("Miss for " + fqn.toString());
            }
        }
        return (T)o;
    }

    /**
     * Put an object into the cache. We need to record the caller so that we can later
     * check if the caller can access that object or not.
     * @param fqn Fully qualified name (=path to object)
     * @param o Object to put
     * @return true if put was successful
     * @see #getFromCache(org.jboss.cache.Fqn, Class)
     */
    @SuppressWarnings("unchecked")
    protected <T>boolean putToCache(Fqn fqn,T o) {
        boolean success = false;
        try {
            Set<Integer> readers;
            if (treeCache.exists(fqn)) {
                Node n = treeCache.get(fqn);
                readers = (Set<Integer>) n.get("readers");
            } else {
                readers = new HashSet<Integer>();
            }
            readers.add(caller.getId());
            treeCache.put(fqn,"readers",readers);
            treeCache.put(fqn,"item",o);
            success = true;
            log.debug("Put " + fqn);
        } catch (CacheException e) {
            log.warn(e.getMessage());
        }
        return success;
    }

    /**
     * Put an object into the cache identified by its type and id
     * @param id Id of the object to put
     * @param clazz Type to put in
     * @param o Object to put
     * @return true if put was successful
     * @see #putToCache(org.jboss.cache.Fqn, Object)
     */
    protected <T>boolean putToCache(int id,Class<T> clazz,T o) {
        Fqn fqn = getFqn(id, clazz);
        return putToCache(fqn,o);
    }

    protected void putResourceToCache(Resource res) {
        putToCache(res.getId(),Resource.class,res);

        Fqn callerFqn = new Fqn(new String[]{"user", String.valueOf(caller.getId()),"resources"});
        Set<Integer> visibleResources;
        try {
            if (treeCache.exists(callerFqn)) {
                Node n = treeCache.get(callerFqn);
                visibleResources = (Set<Integer>) n.get("visibleResources");
            }
            else {
                visibleResources = new HashSet<Integer>();
            }
            visibleResources.add(res.getId());
            treeCache.put(callerFqn,"visibleResources",visibleResources);

            Fqn resourceMeta = new Fqn(new String[] { "resourceMeta"});
            Map<Integer,Integer> childParentMap;
            if (treeCache.exists(resourceMeta)) {
                childParentMap = (Map<Integer, Integer>) treeCache.get(resourceMeta,"childParentMap");
            }
            else {
                childParentMap = new HashMap<Integer,Integer>();
            }
            int pid = res.getParentResource() == null ? 0 : res.getParentResource().getId();
            childParentMap.put(res.getId(), pid);
            treeCache.put(resourceMeta,"childParentMap",childParentMap);
        } catch (CacheException e) {
            log.warn(e.getMessage());
        }
    }

    protected List<Resource> getResourcesFromCacheByParentId(int pid) {
        List<Integer> candidateIds = new ArrayList<Integer>();
        List<Resource> ret = new ArrayList<Resource>();

        // First determine candidate children
        Map<Integer,Integer> childParentMap;
        Fqn resourceMeta = new Fqn(new String[] { "resourceMeta"});
        if (treeCache.exists(resourceMeta)) {
            try {
                childParentMap = (Map<Integer, Integer>) treeCache.get(resourceMeta,"childParentMap");
                for (Map.Entry<Integer,Integer> entry : childParentMap.entrySet()) {
                    if (entry.getValue() == pid)
                        candidateIds.add(entry.getKey());
                }
                // then see if the current user can see them
                Fqn callerFqn = new Fqn(new String[]{"user", String.valueOf(caller.getId()),"resources"});
                Set<Integer> visibleResources = (Set<Integer>) treeCache.get(callerFqn,"visibleResources");
                Iterator<Integer> iter = candidateIds.iterator();
                while (iter.hasNext()) {
                    Integer resId = iter.next();
                    if (!visibleResources.contains(resId)) {
                        iter.remove();
                    }
                }

                // Last but not least, get the resources and return them
                for (Integer resId : candidateIds) {
                    ret.add(getFromCache(resId, Resource.class));
                }
            } catch (CacheException e) {
                log.warn(e.getMessage());
            }

        }
        return ret;
    }

    protected Resource getResourceFromCache(int resourceid) {

        Resource res = null;
        // check if the current user can see the resource
        Fqn callerFqn = new Fqn(new String[]{"user", String.valueOf(caller.getId()),"resources"});
        if (treeCache.exists(callerFqn)) {
            try {
                Set<Integer> visibleResources = (Set<Integer>) treeCache.get(callerFqn,"visibleResources");
                if (visibleResources.contains(resourceid))
                    res = getFromCache(resourceid,Resource.class);
            } catch (CacheException e) {
                log.warn(e.getMessage());
            }
        }

        return res;
    }


    /**
     * Construct a Fqn object from the passed data
     * @param id Id of the target object
     * @param clazz Type of object for that node
     * @return Fqn object
     */
    protected <T> Fqn getFqn(int id, Class<T> clazz) {
        return new Fqn(new Object[]{clazz.getName(), String.valueOf(id)});
    }

    /**
     * Remove an item from the cache
     * @param operationId Id of the item
     * @param clazz Type of object for that node
     * @return true if object is no longer in cache
     */
    protected <T> boolean removeFromCache(int operationId, Class<T> clazz) {
        Fqn fqn = getFqn(operationId,clazz);
        if (treeCache.exists(fqn)) {
            try {
                treeCache.remove(fqn);
                log.debug("Cancel " + fqn);
                return true;
            } catch (CacheException e) {
                return false;
            }
        }
        return true;
    }

    public ResourceWithType fillRWT(org.rhq.core.domain.resource.Resource res, UriInfo uriInfo) {
        ResourceType resourceType = res.getResourceType();
        ResourceWithType rwt = new ResourceWithType(res.getName(),res.getId());
        rwt.setTypeName(resourceType.getName());
        rwt.setTypeId(resourceType.getId());
        rwt.setPluginName(resourceType.getPlugin());
        org.rhq.core.domain.resource.Resource parent = res.getParentResource();
        if (parent!=null) {
            rwt.setParentId(parent.getId());
        }
        else
            rwt.setParentId(0);
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/operation/definitions");
        uriBuilder.queryParam("resourceId",res.getId());
        URI uri = uriBuilder.build();
        Link link = new Link("operationDefinitions",uri.toString());
        rwt.addLink(link);

/*
        for (Resource child : getResourcesFromCacheByParentId(res.getId())) {
            uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}");
            uri = uriBuilder.build(child.getId());
            link = new Link("child",uri.toString());
            rwt.addLink(link);
        }
*/
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}");
        uri = uriBuilder.build(res.getId());
        link = new Link("self",uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/schedules");
        uri = uriBuilder.build(res.getId());
        link = new Link("schedules",uri.toString());
        rwt.addLink(link);
        uriBuilder = uriInfo.getBaseUriBuilder();
        uriBuilder.path("/resource/{id}/children");
        uri = uriBuilder.build(res.getId());
        link = new Link("children",uri.toString());
        rwt.addLink(link);
        if (parent!=null) {
            uriBuilder = uriInfo.getBaseUriBuilder();
            uriBuilder.path("/resource/{id}/");
            uri = uriBuilder.build(parent.getId());
            link = new Link("parent",uri.toString());
            rwt.addLink(link);
        }

        return rwt;
    }
}
