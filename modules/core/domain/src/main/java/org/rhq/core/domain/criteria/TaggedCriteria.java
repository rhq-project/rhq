/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.criteria;

import org.rhq.core.domain.tagging.Tag;

/**
 * @author Greg Hinkle
 */
@SuppressWarnings("unused")
public abstract class TaggedCriteria extends Criteria {

    private static final long serialVersionUID = 1L;

    private Tag filterTag;

    private boolean fetchTags;

    protected TaggedCriteria() {

        String entityName = getPersistentClass().getName();
        entityName = entityName.substring(entityName.lastIndexOf(".") + 1);

        filterOverrides.put("tag", "" //
            + "id IN ( SELECT taggedEntity.id \n" //
            + "          FROM " + entityName + " taggedEntity \n" //
            + "          JOIN taggedEntity.tags tag \n " //
            + "         WHERE (tag.namespace LIKE :tagNamespace OR :tagNamespace IS NULL ) \n" // first '?' will get ordinal 1
            + "           AND (tag.semantic LIKE :tagSemantic OR :tagSemantic IS NULL ) \n" // second '?' will get ordinal 2
            + "           AND (tag.name LIKE :tagName OR :tagName IS NULL ) )"); // third '?' will get ordinal 3
    }

    public void addFilterTagNamespace(String filterTagNamespace) {
        if (filterTagNamespace == null) {
            return; // do nothing if the filter is empty
        }
        if (this.filterTag == null) {
            this.filterTag = new Tag();
        }
        this.filterTag.setNamespace(filterTagNamespace);
    }

    public void addFilterTagSemantic(String filterTagSemantic) {
        if (filterTagSemantic == null) {
            return; // do nothing if the filter is empty
        }
        if (this.filterTag == null) {
            this.filterTag = new Tag();
        }
        this.filterTag.setSemantic(filterTagSemantic);
    }

    public void addFilterTagName(String filterTagName) {
        if (filterTagName == null) {
            return; // do nothing if the filter is empty
        }
        if (this.filterTag == null) {
            this.filterTag = new Tag();
        }
        this.filterTag.setName(filterTagName);
    }

    public void addFilterTag(Tag tag) {
        this.filterTag = tag;
    }

    public boolean isTagFiltered() {
        return this.filterTag != null;
    }

    public void fetchTags(boolean fetchTags) {
        this.fetchTags = fetchTags;
    }

}
