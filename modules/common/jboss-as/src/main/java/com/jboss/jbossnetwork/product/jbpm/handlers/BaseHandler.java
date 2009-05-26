/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package com.jboss.jbossnetwork.product.jbpm.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployPackageStep;

/**
 * Base class for all of our JBPM handlers, providing some basic functionality for transitioning between steps.
 *
 * @author Jason Dobies
 */
public abstract class BaseHandler implements ActionHandler {
    /**
     * Describes a successful transition between steps.
     */
    protected static final String TRANSITION_SUCCESS = "success";

    /**
     * Describes an error in a step, preventing transitioning.
     */
    protected static final String TRANSITION_ERROR = "error";

    /**
     * Standard message to describe that no changes were made by the currently executing step.
     */
    protected static final String MESSAGE_NO_CHANGES = "No changes were made in this step.";

    /**
     * Logger, keyed to the subclass.
     */
    protected final Log logger = LogFactory.getLog(this.getClass());

    /**
     * Returns a user readable description of what the step in the workflow entails.
     *
     * @return should not be <code>null</code>
     */
    public abstract String getDescription();

    /**
     * Tells the handle implementation to actually perform the step indicated.
     *
     * @param executionContext cannot be <code>null</code>
     */
    public abstract void run(ExecutionContext executionContext);

    /**
     * Sets the default values for properties used by the handler implementation. This may optionally be overridden by
     * action handler implementations if necessary.
     */
    public void setPropertyDefaults() {
        // Stub implementation
    }

    /**
     * Requests the action handler substitute into its node any variables necessary, taking the values for these from
     * the provided execution context. This may optionally be overridden by action handler implementations if necessary.
     *
     * @param  executionContext JBPM execution context from which the property values should be extracted
     *
     * @throws ActionHandlerException if there is an error extraction or substituting the variables
     */
    public void substituteVariables(ExecutionContext executionContext) throws ActionHandlerException {
        // Stub implementation
    }

    /**
     * Ensures the property values that were set in {@link #substituteVariables(org.jbpm.graph.exe.ExecutionContext)}
     * are valid. This may optionally be overridden by action handler implementations if necessary.
     *
     * @throws ActionHandlerException if any of the properties are invalid
     */
    protected void checkProperties() throws ActionHandlerException {
        // Stub implementation
    }

    public void execute(ExecutionContext executionContext) throws Exception {
        try {
            if (logger.isDebugEnabled()) {
                String nodeName = executionContext.getNode().getName();
                logger.debug("Description of step [" + nodeName + "] prior to setting defaults: " + getDescription());
            }

            setPropertyDefaults();

            if (logger.isDebugEnabled()) {
                String nodeName = executionContext.getNode().getName();
                logger.debug("Description of step [" + nodeName + "] prior to substituting variables: "
                    + getDescription());
            }

            substituteVariables(executionContext);

            if (logger.isDebugEnabled()) {
                String nodeName = executionContext.getNode().getName();
                logger
                    .debug("Description of step [" + nodeName + "] prior to checking properties: " + getDescription());
            }

            checkProperties();
        } catch (ActionHandlerException e) {
            error(executionContext, e, MESSAGE_NO_CHANGES, TRANSITION_ERROR);
            return;
        }

        try {
            run(executionContext);
        } catch (Exception e) {
            //            error(executionContext, new ActionHandlerException(e), e.getMessage(), TRANSITION_ERROR);
            logger.error("Error caught from run", e);
        }
    }

    /**
     * Called by a handler at the end of its processing to indicate an error occurred in the execution of a given step
     * and closes out the step.
     *
     * @param executionContext  context in which the step was executing
     * @param throwable         exception that occurred to trigger this call to error
     * @param additionalMessage additional details to report on the step
     * @param leavingTransition ?
     */
    protected void error(ExecutionContext executionContext, Throwable throwable, String additionalMessage,
        String leavingTransition) {
        String nodeName = executionContext.getNode().getName();

        ActionHandlerMessageLog log = logStep(executionContext, throwable, additionalMessage,
            ContentResponseResult.FAILURE);

        logger.info("Description of step [" + nodeName + "]: " + getDescription());
        logger.error("Result of step [" + nodeName + "]: " + log);

        executionContext.leaveNode(leavingTransition);
    }

    /**
     * Called by a handler at the end of its processing to indicate a step was completed successfully and transitions to
     * the next step in the workflow.
     *
     * @param executionContext context in which the step was executing
     * @param message          additional details on the step
     */
    protected void complete(ExecutionContext executionContext, String message) {
        String nodeName = executionContext.getNode().getName();

        logger.info("Description of step [" + nodeName + "]: " + getDescription());
        ActionHandlerMessageLog log = logStep(executionContext, null, message, ContentResponseResult.SUCCESS);
        logger.info("Result of step [" + nodeName + "]: " + log);

        executionContext.leaveNode(TRANSITION_SUCCESS);
    }

