package org.rhq.enterprise.gui.test;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Scope(ScopeType.PAGE)
@Name("TestConditionalFormUIBean")
public class TestConditionalFormUIBean {

    private String currentType = "";
    private String currentItem = "";
    private String favoriteCharacter = "";
    private String result;

    public List<SelectItem> firstList = new ArrayList<SelectItem>();
    public List<SelectItem> secondList = new ArrayList<SelectItem>();
    public List<SelectItem> thirdList = new ArrayList<SelectItem>();

    private static final String[] FRUITS = { "Banana", "Cranberry", "Blueberry", "Orange" };
    private static final String[] VEGETABLES = { "Potatoes", "Broccoli", "Garlic", "Carrot" };

    private boolean debug = true;

    public TestConditionalFormUIBean() {
        init();
    }

    public void init() {
        firstList.add(new SelectItem("none", "Select..."));
        firstList.add(new SelectItem("fruits", "Fruits"));
        firstList.add(new SelectItem("vegetables", "Vegetables"));
    }

    public List<SelectItem> getFirstList() {
        debug("getFirstList() -> " + prettyPrint(firstList));
        return firstList;
    }

    public List<SelectItem> getSecondList() {
        debug("getSecondList() -> " + prettyPrint(secondList));
        return secondList;
    }

    public List<SelectItem> getThirdList() {
        debug("getThirdList() -> " + prettyPrint(thirdList));
        return thirdList;
    }

    private String prettyPrint(List<SelectItem> list) {
        StringBuilder results = new StringBuilder();
        boolean first = true;
        for (SelectItem nextItem : list) {
            if (first) {
                first = false;
            } else {
                results.append(", ");
            }
            results.append(nextItem.getValue() + ":" + nextItem.getLabel());
        }
        return results.toString();
    }

    private boolean noEffect(ValueChangeEvent event) {
        Object oldValue = event.getOldValue();
        if (event.getNewValue() == null) {
            debug("noEffect: nothing selected");
            return true; // nothing was actually selected, thus no effect
        }

        Object newValue = event.getNewValue();
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            debug("nothing changed");
            return true; // nothing was changed, thus no effect
            // NOTE: ValueChangeEvent is sometimes suppressed client-side for no-change events; depends on the component 
        }

        debug("noEffect: change detected");
        return false;
    }

    public void currentTypeChanged(ValueChangeEvent event) {
        debug("currentTypeChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        secondList.clear();

        String[] currentItems;
        String selectedCurrentType = (String) event.getNewValue();

        if (selectedCurrentType.equals("none")) {
            currentItems = new String[0];
        } else {
            secondList.add(new SelectItem("none", "Select..."));
            if (selectedCurrentType.equals("fruits")) {
                currentItems = FRUITS;
            } else {
                currentItems = VEGETABLES;
            }
        }
        for (int i = 0; i < currentItems.length; i++) {
            SelectItem item = new SelectItem(currentItems[i]);
            secondList.add(item);
        }

        // clean-up dependent form elements
        debug("currentTypeChanged: clearing thirdList, nulling-out result");
        thirdList.clear();
        result = null;
    }

    public void currentItemChanged(ValueChangeEvent event) {
        debug("currentItemChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        thirdList.clear();
        thirdList.add(new SelectItem("none", "Select..."));
        String selectedCurrentItem = (String) event.getNewValue();
        if (selectedCurrentItem.equals("none") == false) {
            for (char nextChar : selectedCurrentItem.toCharArray()) {
                SelectItem item = new SelectItem(nextChar);
                thirdList.add(item);
            }
        }

        // clean-up dependent form elements
        debug("currentItemChanged: nulling-out result");
        result = null;
    }

    public void currentCharChanged(ValueChangeEvent event) {
        debug("currentCharChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        // NOTE: calling getFavoriteCharacter results stale data here, because the
        //       ValueChangeEvent is fired before the setFavoriteCharacter method
        result = null;
        String selectedCurrentChar = (String) event.getNewValue();
        if (selectedCurrentChar.equals("none") == false) {
            result = getCurrentType() + " : " + getCurrentItem() + " : " + selectedCurrentChar;
        }

        // no dependent form elements
    }

    public String getCurrentType() {
        debug("getCurrentType() -> " + currentType);
        return currentType;
    }

    public void setCurrentType(String currentType) {
        debug("setCurrentType(" + currentType + ")");
        this.currentType = currentType;
    }

    public String getCurrentItem() {
        debug("getCurrentItem() -> " + currentItem);
        return currentItem;
    }

    public void setCurrentItem(String currentItem) {
        debug("setCurrentItem(" + currentItem + ")");
        this.currentItem = currentItem;
    }

    public String getFavoriteCharacter() {
        debug("getFavoriteCharacter() -> " + favoriteCharacter);
        return favoriteCharacter;
    }

    public void setFavoriteCharacter(String favoriteCharacter) {
        debug("setFavoriteCharacter(" + favoriteCharacter + ")");
        this.favoriteCharacter = favoriteCharacter;
    }

    public String getResult() {
        debug("getResult() -> " + result);
        return result;
    }

    public void setResult(String result) {
        debug("setResult(" + result + ")");
        this.result = result;
    }

    private void debug(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
