/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.core.gui.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.DataModelListener;

import org.ajax4jsf.model.DataVisitor;
import org.ajax4jsf.model.ExtendedDataModel;
import org.ajax4jsf.model.Range;
import org.ajax4jsf.model.SequenceRange;
import org.ajax4jsf.model.SerializableDataModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.model.DataProvider;
import org.richfaces.model.ExtendedTableDataModel;
import org.richfaces.model.Field;
import org.richfaces.model.FilterField;
import org.richfaces.model.LocaleAware;
import org.richfaces.model.Modifiable;
import org.richfaces.model.SortField2;
import org.richfaces.model.impl.expressive.JavaBeanWrapper;
import org.richfaces.model.impl.expressive.ObjectWrapperFactory;
import org.richfaces.model.impl.expressive.WrappedBeanComparator2;
import org.richfaces.model.impl.expressive.WrappedBeanFilter;

/**
 * @author Konstantin Mishin
 *
 */
public class AbstractPagedDataModel2<T> extends ExtendedDataModel 
        implements Modifiable, LocaleAware {

    protected class RowKeyWrapperFactory extends ObjectWrapperFactory {

		private class ExtendedJavaBeanWrapper extends JavaBeanWrapper {

			private Object key;

			public ExtendedJavaBeanWrapper(Object key, Object o, Map<Object, Object> props) {
				super(o, props);
				this.key = key;
			}

			public Object getKey() {
				return key;
			}
		}

		public RowKeyWrapperFactory(FacesContext context, String var,
				List<? extends Field> sortOrder) {
			super(context, var, sortOrder);
		}

		@Override
		public JavaBeanWrapper wrapObject(Object key) {
			originalModel.setRowKey(key);
			JavaBeanWrapper wrapObject = super.wrapObject(originalModel.getRowData());
			return new ExtendedJavaBeanWrapper(key, wrapObject.getWrappedObject(), wrapObject.getProperties());
		}

		@Override
		public Object unwrapObject(Object wrapper) {
			return ((ExtendedJavaBeanWrapper) wrapper).getKey();
		}
	}

	private static final Log log = LogFactory.getLog(AbstractPagedDataModel2.class);

    private ExtendedTableDataModel<T> originalModel;
	protected List<Object> rowKeys;
	protected String var;
	protected Locale locale;

	private boolean sortNeeded = true;
	private boolean filterNeeded = true;

    @SuppressWarnings("unchecked")
	public AbstractPagedDataModel2(DataProvider<T> dataProvider, String var) {
		this(new ExtendedTableDataModel<T>(dataProvider), var);
	}

	public AbstractPagedDataModel2(DataProvider<T> dataProvider) {
		this(dataProvider, null);
	}

	@SuppressWarnings("unchecked")
	public AbstractPagedDataModel2(ExtendedTableDataModel<T> dataModel, String var) {
		this.originalModel = dataModel;
        this.var = var;
	}

	@Override
	public void addDataModelListener(DataModelListener listener) {
		originalModel.addDataModelListener(listener);
	}

	@Override
	public DataModelListener[] getDataModelListeners() {
		return originalModel.getDataModelListeners();
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public Object getRowKey() {
		return originalModel.getRowKey();
	}

	@Override
	public void setRowKey(Object key) {
		originalModel.setRowKey(key);
	}

	@Override
	public void walk(FacesContext context, DataVisitor visitor, Range range,
			Object argument) throws IOException {
		final SequenceRange seqRange = (SequenceRange) range;
		int rows = seqRange.getRows();
		int rowCount = getRowCount();
		int currentRow = seqRange.getFirstRow();
		if(rows > 0){
			rows += currentRow;
			rows = Math.min(rows, rowCount);
		} else {
			rows = rowCount;
		}
		for (; currentRow < rows; currentRow++) {
			visitor.process(context, rowKeys.get(currentRow), argument);
		}
	}

	/**
	 * Resets internal cached data. Call this method to reload data from data
	 * provider on first access for data.
	 */
	public void reset(){
		originalModel.reset();
		rowKeys = null;
		sortNeeded = true;
		filterNeeded = true;
	}

	public Object getKey(T o) {
		return originalModel.getKey(o);
	}

	public T getObjectByKey(Object key) {
		return originalModel.getObjectByKey(key);
	}

	@Override
	public int getRowCount() {
		if (rowKeys == null) {
			return -1;
		} else {
			return rowKeys.size();
		}
	}

	@Override
	public Object getRowData() {
		return originalModel.getRowData();
	}

	@Override
	public int getRowIndex() {
		return rowKeys.indexOf(originalModel.getRowKey());
	}

	@Override
	public Object getWrappedData() {
		return originalModel.getWrappedData();
	}

	@Override
	public boolean isRowAvailable() {
		return originalModel.isRowAvailable();
	}

	@Override
	public void setRowIndex(int rowIndex) {
		Object originalKey = null;
		if (rowIndex >= 0 &&  rowIndex < rowKeys.size()) {
			originalKey = rowKeys.get(rowIndex);
		}
		originalModel.setRowKey(originalKey);
	}

	@Override
	public void setWrappedData(Object data) {
		originalModel.setWrappedData(data);
	}

	@Override
	public SerializableDataModel getSerializableModel(Range range) {
		return originalModel.getSerializableModel(range);
	}

	@Override
	public void removeDataModelListener(DataModelListener listener) {
		originalModel.removeDataModelListener(listener);
	}

	public void modify(List<FilterField> filterFields, List<SortField2> sortFields) {
		if (sortNeeded || filterNeeded){
			if (var == null){
				throw new IllegalStateException("\"var\" model attribute cannot be null.");
			}
            int rowCount = originalModel.getRowCount();

            if (rowCount > 0) {
                rowKeys = new ArrayList<Object>(rowCount);
            } else {
                rowKeys = new ArrayList<Object>();
            }

            FacesContext context = FacesContext.getCurrentInstance();
            try {

                originalModel.walk(context, new DataVisitor() {
                    public void process(FacesContext context, Object rowKey,
                            Object argument) throws IOException {
                        originalModel.setRowKey(rowKey);
                        if (originalModel.isRowAvailable()) {
                            rowKeys.add(rowKey);
                        }
                    }
                }, new SequenceRange(0, -1),
                null);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }

            filter(filterFields);
            filterNeeded = false;
            sort(sortFields);
            sortNeeded = false;
        }
	}

	public void resetSort(){
		this.sortNeeded = true;
	}

	public void resetFilter(){
		this.filterNeeded = true;
	}

	public void setVar(String var){
		this.var = var;
	}

	protected List<Object> filter(List<FilterField> filterFields) {
		if (filterFields != null && !filterFields.isEmpty()) {
			FacesContext context = FacesContext.getCurrentInstance();
			List <Object> filteredCollection = new ArrayList<Object>();
			ObjectWrapperFactory wrapperFactory = new RowKeyWrapperFactory(context, var, filterFields);

			WrappedBeanFilter wrappedBeanFilter = new WrappedBeanFilter(filterFields, locale);
			wrapperFactory.wrapList(rowKeys);
			for (Object object : rowKeys) {
				if(wrappedBeanFilter.accept((JavaBeanWrapper)object)) {
					filteredCollection.add(object);
				}
			}
			rowKeys = filteredCollection;
			wrapperFactory.unwrapList(rowKeys);
		}
		return rowKeys;
	}

	protected void sort(List<SortField2> sortFields) {
		if (sortFields != null && !sortFields.isEmpty()) {
			FacesContext context = FacesContext.getCurrentInstance();
			ObjectWrapperFactory wrapperFactory = new RowKeyWrapperFactory(
					context, var, sortFields);

			WrappedBeanComparator2 wrappedBeanComparator = new WrappedBeanComparator2(
				sortFields, locale);
			wrapperFactory.wrapList(rowKeys);
			Collections.sort(rowKeys, wrappedBeanComparator);
			wrapperFactory.unwrapList(rowKeys);
		}
	}
}
