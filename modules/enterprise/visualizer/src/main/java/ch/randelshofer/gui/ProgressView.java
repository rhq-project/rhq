/*
 * @(#)ProgressView.java  1.6 2007-09-16
 *
 * Copyright (c) 2002-2007 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */
package ch.randelshofer.gui;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.event.*;
/**
 * A class to monitor the progress of some operation.
 *
 *
 * @author Werner Randelshofer
 * @version 1.6 2007-09-16 Support for indeterminate progress added.
 * <br>1.5 2006-09-18 Interface PProgressObserverimplemented
 * <br>1.4.1 2004-12-23 Use invokeAndWait in method setCancelable instead
 * of invokeLater.
 * <br>1.4 2004-04-19 Reworked to have a BoundedRangeModel as the data model.
 * <br>1.2 2002-12-23 Operation getProgress() added.
 * <br>1.1 2002-07-28 ScrollPane in class ProgressView added.
 * <br>1.0 2002-05-10 Created.
 */
public class ProgressView extends javax.swing.JPanel implements ProgressObserver {
    private boolean isCanceled, isClosed, isCancelable = true;
    private javax.swing.BoundedRangeModel model;
    private Runnable doCancel;
private ChangeListener changeHandler = new ChangeListener() {
    public void stateChanged(ChangeEvent e) {
        if (model != null && model.getValue() >= model.getMaximum()) {
            close();
        }
    }
};

    /**
     * Creates new form ProgressView
     */
    public ProgressView(String message, String note, int min, int max) {
        initComponents();
        setModel(new javax.swing.DefaultBoundedRangeModel(min, 0, min, max));
        messageLabel.setText(message);
        noteLabel.setText(note);
        ProgressFrame.getInstance().addProgressView(this);
    }
    /**
     * Creates new form ProgressView
     */
    public ProgressView(String message, String note) {
        initComponents();
        setModel(new javax.swing.DefaultBoundedRangeModel(0, 0, 0, 1));
        messageLabel.setText(message);
        noteLabel.setText(note);
        ProgressFrame.getInstance().addProgressView(this);
        progressBar.setIndeterminate(true);
    }

    public void setIndeterminate(boolean newValue) {
        progressBar.setIndeterminate(newValue);

    }

    public void setModel(BoundedRangeModel brm) {
        if (model != null) {
            model.removeChangeListener(changeHandler);
        }
        model = brm;
        progressBar.setModel(brm);
        if (model != null) {
            model.addChangeListener(changeHandler);
        }
    }
    public BoundedRangeModel getModel() {
        return model;
    }

    /**
     * Set cancelable to false if the operation can not be canceled.
     */
    public void setCancelable(final boolean b) {
        isCancelable = b;
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    cancelButton.setVisible(b);
                    invalidate();
                    validate();
                }
            });
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new InternalError(e.getMessage());
        } catch (InterruptedException e) {
            // empty
        }
    }

    /**
     * The specified Runnable is executed when the user presses
     * the cancel button.
     */
    public void setDoCancel(Runnable doCancel) {
        this.doCancel = doCancel;
    }

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
    public void setProgress(int nv) {
        model.setValue(nv);
    }

    /**
     * Returns the progress of the operation being monitored.
     */
    public int getProgress() {
        return model.getValue();
    }

    /**
     * Indicate that the operation is complete.  This happens automatically
     * when the value set by setProgress is >= max, but it may be called
     * earlier if the operation ends early.
     */
    public void close() {
        if (! isClosed) {
            isClosed = true;
            ProgressFrame.getInstance().removeProgressView(this);
            if (model != null) model.removeChangeListener(changeHandler);
        }
    }


    /**
     * Returns the minimum value -- the lower end of the progress value.
     *
     * @return an int representing the minimum value
     * @see #setMinimum
     */
    public int getMinimum() {
        return model.getMinimum();
    }


    /**
     * Specifies the minimum value.
     *
     * @param m  an int specifying the minimum value
     * @see #getMinimum
     */
    public void setMinimum(int m) {
        model.setMinimum(m);
    }


    /**
     * Returns the maximum value -- the higher end of the progress value.
     *
     * @return an int representing the maximum value
     * @see #setMaximum
     */
    public int getMaximum() {
        return model.getMaximum();
    }


    /**
     * Specifies the maximum value.
     *
     * @param m  an int specifying the maximum value
     * @see #getMaximum
     */
    public void setMaximum(int m) {
        model.setMaximum(m);
    }


    /**
     * Returns true if the user has hit the Cancel button in the progress dialog.
     */
    public boolean isCanceled() {
        return isCanceled;
    }
    /**
     * Returns true if the ProgressView is closed.
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Cancels the operation.
     * This method must be invoked from the user event dispatch thread.
     */
    public void cancel() {
        if (isCancelable) {
            isCanceled = true;
            cancelButton.setEnabled(false);
            noteLabel.setText("Canceling...");
            if (doCancel != null) {
                doCancel.run();
            }
        } else {
            noteLabel.setText("This process can not be canceled!");
        }
    }
    /**
     * Specifies the additional note that is displayed along with the
     * progress message. Used, for example, to show which file the
     * is currently being copied during a multiple-file copy.
     *
     * @param note  a String specifying the note to display
     * @see #getNote
     */
    public void setNote(String note) {
        if (! isCanceled) {
            noteLabel.setText(note);
        }
    }


    /**
     * Specifies the additional note that is displayed along with the
     * progress message.
     *
     * @return a String specifying the note to display
     * @see #setNote
     */
    public String getNote() {
        return noteLabel.getText();
    }

    public boolean isIndeterminate() {
        return progressBar.isIndeterminate();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        messageLabel = new javax.swing.JLabel();
        noteLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        cancelButton = new javax.swing.JButton();
        separator = new javax.swing.JSeparator();

        setLayout(new java.awt.GridBagLayout());

        messageLabel.setFont(new java.awt.Font("Lucida Grande", 1, 13));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 0, 12);
        add(messageLabel, gridBagConstraints);

        noteLabel.setFont(new java.awt.Font("Lucida Grande", 0, 11));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 12, 0, 12);
        add(noteLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 12, 12, 12);
        add(progressBar, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancel(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 12, 12);
        add(cancelButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weighty = 1.0;
        add(separator, gridBagConstraints);

    }//GEN-END:initComponents

    private void cancel(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancel
        cancel();
    }//GEN-LAST:event_cancel



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JLabel noteLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JSeparator separator;
    // End of variables declaration//GEN-END:variables

}
