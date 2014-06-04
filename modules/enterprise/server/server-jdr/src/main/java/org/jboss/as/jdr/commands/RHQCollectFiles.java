/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.jboss.as.jdr.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.jdr.commands.JdrCommand;
import org.jboss.as.jdr.util.Sanitizer;
import org.jboss.as.jdr.util.Utils;
import org.jboss.as.jdr.vfs.Filters;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

public class RHQCollectFiles extends JdrCommand {
    private static final String RHQ_PREFIX = "rhq" + File.separator;
    private VirtualFileFilter filter = Filters.TRUE;
    private Filters.BlacklistFilter blacklistFilter = Filters.wildcardBlackList();
    private LinkedList<Sanitizer> sanitizers = new LinkedList<Sanitizer>();
    private Comparator<VirtualFile> sorter = new Comparator<VirtualFile>() {
        @Override
        public int compare(VirtualFile resource, VirtualFile resource1) {
            return Long.signum(resource.getLastModified() - resource1.getLastModified());
        }
    };

    // -1 means no limit
    private long limit = -1;

    public RHQCollectFiles(VirtualFileFilter filter) {
        this.filter = filter;
    }

    public RHQCollectFiles(String pattern) {
        this.filter = Filters.wildcard(pattern);
    }

    public RHQCollectFiles sanitizer(Sanitizer ... sanitizers) {
        for (Sanitizer s : sanitizers) {
            this.sanitizers.add(s);
        }
        return this;
    }

    public RHQCollectFiles sorter(Comparator<VirtualFile> sorter){
        this.sorter = sorter;
        return this;
    }

    public RHQCollectFiles limit(final long limit){
        this.limit = limit;
        return this;
    }

    public RHQCollectFiles omit(String pattern) {
        blacklistFilter.add(pattern);
        return this;
    }

    @Override
    public void execute() throws Exception {
        String basedir = System.getProperty("rhq.server.home");
        if (basedir==null) {
            throw new IllegalStateException("Did not find 'rhq.server.home'");
        }

        File dir = new File(basedir);
        if (!dir.exists()) {
            throw new IllegalArgumentException("Directory " + dir.getAbsolutePath() + " does not exist");
        }
        if (!dir.canExecute() || !dir.canRead()) {
            throw new IllegalArgumentException("Directory " + dir.getAbsolutePath() + " can not be accessed");
        }

        VirtualFile root = VFS.getChild(dir.toURI());
        List<VirtualFile> matches = root.getChildrenRecursively(Filters.and(this.filter, this.blacklistFilter));

        if(sorter != null){
            Collections.sort(matches, sorter);
        }

        // limit how much data we collect
        Limiter limiter = new Limiter(limit);
        Iterator<VirtualFile> iter = matches.iterator();

        while(iter.hasNext() && !limiter.isDone()) {

            VirtualFile f = iter.next();
            InputStream stream = limiter.limit(f);

            for (Sanitizer sanitizer : this.sanitizers) {
                if(sanitizer.accepts(f)){
                    stream = sanitizer.sanitize(stream);
                }
            }
            this.env.getZip().add(stream, RHQ_PREFIX + f.getName());
            Utils.safelyClose(stream);
        }
    }

    /**
     * A Limiter is constructed with a number, and it can be repeatedly given VirtualFiles for which it will return an
     * InputStream that possibly is adjusted so that the number of bytes the stream can provide, when added to what the
     * Limiter already has seen, won't be more than the limit.
     *
     * If the VirtualFiles's size minus the amount already seen by the Limiter is smaller than the limit, the
     * VirtualFiles's InputStream is simply returned and its size added to the number of bytes the Limiter has seen.
     * Otherwise, the VirtualFiles's InputStream is skipped ahead so that the total number of bytes it will provide
     * before exhaustion will make the total amount seen by the Limiter equal to its limit.
     */
    private static class Limiter {

        private long amountRead = 0;
        private long limit = -1;
        private boolean done = false;

        public Limiter(long limit){
            this.limit = limit;
        }

        public boolean isDone(){
            return done;
        }

        /**
         * @return
         * @throws IOException
         */
        public InputStream limit(VirtualFile resource) throws IOException {

            InputStream is = resource.openStream();
            long resourceSize = resource.getSize();

            // if we're limiting and know we're not going to consume the whole file, we skip
            // ahead so that we get the tail of the file instead of the beginning of it, and we
            // toggle the done switch.
            if(limit != -1){
                long leftToRead = limit - amountRead;
                if(leftToRead < resourceSize){
                    Utils.skip(is, resourceSize - leftToRead);
                    done = true;
                } else {
                    amountRead += resourceSize;
                }
            }
            return is;

        }

    }
}
