/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.inventory.common.detail.monitoring;

import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.HTMLPane;

import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * 
 * @deprecated  Should be replaced now with GWT graphs and not portal war JSF graphs.
 * @author Lukas Krejci
 */

public class IFrameWithMeasurementRangeEditorView extends EnhancedVLayout implements RefreshableView {

    UserPreferencesMeasurementRangeEditor editor;
    HTMLPane iframe;

    public IFrameWithMeasurementRangeEditorView(final String url) {
        super();

        iframe = new HTMLPane();
        iframe.setContentsURL(url);
        iframe.setContentsType(ContentsType.PAGE);
        iframe.setWidth100();

        addMember(iframe);

        EnhancedToolStrip footer = new EnhancedToolStrip();
        footer.setWidth100();
        addMember(footer);

        editor = new UserPreferencesMeasurementRangeEditor() {
            @Override
            public void setMetricRangeProperties(MetricRangePreferences prefs) {
                super.setMetricRangeProperties(prefs);
                iframe.setContentsURL(url);
            }
        };

        footer.addMember(editor);
    }

    public void refresh() {
        editor.refresh(null);
    }
}
