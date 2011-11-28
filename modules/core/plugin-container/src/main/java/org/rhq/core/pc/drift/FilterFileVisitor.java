/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.pc.drift;

import static org.rhq.core.util.file.FileUtil.generateRegex;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import org.rhq.core.domain.drift.Filter;
import org.rhq.core.util.file.FileVisitor;
import org.rhq.core.util.file.PathFilter;

/**
 * A file visitor that peforms filtering using specified filters. When a file matches a
 * filter this visitor delegates to another {@link FileVisitor} object. The filtering rules
 * are as follows:
 * <p/>
 * <ul>
 *     <li>When no filters are specified, are files are considered matches.</li>
 *     <li>
 *         When one or more includes filters is specified and no excludes filters are
 *         specified, only files matching at least one of the includes filter(s) are
 *         considered matches.
 *     </li>
 *     <li>
 *         When one or more excludes filters is specified and no includes filters are
 *         specified, only files that do not match any of the excludes filters are
 *         considered matches.
 *     </li>
 *     <li>
 *         When both includes and excludes filters are specified, only files that match at
 *         at least one of the includes and do not match any of the excludes are considered
 *         matches.
 *     </li>
 * </ul>
 * <br/><br/>
 * Note that the filtering is done on files and not on directories. This is a subtle yet
 * important distinction. Suppose we have the following filter pattern, *.war, that we apply
 * to the deploy directory of our JBoss application server. That filter will match things
 * like foo.war, foo-123.war, or more precisely, any file that ends with .war. The pattern
 * will not however match things like foo.war/ where foo.war is a directory (i.e., exploded
 * webapp) since matching is not done on directories. To match the contents of foo.war/
 * you could use something like *.war/**.
 * <br/><br/>
 * If a filter path denotes a directory and if no pattern is specified, then it is assumed
 * everything in the directory (including subdirectories) should be considered a match.
 */
public class FilterFileVisitor implements FileVisitor {

    private List<PathFilter> includes;

    private List<PathFilter> excludes;

    private FileVisitor visitor;

    private Pattern includesPattern;

    private Pattern excludesPattern;

    public FilterFileVisitor(File basedir, List<Filter> includes, List<Filter> excludes, FileVisitor visitor) {
        this.includes = convert(basedir, includes);
        this.excludes = convert(basedir, excludes);
        this.visitor = visitor;
        includesPattern = generateRegex(this.includes);
        excludesPattern = generateRegex(this.excludes);
    }

    private List<PathFilter> convert(File basedir, List<Filter> filters) {
        List<PathFilter> pathFilters = new ArrayList<PathFilter>(filters.size());
        for (Filter filter : filters) {
            pathFilters.add(normalize(basedir, filter));
        }
        return pathFilters;
    }

    /**
     * Besides converting the {@link Filter} into a {@link PathFilter}, this method does a
     * couple additional things. If the path is a relative, it is expanded into an absolute
     * path. If the path denotes a directory and if no pattern is specified, it is assumed
     * that everything under that directory including sub directories should be considered
     * matches.
     *
     * @param basedir The base directory from which drift detection is being done
     * @param filter The filter to convert and normalize
     * @return The converted and normalized filter
     */
    private PathFilter normalize(File basedir, Filter filter) {
        File path = new File(filter.getPath());
        File filterPath;
        String filterPattern;

        if (path.isAbsolute()) {
            filterPath = path;
        } else {
            filterPath = new File(basedir, filter.getPath());
        }

        if (filterPath.isDirectory() && isEmpty(filter.getPattern())) {
            filterPattern = "**/*";
        } else {
            filterPattern = filter.getPattern();
        }

        return new PathFilter(FilenameUtils.normalize(filterPath.getAbsolutePath()), filterPattern);
    }

    private boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    @Override
    public void visit(File file) {
        if (includes.isEmpty() && excludes.isEmpty()) {
            visitor.visit(file);
        } else if (!includes.isEmpty() && excludes.isEmpty()) {
            if (includesPattern.matcher(file.getAbsolutePath()).matches()) {
                visitor.visit(file);
            }
        } else if (includes.isEmpty() && !excludes.isEmpty()) {
            if (!excludesPattern.matcher(file.getAbsolutePath()).matches()) {
                visitor.visit(file);
            }
        } else {
            // else neither includes nor excludes is empty
            if (includesPattern.matcher(file.getAbsolutePath()).matches()
                && !excludesPattern.matcher(file.getAbsolutePath()).matches()) {
                visitor.visit(file);
            }
        }
    }
}
