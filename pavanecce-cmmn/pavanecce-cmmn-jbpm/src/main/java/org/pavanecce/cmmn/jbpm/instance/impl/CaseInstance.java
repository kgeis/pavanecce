package org.pavanecce.cmmn.jbpm.instance.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.core.WorkItemHandlerNotFoundException;
import org.drools.core.process.instance.WorkItem;
import org.drools.core.process.instance.WorkItemManager;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.drools.core.spi.ProcessContext;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.services.task.wih.util.PeopleAssignmentHelper;
import org.jbpm.workflow.instance.NodeInstanceContainer;
import org.kie.api.definition.process.Node;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.task.model.Task;
import org.pavanecce.cmmn.jbpm.event.SubscriptionManager;
import org.pavanecce.cmmn.jbpm.flow.Case;
import org.pavanecce.cmmn.jbpm.flow.CaseFileItem;
import org.pavanecce.cmmn.jbpm.flow.CaseFileItemOnPart;
import org.pavanecce.cmmn.jbpm.flow.CaseParameter;
import org.pavanecce.cmmn.jbpm.flow.DefaultJoin;
import org.pavanecce.cmmn.jbpm.flow.PlanItem;
import org.pavanecce.cmmn.jbpm.flow.PlanItemContainer;
import org.pavanecce.cmmn.jbpm.flow.PlanItemDefinition;
import org.pavanecce.cmmn.jbpm.flow.PlanItemTransition;
import org.pavanecce.cmmn.jbpm.flow.TableItem;
import org.pavanecce.cmmn.jbpm.flow.TaskDefinition;
import org.pavanecce.cmmn.jbpm.infra.OnPartInstanceSubscription;
import org.pavanecce.cmmn.jbpm.instance.CaseInstanceLifecycle;
import org.pavanecce.cmmn.jbpm.instance.ControllablePlanItemInstanceLifecycle;
import org.pavanecce.cmmn.jbpm.instance.PlanElementLifecycleWithTask;
import org.pavanecce.cmmn.jbpm.instance.PlanElementState;
import org.pavanecce.cmmn.jbpm.instance.PlanItemInstanceContainer;
import org.pavanecce.cmmn.jbpm.instance.PlanItemInstanceLifecycle;
import org.pavanecce.common.ObjectPersistence;

public class CaseInstance extends RuleFlowProcessInstance implements PlanItemInstanceContainer, CaseInstanceLifecycle {
	private static final long serialVersionUID = 8715128915363796623L;
	private boolean shouldUpdateSubscriptions;
	private transient int signalCount = 0;
	private PlanElementState planElementState = PlanElementState.ACTIVE;
	private long workItemId = -1;
	transient private WorkItem workItem;

	public Case getCase() {
		return (Case) getProcess();
	}

	public void markSubscriptionsForUpdate() {
		this.shouldUpdateSubscriptions = true;
	}

	@Override
	public org.jbpm.workflow.instance.NodeInstance getNodeInstance(Node node) {
		if (node instanceof DefaultJoin) {
			System.out.println();
		}
		return super.getNodeInstance(node);
	}

	@Override
	public void signalEvent(String type, Object event) {
		signalCount++;
		if (type.equals("workItemUpdated") && isMyWorkItem((WorkItem) event)) {
			this.workItem = (WorkItem) event;
			PlanItemTransition transition = (PlanItemTransition) workItem.getResult(PlanElementLifecycleWithTask.TRANSITION);
			if (getPlanElementState().isTerminalState() && transition == PlanItemTransition.TERMINATE) {
				System.out.println("ignore - called from task service: " + TRANSITION);
			} else if (transition == PlanItemTransition.COMPLETE) {
				if (canComplete()) {
					transition.invokeOn(this);
				} else {
					// TODO what now?
				}
			} else {
				transition.invokeOn(this);
			}
		} else {
			super.signalEvent(type, event);
		}
		signalCount--;
		if (shouldUpdateSubscriptions && signalCount == 0) {
			updateSubscriptions();
		}
	}

