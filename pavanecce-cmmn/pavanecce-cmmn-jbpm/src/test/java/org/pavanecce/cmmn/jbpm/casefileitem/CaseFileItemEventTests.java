package org.pavanecce.cmmn.jbpm.casefileitem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import org.junit.Test;
import org.pavanecce.cmmn.jbpm.AbstractConstructionTestCase;
import org.pavanecce.cmmn.jbpm.TaskParameters;
import org.pavanecce.cmmn.jbpm.lifecycle.impl.CaseInstance;

import test.ConstructionCase;
import test.House;
import test.HousePlan;
import test.RoofPlan;
import test.WallPlan;

public abstract class CaseFileItemEventTests extends AbstractConstructionTestCase {

	public CaseFileItemEventTests() {
		super(true, true, "org.jbpm.persistence.jpa");
	}

	protected void maybeStartSubscription() {

	}

	protected void endSubscription() {

	}

	@Test
	public void testCreationOfObjectInCollectionFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		// *****WHEN
		maybeStartSubscription();
		addWallPlanAsChildToHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenWallPlanCreated");
		assertNodeTriggered(caseInstance.getId(), "WaitingForWallPlanCreatedSentry");
	}

	@Test
	public void testCreationOfObjectInSingletonFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		// *****WHEN
		maybeStartSubscription();
		addRoofPlanAsChildToHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenRoofPlanCreated");
		assertNodeTriggered(caseInstance.getId(), "WaitingForRoofPlanCreatedSentry");
	}

	@Test
	public void testAddChildOfObjectInCollectionFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		// *****WHEN
		maybeStartSubscription();
		addWallPlanAsChildToHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenWallPlanAddedAsChild");
		assertNodeTriggered(caseInstance.getId(), "WaitingForWallPlanAddedAsChildSentry");
	}

	@Test
	public void testAddChildOfObjectInSingletonFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		// *****WHEN
		maybeStartSubscription();
		addRoofPlanAsChildToHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenRoofPlanAddedAsChild");
		assertNodeTriggered(caseInstance.getId(), "WaitingForRoofPlanAddedAsChildSentry");
	}

	@Test
	public void testAddReferenceOfObjectInCollectionFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addWallPlanAsChildToHousePlan();
		// *****WHEN
		maybeStartSubscription();
		addWallPlanAsReferenceToHouse();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenWallPlanAddedAsReference");
		assertNodeTriggered(caseInstance.getId(), "WaitingForWallPlanAddedAsReferenceSentry");
	}

	@Test
	public void testAddReferenceOfObjectInSingletonFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addRoofPlanAsChildToHousePlan();
		// *****WHEN

		maybeStartSubscription();
		addRoofPlanAsReferenceToHouse();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenRoofPlanAddedAsReference");
		assertNodeTriggered(caseInstance.getId(), "WaitingForRoofPlanAddedAsReferenceSentry");
	}

	@Test
	public void testDeletionOfObjectInCollectionFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addWallPlanAsChildToHousePlan();

		// *****WHEN
		maybeStartSubscription();
		removeWallPlansFromHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenWallPlanDeleted");
		assertNodeTriggered(caseInstance.getId(), "WaitingForWallPlanDeletedSentry");
	}

	protected void removeWallPlansFromHousePlan() {
		getPersistence().start();
		housePlan = getPersistence().find(HousePlan.class, housePlan.getId());
		Set<WallPlan> wallPlans = new HashSet<WallPlan>(housePlan.getWallPlans());
		for (WallPlan wallPlan : wallPlans) {
			getPersistence().remove(wallPlan);
		}
		housePlan.getWallPlans().clear();
		getPersistence().update(housePlan);
		getPersistence().commit();
	}

	@Test
	public void testDeletionOfObjectInSingletonFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addRoofPlanAsChildToHousePlan();
		// *****WHEN
		maybeStartSubscription();
		removeRoofPlanAsChildFromHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenRoofPlanDeleted");
		assertNodeTriggered(caseInstance.getId(), "WaitingForRoofPlanDeletedSentry");
	}

	@Test
	public void testRemoveChildOfObjectInCollectionFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addWallPlanAsChildToHousePlan();
		// *****WHEN
		maybeStartSubscription();
		removeWallPlansFromHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenWallPlanRemovedAsChild");
		assertNodeTriggered(caseInstance.getId(), "WaitingForWallPlanRemovedAsChildSentry");
	}

	@Test
	public void testRemoveChildOfObjectInSingletonFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addRoofPlanAsChildToHousePlan();
		// *****WHEN
		maybeStartSubscription();
		removeRoofPlanAsChildFromHousePlan();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenRoofPlanRemovedAsChild");
		assertNodeTriggered(caseInstance.getId(), "WaitingForRoofPlanRemovedAsChildSentry");
	}

	@Test
	public void testRemoveReferenceOfObjectInCollectionFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addWallPlanAsChildToHousePlan();
		addWallPlanAsReferenceToHouse();
		// *****WHEN
		maybeStartSubscription();
		removeWallPlansAsReferenceFromHouse();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenWallPlanRemovedAsReference");
		assertNodeTriggered(caseInstance.getId(), "WaitingForWallPlanRemovedAsReferenceSentry");
	}

	@Test
	public void testRemoveReferenceOfObjectInSingletonFileItem() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		addRoofPlanAsChildToHousePlan();
		addRoofPlanAsReferenceToHouse();

		// *****WHEN
		maybeStartSubscription();
		removeRoofPlanAsReferenceFromHouse();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenRoofPlanRemovedAsReference");
		assertNodeTriggered(caseInstance.getId(), "WaitingForRoofPlanRemovedAsReferenceSentry");
	}

	@Test
	public void testObjectUpdated() throws Exception {
		// *****GIVEN
		givenThatTheTestCaseIsStarted();
		// *****WHEN
		this.maybeStartSubscription();
		getPersistence().start();
		house = getPersistence().find(House.class, house.getId());
		house.setDescription("newDescription");
		getPersistence().update(house);
		getPersistence().commit();
		endSubscription();
		// *****THEN
		/*
		 * Verify Sentry Triggered: Sentries with a single OnPart are implemented merely as CatchLinkNodes
		 */
		assertNodeTriggered(caseInstance.getId(), "PlanItemEnteredWhenHouseUpdated");
		assertNodeTriggered(caseInstance.getId(), "WaitingForHouseUpdatedSentry");
	}

	protected void removeRoofPlanAsReferenceFromHouse() {
		getPersistence().start();
		house = getPersistence().find(House.class, house.getId());
		house.setRoofPlan(null);
		getPersistence().update(house);
		getPersistence().commit();
	}

	private void removeRoofPlanAsChildFromHousePlan() throws NamingException {
		getPersistence().start();
		housePlan = getPersistence().find(HousePlan.class, housePlan.getId());
		RoofPlan roofPlan = housePlan.getRoofPlan();
		housePlan.zz_internalSetRoofPlan(null);
		getPersistence().remove(roofPlan);
		getPersistence().update(housePlan);
		getPersistence().commit();
	}

	private void addWallPlanAsReferenceToHouse() throws Exception {
		getPersistence().start();
		this.house = getPersistence().find(House.class, house.getId());
		house.getWallPlans().addAll(housePlan.getWallPlans());
		getPersistence().update(house);
		getPersistence().commit();

	}

	private void addRoofPlanAsReferenceToHouse() {
		getPersistence().start();
		this.house = getPersistence().find(House.class, house.getId());
		house.setRoofPlan(this.housePlan.getRoofPlan());
		getPersistence().update(house);
		getPersistence().commit();
	}

	private void addRoofPlanAsChildToHousePlan() {
		getPersistence().start();
		housePlan = getPersistence().find(HousePlan.class, housePlan.getId());
		new RoofPlan(housePlan);
		getPersistence().update(housePlan);
		getPersistence().commit();

	}

	protected void givenThatTheTestCaseIsStarted() {
		createRuntimeManager("test/casefileitem/CaseFileItemEventTests.cmmn");
		Map<String, Object> params = new HashMap<String, Object>();
		getPersistence().start();
		ConstructionCase cc = new ConstructionCase("/cases/case1");
		housePlan = new HousePlan(cc);
		house = new House(cc);
		getPersistence().persist(cc);
		getPersistence().commit();
		params.put("housePlan", housePlan);
		params.put("house", house);
		params.put(TaskParameters.INITIATOR, "Spielman");
		getPersistence().start();
		caseInstance = (CaseInstance) getRuntimeEngine().getKieSession().startProcess("CaseFileItemEventTests", params);
		getPersistence().commit();
		assertProcessInstanceActive(caseInstance.getId(), getRuntimeEngine().getKieSession());
		assertNodeTriggered(caseInstance.getId(), "defaultSplit");
		getPersistence().start();
		assertNodeActive(caseInstance.getId(), getRuntimeEngine().getKieSession(), "OnWallPlanCreatedPart");
		assertNodeActive(caseInstance.getId(), getRuntimeEngine().getKieSession(), "OnRoofPlanCreatedPart");
		assertNodeActive(caseInstance.getId(), getRuntimeEngine().getKieSession(), "OnWallPlanAddedAsChildPart");
		assertNodeActive(caseInstance.getId(), getRuntimeEngine().getKieSession(), "OnRoofPlanAddedAsChildPart");
		assertNodeActive(caseInstance.getId(), getRuntimeEngine().getKieSession(), "OnWallPlanAddedAsReferencePart");
		assertNodeActive(caseInstance.getId(), getRuntimeEngine().getKieSession(), "OnRoofPlanAddedAsReferencePart");
		getPersistence().commit();
	}

	private void removeWallPlansAsReferenceFromHouse() {
		getPersistence().start();
		house = getPersistence().find(House.class, house.getId());
		house.getWallPlans().clear();
		getPersistence().update(house);
		getPersistence().commit();
	}

	private void addWallPlanAsChildToHousePlan() throws Exception {
		getPersistence().start();
		housePlan = getPersistence().find(HousePlan.class, housePlan.getId());
		new WallPlan(housePlan);
		getPersistence().update(housePlan);
		getPersistence().commit();
	}

}
