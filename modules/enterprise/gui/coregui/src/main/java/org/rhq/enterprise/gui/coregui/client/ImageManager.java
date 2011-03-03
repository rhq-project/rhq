package org.rhq.enterprise.gui.coregui.client;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;

/**
 * Provides an API to obtain links to images and icons, thus avoiding hardcoding image URLs throughout client code.
 * 
 * For each icon, there is typically a small and large version (16x16 and 24x24). To obtain the smaller icon,
 * you use the "getXXXIcon" methods and to obtain the larger icon you use the "getXXXLargeIcon" method.
 * 
 * @author John Mazzitelli
 *
 */
public class ImageManager {

    public static final String IMAGES_DIR = "images/";

    public static String getLoadingIcon() {
        return "ajax-loader.gif";
    }

    /**
     * Returns a generic "upload" icon.
     */
    public static String getUploadIcon() {
        return "global/upload.png";
    }

    /**
     * Returns a generic edit icon.
     */
    public static String getViewIcon() {
        return "[SKIN]/actions/view.png";
    }

    /**
     * Returns a generic edit icon.
     */
    public static String getEditIcon() {
        return "[SKIN]/actions/edit.png";
    }

    /**
     * Returns a generic approve (aka "ok") icon.
     */
    public static String getApproveIcon() {
        return "[SKIN]/actions/approve.png";
    }

    /**
     * Returns a generic cancel icon.
     */
    public static String getCancelIcon() {
        return "[SKIN]/actions/undo.png";
    }

    /**
     * Returns the operation status icon. If status is null, returns
     * the plain, unbadged, operation icon.
     */
    public static String getOperationResultsIcon(OperationRequestStatus status) {
        String icon = "";
        if (status != null) {
            switch (status) {
            case INPROGRESS:
                icon = "_inprogress";
                break;
            case SUCCESS:
                icon = "_ok";
                break;
            case FAILURE:
                icon = "_failed";
                break;
            case CANCELED:
                icon = "_cancel";
                break;
            }
        }

        return "subsystems/control/Operation" + icon + "_16.png";
    }

    /**
     * All methods in this ImageManager class return image paths relative to the top
     * {@link #IMAGES_DIR images directory}. If you need a full path to the image, including
     * this top images directory name (for example, if you need to populate an explicit HTML
     * img tag's src attribute) pass in an image path to this {@link #getFullImagePath(String)}
     * method to obtain the full path.  The caller can optionally prepend {@link #IMAGES_DIR}
     * to any path returned by ImageManager, which is all this method really does.
     * 
     * @param image a relative image path
     * @return a full image path
     */
    public static String getFullImagePath(String image) {
        return IMAGES_DIR + image;
    }

    public static String getResourceIcon(Resource resource) {
        return getResourceIcon(resource, "16");
    }

    public static String getResourceLargeIcon(Resource resource) {
        return getResourceIcon(resource, "24");
    }

    private static String getResourceIcon(Resource resource, String size) {
        ResourceType type = resource.getResourceType();
        ResourceCategory category;
        if (type != null) {
            category = type.getCategory();
        } else {
            category = ResourceCategory.SERVICE;
        }

        ResourceAvailability resourceAvail = resource.getCurrentAvailability();
        Boolean avail;
        if (resourceAvail != null) {
            AvailabilityType availType = resourceAvail.getAvailabilityType();
            if (availType != null) {
                avail = Boolean.valueOf(availType == AvailabilityType.UP);
            } else {
                avail = null;
            }
        } else {
            avail = null;
        }
        return getResourceIcon(category, avail, size);
    }

    public static String getClusteredResourceIcon(ResourceCategory category) {
        String categoryName = null;

        switch (category) {
        case PLATFORM: {
            categoryName = "Platform";
            break;
        }
        case SERVER: {
            categoryName = "Server";
            break;
        }
        case SERVICE: {
            categoryName = "Service";
            break;
        }
        }

        return "resources/" + categoryName + "_Group_16.png";
    }

