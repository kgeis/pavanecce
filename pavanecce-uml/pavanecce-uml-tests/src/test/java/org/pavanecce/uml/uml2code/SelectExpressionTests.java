package org.pavanecce.uml.uml2code;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.LiteralUnlimitedNatural;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.junit.Test;
import org.pavanecce.common.code.metamodel.CodeClass;
import org.pavanecce.common.code.metamodel.CodeMethod;
import org.pavanecce.common.code.metamodel.expressions.PortableExpression;
import org.pavanecce.common.code.metamodel.statement.CodeForStatement;
import org.pavanecce.uml.uml2code.java.JavaCodeGenerator;

public class SelectExpressionTests extends AbstractOcl2CodeModelTest {
	JavaCodeGenerator jcg=new JavaCodeGenerator();
	@Test
	public void testPrimitiveLiterals() throws IOException {
		super.adaptor.startVisiting(builder, model);
		super.adaptor.startVisiting(oclCodeBuilder, model);
		CodeClass theClass = super.adaptor.getCodeModel().getDescendent("model","pkg2","TheClass");
		assertGetDefaultInteger(theClass);
	}

	protected void assertGetDefaultInteger(CodeClass theClass) throws IOException {
		CodeMethod getDefaultInteger = theClass.getMethod("getDefaultInteger", Collections.emptyList());
		assertTrue(getDefaultInteger.getResult() instanceof PortableExpression);
		CodeMethod any1 = theClass.getMethod("select1", Collections.emptyList());
		assertTrue(any1.getBody().getStatements().get(0) instanceof CodeForStatement);
		String methodBody = jcg.toMethodBody(any1);
		System.out.println(methodBody);
		BufferedReader getDefaultIntegerBody = new BufferedReader(new StringReader(methodBody));
		assertEquals("    Set<Integer> result = new HashSet<Integer>();", getDefaultIntegerBody.readLine());
		assertEquals("    for(Integer x : collectionLiteral0()){", getDefaultIntegerBody.readLine());
		assertEquals("      if(( ( x % 3 ) = 1 )){", getDefaultIntegerBody.readLine());
		assertEquals("        result.add( x );", getDefaultIntegerBody.readLine());
		assertEquals("      }", getDefaultIntegerBody.readLine());
		assertEquals("    }", getDefaultIntegerBody.readLine());
		assertEquals("    return result;", getDefaultIntegerBody.readLine());
	}

	@Override
	protected void populateTheClass(Class theClass) {
		Property defaultIntegers = theClass.getOwnedAttribute("defaultIntegers", library.getIntegerType(), false, UMLPackage.eINSTANCE.getProperty(), true);
		defaultIntegers.setUnlimitedNaturalDefaultValue(LiteralUnlimitedNatural.UNLIMITED);
		Property defaultInteger = theClass.getOwnedAttribute("defaultInteger", library.getIntegerType(), false, UMLPackage.eINSTANCE.getProperty(), true);
		OpaqueExpression oe3 = UMLFactory.eINSTANCE.createOpaqueExpression();
		oe3.getLanguages().add("OCL");
		oe3.getBodies().add("defaultIntegers->select(x|x.mod(3) = 1)");
		defaultInteger.setDefaultValue(oe3);
		defaultInteger.setIsDerived(true);
	}
}
