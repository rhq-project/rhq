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
package org.rhq.enterprise.server.alert;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * all methods that aren't getters appropriately update the contents of the AlertConditionCache
 *
 * @author Joseph Marques
 */
@Local
public interface AlertDefinitionManagerLocal extends AlertDefinitionManagerRemote {
    PageList<AlertDefinition> findAlertDefinitions(Subject subject, int resourceId, PageControl pageControl);

    AlertDefinition getAlertDefinitionById(Subject subject, int alertDefinitionId);

    List<IntegerOptionItem> findAlertDefinitionOptionItemsForResource(Subject subject, int resourceId);

    List<IntegerOptionItem> findAlertDefinitionOptionItemsForGroup(Subject subject, int groupId);

    /**
     * Creates a new alert definition. Note that the suject is checked to have necessary authz, which might not
     * be what you want in all use cases. See {@link #createDependentAlertDefinition(Subject, AlertDefinition, int)}
     * for further discussion of this.
     *
     * @param subject the user creating the alert definition
     * @param alertDefinition the new alert definition to persist
     * @param resourceId the resource id for which the def is being created
     * @param finalizeNotificationConfiguration if true, the configuration of the def's notifications is validated.
     * This is NOT what you want if, for example, you are merely creating a copy of an existing definition.
     * Some notifications might require more input when creating the notification than is then persisted in their configs
     * (prominent example being the CLI alert sender).
     * This would then cause the validation to fail every time you created a copy of a definition and tried
     * to persist it. Note that passing false AND having new, unpersisted notifications in the alert definition can
     * lead to invalid configuration being stored for the notifications.
     * @return the newly persisted alert definition
     * @throws InvalidAlertDefinitionException
     */
    AlertDefinition createAlertDefinitionInNewTransaction(Subject subject, AlertDefinition alertDefinition,
        Integer resourceId, boolean finalizeNotificationConfiguration) throws InvalidAlertDefinitionException;

    /**
     * This is exactly the same as {@link #createAlertDefinitionInNewTransaction(Subject, AlertDefinition, Integer, boolean)} but
     * assumes the resource is part of a group (or has given resource type for templates) for which
     * a group or template alert definition is being created.
     * <p>
     * This method assumes the caller already checked the subject has permissions to create a group or template alert
     * definition on a group / resource type the resource is member of.
     * <p>
     * In another words this method is a helper to
     * {@link GroupAlertDefinitionManagerLocal#createGroupAlertDefinitions(Subject, AlertDefinition, Integer)} and
     * {@link AlertTemplateManagerLocal#createAlertTemplate(Subject, AlertDefinition, Integer)}.
     *
     * @param subject the user that is creating the group or template alert definition
     * @param alertDefinition the alert definition on the resource
     * @param resourceId the resource
     * @return the id of the newly created alert definition
     */
    int createDependentAlertDefinition(Subject subject, AlertDefinition alertDefinition, int resourceId);

    boolean isEnabled(Integer definitionId);

    boolean isTemplate(Integer definitionId);

    boolean isGroupAlertDefinition(Integer definitionId);

    boolean isResourceAlertDefinition(Integer definitionId);

    List<AlertDefinition> findAllRecoveryDefinitionsById(Subject subject, Integer alertDefinitionId);

    void copyAlertDefinitions(Subject subject, Integer[] alertDefinitionIds);

    /**
     * @param subject
     * @param alertDefinitionId
     * @param alertDefinition
     * @param resetMatching Incur the overhead of resetting any partial alert matching that has taken place. This *must*
     * be set true if updating conditions, dampening rules or the conditionExpressin (ANY vs ALL).  If in doubt, set to true
     * as the loss of partial matching is better than corrupted matching.
     * @return
     * @throws InvalidAlertDefinitionException
     * @throws AlertDefinitionUpdateException
     */
    AlertDefinition updateAlertDefinition(Subject subject, int alertDefinitionId, AlertDefinition alertDefinition,
        boolean resetMatching) throws InvalidAlertDefinitionException, AlertDefinitionUpdateException;

    /**
     * This method is similar in use-case to {@link #createDependentAlertDefinition(Subject, AlertDefinition, int)}.
     * It assumes that the the update is part of some more complex operation (like updating alert definition on a group
     * or a template) and that authz checks have already been performed.
     * <p>
     * This method is therefore identical to {@link #updateAlertDefinition(Subject, int, AlertDefinition, boolean)} but
     * does not perform any authorization checks.
     *
     * @param subject the user that is updating the alert definition
     * @param alertDefinitionId
     * @param alertDefinition
     * @param resetMatching
     * @return
     * @throws InvalidAlertDefinitionException
     * @throws AlertDefinitionUpdateException
     */
    AlertDefinition updateDependentAlertDefinition(Subject subject, int alertDefinitionId,
        AlertDefinition alertDefinition, boolean resetMatching) throws InvalidAlertDefinitionException,
        AlertDefinitionUpdateException;

    int purgeUnusedAlertDefinitions();

    void purgeInternals(int alertDefinitionId);

    AlertDefinition updateAlertDefinitionInternal(Subject subject, int alertDefinitionId,
        AlertDefinition alertDefinition, boolean resetMatching, boolean checkPerms, boolean finalizeNotifications)
        throws InvalidAlertDefinitionException, AlertDefinitionUpdateException;

    /**
     * INTERNAL-USE-ONLY!  Use {@link AlertDefinitionManagerRemote#enableAlertDefinitions(Subject, int[])} if you
     * aren't sure you should use this method.
     * <p/>
     * Enable the specified resource alert definitions. No permission checking is performed and it is assumed that the
     * alert definitions are resource-level.
     */
    int enableResourceAlertDefinitions(Subject subject, int[] resourceAlertDefinitionIds);

    /**
     * INTERNAL-USE-ONLY!  Use {@link AlertDefinitionManagerRemote#disableAlertDefinitions(Subject, int[])} if you
     * aren't sure you should use this method.
     * <p/>
     * Disable the specified resource alert definitions. No permission checking is performed and it is assumed that the
     * alert definitions are resource-level.
     */
    int disableResourceAlertDefinitions(Subject subject, int[] resourceAlertDefinitionIds);

    /**
     * INTERNAL-USE-ONLY!  Use {@link AlertDefinitionManagerRemote#removeAlertDefinitions(Subject, int[])} if you
     * aren't sure you should use this method.
     * <p/>
     * Delete the specified resource alert definitions. No permission checking is performed and it is assumed that the
     * alert definitions are resource-level.
     */
    int removeResourceAlertDefinitions(Subject subject, int[] resourceAlertDefinitionIds);

}