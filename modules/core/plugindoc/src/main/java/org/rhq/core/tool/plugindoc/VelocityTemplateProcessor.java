/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.tool.plugindoc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * @author Ian Springer
 */
public class VelocityTemplateProcessor {
    private VelocityContext context;
    private Template template;

    public VelocityTemplateProcessor(String templateResourcePath, String macroLibrary, Class referenceInsertionClass) {
        this.context = new VelocityContext();
        Properties config = createVelocityConfiguration(macroLibrary, referenceInsertionClass);
        try
        {
            VelocityEngine engine = new VelocityEngine(config);
            this.template = engine.getTemplate(templateResourcePath);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Properties createVelocityConfiguration(String macroLibrary, Class referenceInsertionClass) {
        Properties velocityConfig = new Properties();
        // Velocity configuration syntax reference:
        //   http://velocity.apache.org/engine/releases/velocity-1.6/developer-guide.html
        velocityConfig.setProperty("resource.loader", "class");
        velocityConfig.setProperty("class.resource.loader.class",
            "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        if (referenceInsertionClass != null)
            velocityConfig.setProperty("eventhandler.referenceinsertion.class", referenceInsertionClass.getName());
        String macroLibraries = "common-macros.vm";
        if (macroLibrary != null)
           macroLibraries += ", " + macroLibrary;
        velocityConfig.setProperty("velocimacro.library", macroLibraries);
        return velocityConfig;
    }

    public VelocityContext getContext() {
        return this.context;
    }

    public void processTemplate(File outputFile) {
        try {
            OutputStream outputStream = new FileOutputStream(outputFile);
            Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            this.template.merge(this.context, writer);
            writer.close();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}