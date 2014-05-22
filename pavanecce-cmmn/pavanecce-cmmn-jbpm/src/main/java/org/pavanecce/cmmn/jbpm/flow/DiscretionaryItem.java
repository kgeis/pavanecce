package org.pavanecce.cmmn.jbpm.flow;

import java.util.HashMap;
import java.util.Map.Entry;

import org.drools.core.process.core.ParameterDefinition;
import org.drools.core.process.core.Work;
import org.drools.core.process.core.impl.ParameterDefinitionImpl;
import org.drools.core.process.core.impl.WorkImpl;
import org.jbpm.services.task.wih.util.PeopleAssignmentHelper;

public class DiscretionaryItem<T extends PlanItemDefinition> extends TableItem implements TaskItemWithDefinition<T> {
	private static final long serialVersionUID = 2371336993789669482L;
	public static final String PLANNED = "Planned";
	public static final String DISCRETIONARY_ITEM_ID = "DiscretionaryItemId";
	private T definition;
	private String definitionRef;
	private PlanItemControl itemControl;
	private long id;
	private Work work;

	@Override
	public T getDefinition() {
		return definition;
	}

	@Override
	public String getName() {
		return getDefinition().getName();
	}

	public void setDefinition(T definition) {
		this.definition = definition;
	}

	public String getDefinitionRef() {
		return definitionRef;
	}

	public void setDefinitionRef(String definitionRef) {
		this.definitionRef = definitionRef;
	}

	@Override
	public PlanItemControl getItemControl() {
		return itemControl;
	}

	public void setItemControl(PlanItemControl itemControl) {
		this.itemControl = itemControl;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public Work getWork() {
		if (work == null) {
			work = new WorkImpl();
			Work sourceWork = null;
			if (getDefinition() instanceof TaskDefinition) {
				sourceWork = ((TaskDefinition) getDefinition()).getWork();
			} else if (getDefinition() instanceof Stage) {
				sourceWork = ((Stage) getDefinition()).getWork();
			}
			work.setName(sourceWork.getName());
			for (ParameterDefinition pd : sourceWork.getParameterDefinitions()) {
				work.addParameterDefinition(new ParameterDefinitionImpl(pd.getName(), pd.getType()));
			}
			for (Entry<String, Object> entry : sourceWork.getParameters().entrySet()) {
				work.setParameter(entry.getKey(), entry.getValue());
			}
			work.setParameter("NodeName", getName());
			work.setParameter(PeopleAssignmentHelper.GROUP_ID, TableItem.getPlannerRoles(this));
			work.setParameter(PeopleAssignmentHelper.BUSINESSADMINISTRATOR_ID, TableItem.getPlannerRoles(this));
		}
		return work;

	}
	public void copyFromPlanItem() {
		HashMap<Object, Object> copiedState = new HashMap<Object, Object>();
		T from = getDefinition();
		this.setNodeContainer(getParentTable().getFirstPlanItemContainer());
		copiedState.put(from, this);
		copy(copiedState, from, this);
	}

	@Override
	public PlanItemContainer getPlanItemContainer() {
		return getParentTable().getFirstPlanItemContainer();
	}

}
