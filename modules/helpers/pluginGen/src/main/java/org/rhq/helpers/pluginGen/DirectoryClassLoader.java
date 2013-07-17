/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.helpers.pluginGen;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Classloader to load from a given directory
 * @author Heiko W. Rupp
 */
public class DirectoryClassLoader extends ClassLoader {
    private Hashtable classes = new Hashtable(); //used to cache already defined classes
    private String baseDir;

    @Override
    protected Class<?> findClass(String pathName) throws ClassNotFoundException {


        if (baseDir==null) {
            throw new IllegalStateException("Must set baseDir first");
        }

        byte classByte[];
        Class result = null;

        String className = pathName.substring(baseDir.length()+1); // remove base dir
        className = className.substring(0,className.length()-6); // remove .class
        className = className.replaceAll(File.separator,"."); // change / -> .

        result = (Class) classes.get(className); //checks in cached classes
        if (result != null) {
            return result;
        }
        try {
            File classFile = new File(pathName);
            FileInputStream fis = new FileInputStream(classFile);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            int nextValue = fis.read();
            while (-1 != nextValue) {
                byteStream.write(nextValue);
                nextValue = fis.read();
            }

            classByte = byteStream.toByteArray();
            result = defineClass(className, classByte, 0, classByte.length, null);
            classes.put(className, result);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public List<Class> findClasses() {
        if (baseDir==null) {
            throw new IllegalStateException("Must set baseDir first");
        }

        File baseFile = new File(baseDir);
        if(!baseFile.isDirectory()) {
            throw new IllegalStateException("BaseDir is no directory");
        }
        if (!baseFile.canRead()) {
            throw new IllegalStateException("BaseDir is not readable");
        }

        List<File> files = walk(baseFile);

        List<Class> classes = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getAbsolutePath();

            Class clazz = null;
            try {
                clazz = findClass(fileName);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
            classes.add(clazz);

        }

        return classes;
    }

    private List<File> walk(File path) {

        List<File> files = new ArrayList<>();

        File[] list = path.listFiles();

        if (list == null) {
            return files;
        }

        for ( File f : list ) {
            if ( f.isDirectory() ) {
                List<File> newFiles = walk( f );
                System.out.println( "Dir:" + f.getAbsoluteFile() );
                files.addAll(newFiles);
            }
            else {
                System.out.println( "File:" + f.getAbsoluteFile() );
                if (f.getName().endsWith(".class") && !f.getName().contains("$")) {
                    files.add(f);
                }

            }
        }
        return files;
    }
}
