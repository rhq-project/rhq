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
 * The annotation can be given at class or method level, where a method level annotation
 * overrides a class level one.
 *
 * @author Lukas Krejci
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD, ElementType.TYPE })
public @interface DatabaseState {

    /**
     * The location of the database state export file.
     */
    String url();

    /**
     * The version of the RHQ database the export file is generated from.
     * Before the data from the export file are imported into the database, the database
     * is freshly created and upgraded to this version. After that, the export file
     * is imported to it and the database is then upgraded to the latest version.
     */
    String dbVersion();

    /**
     * Where is the export file accessible from (defaults to {@link DatabaseStateStorage#CLASSLOADER}).
     */
    DatabaseStateStorage storage() default DatabaseStateStorage.CLASSLOADER;

    /**
     * The format of the export file (defaults to zipped xml).
     */
    FileFormat format() default FileFormat.ZIPPED_XML;

}
