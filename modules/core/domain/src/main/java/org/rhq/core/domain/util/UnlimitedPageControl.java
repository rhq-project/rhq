package org.rhq.core.domain.util;

/**
 * A page control that shows all results. The page number and page size are 0 and @{link #SIZE_UNLIMITED} respectively,
 * and are immutable.
 *
 * @author Ian Springer
 */
public class UnlimitedPageControl extends PageControl {

    private static final long serialVersionUID = 1L;

    public UnlimitedPageControl() {
        this(new OrderingField[0]);
    }

    public UnlimitedPageControl(OrderingField... orderingFields) {
        super(0, SIZE_UNLIMITED, orderingFields);
    }

    @Override
    public void setPageNumber(int pageNumber) {
        throw new UnsupportedOperationException("page number cannot be changed from 0 for an UnlimitedPageControl.");
    }

    @Override
    public void setPageSize(int pageSize) {
        throw new UnsupportedOperationException("page size cannot be changed from " + SIZE_UNLIMITED
            + " for an UnlimitedPageControl.");
    }

    @Override
    public void reset() {
        getOrderingFields().clear();
    }

}
