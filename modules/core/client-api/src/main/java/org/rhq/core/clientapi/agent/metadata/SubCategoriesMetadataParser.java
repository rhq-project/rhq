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
package org.rhq.core.clientapi.agent.metadata;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.descriptor.plugin.SubCategoryDescriptor;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.StringUtils;

/**
 * Parser responsible for translating the subcategories section of the rhq-plugin.xml descriptor into domain objects.
 * Also has methods which deal with translating the subcategory="blah" attribute on Platform/Server/Service ojbects into
 * an actual ResourceSubCategory object
 *
 * @author Charles Crouch
 */
public class SubCategoriesMetadataParser {
    /**
     * Parses the contents of the SubCategoryDescriptor and populates an instance of the domain model representation,
     * ResourceSubCategory
     *
     * @param  descriptor non-null SubCategoryDescriptor.
     *
     * @return domain model object populated with the descriptor's values.
     */
    public static ResourceSubCategory getSubCategory(SubCategoryDescriptor descriptor, ResourceType resourceType) {
        ResourceSubCategory subCat = new ResourceSubCategory();

        subCat.setName(descriptor.getName());
        if (descriptor.getDisplayName() != null) {
            subCat.setDisplayName(descriptor.getDisplayName());
        } else {
            subCat.setDisplayName(StringUtils.deCamelCase(descriptor.getName()));
        }

        subCat.setDescription(descriptor.getDescription());

        List<SubCategoryDescriptor> childDescriptors = descriptor.getSubcategory();
        if (childDescriptors != null) {
            for (SubCategoryDescriptor childDescriptor : childDescriptors) {
                subCat.addChildSubCategory(getSubCategory(childDescriptor, resourceType));
            }
        }

        return subCat;
    }

    /**
     * Given a resourceType this method looks on the parent resourcetypes/grandparent resourcetypes... of the specified
     * resourceType to see if a child ResourceSubCategory with the specified name has been defined on one of them.
     * If the ResourceSubCategory cannot be found, null will be returned
     *
     * @param  resourceType
     * @param  subCategoryName
     *
     * @return
     */
    @Nullable
    public static ResourceSubCategory findSubCategoryOnResourceTypeAncestor(@NotNull
    ResourceType resourceType, @Nullable
    String subCategoryName) {
        ResourceSubCategory selectedSubCategory = null;

        if (subCategoryName != null) {
            Set<ResourceType> parents = resourceType.getParentResourceTypes();
            for (ResourceType parent : parents) {
                List<ResourceSubCategory> ownedSubCategories = parent.getChildSubCategories();
                if (ownedSubCategories != null) {
                    for (ResourceSubCategory ownedSubCategory : ownedSubCategories) {
                        // need to check whether this subCat or any of its children match
                        selectedSubCategory = findSubCategoryOnSubCategoryDescendant(ownedSubCategory, subCategoryName);
                        if (selectedSubCategory != null) {
                            // found the category in the parent or one of its children
                            return selectedSubCategory;
                        }
                    }
                }
                // Recurse...
                selectedSubCategory = findSubCategoryOnResourceTypeAncestor(parent, subCategoryName);
                if (selectedSubCategory != null) {
                    break; // found the category in one of the ancestors of the parent
                }
            }
        }

        return selectedSubCategory;
    }

    /**
     * Checks whether a ResourceSubCategory with the specified name can be found in the tree of ResourceSubCategories
     * who's top level parent is specified by parentSubCategory.
     */
    @Nullable
    private static ResourceSubCategory findSubCategoryOnSubCategoryDescendant(@NotNull
    ResourceSubCategory parentSubCategory, @NotNull
    String subCategoryName) {
        ResourceSubCategory selectedSubCategory = null;
        if (parentSubCategory.getName().equals(subCategoryName)) {
            selectedSubCategory = parentSubCategory;
            return selectedSubCategory;
        } else // parent doesn't match, check children
        {
            for (ResourceSubCategory childSubCategory : parentSubCategory.getChildSubCategories()) {
                selectedSubCategory = findSubCategoryOnSubCategoryDescendant(childSubCategory, subCategoryName);
                if (selectedSubCategory != null) {
                    break; // found the category in one of the children of the parent
                }
            }
        }

        return selectedSubCategory;
    }
}