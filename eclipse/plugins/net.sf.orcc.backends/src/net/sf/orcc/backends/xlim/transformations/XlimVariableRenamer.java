/*
 * Copyright (c) 2011, IRISA
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
 *   * Neither the name of the IRISA nor the names of its
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
package net.sf.orcc.backends.xlim.transformations;

import java.util.Map;

import net.sf.orcc.backends.transformations.VariableRenamer;
import net.sf.orcc.backends.xlim.XlimActorTemplateData;
import net.sf.orcc.df.Action;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Pattern;
import net.sf.orcc.df.Port;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.Var;

/**
 * This class defines an extension of VariableRenamer to do specified treatment
 * with XLIM backend.
 * 
 * @author Herve Yviquel
 * 
 */
public class XlimVariableRenamer extends DfVisitor<Void> {

	private Action action;

	public XlimVariableRenamer() {
		this.irVisitor = new VariableRenamer();
	}

	@Override
	public Void casePattern(Pattern pattern) {
		for (Var var : pattern.getVariables()) {
			if (!action.getBody().getLocals().contains(var)) {
				var.setName(action.getName() + "_" + var.getName());
			}
		}
		return null;
	}

	@Override
	public Void caseAction(Action action) {
		this.action = action;
		return super.caseAction(action);
	}

	@Override
	public Void caseActor(Actor actor) {
		XlimActorTemplateData data = (XlimActorTemplateData) actor
				.getTemplateData();
		for (Action action : data.getCustomPeekedMapPerAction().keySet()) {
			Map<Port, Map<Integer, Var>> customPeekedMap = data
					.getCustomPeekedMapPerAction().get(action);
			for (Map<Integer, Var> indexToVarMap : customPeekedMap.values()) {
				for (Var var : indexToVarMap.values()) {
					var.setName(action.getName() + "_" + var.getName());
				}
			}
		}
		return super.caseActor(actor);
	}

}
