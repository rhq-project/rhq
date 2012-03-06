/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.shrinkwrap;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchiveFormat;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Assignable;
import org.jboss.shrinkwrap.api.Configuration;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.NamedAsset;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.formatter.Formatter;
import org.jboss.shrinkwrap.impl.base.ArchiveBase;
import org.jboss.shrinkwrap.impl.base.ConfigurableArchiveImpl;
import org.jboss.shrinkwrap.spi.Configurable;

/**
 * This class is a hack. It doesn't need to inherit from {@link ArchiveBase} to achieve the filtering but at the same
 * time it needs to act as an implementation of an Archive. Classes in the ShrinkWrap SPI assume that 
 * archive impls inherit from ArchiveBase so we need to make them happy (at least the {@link ConfigurableArchiveImpl} does).
 *
 * @author Lukas Krejci
 */
public class FilteredViewImpl extends ArchiveBase<FilteredView> implements FilteredView {

    private Filter<ArchivePath> filter;
    private final Archive<?> archive;
    
    private class FilteringNode implements Node {
        private Node orig;
        
        public FilteringNode(Node orig) {
            this.orig = orig;
        }
        
        @Override
        public Asset getAsset() {
            return orig.getAsset();
        }

        @Override
        public Set<Node> getChildren() {
            if (filter == null) {
                return wrap(orig.getChildren());
            } else {
                Set<Node> children = orig.getChildren();
                Set<Node> ret = new LinkedHashSet<Node>();
                for(Node child : children) {
                    if (conforms(child.getPath())) {
                        ret.add(new FilteringNode(child));
                    }
                }
                
                return Collections.unmodifiableSet(ret);
            }
        }

        @Override
        public ArchivePath getPath() {
            return orig.getPath();
        }
        
        public Set<Node> wrap(Set<Node> nodes) {
            Set<Node> ret = new LinkedHashSet<Node>(nodes.size());
            for(Node n : nodes) {
                ret.add(new FilteringNode(n));
            }
            
            return ret;
        }        
    }
    
    /**
     * @param archive
     */
    public FilteredViewImpl(Archive<?> archive) {
        super(archive.getName(), archive.as(Configurable.class).getConfiguration());
        this.archive = archive;
    }

    @Override
    public final <T extends Assignable> T as(Class<T> type) {
        return getConfiguration().getExtensionLoader().load(type, this);
    }
    
    protected final Archive<?> getArchive() {
        return archive;
    }
    
    @Override
    public Configuration getConfiguration() {
        return archive.as(Configurable.class).getConfiguration();
    }
    
    @Override
    public FilteredView filterContents(Filter<ArchivePath> filter) {
        this.filter = filter;
        
        return this;
    }
    
    @Override
    protected Class<FilteredView> getActualClass() {
        return FilteredView.class;
    }
    
    @Override
    public FilteredView add(Asset asset, ArchivePath target) throws IllegalArgumentException {
        getArchive().add(asset, target);
        return this;
    }

    @Override
    public FilteredView add(Asset asset, ArchivePath target, String name) throws IllegalArgumentException {
        getArchive().add(asset, target, name);
        return this;
    }

    @Override
    public FilteredView add(Asset asset, String target, String name) throws IllegalArgumentException {
        getArchive().add(asset, target, name);
        return this;
    }

    @Override
    public FilteredView add(NamedAsset namedAsset) throws IllegalArgumentException {
        getArchive().add(namedAsset);
        return this;
    }

    @Override
    public FilteredView add(Asset asset, String target) throws IllegalArgumentException {
        getArchive().add(asset, target);
        return this;
    }

    @Override
    public FilteredView addAsDirectory(String path) throws IllegalArgumentException {
        getArchive().addAsDirectory(path);
        return this;
    }

    @Override
    public FilteredView addAsDirectories(String... paths) throws IllegalArgumentException {
        getArchive().addAsDirectories(paths);
        return this;
    }

    @Override
    public FilteredView addAsDirectory(ArchivePath path) throws IllegalArgumentException {
        getArchive().addAsDirectory(path);
        return this;
    }

    @Override
    public FilteredView addAsDirectories(ArchivePath... paths) throws IllegalArgumentException {
        getArchive().addAsDirectories(paths);
        return this;
    }

    @Override
    public Node get(ArchivePath path) throws IllegalArgumentException {  
        if (conforms(path)) {
            return new FilteringNode(getArchive().get(path));
        } else {
            return null;
        }
    }

    @Override
    public Node get(String path) throws IllegalArgumentException {
        if (conforms(path)) {
            return new FilteringNode(getArchive().get(path));
        } else {
            return null;
        }
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, String path) {
        if (conforms(path)) {
            return getArchive().getAsType(type, path);
        } else {
            return null;
        }
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, ArchivePath path) {
        if (conforms(path)) {
            return getArchive().getAsType(type, path);
        } else {
            return null;
        }
    }

