package org.rhq.coregui.client.dashboard.portlets.resource;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.coregui.client.components.lookup.ResourceLookupComboBoxItem;
import org.rhq.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.detail.summary.TimelineView;

/**
 * @author Jay Shaughnessy
 */
public class ResourceTimelinePortlet extends TimelineView implements CustomSettingsPortlet {
    public static final String CFG_RESOURCE_ID = "resourceId";
    public static final String CFG_TITLE = "title";

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceTimeline";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_tabs_common_timeline();

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;

    public ResourceTimelinePortlet(int resourceId) {
        super(resourceId);
    }

    public PortletWindow getPortletWindow() {
        return portletWindow;
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow("Help text for Timeline (TODO: I18N)");
    }

    @Override
    protected void onDraw() {
        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        PropertySimple simple = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID);
        if (simple == null || simple.getIntegerValue() == null) {
            removeMembers(getMembers());
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        } else {
            simple = storedPortlet.getConfiguration().getSimple(CFG_TITLE);
            portletWindow.setTitle(null == simple ? NAME : simple.getStringValue());
            super.onDraw();
        }
    }

    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        // the portletWindow does not change, so we can hold onto it locally
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            renderIFrame(Integer.valueOf(storedPortlet.getConfiguration().getSimpleValue(CFG_RESOURCE_ID)));
        }
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        final DynamicForm form = new DynamicForm();

        final ResourceLookupComboBoxItem resourceLookupComboBoxItem = new ResourceLookupComboBoxItem(CFG_RESOURCE_ID,
            MSG.common_title_resource());
        resourceLookupComboBoxItem.setWidth(300);

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            Integer integerValue = storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID).getIntegerValue();
            if (integerValue != null) {
                form.setValue(CFG_RESOURCE_ID, integerValue);
            }
        }

        form.setFields(resourceLookupComboBoxItem);

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_RESOURCE_ID, form.getValue(CFG_RESOURCE_ID)));

                String name = resourceLookupComboBoxItem.getDisplayValue();
                ListGridRecord r = resourceLookupComboBoxItem.getSelectedRecord();
                String ancestry = AncestryUtil.getAncestryValue(r, false);
                String title = NAME + ": " + name;
                title = ancestry.isEmpty() ? title : title + "  [" + ancestry + "]";

                storedPortlet.getConfiguration().put(new PropertySimple(CFG_TITLE, title));
                portletWindow.setTitle(title);

                // this will cause the graph to draw
                configure(portletWindow, storedPortlet);
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.Resource == context.getType()) {
                return new ResourceTimelinePortlet(context.getResourceId());
            }

            return new ResourceTimelinePortlet(-1);
        }
    }

}
