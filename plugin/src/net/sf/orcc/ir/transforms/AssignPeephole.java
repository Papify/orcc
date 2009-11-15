/*
 * Copyright (c) 2009, IETR/INSA of Rennes
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
package net.sf.orcc.ir.transforms;

import java.util.ListIterator;

import net.sf.orcc.ir.CFGNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.LocalVariable;
import net.sf.orcc.ir.Location;
import net.sf.orcc.ir.Variable;
import net.sf.orcc.ir.expr.IntExpr;
import net.sf.orcc.ir.expr.VarExpr;
import net.sf.orcc.ir.instructions.Assign;
import net.sf.orcc.ir.instructions.PhiAssignment;
import net.sf.orcc.ir.nodes.BlockNode;
import net.sf.orcc.ir.nodes.IfNode;
import net.sf.orcc.ir.nodes.WhileNode;
import net.sf.orcc.util.OrderedMap;

/**
 * This class defines a transformation. But what does it do??
 * 
 * @author J�r�me Gorin
 * @author Matthieu Wipliez
 * 
 */
public class AssignPeephole extends AbstractActorTransformation {

	@Override
	@SuppressWarnings("unchecked")
	public void visit(Assign node, Object... args) {
		ListIterator<CFGNode> it = (ListIterator<CFGNode>) args[0];
		if ((node.getValue().getType() == Expression.BOOLEAN)
				|| (node.getValue().getType() == Expression.INT)
				|| (node.getValue().getType() == Expression.STRING)) {
			LocalVariable vardef = node.getTarget();
			vardef.setConstant(node.getValue());
			it.remove();
		} else if (node.getValue().getType() == Expression.VAR) {
			VarExpr expr = (VarExpr) node.getValue();

			// we can safely cast because in a VarExpr in an actor, only local
			// variables are used (globals must be load'ed first)
			LocalVariable other = (LocalVariable) expr.getVar().getVariable();
			node.setTarget(new LocalVariable(other));
			it.remove();
		}
	}

	@Override
	public Object visit(BlockNode node, Object... args) {
		for (Instruction instruction : node) {
			if (instruction instanceof PhiAssignment) {
				PhiAssignment phi = (PhiAssignment) instruction;
				LocalVariable source = (LocalVariable) phi.getVars().get(0)
						.getVariable();
				// if source is a local variable with index = 0, we remove
				// it
				// from the procedure and translate the PHI by an assignment
				// of
				// 0 (zero) to target.
				// Otherwise, we just create an assignment target = source.
				OrderedMap<Variable> parameters = procedure.getParameters();
				if (source.getIndex() == 0 && !parameters.contains(source)) {
					IntExpr expr = new IntExpr(new Location(), 0);
					source.setConstant(expr);
				}
			}
		}
		return null;
	}

	@Override
	public Object visit(IfNode node, Object... args) {
		visit(node.getThenNodes());
		visit(node.getElseNodes());
		visit(node.getJoinNode(), args);
		return null;
	}

	@Override
	public Object visit(WhileNode node, Object... args) {
		visit(node.getNodes());
		visit(node.getJoinNode(), args);
		return null;
	}

}