    public static String getResourceIcon(ResourceCategory category) {
        return getResourceIcon(category, Boolean.TRUE);
    }

    public static String getResourceLargeIcon(ResourceCategory category) {
        return getResourceLargeIcon(category, Boolean.TRUE);
    }

    public static String getResourceIcon(ResourceCategory category, Boolean avail) {
        return getResourceIcon(category, avail, "16");
    }

    public static String getResourceLargeIcon(ResourceCategory category, Boolean avail) {
        return getResourceIcon(category, avail, "24");
    }

    private static String getResourceIcon(ResourceCategory category, Boolean avail, String size) {
        String categoryName = null;
        String availName = null;

        switch (category) {
        case PLATFORM: {
            categoryName = "Platform";
            availName = (avail != null && avail.booleanValue()) ? "up" : "down";
            break;
        }
        case SERVER: {
            categoryName = "Server";
            // only server icons have an explicit "unknown" icon, the others will be assumed down when null
            availName = (avail != null) ? (avail.booleanValue() ? "up" : "down") : "unknown";
            break;
        }
        case SERVICE: {
            categoryName = "Service";
            availName = (avail != null && avail.booleanValue()) ? "up" : "down";
            break;
        }
        }

        return "types/" + categoryName + "_" + availName + "_" + size + ".png";
    }

    public static String getGroupIcon(GroupCategory groupType) {
        String category = groupType == GroupCategory.COMPATIBLE ? "Cluster" : "Group";
        return "types/" + category + "_up_16.png";
    }

    public static String getGroupLargeIcon(GroupCategory groupType) {
        String category = groupType == GroupCategory.COMPATIBLE ? "Cluster" : "Group";
        return "types/" + category + "_up_24.png";
    }

    /**
     * Returns the group icon badged with availability icon. Avails is the
     * percentage of resources in the group that are UP. If avails is 0, it is
     * red (no resources are available), if it is 1, it is green (all resources
     * are available), if it is between 0 and 1, it is yellow.
     * 
     * If avails is null, this means there are no resources in the group. In that
     * case, this method returns the "UP" badged icon. 
     *  
     * @param groupType the type of group
     * @param avails percentage of resources that are UP
     * @return the group badge icon
     */
    public static String getGroupIcon(GroupCategory groupType, Double avails) {
        return getGroupIcon(groupType, avails, "16");
    }

    public static String getGroupLargeIcon(GroupCategory groupType, Double avails) {
        return getGroupIcon(groupType, avails, "24");
    }

    private static String getGroupIcon(GroupCategory groupType, Double avails, String size) {
        String category = groupType == GroupCategory.COMPATIBLE ? "Cluster" : "Group";

        if (avails == null) {
            return "types/" + category + "_up_" + size + ".png";
        }

        double val = avails.doubleValue();

        if (val == 0.0d) {
            return "types/" + category + "_down_" + size + ".png";
        } else if (val > 0.0d && val < 1.0d) {
            return "types/" + category + "_warning_" + size + ".png";
        } else {
            return "types/" + category + "_up_" + size + ".png";
        }
    }

    public static String getAvailabilityIconFromAvailType(AvailabilityType availType) {
        return getAvailabilityIcon((availType != null) ? Boolean.valueOf(availType == AvailabilityType.UP) : null);
    }

    public static String getAvailabilityLargeIconFromAvailType(AvailabilityType availType) {
        return getAvailabilityLargeIcon((availType != null) ? Boolean.valueOf(availType == AvailabilityType.UP) : null);
    }

    /**
     * Given a Boolean to indicate if something is to be considered available or unavailable, the appropriate
     * availability icon is returned. If the given Boolean is null, the availability will be considered unknown
     * and thus the unknown/question icon is returned.
     * 
     * @param avail
     * @return the avail icon
     */
    public static String getAvailabilityIcon(Boolean avail) {
        return "subsystems/availability/availability_" + ((avail == null) ? "grey" : (avail ? "green" : "red"))
            + "_16.png";
    }

