package org.pavanecce.cmmn.jbpm.instance;

import static org.pavanecce.cmmn.jbpm.flow.PlanItemTransition.*;

import org.pavanecce.cmmn.jbpm.event.PlanItemEvent;
import org.pavanecce.cmmn.jbpm.flow.OnPart;
import org.pavanecce.cmmn.jbpm.flow.PlanItemTransition;

public enum PlanElementState {
	AVAILABLE() {
		@Override
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[0];
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] { SUSPEND, TERMINATE, PARENT_SUSPEND, PARENT_TERMINATE, OCCUR };
			} else {
				return new PlanItemTransition[] { ENABLE, START, SUSPEND, PARENT_SUSPEND, PARENT_TERMINATE, EXIT };
			}
		}

	},
	SUSPENDED() {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[] { REACTIVATE, CLOSE };
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] { TERMINATE, PARENT_TERMINATE, RESUME };
			} else {
				return new PlanItemTransition[] { PARENT_RESUME, RESUME, EXIT };
			}
		}
	},
	COMPLETED() {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[] { REACTIVATE, CLOSE };
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else {
				return new PlanItemTransition[] {};
			}
		}
	},
	TERMINATED() {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[] { REACTIVATE, CLOSE };
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else {
				return new PlanItemTransition[] {};
			}
		}
	},
	CLOSED() {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			return new PlanItemTransition[] {};
		}
	},
	FAILED() {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[] { REACTIVATE, CLOSE };
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else {
				return new PlanItemTransition[] { REACTIVATE, EXIT };
			}
		}
	},
	ACTIVE() {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[] { COMPLETE, TERMINATE, FAULT, SUSPEND };
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else {
				return new PlanItemTransition[] { SUSPEND, PARENT_SUSPEND, EXIT, TERMINATE, COMPLETE, FAULT };
			}
		}
	},
	ENABLED() {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else {
				return new PlanItemTransition[] { DISABLE, MANUAL_START, PARENT_SUSPEND, TERMINATE, EXIT };
			}
		}

	},
	DISABLED {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			if (pii instanceof CaseInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else if (pii instanceof OccurrablePlanItemInstanceLifecycle) {
				return new PlanItemTransition[] {};
			} else {
				return new PlanItemTransition[] { REENABLE, PARENT_SUSPEND, EXIT };
			}
		}
	},
	NONE, INITIAL {
		public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
			return new PlanItemTransition[] { CREATE };
		}
	};
	private void validateTransition(PlanElementLifecycle pi, PlanItemTransition t) {
		for (PlanItemTransition pit : getSupportedTransitions(pi)) {
			if (t == pit) {
				return;
			}
		}
		throw new IllegalPlanItemStateException(this, t);
	}

	public final boolean isBusyState(PlanElementLifecycle pl) {
		if (pl instanceof CaseInstanceLifecycle) {
			return this == ACTIVE;
		} else if (isComplexLifecycle(pl)) {
			return this == ACTIVE || this == DISABLED || this == ENABLED || this == AVAILABLE;
		} else {
			return this == AVAILABLE;
		}
	}

	public void enable(ControllablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, ENABLE);
		signalEvent(pi, PlanItemTransition.ENABLE);
		pi.setPlanElementState(ENABLED);
	}

	private void signalEvent(PlanItemInstanceLifecycle<?> pi, PlanItemTransition transition) {
		String eventToTrigger = OnPart.getType(pi.getPlanItemName(), transition);
		Object eventObject = null;
		if (pi instanceof PlanElementLifecycleWithTask) {
			eventObject = ((PlanElementLifecycleWithTask) pi).getTask();
		}
		if (eventObject == null) {
			eventObject = new Object();
		}
		PlanItemEvent event = new PlanItemEvent(pi.getPlanItemName(), transition, eventObject);
		pi.getCaseInstance().signalEvent(eventToTrigger, event);
	}

	public void disable(ControllablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, DISABLE);
		signalEvent(pi, PlanItemTransition.DISABLE);
		pi.setPlanElementState(DISABLED);
	}

	public void reenable(ControllablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, REENABLE);
		signalEvent(pi, PlanItemTransition.REENABLE);
		pi.setPlanElementState(ENABLED);
	}

	public void start(ControllablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, START);
		signalEvent(pi, PlanItemTransition.START);
		pi.setPlanElementState(ACTIVE);
	}

	public void manualStart(ControllablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, MANUAL_START);
		signalEvent(pi, PlanItemTransition.MANUAL_START);
		pi.setPlanElementState(ACTIVE);
	}

	public void reactivate(PlanElementLifecycle pi) {
		validateTransition(pi, REACTIVATE);
		if (pi instanceof PlanItemInstanceLifecycle) {
			signalEvent((PlanItemInstanceLifecycle<?>) pi, PlanItemTransition.REACTIVATE);
		}
		if (pi instanceof CaseInstanceLifecycle) {
			if (pi.getPlanElementState() == SUSPENDED) {
				resumeChildren((PlanItemInstanceContainer) pi);
			} else {
				// TODO find out
			}
		}
		pi.setPlanElementState(ACTIVE);
	}

	public void suspend(PlanElementLifecycle pi) {
		validateTransition(pi, SUSPEND);
		if (pi instanceof PlanItemInstanceLifecycle) {
			signalEvent((PlanItemInstanceLifecycle<?>) pi, PlanItemTransition.SUSPEND);
		}
		pi.setPlanElementState(SUSPENDED);
		if (pi instanceof PlanItemInstanceContainer) {
			PlanItemInstanceContainer pic = (PlanItemInstanceContainer) pi;
			for (PlanItemInstanceLifecycle<?> child : pic.getChildren()) {
				if (child.getPlanElementState().isBusyState(child)) {
					if(isComplexLifecycle(child)){
						((PlanItemInstanceLifecycleWithHistory<?>) child).parentSuspend();
					}else if (child instanceof OccurrablePlanItemInstanceLifecycle) {
						((OccurrablePlanItemInstanceLifecycle<?>) child).suspend();
					}
				}
			}
		}
	}

	public void resume(PlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, RESUME);
		signalEvent(pi, PlanItemTransition.RESUME);
		if (pi instanceof ControllablePlanItemInstanceLifecycle) {
			pi.setPlanElementState(ACTIVE);
		} else {
			pi.setPlanElementState(AVAILABLE);
		}
		if (pi instanceof PlanItemInstanceContainer) {
			resumeChildren((PlanItemInstanceContainer) pi);
		}
	}

	public static void resumeChildren(PlanItemInstanceContainer pi) {
		for (PlanItemInstanceLifecycle<?> child : pi.getChildren()) {
			if (child.getPlanElementState() == SUSPENDED) {
				if (child instanceof OccurrablePlanItemInstanceLifecycle) {
					((OccurrablePlanItemInstanceLifecycle<?>) child).resume();
				} else if (isComplexLifecycle(child)) {
					((PlanItemInstanceLifecycleWithHistory<?>)child).parentResume();
				}
			}
		}
	}

	public void terminate(PlanElementLifecycle pi) {
		validateTransition(pi, TERMINATE);
		if (pi instanceof PlanItemInstanceLifecycle) {
			signalEvent((PlanItemInstanceLifecycle<?>) pi, PlanItemTransition.TERMINATE);
		}
		pi.setPlanElementState(TERMINATED);
		if (pi instanceof PlanItemInstanceContainer) {
			PlanItemInstanceContainer pi2 = (PlanItemInstanceContainer) pi;
			terminateChildren(pi2);
		}
	}

	public static void terminateChildren(PlanItemInstanceContainer pi2) {
		for (PlanItemInstanceLifecycle<?> child : pi2.getChildren()) {
			if (!child.getPlanElementState().isTerminalState()) {
				if (child instanceof OccurrablePlanItemInstanceLifecycle) {
					((OccurrablePlanItemInstanceLifecycle<?>) child).parentTerminate();
				} else if (child instanceof ControllablePlanItemInstanceLifecycle) {
					((ControllablePlanItemInstanceLifecycle<?>) child).exit();
				}
			}
		}
	}

	public void exit(ControllablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, EXIT);
		signalEvent(pi, PlanItemTransition.EXIT);
		pi.setPlanElementState(TERMINATED);
		PlanItemInstanceUtil.exitPlanItem(pi);
	}

	public void complete(PlanElementLifecycle pi) {
		validateTransition(pi, COMPLETE);
		if (pi instanceof PlanItemInstanceLifecycle) {
			signalEvent((PlanItemInstanceLifecycle<?>) pi, PlanItemTransition.COMPLETE);
		}
		pi.setPlanElementState(COMPLETED);
		pi.getCaseInstance().markSubscriptionsForUpdate();
	}

	public void parentSuspend(PlanItemInstanceLifecycleWithHistory<?> pi) {
		validateTransition(pi, PARENT_SUSPEND);
		signalEvent(pi, PlanItemTransition.PARENT_SUSPEND);
		pi.setLastBusyState(pi.getPlanElementState());
		pi.setPlanElementState(SUSPENDED);
	}

	public void parentResume(PlanItemInstanceLifecycleWithHistory<?> pi) {
		validateTransition(pi, PARENT_RESUME);
		signalEvent(pi, PlanItemTransition.PARENT_RESUME);
		pi.setPlanElementState(pi.getLastBusyState());
	}

	public void parentTerminate(OccurrablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, PARENT_TERMINATE);
		signalEvent(pi, PlanItemTransition.PARENT_TERMINATE);
		pi.setPlanElementState(TERMINATED);
	}

	public void create(PlanElementLifecycle pi) {
		validateTransition(pi, CREATE);
		if (pi instanceof PlanItemInstanceLifecycle) {
			signalEvent((PlanItemInstanceLifecycle<?>) pi, PlanItemTransition.CREATE);
		}
		pi.setPlanElementState(AVAILABLE);
	}

	public void create(PlanItemInstanceLifecycleWithHistory<?> pi) {
		PlanItemTransition transition = PlanItemTransition.CREATE;
		String eventToTrigger = OnPart.getType(pi.getPlanItem().getName(), transition);
		Object eventObject = null;
		if (eventObject == null) {
			eventObject = new Object();
		}
		PlanItemEvent event = new PlanItemEvent(pi.getPlanItem().getName(), transition, eventObject);
		pi.getCaseInstance().signalEvent(eventToTrigger, event);
		pi.setPlanElementState(AVAILABLE);
	}

	public void fault(PlanElementLifecycle pi) {
		validateTransition(pi, FAULT);
		if (pi instanceof PlanItemInstanceLifecycle) {
			signalEvent((PlanItemInstanceLifecycle<?>) pi, PlanItemTransition.FAULT);
		}
		pi.setPlanElementState(FAILED);
	}

	public void occur(OccurrablePlanItemInstanceLifecycle<?> pi) {
		validateTransition(pi, OCCUR);
		signalEvent(pi, PlanItemTransition.OCCUR);
		pi.setPlanElementState(COMPLETED);
	}

	public void close(CaseInstanceLifecycle pi) {
		validateTransition(pi, CLOSE);
		if (pi instanceof PlanItemInstanceLifecycle) {
			signalEvent((PlanItemInstanceLifecycle<?>) pi, PlanItemTransition.CLOSE);
		}
		pi.setPlanElementState(CLOSED);
	}

	public PlanItemTransition[] getSupportedTransitions(PlanElementLifecycle pii) {
		return new PlanItemTransition[0];
	}

	public boolean isTerminalState() {
		return this == CLOSED || this == COMPLETED || this == TERMINATED;
	}

	public boolean isSemiTerminalState(PlanElementLifecycle pe) {
		if (pe instanceof CaseInstanceLifecycle) {
			return this == DISABLED || this == FAILED || this == COMPLETED;
		} else {
			if (isComplexLifecycle(pe)) {
				return this == DISABLED || this == FAILED;
			} else {
				return false;
			}
		}
	}

	private static boolean isComplexLifecycle(PlanElementLifecycle pe) {
		if (pe instanceof PlanItemInstanceLifecycleWithHistory) {
			return ((PlanItemInstanceLifecycleWithHistory<?>) pe).isComplexLifecycle();
		}
		return false;
	}
}