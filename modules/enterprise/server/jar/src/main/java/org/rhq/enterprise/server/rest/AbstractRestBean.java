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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.Stateless;

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

/**
 * Abstract base class for EJB classes that implement REST methods.
 * For the cache and its evicion policies see rhq-cache-service.xml
 * @author Heiko W. Rupp
 */
@Stateless
@Resource(name="cache",type= TreeCacheMBean.class,mappedName = "RhqCache")
public class AbstractRestBean {

    Log log = LogFactory.getLog(getClass().getName());

    /** Subject of the caller that gets injected via {@link SetCallerInterceptor} */
    Subject caller;

    /** The cache to use */
    @Resource(name="cache")
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
            log.info("Hit for " + fqn.toString());
            try{
                Node n = treeCache.get(fqn);
                Set<Integer> readers= (Set<Integer>) n.get("readers");
                if (readers.contains(caller.getId())) {
                    o = n.get("item");
                }
                else {
                    log.info("No object for caller " +caller.toString() + " found");
                }
            } catch (CacheException e) {
                e.printStackTrace();  // TODO: Customise this generated block
                log.info("Miss for " + fqn.toString());
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
            log.info("Put " + fqn);
        } catch (CacheException e) {
            e.printStackTrace();  // TODO: Customise this generated block
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
                log.info("Cancel " + fqn);
                return true;
            } catch (CacheException e) {
                return false;
            }
        }
        return true;
    }
}
