/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.search.translation.jpql;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.enterprise.server.search.translation.SearchTranslator;

/**
 * A construct used by {@link SearchTranslator}s which helps to classify the type of condition that should be generated
 * as part of the larger JPQL statement generated during translation.  As each {@link SearchTranslator} is passed the
 * various bits that were parsed out of the user's search expression, it has the responsibility to return a valid JPQL
 * fragment 
 * 
 * @author Joseph Marques
 * @see Type
 */
public class SearchFragment {

    /**
     * This indicates the representative form of the JPQL fragment.
     */
    public enum Type {
        /**
         * A full where-clause fragment that can be included in a larger JPQL statement.  This should be used in cases
         * where the target can be found using (arbitrarily long) path expressions (by traversing over {@link OneToOne}
         * or {@link ManyToOne} mappings, e.g. "alertDefinition.resource.resourceType.category = 'PLATFORM'", which
         * might be useful to support filtering resources that have any alerts on platforms.  Granted, the path 
         * expression is rarely ever that long, but it shows what's possible for this type's corresponding JPQL fragment.
         */
        WHERE_CLAUSE,
        /**
         * More complicated filters can sometimes not restrict data by using path expressions because they need to
         * navigate across {@link OneToMany} or {@link ManyToMany} mappings.  In this case, since the fragment is
         * intended to be used in a larger JPQL statement, a sub-query must be employed which will allow the use of 
         * joins (to navigate those more complex entity mappings).  These sub-queries must always return a list of
         * primary keys for the type of entity represented by the {@link SearchSubsystem} for the corresponding
         * {@link SearchTranslator} in question.
         */
        PRIMARY_KEY_SUBQUERY;
    }

    private final Type type;
    private final String jpqlFragment;

    public SearchFragment(Type type, String jpqlFragment) {
        this.type = type;
        this.jpqlFragment = jpqlFragment;
    }

    public Type getType() {
        return type;
    }

    public String getJPQLFragment() {
        return jpqlFragment;
    }
}
