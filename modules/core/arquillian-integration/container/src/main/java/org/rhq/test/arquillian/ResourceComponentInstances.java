/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.arquillian;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * This is a similar to the {@link DiscoveredResources} annotation but
 * provides the actual instances of the resource components of the discovered
 * resources.
 * <p>
 * The type of the field carrying this annotation is assumed to a be a {@link Set}
 * of the actual resource component type corresponding to the resource type
 * specified by this annotation.
 * <p>
 * I.e. if the resource type 'RT' in plugin 'P', defines the resource component to
 * be <code>com.example.FooResourceComponent</code>, the type of the field with this annotation 
 * is assumed to be <code>Set&lt;com.example.FooResourceComponent&gt;</code> 
 * 
 * @author Lukas Krejci
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ResourceComponentInstances {

    String plugin();
    
    String resourceType();
}
