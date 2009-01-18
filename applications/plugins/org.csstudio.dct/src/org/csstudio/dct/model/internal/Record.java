package org.csstudio.dct.model.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.csstudio.dct.metamodel.IRecordDefinition;
import org.csstudio.dct.model.IContainer;
import org.csstudio.dct.model.IPrototype;
import org.csstudio.dct.model.IRecord;
import org.csstudio.dct.model.IVisitor;
import org.csstudio.dct.util.CompareUtil;

/**
 * Standard implementation of {@link IRecord}.
 * 
 * @author Sven Wende
 */
public final class Record extends AbstractPropertyContainer implements IRecord {
	
	private String type;
	private String epicsName;
	private IRecord parentRecord;
	private IContainer container;
	private Map<String, Object> fields = new HashMap<String, Object>();
	private List<IRecord> inheritingRecords = new ArrayList<IRecord>();

	/**
	 * Constructor.
	 * @param name the name
	 * @param type the type
	 * @param id the id
	 */
	public Record(String name, String type, UUID id) {
		super(name, id);
		this.type = type;
	}

	/**
	 * Constructor.
	 * @param parentRecord the parent record
	 * @param id the id
	 */
	public Record(IRecord parentRecord, UUID id) {
		super(null, id);
		this.parentRecord = parentRecord;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getType() {
		assert type != null || parentRecord != null : "type!=null || parentRecord!=null";
		return type != null ? type : parentRecord.getType();
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> getFinalProperties() {
		Map<String, String> result = new HashMap<String, String>();

		Stack<IRecord> stack = getRecordStack();

		// add the field values of the parent hierarchy, values can be overriden
		// by children
		while (!stack.isEmpty()) {
			IRecord top = stack.pop();
			result.putAll(top.getProperties());
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addField(String key, Object value) {
		fields.put(key, value);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getField(String key) {
		return fields.get(key);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeField(String key) {
		fields.remove(key);
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Object> getFields() {
		return fields;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Object> getFinalFields() {
		Map<String, Object> result = new HashMap<String, Object>();

		Stack<IRecord> stack = getRecordStack();

		// add the field values of the parent hierarchy, values can be overriden
		// by children
		while (!stack.isEmpty()) {
			IRecord top = stack.pop();
			result.putAll(top.getFields());
		}

		return result;
	}

	/**
	 *{@inheritDoc}
	 */
	public String getEpicsName() {
		return epicsName;
	}
	
	/**
	 *{@inheritDoc}
	 */
	public void setEpicsName(String epicsName) {
		this.epicsName = epicsName;
	}


	/**
	 *{@inheritDoc}
	 */
	public String getEpicsNameFromHierarchy() {
		String name = "unknown";

		Stack<IRecord> stack = getRecordStack();

		while (!stack.isEmpty()) {
			IRecord top = stack.pop();

			if (top.getEpicsName() != null && top.getEpicsName().length() > 0) {
				name = top.getEpicsName();
			}
		}

		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public IRecord getParentRecord() {
		return parentRecord;
	}

	/**
	 * {@inheritDoc}
	 */
	public IContainer getContainer() {
		return container;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setParentRecord(IRecord parentRecord) {
		this.parentRecord = parentRecord;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContainer(IContainer container) {
		this.container = container;
	}

	/**
	 *{@inheritDoc}
	 */
	public boolean isAbstract() {
		return getRootContainer(getContainer()) instanceof IPrototype;
	}
	
	/**
	 * Recursive helper method which determines the root container.
	 * 
	 * @param container a starting container
	 * 
	 * @return the root container of the specified starting container
	 */
	private IContainer getRootContainer(IContainer container) {
		if(container.getContainer()!=null) {
			return getRootContainer(container.getContainer());
		} else {
			return container;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isInherited() {
		boolean result = !(getParentRecord() instanceof BaseRecord);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addDependentRecord(IRecord record) {
		assert record != null;
		assert record.getParentRecord() == this : "Record must inherit from here.";
		inheritingRecords.add(record);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<IRecord> getDependentRecords() {
		return inheritingRecords;
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeDependentRecord(IRecord record) {
		assert record != null;
		assert record.getParentRecord() == this : "Record must inherit from here.";
		inheritingRecords.remove(record);
	}

	/**
	 * {@inheritDoc}
	 */
	public IRecordDefinition getRecordDefinition() {
		IRecord base = getRecordStack().pop();
		return base.getRecordDefinition();
	}

	/**
	 * {@inheritDoc}
	 */
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

	
	/**
	 *{@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
//		result = prime * result + ((fields == null) ? 0 : fields.hashCode());
//		result = prime * result + ((inheritingRecords == null) ? 0 : inheritingRecords.hashCode());
//		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/**
	 *{@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = false;

		if (obj instanceof Record) {
			Record record = (Record) obj;

			if (super.equals(obj)) {
				// .. type
				if (CompareUtil.equals(getType(), record.getType())) {
					// .. fields
					if (getFields().equals(record.getFields())) {
						// .. parent record id (we check the id only, to prevent
						// stack overflows)
						if (CompareUtil.idsEqual(getParentRecord(), record.getParentRecord())) {
							// .. container (we check the id only, to prevent
							// stack overflows)
							if (CompareUtil.idsEqual(getContainer(), record.getContainer())) {
								result = true;
							}
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Collect all parent records in a stack. On top of the returned stack is
	 * the parent that resides at the top of the hierarchy.
	 * 
	 * @return all parent records, including this
	 */
	private Stack<IRecord> getRecordStack() {
		Stack<IRecord> stack = new Stack<IRecord>();

		IRecord r = this;

		while (r != null) {
			stack.add(r);
			r = r.getParentRecord();
		}
		return stack;
	}
}
