package org.pavanecce.cmmn;

import static org.kie.api.runtime.EnvironmentName.OBJECT_MARSHALLING_STRATEGIES;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.drools.core.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.drools.persistence.jpa.marshaller.JPAPlaceholderResolverStrategy;
import org.jbpm.marshalling.impl.ProcessInstanceResolverStrategy;
import org.jbpm.marshalling.impl.ProcessMarshallerRegistry;
import org.jbpm.process.instance.ProcessInstanceFactoryRegistry;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.jbpm.workflow.instance.impl.NodeInstanceFactoryRegistry;
import org.jbpm.workflow.instance.impl.factory.CreateNewNodeFactory;
import org.jbpm.workflow.instance.impl.factory.ReuseNodeFactory;
import org.junit.BeforeClass;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.pavanecce.cmmn.flow.Case;
import org.pavanecce.cmmn.flow.CaseFileItemDefinitionType;
import org.pavanecce.cmmn.flow.CaseFileItemOnPart;
import org.pavanecce.cmmn.flow.JoiningSentry;
import org.pavanecce.cmmn.flow.OnPart;
import org.pavanecce.cmmn.flow.PlanItem;
import org.pavanecce.cmmn.flow.PlanItemOnPart;
import org.pavanecce.cmmn.flow.Sentry;
import org.pavanecce.cmmn.flow.SimpleSentry;
import org.pavanecce.cmmn.flow.builder.DefaultTypeMap;
import org.pavanecce.cmmn.flow.builder.DefinitionsHandler;
import org.pavanecce.cmmn.instance.AbstractSubscriptionManager;
import org.pavanecce.cmmn.instance.CaseInstanceFactory;
import org.pavanecce.cmmn.instance.CaseInstanceMarshaller;
import org.pavanecce.cmmn.instance.ObjectPersistence;
import org.pavanecce.cmmn.instance.OnPartInstance;
import org.pavanecce.cmmn.instance.PlanItemInstance;
import org.pavanecce.cmmn.instance.SentryInstance;
import org.pavanecce.cmmn.instance.SubscriptionManager;
import org.pavanecce.cmmn.jpa.CollectionPlaceHolderResolveStrategy;
import org.pavanecce.cmmn.jpa.HibernateSubscriptionManager;
import org.pavanecce.cmmn.jpa.JpaObjectPersistence;
import org.pavanecce.cmmn.ocm.OcmCaseFileItemSubscriptionInfo;
import org.pavanecce.cmmn.ocm.OcmCaseSubscriptionInfo;
import org.pavanecce.cmmn.ocm.OcmCollectionPlaceHolderResolveStrategy;
import org.pavanecce.cmmn.ocm.OcmFactory;
import org.pavanecce.cmmn.ocm.OcmObjectPersistence;
import org.pavanecce.cmmn.ocm.OcmPlaceHolderResolveStrategy;
import org.pavanecce.cmmn.ocm.OcmSubscriptionManager;
import org.pavanecce.cmmn.test.domain.ConstructionCase;
import org.pavanecce.cmmn.test.domain.House;
import org.pavanecce.cmmn.test.domain.HousePlan;
import org.pavanecce.cmmn.test.domain.RoofPlan;
import org.pavanecce.cmmn.test.domain.Wall;
import org.pavanecce.cmmn.test.domain.WallPlan;
import org.pavanecce.cmmn.test.domain.WallQuote;

import bitronix.tm.resource.jdbc.PoolingDataSource;

public abstract class AbstrasctJbpmCaseBaseTestCase extends JbpmJUnitBaseTestCase {
	ObjectPersistence persistence;
	private boolean isJpa = false;
	private OcmFactory ocmFactory;

	public AbstrasctJbpmCaseBaseTestCase() {
		super();
	}

	public AbstrasctJbpmCaseBaseTestCase(boolean setupDataSource, boolean sessionPersistence) {
		super(setupDataSource, sessionPersistence);
	}

	public AbstrasctJbpmCaseBaseTestCase(boolean setupDataSource, boolean sessionPersistence, String persistenceUnitName) {
		super(setupDataSource, sessionPersistence, persistenceUnitName);
	}

	@BeforeClass
	public static void deleteJcrRepo() throws IOException {
		JcrTestCase.deleteTempRepo();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		getPersistence().close();
	}

