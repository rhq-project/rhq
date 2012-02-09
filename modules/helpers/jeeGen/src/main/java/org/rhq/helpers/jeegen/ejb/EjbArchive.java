/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.helpers.jeegen.ejb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchiveFormat;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Assignable;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.asset.NamedAsset;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.formatter.Formatter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.path.BasicPath;

/**
 * An EJB JAR.
 *
 * @author Ian Springer
 */
public class EjbArchive implements JavaArchive {

    private JavaArchive delegate;

    /**
     * Create a new JavaArchive with any type storage engine as backing.
     *
     * @param delegate The storage backing.
     */
    public EjbArchive(JavaArchive delegate, int statelessSessionBeanCount, int statefulSessionBeanCount) {
        this.delegate = delegate;

        Configuration cfg = new Configuration();
        cfg.setClassForTemplateLoading(getClass(), "");
        cfg.setObjectWrapper(new DefaultObjectWrapper());

        ByteArrayOutputStream byteArrayOutputStream;
        try {
            Template temp = cfg.getTemplate("v2/ejb-jar.xml.ftl");

            Map dataModel = new HashMap();
            dataModel.put("name", "Test");
            dataModel.put("package", "test.ejb.v2");
            dataModel.put("statelessSessionBeanCount", statelessSessionBeanCount);
            dataModel.put("statefulSessionBeanCount", statefulSessionBeanCount);

            byteArrayOutputStream = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(byteArrayOutputStream);
            temp.process(dataModel, out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add ejb-jar.xml to EJB-JAR.", e);
        }

        Asset ejbJarXml = new ByteArrayAsset(byteArrayOutputStream.toByteArray());                

        // add deployment descriptor
        addAsManifestResource(ejbJarXml, new BasicPath("ejb-jar.xml"));

        // add stateless session bean classes
        addAsResource(new ClassLoaderAsset("test/ejb/v2/StatelessSessionEJBHome.class"), "test/ejb/v2/StatelessSessionEJBHome.class");
        addAsResource(new ClassLoaderAsset("test/ejb/v2/StatelessSessionEJBObject.class"),"test/ejb/v2/StatelessSessionEJBObject.class");
        addAsResource(new ClassLoaderAsset("test/ejb/v2/StatelessSessionBean.class"), "test/ejb/v2/StatelessSessionBean.class");

        // add stateful session bean classes
        addAsResource(new ClassLoaderAsset("test/ejb/v2/StatefulSessionEJBHome.class"), "test/ejb/v2/StatefulSessionEJBHome.class");
        addAsResource(new ClassLoaderAsset("test/ejb/v2/StatefulSessionEJBObject.class"),"test/ejb/v2/StatefulSessionEJBObject.class");
        addAsResource(new ClassLoaderAsset("test/ejb/v2/StatefulSessionBean.class"), "test/ejb/v2/StatefulSessionBean.class");
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public JavaArchive add(Asset asset, ArchivePath target) throws IllegalArgumentException {
        return delegate.add(asset, target);
    }

    @Override
    public JavaArchive add(Asset asset, ArchivePath target, String name) throws IllegalArgumentException {
        return delegate.add(asset, target, name);
    }

    @Override
    public JavaArchive add(Asset asset, String target, String name) throws IllegalArgumentException {
        return delegate.add(asset, target, name);
    }

    @Override
    public JavaArchive add(NamedAsset namedAsset) throws IllegalArgumentException {
        return delegate.add(namedAsset);
    }

    @Override
    public JavaArchive add(Asset asset, String target) throws IllegalArgumentException {
        return delegate.add(asset, target);
    }

    @Override
    public JavaArchive addAsDirectory(String path) throws IllegalArgumentException {
        return delegate.addAsDirectory(path);
    }

    @Override
    public JavaArchive addAsDirectories(String... paths) throws IllegalArgumentException {
        return delegate.addAsDirectories(paths);
    }

    @Override
    public JavaArchive addAsDirectory(ArchivePath path) throws IllegalArgumentException {
        return delegate.addAsDirectory(path);
    }

    @Override
    public JavaArchive addAsDirectories(ArchivePath... paths) throws IllegalArgumentException {
        return delegate.addAsDirectories(paths);
    }

    @Override
    public Node get(ArchivePath path) throws IllegalArgumentException {
        return delegate.get(path);
    }

    @Override
    public Node get(String path) throws IllegalArgumentException {
        return delegate.get(path);
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, String path) {
        return delegate.getAsType(type, path);
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, ArchivePath path) {
        return delegate.getAsType(type, path);
    }

    @Override
    public <X extends Archive<X>> Collection<X> getAsType(Class<X> type, Filter<ArchivePath> filter) {
        return delegate.getAsType(type, filter);
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, String path, ArchiveFormat archiveFormat) {
        return delegate.getAsType(type, path, archiveFormat);
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, ArchivePath path, ArchiveFormat archiveFormat) {
        return delegate.getAsType(type, path, archiveFormat);
    }

    @Override
    public <X extends Archive<X>> Collection<X> getAsType(Class<X> type, Filter<ArchivePath> filter, ArchiveFormat archiveFormat) {
        return delegate.getAsType(type, filter, archiveFormat);
    }

    @Override
    public boolean contains(ArchivePath path) throws IllegalArgumentException {
        return delegate.contains(path);
    }

    @Override
    public boolean contains(String path) throws IllegalArgumentException {
        return delegate.contains(path);
    }

    @Override
    public Node delete(ArchivePath path) throws IllegalArgumentException {
        return delegate.delete(path);
    }

    @Override
    public Node delete(String archivePath) throws IllegalArgumentException {
        return delegate.delete(archivePath);
    }

    @Override
    public Map<ArchivePath, Node> getContent() {
        return delegate.getContent();
    }

    @Override
    public Map<ArchivePath, Node> getContent(Filter<ArchivePath> filter) {
        return delegate.getContent(filter);
    }

    @Override
    public JavaArchive add(Archive<?> archive, ArchivePath path, Class<? extends StreamExporter> exporter) throws IllegalArgumentException {
        return delegate.add(archive, path, exporter);
    }

    @Override
    public JavaArchive add(Archive<?> archive, String path, Class<? extends StreamExporter> exporter) throws IllegalArgumentException {
        return delegate.add(archive, path, exporter);
    }

    @Override
    public JavaArchive merge(Archive<?> source) throws IllegalArgumentException {
        return delegate.merge(source);
    }

    @Override
    public JavaArchive merge(Archive<?> source, Filter<ArchivePath> filter) throws IllegalArgumentException {
        return delegate.merge(source, filter);
    }

    @Override
    public JavaArchive merge(Archive<?> source, ArchivePath path) throws IllegalArgumentException {
        return delegate.merge(source, path);
    }

    @Override
    public JavaArchive merge(Archive<?> source, String path) throws IllegalArgumentException {
        return delegate.merge(source, path);
    }

    @Override
    public JavaArchive merge(Archive<?> source, ArchivePath path, Filter<ArchivePath> filter) throws IllegalArgumentException {
        return delegate.merge(source, path, filter);
    }

    @Override
    public JavaArchive merge(Archive<?> source, String path, Filter<ArchivePath> filter) throws IllegalArgumentException {
        return delegate.merge(source, path, filter);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public String toString(boolean verbose) {
        return delegate.toString(verbose);
    }

    @Override
    public String toString(Formatter formatter) throws IllegalArgumentException {
        return delegate.toString(formatter);
    }

    @Override
    public void writeTo(OutputStream outputStream, Formatter formatter) throws IllegalArgumentException {
        delegate.writeTo(outputStream, formatter);
    }

    @Override
    public <TYPE extends Assignable> TYPE as(Class<TYPE> clazz) {
        return delegate.as(clazz);
    }

    @Override
    public JavaArchive addAsServiceProviderAndClasses(Class<?> serviceInterface, Class<?>... serviceImpls) throws IllegalArgumentException {
        return delegate.addAsServiceProviderAndClasses(serviceInterface, serviceImpls);
    }

    @Override
    public JavaArchive setManifest(String resourceName) throws IllegalArgumentException {
        return delegate.setManifest(resourceName);
    }

    @Override
    public JavaArchive setManifest(File resource) throws IllegalArgumentException {
        return delegate.setManifest(resource);
    }

    @Override
    public JavaArchive setManifest(URL resource) throws IllegalArgumentException {
        return delegate.setManifest(resource);
    }

    @Override
    public JavaArchive setManifest(Asset resource) throws IllegalArgumentException {
        return delegate.setManifest(resource);
    }

    @Override
    public JavaArchive setManifest(Package resourcePackage, String resourceName) throws IllegalArgumentException {
        return delegate.setManifest(resourcePackage, resourceName);
    }

    @Override
    public JavaArchive addAsManifestResource(String resourceName) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resourceName);
    }

    @Override
    public JavaArchive addAsManifestResource(File resource) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resource);
    }

    @Override
    public JavaArchive addAsManifestResource(String resourceName, String target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resourceName, target);
    }

    @Override
    public JavaArchive addAsManifestResource(File resource, String target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resource, target);
    }

    @Override
    public JavaArchive addAsManifestResource(URL resource, String target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resource, target);
    }

    @Override
    public JavaArchive addAsManifestResource(Asset resource, String target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resource, target);
    }

    @Override
    public JavaArchive addAsManifestResource(String resourceName, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resourceName, target);
    }

    @Override
    public JavaArchive addAsManifestResource(File resource, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resource, target);
    }

    @Override
    public JavaArchive addAsManifestResource(URL resource, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resource, target);
    }

    @Override
    public JavaArchive addAsManifestResource(Asset resource, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resource, target);
    }

    @Override
    public JavaArchive addAsManifestResources(Package resourcePackage, String... resourceNames) throws IllegalArgumentException {
        return delegate.addAsManifestResources(resourcePackage, resourceNames);
    }

    @Override
    public JavaArchive addAsManifestResource(Package resourcePackage, String resourceName) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resourcePackage, resourceName);
    }

    @Override
    public JavaArchive addAsManifestResource(Package resourcePackage, String resourceName, String target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resourcePackage, resourceName, target);
    }

    @Override
    public JavaArchive addAsManifestResource(Package resourcePackage, String resourceName, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsManifestResource(resourcePackage, resourceName, target);
    }

    @Override
    public JavaArchive addAsServiceProvider(Class<?> serviceInterface, Class<?>... serviceImpls) throws IllegalArgumentException {
        return delegate.addAsServiceProvider(serviceInterface, serviceImpls);
    }

    @Override
    public JavaArchive addManifest() throws IllegalArgumentException {
        return delegate.addManifest();
    }

    @Override
    public JavaArchive addClass(Class<?> clazz) throws IllegalArgumentException {
        return delegate.addClass(clazz);
    }

    @Override
    public JavaArchive addClass(String fullyQualifiedClassName) throws IllegalArgumentException {
        return delegate.addClass(fullyQualifiedClassName);
    }

    @Override
    public JavaArchive addClass(String fullyQualifiedClassName, ClassLoader cl) throws IllegalArgumentException {
        return delegate.addClass(fullyQualifiedClassName, cl);
    }

    @Override
    public JavaArchive addClasses(Class<?>... classes) throws IllegalArgumentException {
        return delegate.addClasses(classes);
    }

    @Override
    public JavaArchive addPackage(Package pack) throws IllegalArgumentException {
        return delegate.addPackage(pack);
    }

    @Override
    public JavaArchive addDefaultPackage() {
        return delegate.addDefaultPackage();
    }

    @Override
    public JavaArchive addPackages(boolean recursive, Package... packages) throws IllegalArgumentException {
        return delegate.addPackages(recursive, packages);
    }

    @Override
    public JavaArchive addPackages(boolean recursive, Filter<ArchivePath> filter, Package... packages) throws IllegalArgumentException {
        return delegate.addPackages(recursive, filter, packages);
    }

    @Override
    public JavaArchive addPackage(String pack) throws IllegalArgumentException {
        return delegate.addPackage(pack);
    }

    @Override
    public JavaArchive addPackages(boolean recursive, String... packages) throws IllegalArgumentException {
        return delegate.addPackages(recursive, packages);
    }

    @Override
    public JavaArchive addPackages(boolean recursive, Filter<ArchivePath> filter, String... packages) throws IllegalArgumentException {
        return delegate.addPackages(recursive, filter, packages);
    }

    @Override
    public JavaArchive addAsResource(String resourceName) throws IllegalArgumentException {
        return delegate.addAsResource(resourceName);
    }

    @Override
    public JavaArchive addAsResource(File resource) throws IllegalArgumentException {
        return delegate.addAsResource(resource);
    }

    @Override
    public JavaArchive addAsResource(String resourceName, String target) throws IllegalArgumentException {
        return delegate.addAsResource(resourceName, target);
    }

    @Override
    public JavaArchive addAsResource(File resource, String target) throws IllegalArgumentException {
        return delegate.addAsResource(resource, target);
    }

    @Override
    public JavaArchive addAsResource(URL resource, String target) throws IllegalArgumentException {
        return delegate.addAsResource(resource, target);
    }

    @Override
    public JavaArchive addAsResource(Asset resource, String target) throws IllegalArgumentException {
        return delegate.addAsResource(resource, target);
    }

    @Override
    public JavaArchive addAsResource(String resourceName, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsResource(resourceName, target);
    }

    @Override
    public JavaArchive addAsResource(String resourceName, ArchivePath target, ClassLoader classLoader) throws IllegalArgumentException {
        return delegate.addAsResource(resourceName, target, classLoader);
    }

    @Override
    public JavaArchive addAsResource(File resource, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsResource(resource, target);
    }

    @Override
    public JavaArchive addAsResource(URL resource, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsResource(resource, target);
    }

    @Override
    public JavaArchive addAsResource(Asset resource, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsResource(resource, target);
    }

    @Override
    public JavaArchive addAsResources(Package resourcePackage, String... resourceNames) throws IllegalArgumentException {
        return delegate.addAsResources(resourcePackage, resourceNames);
    }

    @Override
    public JavaArchive addAsResource(Package resourcePackage, String resourceName) throws IllegalArgumentException {
        return delegate.addAsResource(resourcePackage, resourceName);
    }

    @Override
    public JavaArchive addAsResource(Package resourcePackage, String resourceName, String target) throws IllegalArgumentException {
        return delegate.addAsResource(resourcePackage, resourceName, target);
    }

    @Override
    public JavaArchive addAsResource(Package resourcePackage, String resourceName, ArchivePath target) throws IllegalArgumentException {
        return delegate.addAsResource(resourcePackage, resourceName, target);
    }
}
