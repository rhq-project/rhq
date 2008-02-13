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
package org.rhq.enterprise.gui.legacy.beans;

import java.util.List;
import org.apache.struts.util.LabelValueBean;

/**
 * Bean for a drop-down that controls another drop down. It is a LabelValueBean, but also a List of LabelValueBean
 * objects.
 */
public final class RelatedOptionBean extends LabelValueBean {
    private List relatedOptions;

    public RelatedOptionBean(Object label, Object value, List relatedOptions) {
        super(String.valueOf(label), String.valueOf(value));
        this.relatedOptions = relatedOptions;
    }

    public List getRelatedOptions() {
        return relatedOptions;
    }

    public void setRelatedOptions(List relatedOptions) {
        this.relatedOptions = relatedOptions;
    }
}

// EOF