    @Override
    public <X extends Archive<X>> Collection<X> getAsType(Class<X> type, Filter<ArchivePath> filter) {        
        Filter<ArchivePath> f = filter;
        
        if (this.filter != null) {
            f = new AndFilter<ArchivePath>(filter, this.filter);
        }
        
        return getArchive().getAsType(type, f);
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, String path, ArchiveFormat archiveFormat) {
        if (conforms(path)) {
            return getArchive().getAsType(type, path, archiveFormat);
        } else {
            return null;
        }
    }

    @Override
    public <X extends Archive<X>> X getAsType(Class<X> type, ArchivePath path, ArchiveFormat archiveFormat) {
        if (conforms(path)) {
            return getArchive().getAsType(type, path, archiveFormat);
        } else {
            return null;
        }
    }

    @Override
    public <X extends Archive<X>> Collection<X> getAsType(Class<X> type, Filter<ArchivePath> filter,
        ArchiveFormat archiveFormat) {
        
        Filter<ArchivePath> f = filter;
        
        if (this.filter != null) {
            f = new AndFilter<ArchivePath>(filter, this.filter);
        }
        
        return getArchive().getAsType(type, f, archiveFormat);
    }

    @Override
    public boolean contains(ArchivePath path) throws IllegalArgumentException {
        if (conforms(path)) {
            return getArchive().contains(path);
        } else {
            return false;
        }
    }

    @Override
    public boolean contains(String path) throws IllegalArgumentException {
        if (conforms(path)) {
            return getArchive().contains(path);
        } else {
            return false;
        }
    }

    @Override
    public Node delete(ArchivePath path) throws IllegalArgumentException {
        return getArchive().delete(path);
    }

    @Override
    public Node delete(String archivePath) throws IllegalArgumentException {
        return getArchive().delete(archivePath);
    }

    @Override
    public Map<ArchivePath, Node> getContent() {
        if (filter == null) {
            return getArchive().getContent();
        } else {
            return getArchive().getContent(filter);
        }
    }

    @Override
    public Map<ArchivePath, Node> getContent(Filter<ArchivePath> filter) {
        Filter<ArchivePath> f = filter;
        if (this.filter != null) {
            f = new AndFilter<ArchivePath>(filter, this.filter);
        }
        
        return getArchive().getContent(f);
    }

    @Override
    public FilteredView add(Archive<?> archive, ArchivePath path, Class<? extends StreamExporter> exporter)
        throws IllegalArgumentException {
        getArchive().add(archive, path, exporter);
        return this;
    }

    @Override
    public FilteredView add(Archive<?> archive, String path, Class<? extends StreamExporter> exporter)
        throws IllegalArgumentException {
        getArchive().add(archive, path, exporter);
        return this;
    }

    @Override
    public FilteredView merge(Archive<?> source) throws IllegalArgumentException {
        getArchive().merge(source);
        return this;
    }

    @Override
    public FilteredView merge(Archive<?> source, Filter<ArchivePath> filter) throws IllegalArgumentException {
        getArchive().merge(source, filter);
        return this;
    }

    @Override
    public FilteredView merge(Archive<?> source, ArchivePath path) throws IllegalArgumentException {
        getArchive().merge(source, path);
        return this;
    }

    @Override
    public FilteredView merge(Archive<?> source, String path) throws IllegalArgumentException {
        getArchive().merge(source, path);
        return this;
    }

    @Override
    public FilteredView merge(Archive<?> source, ArchivePath path, Filter<ArchivePath> filter) throws IllegalArgumentException {
        getArchive().merge(source, path, filter);
        return this;
    }

    @Override
    public FilteredView merge(Archive<?> source, String path, Filter<ArchivePath> filter) throws IllegalArgumentException {
        getArchive().merge(source, path, filter);
        return this;
    }

    @Override
    public String toString() {
        return getArchive().toString();
    }
    
    @Override
    public String toString(boolean verbose) {
        return getArchive().toString(verbose);
    }

    @Override
    public String toString(Formatter formatter) throws IllegalArgumentException {
        return getArchive().toString(formatter);
    }

    @Override
    public void writeTo(OutputStream outputStream, Formatter formatter) throws IllegalArgumentException {
        getArchive().writeTo(outputStream, formatter);
    }
    
    private boolean conforms(ArchivePath path) {
        if (filter == null) {
            return true;
        }
        
        return filter.include(path);
    }
    
    private boolean conforms(String path) {
        if (filter == null) {
            return true;
        }
        
        return filter.include(ArchivePaths.create(path));
    }
}
