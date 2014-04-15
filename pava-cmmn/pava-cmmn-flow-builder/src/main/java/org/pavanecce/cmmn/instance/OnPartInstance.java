package org.pavanecce.cmmn.instance;

import java.util.Stack;

import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.jbpm.workflow.instance.node.EventNodeInstance;
import org.pavanecce.cmmn.flow.CaseFileItemOnPart;
import org.pavanecce.cmmn.flow.OnPart;
import org.pavanecce.cmmn.flow.PlanItemOnPart;

public class OnPartInstance extends EventNodeInstance {
	public OnPart getOnPart() {
		return (OnPart) getEventNode();
	}

	public void signalEvent(String type, Object event) {
		if (event instanceof CaseFileItemEvent && getOnPart() instanceof CaseFileItemOnPart) {
			CaseFileItemOnPart onPart = (CaseFileItemOnPart) getOnPart();
			CaseFileItemEvent e = (CaseFileItemEvent) event;
			if (onPart.getCaseFileItem().getName().equals(e.getCaseFileItemName()) && e.getTransition()==onPart.getStandardEvent()) {
				getEventStack().push(((CaseFileItemEvent) event).getValue());
				triggerCompleted();
			}
		}else if(event instanceof PlanItemEvent && getOnPart() instanceof PlanItemOnPart){
			PlanItemOnPart onPart=(PlanItemOnPart) getOnPart();
			PlanItemEvent e = (PlanItemEvent) event;
			if(e.getCaseFileItemName().equals(onPart.getPlanItem().getName())&& e.getTransition()==onPart.getStandardEvent()){
				getEventStack().push(((PlanItemEvent) event).getValue());
				triggerCompleted();
			}
		}
	}

	public Object popEvent() {
		return getEventStack().pop();
	}

	private Stack<Object> getEventStack() {
		String variableName = getOnPart().getVariableName();
		VariableScopeInstance variableScopeInstance = (VariableScopeInstance) resolveContextInstance(VariableScope.VARIABLE_SCOPE, getOnPart()
				.getVariableName());
		if (variableScopeInstance == null) {
			throw new IllegalArgumentException("Could not find variable for event node: " + variableName);
		}
		Stack<Object> variable = (Stack<Object>) variableScopeInstance.getVariable(variableName);
		if (variable == null) {
			variableScopeInstance.setVariable(variableName, variable = new Stack<Object>());
		}
		return variable;
	}

	public void triggerCompleted() {
		((org.jbpm.workflow.instance.NodeInstanceContainer) getNodeInstanceContainer()).setCurrentLevel(getLevel());
		triggerCompleted(org.jbpm.workflow.core.Node.CONNECTION_DEFAULT_TYPE, false);
	}
}