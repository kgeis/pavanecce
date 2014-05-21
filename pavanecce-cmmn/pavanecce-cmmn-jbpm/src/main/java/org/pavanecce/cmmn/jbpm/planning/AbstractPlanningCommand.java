package org.pavanecce.cmmn.jbpm.planning;

import java.lang.reflect.Field;

import org.jbpm.services.task.commands.TaskCommand;
import org.jbpm.services.task.impl.TaskServiceEntryPointImpl;
import org.jbpm.shared.services.api.JbpmServicesPersistenceManager;

public abstract class AbstractPlanningCommand <T> extends TaskCommand<T> {

	private static final long serialVersionUID = -4217142163951701835L;
	protected JbpmServicesPersistenceManager pm;
	protected TaskServiceEntryPointImpl ts;
	public AbstractPlanningCommand() {
		super();
	}

	public void init(TaskServiceEntryPointImpl ts) {
		// TODO Won't work in CDI
		try {
			this.ts=ts;
			if (pm == null) {
				Field pmField = ts.getTaskAdminService().getClass().getDeclaredField("pm");
				pmField.setAccessible(true);
				pm = (JbpmServicesPersistenceManager) pmField.get(ts.getTaskAdminService());
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	protected long getWorkItemId(long parentTaskId) {
		return ts.getTaskById(parentTaskId).getTaskData().getWorkItemId();
	}
}