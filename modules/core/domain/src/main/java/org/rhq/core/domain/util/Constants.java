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

package org.rhq.core.domain.util;

/**
 * Any constants useful to the domain library, Hibernate, etc.
 * 
 * @author Jay Shaughnessy
 */
public class Constants {

    // The allocation size on SequenceGenerator annotations must match the Increment set on the
    // underlying Sequence.  We currently use Increments of 1 on all of our sequences and therefore
    // the allocation size must also be set to 1. Note that the JPA default for the annotation is 50,
    // so it must be set explicitly on our annotations.  An increment of 1 forces Hibernate to
    // get the ID on each insert, which is not super-efficient (even though the DB may be caching 
    // several ids).  In the future we may want to alter certain high volume sequences to use a larger
    // increment, at which point the corresponding annotation would be updated with the equivalent
    // value, and not this constant (or we just create ALLOCATION_SIZE_HIGH to be consistent).   
    public static final int ALLOCATION_SIZE = 1;

}
