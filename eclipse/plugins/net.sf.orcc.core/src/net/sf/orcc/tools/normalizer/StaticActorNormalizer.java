/*
 * Copyright (c) 2010, IETR/INSA of Rennes
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the IETR/INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orcc.tools.normalizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.orcc.ir.Action;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Node;
import net.sf.orcc.ir.NodeBlock;
import net.sf.orcc.ir.NodeWhile;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.Pattern;
import net.sf.orcc.ir.Port;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Tag;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.impl.IrFactoryImpl;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.moc.CSDFMoC;

/**
 * This class defines a normalizer for static actors.
 * 
 * @author Matthieu Wipliez
 * @author Jerome Gorin
 * 
 */
public class StaticActorNormalizer {

	/**
	 * This class contains code to transform a pattern to IR code (not entirely
	 * valid because not in SSA form at this point).
	 * 
	 * @author Matthieu Wipliez
	 * 
	 */
	private class MyPatternVisitor implements PatternVisitor {

		private int depth;

		private List<Var> indexes;

		private List<Node> nodes;

		public MyPatternVisitor(Procedure procedure) {
			nodes = procedure.getNodes();
			indexes = new ArrayList<Var>();
		}

		@Override
		public void visit(LoopPattern pattern) {
			final IrFactory factory = IrFactory.eINSTANCE;

			depth++;
			if (indexes.size() < depth) {
				Var varDef = factory.createVar(0, factory.createTypeBool(),
						"loop", true, depth - 1);
				variables.add(varDef);
				indexes.add(varDef);
			}

			Var loopVar = indexes.get(depth - 1);

			// init var
			NodeBlock block = IrUtil.getLast(nodes);
			InstAssign assign = factory.createInstAssign(loopVar,
					factory.createExprInt(0));
			block.add(assign);

			// create while
			List<Node> oldNodes = nodes;
			nodes = new ArrayList<Node>();

			NodeWhile nodeWhile = factory.createNodeWhile();
			nodeWhile.setJoinNode(factory.createNodeBlock());
			nodeWhile.getNodes().addAll(nodes);

			oldNodes.add(nodeWhile);

			// assign condition
			Expression condition = factory.createExprBinary(
					factory.createExprVar(loopVar), OpBinary.LT,
					factory.createExprInt(pattern.getNumIterations()),
					factory.createTypeBool());
			nodeWhile.setCondition(condition);

			// accept sub pattern
			pattern.getPattern().accept(this);

			// add assign
			block = IrUtil.getLast(nodes);
			assign = factory.createInstAssign(loopVar, factory
					.createExprBinary(factory.createExprVar(loopVar),
							OpBinary.PLUS, factory.createExprInt(1),
							factory.createTypeInt(32)));
			block.add(assign);

			// restore stuff
			this.nodes = oldNodes;
			depth--;
		}

		@Override
		public void visit(SequentialPattern pattern) {
			for (ExecutionPattern subPattern : pattern) {
				subPattern.accept(this);
			}
		}

		@Override
		public void visit(SimplePattern pattern) {
			NodeBlock block = IrUtil.getLast(nodes);
			Action action = pattern.getAction();

			// Call the action corresponding to the pattern
			InstCall call = IrFactory.eINSTANCE.createInstCall(null, pattern
					.getAction().getBody(), null);

			// Update variable counter index
			List<Instruction> indexIn = updateIndex(action.getInputPattern());
			List<Instruction> indexOut = updateIndex(action.getOutputPattern());

			// Add all instructions
			block.add(call);
			block.getInstructions().addAll(indexIn);
			block.getInstructions().addAll(indexOut);
		}

		private List<Instruction> updateIndex(Pattern pattern) {
			List<Instruction> instrs = new ArrayList<Instruction>();
			final IrFactory factory = IrFactory.eINSTANCE;

			for (Port port : pattern.getPorts()) {
				Integer tokens = pattern.getNumTokens(port);

				Var varCount = actor.getStateVar(port.getName() + "_count");

				InstStore store = factory.createInstStore(varCount, factory
						.createExprBinary(factory.createExprVar(varCount),
								OpBinary.PLUS, factory.createExprInt(tokens),
								factory.createTypeInt(32)));
				instrs.add(store);

			}

			return instrs;
		}

	}

	private static final String ACTION_NAME = "xxx";

	private static final String SCHEDULER_NAME = "isSchedulable_" + ACTION_NAME;

	private Actor actor;

	private CSDFMoC staticCls;

	private List<Var> variables;

	/**
	 * Creates a new normalizer
	 */
	public StaticActorNormalizer(Actor actor) {
		this.actor = actor;
		staticCls = (CSDFMoC) actor.getMoC();
	}

	/**
	 * Adds state variables to hold the values of data read/stored in the given
	 * pattern. Initializes count to 0.
	 * 
	 * @param procedure
	 *            body of the target action
	 * @param pattern
	 *            input or output pattern
	 */
	private void addStateVariables(Procedure procedure, Pattern pattern) {
		final IrFactory factory = IrFactory.eINSTANCE;

		NodeBlock block = procedure.getLast();
		for (Port port : pattern.getPorts()) {
			int numTokens = pattern.getNumTokens(port);

			Type type = factory.createTypeList(numTokens, port.getType());
			Var var = factory.createVar(0, type, port.getName(), true, true);
			actor.getStateVars().add(var);

			Var varCount = factory.createVar(0, factory.createTypeInt(32),
					port.getName() + "_count", true, factory.createExprInt(0));
			actor.getStateVars().add(varCount);

			InstStore store = factory.createInstStore(varCount,
					factory.createExprInt(0));
			block.add(store);
		}
	}

