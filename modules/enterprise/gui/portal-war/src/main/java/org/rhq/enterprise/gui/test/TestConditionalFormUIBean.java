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

    public TestConditionalFormUIBean() {
        init();
    }

    public void init() {
        SelectItem item = new SelectItem("fruits", "Fruits");
        firstList.add(item);
        item = new SelectItem("vegetables", "Vegetables");
        firstList.add(item);
        for (int i = 0; i < FRUITS.length; i++) {
            item = new SelectItem(FRUITS[i]);
        }
    }

    public List<SelectItem> getFirstList() {
        System.out.println("getFirstList() -> " + firstList);
        return firstList;
    }

    public List<SelectItem> getSecondList() {
        System.out.println("getSecondList() -> " + secondList);
        return secondList;
    }

    public List<SelectItem> getThirdList() {
        System.out.println("getThirdList() -> " + thirdList);
        return thirdList;
    }

    private boolean noEffect(ValueChangeEvent event) {
        if (event.getNewValue() == null) {
            System.out.println("noEffect: nothing selected");
            return true; // nothing was actually selected, thus no effect
        }
        Object oldValue = event.getOldValue();
        Object newValue = event.getNewValue();
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            System.out.println("nothing changed");
            return true; // nothing was changed, thus no effect
            // NOTE: ValueChangeEvent is sometimes suppressed client-side for no-change events; depends on the component 
        }
        System.out.println("noEffect: change detected");
        return false;
    }

    public void currentTypeChanged(ValueChangeEvent event) {
        System.out.println("currentTypeChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        secondList.clear();
        String[] currentItems;
        if (((String) event.getNewValue()).equals("fruits")) {
            currentItems = FRUITS;
        } else {
            currentItems = VEGETABLES;
        }
        for (int i = 0; i < currentItems.length; i++) {
            SelectItem item = new SelectItem(currentItems[i]);
            secondList.add(item);
        }

        // clean-up dependent form elements
        System.out.println("currentTypeChanged: clearing thirdList, nulling-out result");
        thirdList.clear();
        result = null;
    }

    public void currentItemChanged(ValueChangeEvent event) {
        System.out.println("currentItemChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        thirdList.clear();
        String selectedCurrentItem = (String) event.getNewValue();
        for (char nextChar : selectedCurrentItem.toCharArray()) {
            SelectItem item = new SelectItem(nextChar);
            thirdList.add(item);
        }

        // clean-up dependent form elements
        System.out.println("currentItemChanged: nulling-out result");
        result = null;
    }

    public void currentCharChanged(ValueChangeEvent event) {
        System.out.println("currentCharChanged: event fired");
        if (noEffect(event)) {
            // nothing was change or nothing was selected, so do nothing
            return;
        }

        // edit stuff as a result of the change
        // NOTE: calling getFavoriteCharacter results stale data here, because the
        //       ValueChangeEvent is fired before the setFavoriteCharacter method
        String selectedCurrentChar = (String) event.getNewValue();
        result = getCurrentType() + " : " + getCurrentItem() + " : " + selectedCurrentChar;

        // no dependent form elements
    }

    public String getCurrentType() {
        System.out.println("getCurrentType() -> " + currentType);
        return currentType;
    }

    public void setCurrentType(String currentType) {
        System.out.println("setCurrentType(" + currentType + ")");
        this.currentType = currentType;
    }

    public String getCurrentItem() {
        System.out.println("getCurrentItem() -> " + currentItem);
        return currentItem;
    }

    public void setCurrentItem(String currentItem) {
        System.out.println("setCurrentItem(" + currentItem + ")");
        this.currentItem = currentItem;
    }

    public String getFavoriteCharacter() {
        System.out.println("getFavoriteCharacter() -> " + favoriteCharacter);
        return favoriteCharacter;
    }

    public void setFavoriteCharacter(String favoriteCharacter) {
        System.out.println("setFavoriteCharacter(" + favoriteCharacter + ")");
        this.favoriteCharacter = favoriteCharacter;
    }

    public String getResult() {
        System.out.println("getResult() -> " + result);
        return result;
    }

    public void setResult(String result) {
        System.out.println("setResult(" + result + ")");
        this.result = result;
    }
}