	public org.jbpm.workflow.instance.NodeInstance getFirstNodeInstance(final long nodeId) {
		//level logic not relevant.
		for (NodeInstance ni : this.getNodeInstances()) {
			if(ni.getNodeId()==nodeId){
				return (org.jbpm.workflow.instance.NodeInstance) ni;
			}
		}
		return null;
	}

	protected boolean isMyWorkItem(WorkItem event) {
		return event.getId() == getWorkItemId() || (getWorkItemId() == -1 && getWorkItem().getId() == (event.getId()));
	}

	protected WorkItem createWorkItem() {
		workItem = new WorkItemImpl();
		((WorkItem) workItem).setName("Human Task");
		((WorkItem) workItem).setProcessInstanceId(getId());
		((WorkItem) workItem).setParameters(new HashMap<String, Object>());
		((WorkItem) workItem).setParameter("planningTable", "");// TODO
		((WorkItem) workItem).setParameter("NodeName", getCase().getName());
		if (getInitiator() != null) {
			((WorkItem) workItem).setParameter(Case.INITIATOR, getInitiator());
		}
		if (getCaseOwner() != null) {
			((WorkItem) workItem).setParameter(PeopleAssignmentHelper.ACTOR_ID, getCaseOwner());
			((WorkItem) workItem).setParameter(Case.CASE_OWNER, getCaseOwner());
		} else {
			((WorkItem) workItem).setParameter(Case.CASE_OWNER, TableItem.getPlannerRoles(this.getCase()));
		}
		((WorkItem) workItem).setParameter(PeopleAssignmentHelper.GROUP_ID, TableItem.getPlannerRoles(this.getCase()));
		((WorkItem) workItem).setParameter(PeopleAssignmentHelper.BUSINESSADMINISTRATOR_ID, TableItem.getPlannerRoles(this.getCase()));
		return workItem;
	}

	public String getCaseOwner() {
		return (String) getVariable(Case.CASE_OWNER);
	}

	public String getInitiator() {
		return (String) getVariable(Case.INITIATOR);
	}