    public static String getAvailabilityLargeIcon(Boolean avail) {
        return "subsystems/availability/availability_" + ((avail == null) ? "grey" : (avail ? "green" : "red"))
            + "_24.png";
    }

    /**
     * Returns the large availability icon based on the given percentage.
     * Avails is the percentage of availabilities that are UP. If avails is 0, it is
     * red (nothing is available), if it is 1, it is green (everything is available),
     * if it is between 0 and 1, it is yellow.
     * 
     * If avails is null, the icon will be the unknown/grey form.
     *  
     * @param avails percentage of availabilities that are UP
     * @return the large availability icon
     */
    public static String getAvailabilityGroupLargeIcon(Double avails) {
        if (avails == null) {
            return "subsystems/availability/availability_grey_24.png";
        }

        double val = avails.doubleValue();

        if (val == 0.0d) {
            return "subsystems/availability/availability_red_24.png";
        } else if (val > 0.0d && val < 1.0d) {
            return "subsystems/availability/availability_yellow_24.png";
        } else {
            return "subsystems/availability/availability_green_24.png";
        }
    }

    public static String getAvailabilityYellowIcon() {
        return "subsystems/availability/availability_yellow_16.png";
    }

    public static String getAvailabilityYellowLargeIcon() {
        return "subsystems/availability/availability_yellow_24.png";
    }

    public static String getAlertIcon(AlertPriority priority) {
        return "subsystems/alert/Alert_" + priority.name() + "_16.png";
    }

    public static String getAlertIcon() {
        return "subsystems/alert/Alert_16.png";
    }

    public static String getAlertLargeIcon() {
        return "subsystems/alert/Flag_blue_24.png";
    }

    public static String getAlertEditIcon() {
        return "subsystems/alert/Edit_Alert.png";
    }

    public static String getMetricEditIcon() {
        return "subsystems/monitor/Edit_Metric.png";
    }

    public static String getPluginConfigurationIcon(ConfigurationUpdateStatus updateStatus) {
        if (updateStatus != null) {
            switch (updateStatus) {
            case SUCCESS: {
                return "subsystems/inventory/Connection_ok_16.png";
            }
            case FAILURE: {
                return "subsystems/inventory/Connection_failed_16.png";
            }
            case INPROGRESS: {
                return "subsystems/inventory/Connection_inprogress_16.png";
            }
            case NOCHANGE:
                return "subsystems/inventory/Connection_16.png";
            }
        }

        return "subsystems/inventory/Connection_16.png";
    }

    public static String getResourceConfigurationIcon(ConfigurationUpdateStatus updateStatus) {
        if (updateStatus != null) {
            switch (updateStatus) {
            case SUCCESS: {
                return "subsystems/configure/Configure_ok_16.png";
            }
            case FAILURE: {
                return "subsystems/configure/Configure_failed_16.png";
            }
            case INPROGRESS: {
                return "subsystems/configure/Configure_inprogress_16.png";
            }
            case NOCHANGE:
                return "subsystems/configure/Configure_16.png";
            }
        }

        return "subsystems/configure/Configure_16.png";
    }

    public static String getLockedIcon() {
        return "global/Locked_16.png";
    }

    public static String getEventSeverityIcon(EventSeverity severity) {
        String icon = "";
        if (severity != null) {
            switch (severity) {
            case DEBUG:
                icon = "_debug";
                break;
            case INFO:
                icon = "_info";
                break;
            case WARN:
                icon = "_warning";
                break;
            case ERROR:
                icon = "_error";
                break;
            case FATAL:
                icon = "_fatal";
                break;
            }
        }

        return "subsystems/event/Events" + icon + "_16.png";
    }
}
