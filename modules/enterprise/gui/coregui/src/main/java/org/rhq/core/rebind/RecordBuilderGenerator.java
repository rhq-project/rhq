/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.rebind;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import javax.persistence.Entity;

import java.io.PrintWriter;

/**
 * This is the start to a rebinder for our domain entities to allow for the
 * easy generation of Smart's Record objects.
 *
 * TODO: Get this working.
 * TODO: This should ideally be moved to a separate Maven module (e.g. rhq-gwt-tools.jar),
 *       since it's not part of coregui.war, but rather, a tool used to help compile it.
 * 
 * @author Greg Hinkle
 */
public class RecordBuilderGenerator extends Generator {

    protected TreeLogger logger;
    private String packageName;
    private String qualifiedStubClassName;
    private JClassType requestedClass;
    private String simpleStubClassName;
    private SourceWriter sourceWriter;
    private TypeOracle typeOracle;

    /**
     * Create a new type that satisfies the rebind request.
     */
    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {

        System.out.println("classType " + typeName);

        if (!init(logger, context, typeName)) {
            return null;
        }

        writeSource();
        sourceWriter.commit(logger);

        return qualifiedStubClassName;
    }

    protected JClassType getRequestedClass() {
        return requestedClass;
    }

    protected SourceWriter getSourceWriter() {
        return sourceWriter;
    }

    protected TypeOracle getTypeOracle() {
        return typeOracle;
    }

    @SuppressWarnings("unused")
    protected void writeSource() throws UnableToCompleteException {



        JField[] fields = requestedClass.getFields();

        writeBuildRecordMethod(fields, sourceWriter);
    }

    /**
     * Gets the name of the native stub class.
     */
    private String getSimpleStubClassName(JClassType baseClass) {
        return "__" + baseClass.getSimpleSourceName() + "_RecordBuilder";
    }

    private SourceWriter getSourceWriter(TreeLogger logger, GeneratorContext ctx,
                                         String packageName, String className, String superclassName) {

        PrintWriter printWriter = ctx.tryCreate(logger, packageName, className);
        if (printWriter == null) {
            return null;
        }

        ClassSourceFileComposerFactory composerFactory = new ClassSourceFileComposerFactory(
                packageName, className);

        composerFactory.setSuperclass(superclassName);

        return composerFactory.createSourceWriter(ctx, printWriter);
    }

    private boolean init(TreeLogger logger, GeneratorContext context,
                         String typeName) throws UnableToCompleteException {
        this.logger = logger;
        typeOracle = context.getTypeOracle();
        assert typeOracle != null;

        try {
            requestedClass = typeOracle.getType(typeName);
        } catch (NotFoundException e) {
            logger.log(
                    TreeLogger.ERROR,
                    "Could not find type '"
                            + typeName
                            + "'; please see the log, as this usually indicates a previous error ",
                    e);
            throw new UnableToCompleteException();
        }

        if (requestedClass.getAnnotation(Entity.class) == null) {
            System.out.println("Not an entity: " + typeName);
            return false;
        }

        System.out.println("$$$$$$$$Making a record builder for: "+ typeName);

        // Get the stub class name, and see if its source file exists.
        //
        simpleStubClassName = getSimpleStubClassName(requestedClass);
        packageName = requestedClass.getPackage().getName();
        qualifiedStubClassName = packageName + "." + simpleStubClassName;

        sourceWriter = getSourceWriter(logger, context, packageName,
                simpleStubClassName, requestedClass.getQualifiedSourceName());

        return sourceWriter != null;
    }

    private void writeBuildRecordMethod(JField[] fields, SourceWriter sw) {
        sw.println();
        sw.println("protected final com.smartgwt.client.data.Record buildRecord(" + requestedClass.getQualifiedSourceName() + " entity) throws Throwable {");
        sw.indent();

        sw.println("com.smartgwt.client.data.Record record = new com.smartgwt.client.data.Record();");

        for (JField field :fields) {
            sw.println("record.setAttribute(\"" + field.getName() + "\", " + field.getName() + ");");
        }
        sw.println("return record;");
        sw.outdent();
        sw.println("}");
    }

}