	@Override
	public void start() {
		super.start();
		updateSubscriptions();
		if (workItemId < 0) {
			createWorkItem();
			String deploymentId = (String) getKnowledgeRuntime().getEnvironment().get("deploymentId");
			((WorkItem) workItem).setDeploymentId(deploymentId);
			try {
				((WorkItemManager) getKnowledgeRuntime().getWorkItemManager()).internalExecuteWorkItem((org.drools.core.process.instance.WorkItem) workItem);
			} catch (WorkItemHandlerNotFoundException wihnfe) {
				setState(ProcessInstance.STATE_ABORTED);
				throw wihnfe;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			this.workItemId = workItem.getId();
		}
	}

	@Override
	public String[] getEventTypes() {
		return super.getEventTypes();
	}

	protected void updateSubscriptions() {
		SubscriptionManager subscriptionManager = (SubscriptionManager) getKnowledgeRuntime().getEnvironment().get(SubscriptionManager.ENV_NAME);
		if (subscriptionManager != null) {
			CaseInstance caseInstance = this;
			ObjectPersistence persistence = subscriptionManager.getObjectPersistence(caseInstance);
			Map<CaseFileItem, Collection<Object>> parentSubscriptions = new HashMap<CaseFileItem, Collection<Object>>();
			Collection<Object> subscriptions = new HashSet<Object>();
			populateSubscriptionsActivatedByParameters(parentSubscriptions, subscriptions, getCase().getInputParameters());
			populateSubscriptionsActivatedByParameters(caseInstance, parentSubscriptions, subscriptions);
			subscriptionManager.updateSubscriptions(caseInstance, subscriptions, parentSubscriptions, persistence);
			subscriptionManager.flush(persistence);
		}
		shouldUpdateSubscriptions = false;

	}

	protected void populateSubscriptionsActivatedByParameters(NodeInstanceContainer caseInstance, Map<CaseFileItem, Collection<Object>> parentSubscriptions, Collection<Object> subscriptions) {
		Collection<NodeInstance> nodeInstances = caseInstance.getNodeInstances();
		for (NodeInstance ni : nodeInstances) {
			if (ni.getNode() instanceof PlanItem) {
				PlanItem<?> pi = (PlanItem<?>) ni.getNode();
				if (pi.getPlanInfo().getDefinition() instanceof TaskDefinition && ni instanceof ControllablePlanItemInstanceLifecycle && isSubscribing((ControllablePlanItemInstanceLifecycle<?>) ni)) {
					TaskDefinition td = (TaskDefinition) pi.getPlanInfo().getDefinition();
					populateSubscriptionsActivatedByParameters(parentSubscriptions, subscriptions, td.getOutputs());
				}
			}
			if (ni instanceof StagePlanItemInstance) {
				populateSubscriptionsActivatedByParameters((NodeInstanceContainer) ni, parentSubscriptions, subscriptions);
			}
		}
	}

	protected boolean isSubscribing(ControllablePlanItemInstanceLifecycle<?> ni) {
		return ni.getPlanElementState() == PlanElementState.ACTIVE || ni.getPlanElementState() == PlanElementState.ENABLED;
	}

	@SuppressWarnings("unchecked")
	protected void populateSubscriptionsActivatedByParameters(Map<CaseFileItem, Collection<Object>> parentSubscriptions, Collection<Object> subscriptions, List<CaseParameter> subscribingParameters) {
		for (CaseParameter caseParameter : subscribingParameters) {
			if (caseParameter.getBindingRefinementEvaluator() == null) {
				Object var = getVariable(caseParameter.getBoundVariable().getName());
				if (var instanceof Collection) {
					subscriptions.addAll((Collection<? extends Object>) var);
				} else if (var != null) {
					subscriptions.add(var);
				}
			} else {
				ProcessContext ctx = new ProcessContext(getKnowledgeRuntime());
				ctx.setProcessInstance(this);
				try {
					Object subscribeTo = caseParameter.getBindingRefinementEvaluator().evaluate(ctx);
					if ((subscribeTo instanceof Collection && ((Collection<?>) subscribeTo).isEmpty()) || subscribeTo == null) {
						// Nothing to subscribe to - subscribe to parent for CREATE and DELETE events
						Object parentToSubscribeTo = caseParameter.getBindingRefinementParentEvaluator().evaluate(ctx);
						if (parentToSubscribeTo != null) {
							Collection<Object> collection = parentSubscriptions.get(caseParameter.getBoundVariable());
							if (collection == null) {
								parentSubscriptions.put(caseParameter.getBoundVariable(), collection = new HashSet<Object>());
							}
							if (parentToSubscribeTo instanceof Collection) {
								collection.addAll((Collection<? extends Object>) parentToSubscribeTo);
							} else {
								collection.add(parentToSubscribeTo);
							}
						}
					} else if (subscribeTo instanceof Collection) {
						subscriptions.addAll((Collection<?>) subscribeTo);
					} else if (subscribeTo != null) {
						subscriptions.add(subscribeTo);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public Set<OnPartInstanceSubscription> findOnPartInstanceSubscriptions() {
		Set<CaseParameter> params = new HashSet<CaseParameter>();
		params.addAll(getCase().getInputParameters());
		addSubscribingCaseParameters(params, this);
		Collection<NodeInstance> nodes = getNodeInstances();
		Map<OnPartInstance, OnPartInstanceSubscription> onCaseFileItemParts = new HashMap<OnPartInstance, OnPartInstanceSubscription>();
		for (CaseParameter item : params) {
			findCaseFileItemOnPartsFor(item, nodes, onCaseFileItemParts);
		}
		return new HashSet<OnPartInstanceSubscription>(onCaseFileItemParts.values());

	}

	private void addSubscribingCaseParameters(Set<CaseParameter> params, NodeInstanceContainer caseInstance) {
		for (NodeInstance ni : caseInstance.getNodeInstances()) {
			if (ni.getNode() instanceof PlanItem) {
				PlanItem<?> pi = (PlanItem<?>) ni.getNode();
				PlanItemDefinition def = pi.getPlanInfo().getDefinition();
				if (def instanceof TaskDefinition) {
					params.addAll(((TaskDefinition) def).getOutputs());
				}
			} else if (ni instanceof StagePlanItemInstance) {
				addSubscribingCaseParameters(params, (NodeInstanceContainer) ni);
			}
		}
	}

	private void findCaseFileItemOnPartsFor(CaseParameter parameter, Collection<NodeInstance> nodes, Map<OnPartInstance, OnPartInstanceSubscription> target) {
		for (NodeInstance node : nodes) {
			if (node instanceof OnPartInstance) {
				OnPartInstance onPartInstance = (OnPartInstance) node;
				if (onPartInstance.getOnPart() instanceof CaseFileItemOnPart) {
					CaseFileItemOnPart onPart = (CaseFileItemOnPart) onPartInstance.getOnPart();
					if (onPart.getSourceCaseFileItem().getElementId().equals(parameter.getBoundVariable().getElementId())) {
						OnPartInstanceSubscription subscription = target.get(onPartInstance);
						if (subscription == null) {
							target.put(onPartInstance, new OnPartInstanceSubscription(getCase().getCaseKey(), getId(), onPart, parameter));
						} else {
							subscription.addParameter(parameter);
						}
					}
				}
			} else if (node instanceof StagePlanItemInstance) {
				findCaseFileItemOnPartsFor(parameter, ((StagePlanItemInstance) node).getNodeInstances(), target);
			}
		}
	}

	@Override
	public void setPlanElementState(PlanElementState s) {
		this.planElementState = s;
	}

	@Override
	public void reactivate() {
		planElementState.reactivate(this);
	}

	@Override
	public void suspend() {
		planElementState.suspend(this);
	}

	@Override
	public void terminate() {
		planElementState.terminate(this);
	}

	@Override
	public void complete() {
		planElementState.complete(this);
		((WorkItemManager) getKnowledgeRuntime().getWorkItemManager()).internalAbortWorkItem(getWorkItemId());
	}

	@Override
	public void create() {
		planElementState.create(this);
	}

	@Override
	public void fault() {
		planElementState.fault(this);
	}

	@Override
	public void close() {
		planElementState.close(this);
	}

	@Override
	public PlanElementState getPlanElementState() {
		return planElementState;
	}

	@Override
	public Collection<? extends PlanItemInstanceLifecycle<?>> getChildren() {
		Set<PlanItemInstanceLifecycle<?>> result = new HashSet<PlanItemInstanceLifecycle<?>>();
		for (NodeInstance nodeInstance : getNodeInstances()) {
			if (nodeInstance instanceof PlanItemInstanceLifecycle) {
				result.add((PlanItemInstanceLifecycle<?>) nodeInstance);
			}
		}
		return result;
	}

	@Override
	public CaseInstance getCaseInstance() {
		return this;
	}

	public WorkItem getWorkItem() {
		if (workItem == null && workItemId >= 0) {
			workItem = ((WorkItemManager) getKnowledgeRuntime().getWorkItemManager()).getWorkItem(workItemId);
		}
		return workItem;
	}

	@Override
	public Task getTask() {
		if (getWorkItem() != null) {
			return (Task) getWorkItem().getResult(PlanElementLifecycleWithTask.TASK);
		}
		return null;
	}

	@Override
	public long getWorkItemId() {
		return workItemId;
	}

	@Override
	public String getHumanTaskName() {
		return getCase().getName();
	}

	@Override
	public boolean canComplete() {
		return PlanItemInstanceContainerUtil.canComplete(this);
	}

	@Override
	public PlanItemContainer getPlanItemContainer() {
		return getCase();
	}

	public void setWorkItemId(long readLong) {
		this.workItemId = readLong;
	}

	public void setWorkItem(WorkItem w) {
		this.workItem = w;
		this.workItemId = w.getId();
	}

	public NodeInstance findNodeForWorkItem(long id) {
		for (NodeInstance ni : getNodeInstances()) {
			if (ni instanceof ControllablePlanItemInstanceLifecycle && ((ControllablePlanItemInstanceLifecycle<?>) ni).getWorkItemId() == id) {
				return ni;
			}
		}
		return null;
	}

	@Override
	public void setState(int state) {
		super.setState(state);
		if (state == STATE_SUSPENDED) {
			suspend();
		} else if (state == STATE_ACTIVE && (getPlanElementState().isSemiTerminalState(this) || getPlanElementState() == PlanElementState.SUSPENDED)) {
			reactivate();
		}
	}

}
