package org.pavanecce.common.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.ocl.expressions.CollectionKind;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.pavanecce.common.text.filegeneration.TextFileGenerator;
import org.pavanecce.uml.common.util.UmlResourceSetFactory;
import org.pavanecce.uml.uml2code.AdaptableFileLocator;
import org.pavanecce.uml.uml2code.codemodel.CodeModelBuilder;
import org.pavanecce.uml.uml2code.codemodel.UmlCodeModelVisitorAdaptor;
import org.pavanecce.uml.uml2code.java.AssociationCollectionCodeDecorator;
import org.pavanecce.uml.uml2code.java.JavaCodeGenerator;
import org.pavanecce.uml.uml2code.jpa.AbstractJavaCodeDecorator;

public class ConstructionCaseExample extends AbstractJavaCompilingTest{
	private ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
	protected CodeModelBuilder builder;
	private Model model;
	private UmlCodeModelVisitorAdaptor adaptor;
	protected UmlResourceSetFactory resourceSetFactory = new UmlResourceSetFactory(new AdaptableFileLocator());
	private String testName;

	public ConstructionCaseExample(String name){
		this.testName=name;
	}
	private void createOneToMany(Class from, Class to, CollectionKind collectionKind, AggregationKind aggregationKind) {
		createAssociation(from, to, collectionKind, aggregationKind, 1, -1);
	}

	private void createManyToMany(Class from, Class to, CollectionKind collectionKind) {
		createAssociation(from, to, collectionKind, AggregationKind.NONE_LITERAL, -1, -1);
	}

	private void createOneToOne(Class from, Class to, AggregationKind aggregationKind) {
		createAssociation(from, to, CollectionKind.SET_LITERAL, aggregationKind, 1, 1);
	}

	protected void createAssociation(Class from, Class to, CollectionKind collectionKind, AggregationKind aggregationKind, int fromMultiplicity,
			int toMultiplicity) {
		Association ass = (Association) from.getPackage().createOwnedType(from.getName() + to.getName(), UMLPackage.eINSTANCE.getAssociation());
		Property toEnd = ass.createNavigableOwnedEnd(NameConverter.decapitalize(to.getName()), to);
		toEnd.setUpper(toMultiplicity);
		toEnd.setAggregation(aggregationKind);
		toEnd.setIsUnique(collectionKind == CollectionKind.SET_LITERAL || collectionKind == CollectionKind.ORDERED_SET_LITERAL);
		toEnd.setIsOrdered(collectionKind == CollectionKind.SEQUENCE_LITERAL || collectionKind == CollectionKind.ORDERED_SET_LITERAL);
		Property fromEnd = ass.createNavigableOwnedEnd(NameConverter.decapitalize(from.getName()), from);
		fromEnd.setUpper(fromMultiplicity);
	}
	public void setup(JavaCodeGenerator codeGenerator, AbstractJavaCodeDecorator ... decorators) throws Exception {
		setup(new CodeModelBuilder(true),codeGenerator,decorators);
	}
	public void setup(CodeModelBuilder codeModelBuilder, JavaCodeGenerator codeGenerator, AbstractJavaCodeDecorator ... decorators) throws Exception {
		this.builder = codeModelBuilder;
		oldContextClassLoader = Thread.currentThread().getContextClassLoader();
		super.setup();
		super.setJavaCodeGenerator(codeGenerator);
		getJavaCodeGenerator().addDecorator(new AssociationCollectionCodeDecorator());
		for (AbstractJavaCodeDecorator cd : decorators) {
			getJavaCodeGenerator().addDecorator(cd);
		}
		setAdaptor(new UmlCodeModelVisitorAdaptor());
		ResourceSet rst = resourceSetFactory.prepareResourceSet();
		Resource r = rst.createResource(URI.createFileURI("tmp.uml"));
		setModel(UMLFactory.eINSTANCE.createModel());
		getModel().setName("test");
		r.getContents().add(getModel());
		Class constructionCase = (Class) getModel().createOwnedType("ConstructionCase", UMLPackage.eINSTANCE.getClass_());
		Model primitiveTypes = (Model) rst.getResource(URI.createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI), true).getContents().get(0);
		constructionCase.createOwnedAttribute("name", primitiveTypes.getOwnedType("String"));
		Class house = (Class) getModel().createOwnedType("House", UMLPackage.eINSTANCE.getClass_());
		createOneToOne(constructionCase, house, AggregationKind.COMPOSITE_LITERAL);
		Class housePlan = (Class) getModel().createOwnedType("HousePlan", UMLPackage.eINSTANCE.getClass_());
		createOneToOne(constructionCase, housePlan, AggregationKind.COMPOSITE_LITERAL);
		Class wall = (Class) getModel().createOwnedType("Wall", UMLPackage.eINSTANCE.getClass_());
		Class wallPlan = (Class) getModel().createOwnedType("WallPlan", UMLPackage.eINSTANCE.getClass_());
		createOneToMany(house, wall, CollectionKind.SET_LITERAL, AggregationKind.COMPOSITE_LITERAL);
		createOneToMany(housePlan, wallPlan, CollectionKind.SET_LITERAL, AggregationKind.COMPOSITE_LITERAL);
		Class roomPlan = (Class) getModel().createOwnedType("RoomPlan", UMLPackage.eINSTANCE.getClass_());
		createOneToMany(housePlan, roomPlan, CollectionKind.SET_LITERAL, AggregationKind.COMPOSITE_LITERAL);
		createManyToMany(roomPlan, wallPlan, CollectionKind.SET_LITERAL);
		this.getAdaptor().startVisiting(builder, getModel());
		TextFileGenerator textWorkspace = super.generateJava(this.getAdaptor().getCodeModel());
		setClassLoader(super.compile(textWorkspace.getNewFiles()));
	}

	@Override
	protected String getTestName() {
		return this.testName;
	}
	public void after() {
		Thread.currentThread().setContextClassLoader(oldContextClassLoader);
	}
	public UmlCodeModelVisitorAdaptor getAdaptor() {
		return adaptor;
	}
	public void setAdaptor(UmlCodeModelVisitorAdaptor adaptor) {
		this.adaptor = adaptor;
	}
	public Model getModel() {
		return model;
	}
	public void setModel(Model model) {
		this.model = model;
	}



	
}