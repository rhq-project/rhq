package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.smartgwt.client.widgets.TransferImgButton;

/**
 * Wrapper for com.smartgwt.client.widgets.TransferImgButton that sets the ID for use with selenium scLocators.
 * 
 * @author Jay Shaughnessy
 */
public class LocatableTransferImgButton extends TransferImgButton {

    /** 
     * <pre>
     * ID Format: "scClassname-imgName"
     * </pre>
     * @param img not null.
     */
    public LocatableTransferImgButton(TransferImg img) {
        super(img);
        String safeId = this.getScClassName() + "-" + getSuffix(img);
        setID(safeId);
    }

    /** 
     * <pre>
     * ID Format: "scClassname-locatorId"
     * </pre>
     * @param locatorId not null.
     * @param img not null or empty. 
     */
    public LocatableTransferImgButton(String locatorId, TransferImg img) {
        super(img);
        String unsafeId = this.getScClassName() + "-" + locatorId;
        setID(SeleniumUtility.getSafeId(unsafeId));
    }

    private String getSuffix(TransferImg img) {
        if (TransferImgButton.LEFT == img)
            return "LEFT";
        if (TransferImgButton.LEFT_ALL == img)
            return "LEFT_ALL";
        if (TransferImgButton.RIGHT == img)
            return "RIGHT";
        if (TransferImgButton.RIGHT_ALL == img)
            return "RIGHT_ALL";
        if (TransferImgButton.UP == img)
            return "UP";
        if (TransferImgButton.UP_FIRST == img)
            return "UP_FIRST";
        if (TransferImgButton.DOWN == img)
            return "DOWN";
        if (TransferImgButton.DOWN_LAST == img)
            return "DOWN_LAST";
        if (TransferImgButton.DELETE == img)
            return "DELETE";

        return img.toString();
    }

}
