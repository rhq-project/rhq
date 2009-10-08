 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.common.composite;

public class IntegerOptionItem extends OptionItem<Integer> {
    private static final long serialVersionUID = 1L;

    /*
     * on the surface level, this must appear like a rather unnecessary extension, and it is.  unfortunately, JPA
     * constructor queries can not parse "new OptionItem<Integer>", so we needed to create this so we can say "new
     * IntegerOptionItem" in the query.
     */
    public IntegerOptionItem(Integer id, String displayName) {
        super(id, displayName);
    }
}