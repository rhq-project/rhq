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
        SelectItem item = new SelectItem("fruits", "Fruits");
        firstList.add(item);
        item = new SelectItem("vegetables", "Vegetables");
        firstList.add(item);
        for (int i = 0; i < FRUITS.length; i++) {
            item = new SelectItem(FRUITS[i]);
        }
    }

    public List<SelectItem> getFirstList() {
        return firstList;
    }

    public List<SelectItem> getSecondList() {
        return secondList;
    }

    public List<SelectItem> getThirdList() {
        return thirdList;
    }

    public void currentTypeChanged(ValueChangeEvent event) {
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
        thirdList.clear();
        result = null;
    }

    public void currentItemChanged(ValueChangeEvent event) {
        // edit stuff as a result of the change
        thirdList.clear();
        String selectedCurrentItem = (String) event.getNewValue();
        for (char nextChar : selectedCurrentItem.toCharArray()) {
            SelectItem item = new SelectItem(nextChar);
            thirdList.add(item);
        }
        // clean-up dependent form elements
        result = null;
    }

    public void currentCharChanged(ValueChangeEvent event) {
        // edit stuff as a result of the change
        result = getCurrentType() + " : " + getCurrentItem() + " : " + getFavoriteCharacter();
        // no dependent form elements
    }

    public String getCurrentType() {
        return currentType;
    }

    public void setCurrentType(String currentType) {
        this.currentType = currentType;
    }

    public String getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(String currentItem) {
        this.currentItem = currentItem;
    }

    public String getFavoriteCharacter() {
        return favoriteCharacter;
    }

    public void setFavoriteCharacter(String favoriteCharacter) {
        this.favoriteCharacter = favoriteCharacter;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
