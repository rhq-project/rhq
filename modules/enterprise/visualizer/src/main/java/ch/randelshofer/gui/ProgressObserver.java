/*
 * @(#)ProgressObserver.java  1.1  2007-09-16
 *
 * Copyright (c) 2006-2007 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.gui;

import javax.swing.*;

/**
 * ProgressObserver.
 *
 *
 * @author Werner Randelshofer
 * @version 1.1 2007-09-16 setIndeterminate/isIndeterminate added.
 * <br>1.0 September 18, 2006 Created.
 */
public interface ProgressObserver {

    public void setModel(BoundedRangeModel brm);

    public BoundedRangeModel getModel();

    /**
     * Set cancelable to false if the operation can not be canceled.
     */
    public void setCancelable(boolean b);

    /**
     * The specified Runnable is executed when the user presses
     * the cancel button.
     */
    public void setDoCancel(Runnable doCancel);

    /**
     * Indicate the progress of the operation being monitored.
     * If the specified value is >= the maximum, the progress
     * monitor is closed.
     * @param nv an int specifying the current value, between the
     *        maximum and minimum specified for this component
     * @see #setMinimum
     * @see #setMaximum
     * @see #close
     */
    public void setProgress(int nv);

    /**
     * Returns the progress of the operation being monitored.
     */
    public int getProgress();

    /**
     * Indicate that the operation is complete.  This happens automatically
     * when the value set by setProgress is >= max, but it may be called
     * earlier if the operation ends early.
     */
    public void close();


    /**
     * Returns the minimum value -- the lower end of the progress value.
     *
     * @return an int representing the minimum value
     * @see #setMinimum
     */
    public int getMinimum();


    /**
     * Specifies the minimum value.
     *
     * @param m  an int specifying the minimum value
     * @see #getMinimum
     */
    public void setMinimum(int m);


    /**
     * Returns the maximum value -- the higher end of the progress value.
     *
     * @return an int representing the maximum value
     * @see #setMaximum
     */
    public int getMaximum();


    /**
     * Specifies the maximum value.
     *
     * @param m  an int specifying the maximum value
     * @see #getMaximum
     */
    public void setMaximum(int m);


    /**
     * Returns true if the user has hit the Cancel button in the progress dialog.
     */
    public boolean isCanceled();
    /**
     * Returns true if the ProgressView is closed.
     */
    public boolean isClosed();

    /**
     * Cancels the operation.
     * This method must be invoked from the user event dispatch thread.
     */
    public void cancel();
    /**
     * Specifies the additional note that is displayed along with the
     * progress message. Used, for example, to show which file the
     * is currently being copied during a multiple-file copy.
     *
     * @param note  a String specifying the note to display
     * @see #getNote
     */
    public void setNote(String note);


    /**
     * Specifies the additional note that is displayed along with the
     * progress message.
     *
     * @return a String specifying the note to display
     * @see #setNote
     */
    public String getNote();

    public void setIndeterminate(boolean b);
    public boolean isIndeterminate();
}
