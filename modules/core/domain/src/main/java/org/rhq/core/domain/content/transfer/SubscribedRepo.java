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

package org.rhq.core.domain.content.transfer;

import java.io.Serializable;

public class SubscribedRepo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;

    public SubscribedRepo() {
    }

    public SubscribedRepo(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @return the repo id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the repo id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the repo name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the repo name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
