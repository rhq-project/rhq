/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.jpa;

import java.lang.annotation.Annotation;

import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Names the possible JPA dependency types. {@link OneToMany} and {@link ManyToOne} are considered
 * the same as they represent the opposite sides of a single relationship.
 *
 * @author Lukas Krejci
 */
public enum DependencyType {
    ONE_TO_ONE {
        public Class<? extends Annotation> annotationType() {
            return OneToOne.class;
        }
    },
    ONE_TO_MANY {
        public Class<? extends Annotation> annotationType() {
            return OneToMany.class;
        }
    },
    MANY_TO_MANY {
        public Class<? extends Annotation> annotationType() {
            return ManyToMany.class;
        }
    };

    public abstract Class<? extends Annotation> annotationType();
}