    /**
     * Called by a handler at the end of its processing to indicate a step in the workflow has been skipped and
     * transitions to the next step in the workflow.
     *
     * @param executionContext  context in which the step was executing
     * @param exception         exception that occurred to trigger this call to error
     * @param additionalMessage additional details to report on the step
     * @param leavingTransition ?
     */
    protected void skip(ExecutionContext executionContext, ActionHandlerException exception, String additionalMessage,
        String leavingTransition) {
        String nodeName = executionContext.getNode().getName();

        ActionHandlerMessageLog log = logStep(executionContext, exception, additionalMessage,
            ContentResponseResult.NOT_PERFORMED);

        logger.info("Description of step [" + nodeName + "]: " + getDescription());
        logger.warn("Result of step [" + nodeName + "]: " + log);

        executionContext.leaveNode(leavingTransition);
    }

    /**
     * Called by a handler at the end of its processing to indicate a step was skipped and transitions to the next step
     * in the workflow.
     *
     * @param executionContext context in which the step was executing
     * @param message          additional details on the step
     */
    protected void notRun(ExecutionContext executionContext, String message) {
        String nodeName = executionContext.getNode().getName();

        ActionHandlerMessageLog log = logStep(executionContext, null, message, ContentResponseResult.NOT_PERFORMED);
        logger.info("Result of step [" + nodeName + "]: " + log);

        executionContext.leaveNode(TRANSITION_SUCCESS);
    }

    /**
     * Logs a step into the JBPM context.
     *
     * @param  executionContext  context in which the step executed
     * @param  throwable         optional error that occurred in the step
     * @param  additionalMessage details of the step
     * @param  result            result of executing the step
     *
     * @return populated log message, will not be <code>null</code>
     */
    private ActionHandlerMessageLog logStep(ExecutionContext executionContext, Throwable throwable,
        String additionalMessage, ContentResponseResult result) {
        ActionHandlerMessageLog log = new ActionHandlerMessageLog();

        // The steps must indicate their order for the domain model. Store the variable in the context and use that
        // to keep track of what step we're on.
        Integer stepCounter = (Integer) executionContext.getVariable("stepCounter");
        if (stepCounter == null) {
            stepCounter = 0;
        }

        stepCounter++;

        executionContext.setVariable("stepCounter", stepCounter);

        // This was ported directly from 1.4 like this, but I'm not sure I like the idea of prefixing the throwable's
        // message before the additional message. It reads better if the handler simply provides an error message
        // in the additionalMessage field that is customized to what the handler does (i.e. "Could not download file").
        // Once we do more testing, I may revisit this and reenable this form of description generation.
        // jdobies, Mar 6, 2008

        // lkrejci, 2009-05-25 - added additional check for nullity of the throwable's message because the description mustn't be null 
        String description = (throwable == null || throwable.getMessage() == null) ? additionalMessage : throwable
            .getMessage();

        DeployPackageStep step = new DeployPackageStep(Integer.toString(stepCounter), description);
        step.setStepResult(result);

        if (throwable != null) {
            String errorMessage = StringUtil.getStackTrace(throwable);
            step.setStepErrorMessage(errorMessage);
        }

        log.setStep(step);

        executionContext.getProcessInstance().getLoggingInstance().addLog(log);

        return log;
    }

    /**
     * Substitutes in values found in the workflow (execution context) for the specified expression.
     *
     * @param  expression       expression into which to substitute values from the workflow
     * @param  executionContext context describing the workflow
     *
     * @return expression with the proper values substituted in; <code>null</code> if the expression is <code>
     *         null</code>
     *
     * @throws ActionHandlerException if there is an error during the substitution
     */
    protected String substituteVariable(String expression, ExecutionContext executionContext)
        throws ActionHandlerException {
        // If there is nothing in the value, then a substitution isn't going to change that
        if (expression == null) {
            return null;
        }

        // One bad thing with this evaluator is that if the variable name can't be found in the
        // executionContext it just replaces it with the empty string. Would be nicer if it
        // complained more loudly. That said it shouldn't cause problems since subsequent steps
        // won't work without the variables being substituted properly.
        // ccrouch, 1.4 codebase

        Object valueAfterSubst = JbpmExpressionEvaluator.evaluate(expression, executionContext);
        return (String) valueAfterSubst;
    }
}