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
package org.rhq.enterprise.server.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Will create a tree of resources based off of a short-hand, string representation of the tree Pre-condition: resource
 * names are globally unique (because the parser is too simple to handle uniqueness wrt your parent resource) Notes: all
 * whitespace is ignored For example: A=1,2; 1=a,b; a=i,ii; b=iii,iv; B=3 | | \|/ v +-- A +-- 1 | +-- a | | +-- i | |
 * +-- ii | | | +-- b | +-- iii | +-- iv +-- 2 +-- B +-- 3 For now, all resources have the exact same resourceType, but
 * it would be fairly easy to create rules where different ranges of characters (e.g., [A-Z], [a-z], etc) could be used
 * to represent different resource types or, at the very least, different categories. A more flexible solution could be
 * to enable a separate specification mechanism for resource types and categories. For example, we might use a compound,
 * paranthetical annotation for category-type specification. So if we assume that, p = platform s = server v = service
 * Then our annotated resource would look like A(p1) This would set A's resource type to the newly created resource type
 * with the name of "fake platform 1", and set A's resource category to ResoueceCategory.PLATFORM Thus, the new
 * short-hand would look like: A(p1)=1,2; 1(s1)=a,b; a(v1)=i,ii; b(v2)=iii,iv; B(p2)=3; 2(s2); 3(s3) However, as you can
 * see, this requires a slightly more verbose specification because the type information is parenthetically attached to
 * the LHS of the relationship, requiring leaf nodes in the resource hierarchy to be specified solely to attach this
 * information to it.
 */
public class ResourceTreeHelper {
    /*
     * This will create the resource tree, but won't persist it
     */
    public static List<Resource> createTree(String flatStructure) {
        return createTree(null, flatStructure);
    }

    /*
     * This will create the resource tree, and related resource types
     */
    public static List<Resource> createTree(EntityManager entityManager, String flatStructure) {
        // remove any and all whitespace from the string
        flatStructure = flatStructure.replaceAll("\\s", "");

        // create the fakeType which ALL resources will be attached too
        ResourceType fakeType = new ResourceType("fakeType", "fakePlugin", ResourceCategory.PLATFORM, null);
        persist(entityManager, fakeType);

        // create the map that will hold all of the resources - root, leaf, or inner node
        Map<String, Resource> resources = new HashMap<String, Resource>();

        // relationships in the form of X=y,z are semi-colon delimited
        String[] relationships = flatStructure.split(";");
        for (int i = 0; i < relationships.length; i++) {
            String relationship = relationships[i];
            int splitIndex = relationship.indexOf("=");
            String parent = relationship.substring(0, splitIndex);
            String[] children = relationship.substring(splitIndex + 1).split(",");

            Resource parentResource = null;

            // only add the parent if it doesn't already exist
            if (resources.containsKey(parent)) {
                parentResource = resources.get(parent);
            } else {
                parentResource = new Resource(parent, parent, fakeType);
                resources.put(parent, parentResource);
            }

            for (String child : children) {
                Resource childResource = null;

                // only add the child if it doesn't already exist
                // this happens when children were specified before their parents
                if (resources.containsKey(child)) {
                    childResource = resources.get(child);
                } else {
                    childResource = new Resource(child, child, fakeType);
                    resources.put(child, childResource);
                }

                childResource.setParentResource(parentResource);
                parentResource.addChildResource(childResource);
            }
        }

        // this object returns only root resources, which can be traversed to find the children
        List<Resource> roots = new ArrayList<Resource>();
        for (Resource potentialRoot : resources.values()) {
            persist(entityManager, potentialRoot);

            // roots are those with no parent resource
            if (potentialRoot.getParentResource() == null) {
                roots.add(potentialRoot);
            }
        }

        return roots;
    }

    public static Resource findNode(List<Resource> roots, String name) {
        Resource node = null;
        for (Resource root : roots) {
            node = findNodeHelper(root, name, "");
            if (node != null) {
                break;
            }
        }

        return node;
    }

    public static Resource findNode(Resource rootResource, String name) {
        return findNodeHelper(rootResource, name, "");
    }

    private static Resource findNodeHelper(Resource resource, String name, String prefix) {
        if (name.equals(prefix + resource.getName())) {
            return resource;
        }

        Resource result = null;
        for (Resource child : resource.getChildResources()) {
            result = findNodeHelper(child, name, resource.getName() + ".");
            if (result != null) {
                break;
            }
        }

        return result;
    }

    public static List<Resource> getSubtree(Resource resource) {
        List<Resource> subtree = new ArrayList<Resource>();

        List<Resource> toBeSearched = new LinkedList<Resource>();
        toBeSearched.add(resource);
        while (toBeSearched.size() > 0) {
            Resource next = toBeSearched.get(0);
            subtree.add(next);
            toBeSearched.addAll(next.getChildResources());
        }

        return subtree;
    }

    public static List<Resource> findSubtree(Resource resource, String name) {
        return getSubtree(findNode(resource, name));
    }

    private static void persist(EntityManager entityManager, Object object) {
        if (entityManager != null) {
            entityManager.persist(object);
        }
    }

    public static void printTree(Resource resource) {
        printTreeHelper(resource, 0, "");
    }

    private static void printTreeHelper(Resource resource, int level, String namePrefix) {
        for (int i = 0; i < level; i++) {
            System.out.print('\t');
        }

        System.out.println("<resource name=\"" + namePrefix + resource.getName() + "\" />");

        for (Resource child : resource.getChildResources()) {
            printTreeHelper(child, level + 1, namePrefix + resource.getName() + ".");
        }
    }

    public static void main(String[] args) {
        List<Resource> resourceRoots = createTree("A=1,2; 1=a,b; a=i,ii; b=iii,iv; B=3");
        for (Resource root : resourceRoots) {
            printTree(root);
        }
    }
}