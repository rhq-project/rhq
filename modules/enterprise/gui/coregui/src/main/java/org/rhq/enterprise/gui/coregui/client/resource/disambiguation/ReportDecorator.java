package org.rhq.enterprise.gui.coregui.client.resource.disambiguation;

import java.util.Iterator;
import java.util.List;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.DisambiguationReport.Resource;
import org.rhq.core.domain.resource.composite.DisambiguationReport.ResourceType;

/**
 * Class handles some of the data decoration that used to be done for legacy struts
 * tags.  See org.rhq.enterprise.gui.legacy.taglib.display.DisambiguatedResourceLineageDecorator and 
 * org.rhq.enterprise.gui.legacy.taglib.display.DisambiguatedResourceLineageTag
 * for original functionality.
 * 
 * @author Simeon Pinder
 */
public class ReportDecorator {

    //TODO: pull value from more bookmarking/history definition 
    public final static String GWT_RESOURCE_URL = "/coregui/CoreGUI.html#Resource/";
    public final static String GWT_RECENT_OPERATION_URL = "/coregui/CoreGUI.html#Operation/";
    public static final String DEFAULT_SEPARATOR = " > ";

    /** Generates HTML label from DisambiguationReport data.
     * 
     * @param ResourceType type.  If !null, the ResourceType name is prepended to result.
     * @param resourceName Name of the element from report
     * @param resourceId Id for resource
     * @return String of generated html for a ResourceName.
     */
    public static String decorateResourceName(String specificUrl, ResourceType type, String resourceName, int resourceId) {
        String decorated = "";
        if (type != null) {
            decorated += type.getName();
        }
        //        decorated += " <a href=\"" + GWT_RESOURCE_URL + resourceId + "\">" + resourceName + "</a>";
        decorated += " <a href=\"" + specificUrl + resourceId + "\">" + resourceName + "</a>";
        return decorated;
    }

    /** Generates Html label of Resource Lineage for disambiguation. 
     * 
     * @param parents ResourceLineage provided by DisambiguationReport.
     * @return String of generated html for ResourceLineage.
     */
    public static String decorateResourceLineage(List<Resource> parents) {
        StringBuffer decorated = new StringBuffer();
        if (parents != null && parents.size() > 0) {

            Iterator<DisambiguationReport.Resource> it = parents.iterator();
            DisambiguationReport.Resource parent = it.next();
            //generate first link
            String parentUrl = ReportDecorator.decorateResourceName(GWT_RESOURCE_URL, null, parent.getName(), parent
                .getId());
            decorated = writeResource(decorated, parentUrl, parent.getName(), parent.getType());
            while (it.hasNext()) {
                decorated.append(DEFAULT_SEPARATOR);
                parent = it.next();
                decorated = writeResource(decorated, ReportDecorator.decorateResourceName(GWT_RESOURCE_URL, null,
                    parent.getName(), parent.getId()), parent.getName(), parent.getType());

            }
        }
        return decorated.toString();
    }

    /** Appends resource lineage details with html anchors. 
     * 
     * @param existing 
     * @param url
     * @param resourceName
     * @param resourceType
     * @return
     */
    private static StringBuffer writeResource(StringBuffer existing, String url, String resourceName,
        DisambiguationReport.ResourceType resourceType) {
        if (!resourceType.isSingleton()) {

            existing.append(resourceType.getName()).append(" ");

            if (resourceType.getPlugin() != null) {
                existing.append("(").append(resourceType.getPlugin()).append(" plugin) ");
            }
        }

        if (url != null) {
            existing.append(url);
        }

        if (resourceType.isSingleton() && resourceType.getPlugin() != null) {
            existing.append(" (").append(resourceType.getPlugin()).append(" plugin)");
        }
        return existing;
    }

}
