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
import java.util.Map;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;

/**
 * Abstract base class for EJB classes that implement REST methods.
 * @author Heiko W. Rupp
 */
public class AbstractRestBean {

    Log log = LogFactory.getLog(getClass().getName());

    /** Subject of the caller that gets injected via {@link SetCallerInterceptor} */
    Subject caller;

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
}