	/**
	 * Adds the given action to the actor and to its action scheduler.
	 * 
	 * @param action
	 *            an action
	 */
	private void addStaticAction(Action action) {
		actor.getActions().add(action);
		actor.getActionsOutsideFsm().add(action);
	}

	/**
	 * Cleans up the actor.
	 */
	private void cleanupActor() {
		// removes FSM
		actor.setFsm(null);

		// removes all actions from action scheduler
		actor.getActionsOutsideFsm().clear();

		// all action scheduler now just return true
		for (Action action : actor.getActions()) {
			Procedure scheduler = action.getScheduler();
			scheduler.getLocals().clear();

			List<Node> nodes = scheduler.getNodes();
			nodes.clear();

			NodeBlock block = IrFactoryImpl.eINSTANCE.createNodeBlock();
			nodes.add(block);
			block.add(IrFactory.eINSTANCE.createInstReturn(IrFactory.eINSTANCE
					.createExprBool(true)));
		}
	}

	/**
	 * Creates the static action for this actor.
	 * 
	 * @return a static action
	 */
	private Action createAction() {
		Procedure scheduler = createScheduler();
		Procedure body = createBody();

		Pattern input = staticCls.getInputPattern();
		Pattern output = staticCls.getOutputPattern();
		Tag tag = IrFactory.eINSTANCE.createTag(ACTION_NAME);

		return IrFactory.eINSTANCE.createAction(tag, input, output,
				IrFactory.eINSTANCE.createPattern(), scheduler, body);
	}

	/**
	 * Creates the body of the static action.
	 * 
	 * @return the body of the static action
	 */
	private Procedure createBody() {
		Procedure procedure = IrFactory.eINSTANCE.createProcedure(ACTION_NAME,
				0, IrFactory.eINSTANCE.createTypeVoid());

		variables = procedure.getLocals();

		// add state variables
		addStateVariables(procedure, staticCls.getInputPattern());
		addStateVariables(procedure, staticCls.getOutputPattern());

		// change accesses to FIFO
		new ChangeFifoArrayAccess().doSwitch(actor);

		// add read instructions for input pattern
		createReads(procedure);

		// finds a pattern in the actions and visit it
		LoopPatternRecognizer r = new LoopPatternRecognizer();
		ExecutionPattern pattern = r.getPattern(staticCls.getInvocations());
		System.out.println(pattern);
		pattern.accept(new MyPatternVisitor(procedure));

		// add write instructions for output pattern
		createWrites(procedure);

		return procedure;
	}

	/**
	 * Creates a return instruction that uses the results of the hasTokens tests
	 * previously created.
	 * 
	 * @param block
	 *            block to which return is to be added
	 */
	private void createInputCondition(NodeBlock block) {
		final IrFactory factory = IrFactory.eINSTANCE;

		Expression value;
		Iterator<Var> it = variables.iterator();
		if (it.hasNext()) {
			Var previous = it.next();
			value = factory.createExprVar(previous);

			while (it.hasNext()) {
				Var thisOne = it.next();
				value = factory.createExprBinary(value, OpBinary.LOGIC_AND,
						factory.createExprVar(thisOne),
						factory.createTypeBool());
				previous = thisOne;
			}
		} else {
			value = factory.createExprBool(true);
		}

		InstReturn returnInstr = factory.createInstReturn(value);
		block.add(returnInstr);
	}

	/**
	 * Creates calls to Read instructions.
	 * 
	 * @param procedure
	 *            the body of the static action being created
	 */
	private void createReads(Procedure procedure) {
		Pattern inputPattern = staticCls.getInputPattern();
		for (Port port : inputPattern.getPorts()) {
			int numTokens = inputPattern.getNumTokens(port);
			Var var = actor.getStateVar(port.getName());
			System.out.println("must read " + numTokens + " tokens from "
					+ var.getName());
		}
	}

	/**
	 * Creates an "isSchedulable" procedure for the static action of this actor.
	 * 
	 * @return an "isSchedulable" procedure
	 */
	private Procedure createScheduler() {
		Procedure procedure = IrFactory.eINSTANCE.createProcedure(
				SCHEDULER_NAME, 0, IrFactory.eINSTANCE.createTypeBool());

		variables = procedure.getLocals();

		NodeBlock block = IrFactoryImpl.eINSTANCE.createNodeBlock();
		procedure.getNodes().add(block);

		createInputCondition(block);

		return procedure;
	}

	/**
	 * Creates calls to Write instructions.
	 * 
	 * @param procedure
	 *            the body of the static action being created
	 */
	private void createWrites(Procedure procedure) {
		Pattern outputPattern = staticCls.getOutputPattern();
		for (Port port : outputPattern.getPorts()) {
			int numTokens = outputPattern.getNumTokens(port);
			Var var = actor.getStateVar(port.getName());
			System.out.println("must write " + numTokens + " tokens from "
					+ var.getName());
		}
	}

	/**
	 * Normalizes this actor so it fits the given static class.
	 * 
	 * @param staticCls
	 *            a static class
	 */
	public void normalize() {
		Action staticAction = createAction();
		cleanupActor();
		addStaticAction(staticAction);
	}

}
