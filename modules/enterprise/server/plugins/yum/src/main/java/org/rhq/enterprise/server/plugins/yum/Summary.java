/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugins.yum;

import java.util.ArrayList;
import java.util.List;

class Summary {
    int added = 0;
    int updated = 0;
    int deleted = 0;
    long started = 0;
    long ended = 0;
    Object context;
    List<String> errors = new ArrayList<String>();

    Summary(Object context) {
        this.context = context;
    }

    void markStarted() {
        started = System.currentTimeMillis();
    }

    void markEnded() {
        ended = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        boolean failed = errors.size() > 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Synchronization with yum repo - ");
        sb.append((failed ? "FAILED" : "SUCCEEDED"));
        sb.append("\nLocation: " + context);
        sb.append("\nDuration: " + (ended - started) + " (ms)");
        sb.append("\nPackages:");
        sb.append("\n\tAdded: " + added);
        sb.append("\n\tUpdated: " + updated);
        sb.append("\n\tDeleted: " + deleted);
        sb.append("\n\tErrors: " + errors.size());
        for (String s : errors) {
            sb.append("\n\t\t");
            sb.append(s);
        }

        return sb.toString();
    }
}