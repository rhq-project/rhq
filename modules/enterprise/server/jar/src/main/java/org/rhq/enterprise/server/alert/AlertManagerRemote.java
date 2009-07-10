package org.rhq.enterprise.server.alert;

import javax.jws.WebMethod;
import javax.jws.WebParam;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.FetchException;

public interface AlertManagerRemote {
    /**
     * This find service can be used to find alerts based on various criteria and return various data.
     *
     * @param subject  The logged in user's subject.
     * @param criteria {@link Alert}
     * <pre>
     * If provided the Alert object can specify various search criteria as specified below.
     *   Alert.id : exact match
     *   Alert.name : case insensitive string match
     * </pre>
     * @param pc {@link PageControl}
     * <pre>
     * If provided PageControl specifies page size, requested page, sorting, and optional data.
     * 
     * Supported OptionalData
     *   To specify optional data call pc.setOptionalData() and supply one of more of the DATA_* constants
     *   defined in this interface.
     * 
     * Supported Sorting:
     *   Possible values to provide PageControl for sorting (PageControl.orderingFields)
     *     name
     *     ctime
     *   
     * </pre>
     * @return
     * @throws FetchException
     */
    @WebMethod
    PageList<Alert> findAlertDefinitions( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "name") Alert criteria, //
        @WebParam(name = "priority") AlertPriority priority, //
        @WebParam(name = "resourceIds") int[] resourceIds, //
        @WebParam(name = "beginTime") long beginTime, //
        @WebParam(name = "endTime") long endTime, //
        @WebParam(name = "pc") PageControl pc) //
        throws FetchException;
}
