package org.pavanecce.cmmn.jbpm.flow;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.workflow.core.node.WorkItemNode;

public class TaskNode extends WorkItemNode implements TaskDefinition {
	private static final long serialVersionUID = 2502972573721493216L;
	private String elementId;
	private List<CaseParameter> inputParameters = new ArrayList<CaseParameter>();
	private List<CaseParameter> outputParameters = new ArrayList<CaseParameter>();

	@Override
	public String getElementId() {
		return elementId;
	}

	public void setElementId(String elementId) {
		this.elementId = elementId;
	}

	public void addOutputParameter(CaseParameter cp) {
		this.outputParameters.add(cp);
	}

	public void addInputParameter(CaseParameter cp) {
		this.inputParameters.add(cp);
	}

	@Override
	public List<CaseParameter> getOutputs() {
		return outputParameters;
	}

	@Override
	public List<CaseParameter> getInputs() {
		return inputParameters;
	}
}
