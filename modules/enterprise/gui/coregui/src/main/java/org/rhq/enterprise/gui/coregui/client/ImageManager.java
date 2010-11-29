package org.rhq.enterprise.gui.coregui.client;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.measurement.AvailabilityType;
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
    /**
     * Returns the large group icon badged with availability icon. Avails is the
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
    public static String getGroupLargeIcon(GroupCategory groupType, Double avails) {
        String category = groupType == GroupCategory.COMPATIBLE ? "Cluster" : "Group";

        if (avails == null) {
            return "types/" + category + "_up_24.png";
        }

        double val = avails.doubleValue();

        if (val == 0.0d) {
            return "types/" + category + "_down_24.png";
        } else if (val > 0.0d && val < 1.0d) {
            return "types/" + category + "_warning_24.png";
        } else {
            return "types/" + category + "_up_24.png";
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
}
