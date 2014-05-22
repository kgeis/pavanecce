package org.pavanecce.cmmn.jbpm.lifecycle;

import org.drools.core.process.instance.WorkItem;
import org.pavanecce.cmmn.jbpm.flow.PlanItemTransition;

public interface PlanElementLifecycleWithTask extends PlanElementLifecycle {
	String TRANSITION = "Transition";
	String TASK = "Task";
	String TASK_TRANSITION = "TaskStatus";
	String WORK_ITEM_ID = "WorkItemId";
	String WORK_ITEM_UPDATED = "workItemUpdated";
	String TASK_NODE_NAME = "NodeName";
	String COMMENT = "Comment";
	String PARENT_WORK_ITEM_ID = "ParentId";
	String UPDATE_TASK_STATUS = "UpdateTaskStatusHandler";
	String ACTUAL_OWNER = "ActualOwner";

	WorkItem getWorkItem();

	void reactivate();

	void complete();

	void triggerTransitionOnTask(PlanItemTransition transition);

	void fault();

	Object getTask();

	long getWorkItemId();

	String getHumanTaskName();
}
