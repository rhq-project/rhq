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
package org.rhq.enterprise.server.alert.engine.internal;

/**
 * @author Joseph Marques
 */

public class Tuple<L, R> {
    public L lefty;
    public R righty;

    public Tuple(L lefty, R righty) {
        this.lefty = lefty;
        this.righty = righty;
    }

    public L getLefty() {
        return this.lefty;
    }

    public R getRighty() {
        return this.righty;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((lefty == null) ? 0 : lefty.hashCode());
        result = (31 * result) + ((righty == null) ? 0 : righty.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof Tuple))) {
            return false;
        }

        final Tuple<L, R> other = (Tuple<L, R>) obj;

        if (lefty == null) {
            if (other.lefty != null) {
                return false;
            }
        } else if (!lefty.equals(other.lefty)) {
            return false;
        }

        if (righty == null) {
            if (other.righty != null) {
                return false;
            }
        } else if (!righty.equals(other.righty)) {
            return false;
        }

        return true;
    }
}