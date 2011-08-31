package net.sf.orcc.cal;

import java.io.InputStream;
import java.util.List;

import junit.framework.Assert;
import net.sf.orcc.cal.cal.AstEntity;
import net.sf.orcc.cal.cal.AstVariable;
import net.sf.orcc.cal.util.Util;
import net.sf.orcc.frontend.Frontend;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.Entity;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Type;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.junit4.InjectWith;
import org.eclipse.xtext.junit4.XtextRunner;
import org.eclipse.xtext.junit4.util.ParseHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(CalInjectorProvider.class)
public class AllTests {

	private static final String prefix = "net/sf/orcc/cal/test/";

	private IFolder outputFolder;

	@Inject
	private ParseHelper<AstEntity> parser;

	private XtextResourceSet resourceSet;

	public AllTests() {
		resourceSet = new XtextResourceSet();
		resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL,
				Boolean.TRUE);

		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		outputFolder = root.getFolder(new Path("/net.sf.orcc.cal.tests/bin"));
	}

	/**
	 * Parses, validates, compiles, and interprets the actor defined in the file
	 * whose name is given. Then matches the output of the interpreter with the
	 * <code>expected</code> string.
	 * 
	 * @param expected
	 *            expected output
	 * @param name
	 *            name of a .cal file that contains an entity
	 */
	private void assertExecution(String expected, String name) {
		Entity entity = generateCode(name);
		Assert.assertNotNull("expected parsing, validation, and code "
				+ "generation to be correct for " + name, entity);

		TestInterpreter interpreter = new TestInterpreter((Actor) entity, null);
		interpreter.initialize();
		interpreter.schedule();

		String output = interpreter.getOutput();
		Assert.assertEquals("expected " + expected + ", got " + output,
				expected, output);
	}

	@Test
	public void checkInitialize() throws Exception {
		Assert.assertNotNull("expected correct actor with initialize action",
				parseAndValidate(prefix + "pass/InitializePattern.cal"));
	}

	@Test
	public void checkParam() throws Exception {
		Assert.assertNull(
				"assignment to an actor parameter must not be allowed",
				parseAndValidate(prefix + "xfail/Param.cal"));
	}

	@Test
	public void checkPattern1() throws Exception {
		Assert.assertNull(
				"reference to an output port in an input pattern must not be allowed",
				parseAndValidate(prefix + "xfail/Pattern1.cal"));
	}

	@Test
	public void checkPattern2() throws Exception {
		Assert.assertNull("an input pattern cannot contain expressions",
				parseAndValidate(prefix + "xfail/Pattern2.cal"));
	}

	@Test
	public void checkPattern3() throws Exception {
		Assert.assertNull(
				"combining Pattern1 and Pattern2 must be invalid code",
				parseAndValidate(prefix + "xfail/Pattern3.cal"));
	}

	@Test
	public void checkTypeCheck() throws Exception {
		Assert.assertNull(
				"passing a list in lieu of a scalar must raise a type error",
				parseAndValidate(prefix + "xfail/TypeCheck.cal"));
	}

	@Test
	public void checkTypeInt() throws Exception {
		AstEntity entity = parseAndValidate(prefix + "pass/TypeInt.cal");
		List<AstVariable> stateVars = entity.getActor().getStateVariables();
		AstVariable x = stateVars.get(0);
		AstVariable y = stateVars.get(1);
		Type type = Util.getType(x);
		Assert.assertTrue("type of x should be int(size=5)",
				EcoreUtil.equals(type, IrFactory.eINSTANCE.createTypeInt(5)));

		type = Util.getType(x.getValue());
		Assert.assertTrue("type of value of x should be int(size=4)",
				EcoreUtil.equals(type, IrFactory.eINSTANCE.createTypeInt(4)));

		type = Util.getType(y.getValue());
		Assert.assertTrue("type of value of y should be int(size=6)",
				EcoreUtil.equals(type, IrFactory.eINSTANCE.createTypeInt(6)));
	}

	@Test
	public void execShadow() throws Exception {
		assertExecution("x = 0", prefix + "pass/Shadowing.cal");
	}

	@Test
	public void execWhile() throws Exception {
		assertExecution("idx is 60", prefix + "pass/CodegenWhile.cal");
	}

	/**
	 * Parses, validates, and generates code for the entity defined in the file
	 * whose name is given.
	 * 
	 * @param name
	 *            name of a .cal file that contains an entity
	 * @return an IR entity if the file could be parsed, validated, and
	 *         translated to IR, otherwise <code>null</code>
	 */
	private Entity generateCode(String name) {
		AstEntity entity = parseAndValidate(name);
		if (entity == null) {
			return null;
		}

		Frontend frontend = new Frontend(outputFolder);
		return frontend.compile(entity);
	}

	/**
	 * Parses and validates the entity defined in the file whose name is given.
	 * 
	 * @param name
	 *            name of a .cal file that contains an entity
	 * @return an AST entity if the file could be parsed and validated,
	 *         otherwise <code>null</code>
	 */
	private AstEntity parseAndValidate(String name) {
		InputStream in = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(name);
		URI uri = URI.createPlatformResourceURI(name, true);
		AstEntity entity = parser.parse(in, uri, null, resourceSet);

		boolean isValid = true;

		// contains linking errors
		Resource resource = entity.eResource();
		List<Diagnostic> errors = resource.getErrors();
		if (!errors.isEmpty()) {
			for (Diagnostic error : errors) {
				System.err.println(error);
			}

			isValid = false;
		}

		// validates (unique names and CAL validator)
		IResourceValidator v = ((XtextResource) resource)
				.getResourceServiceProvider().getResourceValidator();
		List<Issue> issues = v.validate(resource, CheckMode.ALL,
				CancelIndicator.NullImpl);

		for (Issue issue : issues) {
			if (issue.getSeverity() == Severity.ERROR) {
				System.err.println(issue.toString());
				isValid = false;
			} else {
				System.out.println(issue.toString());
			}
		}

		return isValid ? entity : null;
	}

}