	public ObjectPersistence getPersistence() {
		try {
			if (persistence == null) {
				if (isJpa) {
					persistence = new JpaObjectPersistence(getEmf());
				} else {
					OcmObjectPersistence ocmObjectPersistence = new OcmObjectPersistence(getOcmFactory());
					persistence = ocmObjectPersistence;
				}
			}
			return persistence;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Class<?>[] getClasses() {
		return new Class<?>[] { ConstructionCase.class, HousePlan.class, House.class, Wall.class, WallPlan.class, RoofPlan.class, WallQuote.class,
				OcmCaseSubscriptionInfo.class, OcmCaseFileItemSubscriptionInfo.class };
	}

	protected PoolingDataSource setupPoolingDataSource() {
		PoolingDataSource pds = new PoolingDataSource();
		pds.setClassName("org.h2.jdbcx.JdbcDataSource");
		pds.setUniqueName("jdbc/jbpm-ds");
		// pds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
		pds.setMaxPoolSize(5);
		pds.setAllowLocalTransactions(true);
		pds.getDriverProperties().put("user", "sa");
		pds.setApplyTransactionTimeout(false);
		pds.getDriverProperties().put("password", "");
		pds.getDriverProperties().put("URL", "jdbc:h2:mem:jbpm-db;MVCC=true");
		pds.init();
		return pds;
	}

	protected RuntimeManager createRuntimeManager(String... processFile) {
		DefinitionsHandler.registerTypeMap(CaseFileItemDefinitionType.UML_CLASS, new DefaultTypeMap());
		ProcessInstanceFactoryRegistry.INSTANCE.register(Case.class, new CaseInstanceFactory());
		CaseInstanceMarshaller m = new CaseInstanceMarshaller();
		ProcessMarshallerRegistry.INSTANCE.register(RuleFlowProcess.RULEFLOW_TYPE, m);
		RuntimeManager createRuntimeManager = super.createRuntimeManager(processFile);
		RuntimeEngine runtimeEngine = getRuntimeEngine();
		Environment env = runtimeEngine.getKieSession().getEnvironment();
		env.set(OBJECT_MARSHALLING_STRATEGIES, getPlaceholdStrategies(env));
		env.set(SubscriptionManager.ENV_NAME, getSubscriptionManager());
		if (!isJpa) {
			env.set(OcmFactory.OBJECT_CONTENT_MANAGER_FACTORY, getOcmFactory());
		}
		NodeInstanceFactoryRegistry nodeInstanceFactoryRegistry = NodeInstanceFactoryRegistry.getInstance(env);
		nodeInstanceFactoryRegistry.register(Sentry.class, new ReuseNodeFactory(SentryInstance.class));
		nodeInstanceFactoryRegistry.register(SimpleSentry.class, new ReuseNodeFactory(SentryInstance.class));
		nodeInstanceFactoryRegistry.register(JoiningSentry.class, new ReuseNodeFactory(SentryInstance.class));
		nodeInstanceFactoryRegistry.register(OnPart.class, new ReuseNodeFactory(OnPartInstance.class));
		nodeInstanceFactoryRegistry.register(CaseFileItemOnPart.class, new ReuseNodeFactory(OnPartInstance.class));
		nodeInstanceFactoryRegistry.register(PlanItemOnPart.class, new ReuseNodeFactory(OnPartInstance.class));
		nodeInstanceFactoryRegistry.register(PlanItem.class, new CreateNewNodeFactory(PlanItemInstance.class));
		return createRuntimeManager;
	}

	protected ObjectMarshallingStrategy[] getPlaceholdStrategies(Environment env) {
		if (isJpa) {
			return new ObjectMarshallingStrategy[] { new ProcessInstanceResolverStrategy(), new JPAPlaceholderResolverStrategy(env),
					new CollectionPlaceHolderResolveStrategy(env), new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT) };
		} else {
			return new ObjectMarshallingStrategy[] { new ProcessInstanceResolverStrategy(), new JPAPlaceholderResolverStrategy(env),
					new CollectionPlaceHolderResolveStrategy(env), new OcmPlaceHolderResolveStrategy(env), new OcmCollectionPlaceHolderResolveStrategy(env),
					new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT) };

		}
	}

	protected AbstractSubscriptionManager<?, ?> getSubscriptionManager() {
		if (isJpa) {
			return new HibernateSubscriptionManager();
		} else {
			return new OcmSubscriptionManager(getOcmFactory());
		}
	}

	@SuppressWarnings("rawtypes")
	protected OcmFactory getOcmFactory() {
		if (this.ocmFactory == null) {
			try {
				TransientRepository tr = new TransientRepository();
				Session session;
				session = tr.login(new SimpleCredentials("admin", "admin".toCharArray()));
				session.getRootNode().addNode("cases");
				session.getRootNode().addNode("subscriptions");
				CndImporter.registerNodeTypes(new InputStreamReader(JcrTestCase.class.getResourceAsStream("/META-INF/pava-definitions.cnd")), session);
				CndImporter.registerNodeTypes(new InputStreamReader(JcrTestCase.class.getResourceAsStream("/build.cnd")), session);
				session.save();
				session.logout();
				ocmFactory = new OcmFactory(tr, "admin", "admin", new AnnotationMapperImpl(Arrays.<Class> asList(getClasses())));
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return ocmFactory;
	}
}