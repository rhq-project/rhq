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
package org.rhq.enterprise.server.search.assist;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.search.SearchSubsystem;

/**
 * @author Joseph Marques
 */
public class SearchAssistantFactory {
    private SearchAssistantFactory() {
        // force use of static methods only
    }

    public static SearchAssistant getAssistant(Subject subject, SearchSubsystem searchContext) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant(subject, null);
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchAssistant(subject, null);
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchContext + "]");
        }
    }

    public static SearchAssistant getTabAwareAssistant(Subject subject, SearchSubsystem searchContext, String tab) {
        if (searchContext == SearchSubsystem.RESOURCE) {
            return new ResourceSearchAssistant(subject, tab);
        } else if (searchContext == SearchSubsystem.GROUP) {
            return new GroupSearchAssistant(subject, tab);
        } else {
            throw new IllegalArgumentException("No SearchAssistant found for SearchSubsystem[" + searchContext + "]");
        }
    }
}
