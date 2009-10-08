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
package org.rhq.enterprise.gui.legacy.action;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.ImageButtonBean;
import org.apache.struts.validator.ValidatorForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience methods for dealing with image-based form buttons.
 */
public class BaseValidatorForm extends ValidatorForm {
    //-------------------------------------instance variables

    private ImageButtonBean add;
    private ImageButtonBean cancel;
    private ImageButtonBean create;
    private ImageButtonBean delete;
    private ImageButtonBean ok;
    private ImageButtonBean okassign;
    private Integer pageSize;
    private ImageButtonBean reset;
    private ImageButtonBean remove;
    private ImageButtonBean enable;
    private ImageButtonBean userset;
    private ImageButtonBean install;
    private ImageButtonBean manualUninstall;
    private ImageButtonBean uninventory;

    /**
     * Holds value of property pn.
     */
    private Integer pn;

    //-------------------------------------constructors

    public BaseValidatorForm() {
        super();
        this.add = new ImageButtonBean();
        this.cancel = new ImageButtonBean();
        this.create = new ImageButtonBean();
        this.delete = new ImageButtonBean();
        this.ok = new ImageButtonBean();
        this.okassign = new ImageButtonBean();
        this.pageSize = null;
        this.reset = new ImageButtonBean();
        this.remove = new ImageButtonBean();
        this.enable = new ImageButtonBean();
        this.userset = new ImageButtonBean();
        this.install = new ImageButtonBean();
        this.manualUninstall = new ImageButtonBean();
        this.uninventory = new ImageButtonBean();
    }

    //-------------------------------------public methods

    public void setAdd(ImageButtonBean add) {
        this.add = add;
    }

    public ImageButtonBean getAdd() {
        return this.add;
    }

    public void setCancel(ImageButtonBean cancel) {
        this.cancel = cancel;
    }

    public ImageButtonBean getCancel() {
        return this.cancel;
    }

    public void setCreate(ImageButtonBean create) {
        this.create = create;
    }

    public ImageButtonBean getCreate() {
        return this.create;
    }

    public void setDelete(ImageButtonBean delete) {
        this.delete = delete;
    }

    public ImageButtonBean getDelete() {
        return this.delete;
    }

    public void setOk(ImageButtonBean ok) {
        this.ok = ok;
    }

    public ImageButtonBean getOk() {
        return this.ok;
    }

    public Integer getPs() {
        return this.pageSize;
    }

    public void setPs(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setRemove(ImageButtonBean remove) {
        this.remove = remove;
    }

    public ImageButtonBean getRemove() {
        return this.remove;
    }

    public void setReset(ImageButtonBean reset) {
        this.reset = reset;
    }

    public ImageButtonBean getReset() {
        return this.reset;
    }

    public void setEnable(ImageButtonBean enable) {
        this.enable = enable;
    }

    public ImageButtonBean getEnable() {
        return this.enable;
    }

    public void setUserset(ImageButtonBean userset) {
        this.userset = userset;
    }

    public ImageButtonBean getUserset() {
        return this.userset;
    }

    public void setInstall(ImageButtonBean install) {
        this.install = install;
    }

    public ImageButtonBean getInstall() {
        return this.install;
    }

    public ImageButtonBean getManualUninstall() {
        return this.manualUninstall;
    }

    /**
     * Setter for property p.
     *
     * @param pn New value of property p.
     */
    public void setPn(Integer pn) {
        this.pn = pn;
    }

    /**
     * Getter for property p.
     *
     * @return Value of property p.
     */
    public Integer getPn() {
        return this.pn;
    }

    /**
     * Sets the okAdd.
     *
     * @param okAdd The okAdd to userset
     */
    public void setOkassign(ImageButtonBean okAdd) {
        this.okassign = okAdd;
    }

    /**
     * @return ImageButtonBean
     */
    public ImageButtonBean getOkassign() {
        return okassign;
    }

    public void setUninventory(ImageButtonBean value) {
        this.uninventory = value;
    }

    public ImageButtonBean getUninventory() {
        return this.uninventory;
    }

    public boolean isAddClicked() {
        return getAdd().isSelected();
    }

    public boolean isCancelClicked() {
        return getCancel().isSelected();
    }

    public boolean isCreateClicked() {
        return getCreate().isSelected();
    }

    public boolean isDeleteClicked() {
        return getDelete().isSelected();
    }

    public boolean isOkClicked() {
        return getOk().isSelected();
    }

    public boolean isOkAssignClicked() {
        return getOkassign().isSelected();
    }

    public boolean isRemoveClicked() {
        return getRemove().isSelected();
    }

    public boolean isResetClicked() {
        return getReset().isSelected();
    }

    public boolean isEnableClicked() {
        return getEnable().isSelected();
    }

    public boolean isUsersetClicked() {
        return getUserset().isSelected();
    }

    public boolean isInstallClicked() {
        return getInstall().isSelected();
    }

    public boolean isManualUninstallClicked() {
        return getManualUninstall().isSelected();
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        this.pageSize = null;
    }

    /**
     * Only validate if 1) the form's ok or okassign button was clicked and 2) the mapping specifies an input form to
     * return to. condition #2 can be false when a form has failed validation and has forwarded to the input page; the
     * ok button request parameter will still be userset, but the prepare action for the input page will not have
     * (another) input page specified.
     */
    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if (shouldValidate(mapping, request)) {
            ActionErrors errs = super.validate(mapping, request);
            return errs;
        } else {
            return null;
        }
    }

    /* Only validate if
     * 1) the form's ok or okassign button was clicked and 2) the mapping specifies an input form to return to.
     *
     * Child classes should call this to decide whether or not to perform custom validation steps.
     */
    protected boolean shouldValidate(ActionMapping mapping, HttpServletRequest request) {
        return (isOkClicked() || isOkAssignClicked()) && (mapping.getInput() != null);
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();

        s.append("add=");
        s.append(add);
        s.append(" cancel=");
        s.append(cancel);
        s.append(" create=");
        s.append(create);
        s.append(" delete=");
        s.append(delete);
        s.append(" ok=");
        s.append(ok);
        s.append(" remove=");
        s.append(remove);
        s.append(" reset=");
        s.append(reset);
        s.append(" enable=");
        s.append(enable);
        s.append(" userset=");
        s.append(userset);
        s.append(" pageSize=");
        s.append(pageSize);
        s.append(" install=");
        s.append(install);
        s.append(" uninventory=");
        s.append(uninventory);

        return s.toString();
    }
}