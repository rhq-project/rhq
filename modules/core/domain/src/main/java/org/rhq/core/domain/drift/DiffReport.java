package org.rhq.core.domain.drift;

import java.util.ArrayList;
import java.util.List;

public class DiffReport<T> {

    private List<T> notInLeft = new ArrayList<T>();

    private List<T> notInRight = new ArrayList<T>();

    private List<T> conflicts = new ArrayList<T>();

    public List<T> getElementsNotInLeft() {
        return notInLeft;
    }

    public void elementNotInLeft(T element) {
        notInLeft.add(element);
    }

    public List<T> getElementsNotInRight() {
        return notInRight;
    }

    public void elementNotInRight(T element) {
        notInRight.add(element);
    }

    public List<T> getElementsInConflict() {
        return conflicts;
    }

    public void elementInConflict(T element) {
        conflicts.add(element);
    }

}
