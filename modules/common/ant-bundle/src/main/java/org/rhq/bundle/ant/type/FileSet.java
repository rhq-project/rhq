/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.bundle.ant.type;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

/**
 * A fileset that supports includes, but not excludes or selectors.
 */
public class FileSet extends AbstractBundleType {
    private PatternSet defaultPatterns = new PatternSet();

    private File dir;

    /**
     * Construct a new <code>IncludesFileSet</code>.
     */
    public FileSet() {
        super();
    }

    /**
     * Makes this instance in effect a reference to another instance.
     * <p/>
     * <p>You must not set another attribute or nest elements inside
     * this element if you make it a reference.</p>
     *
     * @param r the <code>Reference</code> to use.
     */
    public void setRefid(Reference r) throws BuildException {
        if (dir != null || defaultPatterns.hasPatterns(getProject())) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Sets the base-directory for this instance.
     *
     * @param dir the directory's <code>File</code> instance.
     */
    // Pass in a String, rather than a File, since we don't want Ant to resolve the path relative to basedir if it's relative.
    public void setDir(String dir) throws BuildException {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.dir = new File(dir);
    }

    /**
     * Retrieves the base-directory for this instance.
     *
     * @return the base-directory for this instance
     */
    public File getDir() {
        return (isReference()) ? getRef(getProject()).getDir() : this.dir;
    }

    /**
     * Add a name entry to the include list.
     *
     * @return <code>PatternSet.NameEntry</code>.
     */
    public PatternSet.NameEntry createInclude() {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        return defaultPatterns.createInclude();
    }

    /**
     * Creates a single file fileset.
     *
     * @param file the single <code>File</code> included in this
     *             <code>AbstractFileSet</code>.
     */
    public void setFile(String file) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        createInclude().setName(file);
    }

    /**
     * Appends <code>includes</code> to the current list of include
     * patterns.
     * <p/>
     * <p>Patterns may be separated by a comma or a space.</p>
     *
     * @param includes the <code>String</code> containing the include patterns.
     */
    public void setIncludes(String includes) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        defaultPatterns.setIncludes(includes);
    }

    public String[] getIncludePatterns() {
        return this.defaultPatterns.getIncludePatterns(getProject());
    }

    /**
     * Gets as descriptive as possible a name used for this datatype instance.
     *
     * @return <code>String</code> name.
     */
    protected String getDataTypeName() {
        // look up the types in project and see if they match this class
        Project p = getProject();
        if (p != null) {
            Hashtable typedefs = p.getDataTypeDefinitions();
            for (Enumeration e = typedefs.keys(); e.hasMoreElements();) {
                String typeName = (String) e.nextElement();
                Class typeClass = (Class) typedefs.get(typeName);
                if (typeClass == getClass()) {
                    return typeName;
                }
            }
        }
        String classname = getClass().getName();
        return classname.substring(classname.lastIndexOf('.') + 1);
    }

    /**
     * Performs the check for circular references and returns the
     * referenced FileSet.
     */
    protected FileSet getRef(Project p) {
        if (!isChecked()) {
            Stack stk = new Stack();
            stk.push(this);
            dieOnCircularReference(stk, p);
        }
        Object o = getRefid().getReferencedObject(p);
        if (!getClass().isAssignableFrom(o.getClass())) {
            throw new BuildException(getRefid().getRefId()
                    + " doesn\'t denote a " + getDataTypeName());
        }
        return (FileSet) o;
    }

    /**
     * Returns the list of unresolved include patterns.
     *
     * @return the list of unresolved include patterns
     */
    public String toString() {
        String[] includes = getIncludePatterns();
        return Arrays.asList(includes).toString();
    }
}
