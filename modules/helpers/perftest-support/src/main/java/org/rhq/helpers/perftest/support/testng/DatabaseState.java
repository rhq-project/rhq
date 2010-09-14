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

package org.rhq.helpers.perftest.support.testng;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.rhq.helpers.perftest.support.FileFormat;

/**
 * An annotation to associate a test method with a required state of the database.
 * 
 * @author Lukas Krejci
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD })
public @interface DatabaseState {

    /**
     * The location of the database state export file.
     */
    String url();

    /**
     * Where is the export file accessible from (defaults to {@link DatabaseStateStorage#CLASSLOADER}).
     */
    DatabaseStateStorage storage() default DatabaseStateStorage.CLASSLOADER;
    
    /**
     * The format of the export file (defaults to zipped xml).
     */
    FileFormat format() default FileFormat.ZIPPED_XML;
    
    /**
     * The name of the method to provide a JDBC connection object.
     * If the method is not specified, the value of the {@link JdbcConnectionProviderMethod} annotation
     * is used.
     */
    String connectionProviderMethod() default "";
}
