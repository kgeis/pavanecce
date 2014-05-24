package org.pavanecce.cmmn.jbpm.lifecycle;

import java.util.Map;
import java.util.Set;

import org.drools.core.process.instance.WorkItem;
import org.kie.api.runtime.process.NodeInstance;
import org.pavanecce.cmmn.jbpm.ApplicableDiscretionaryItem;
import org.pavanecce.cmmn.jbpm.flow.PlanningTable;

public interface PlanningTableContainer extends PlanElementLifecycleWithTask {
	PlanningTable getPlanningTable();

	PlanItemInstanceContainer getPlanItemInstanceCreator();

	ControllableItemInstanceLifecycle<?> ensurePlanItemCreated(String discretionaryItemId, WorkItem wi);

	void addApplicableItems(Map<String, ApplicableDiscretionaryItem> result, Set<String> roles);

	NodeInstance getPlanningContextNodeInstance();

	WorkItem executeWorkItem(WorkItem wu);

	WorkItem createPlannedItem(String tableItemId);
}
