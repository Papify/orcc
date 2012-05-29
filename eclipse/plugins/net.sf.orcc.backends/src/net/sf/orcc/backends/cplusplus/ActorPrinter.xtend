/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
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
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
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
 
package net.sf.orcc.backends.cplusplus

import net.sf.orcc.df.Action
import net.sf.orcc.df.Actor
import net.sf.orcc.df.FSM
import net.sf.orcc.df.Instance
import net.sf.orcc.df.Port
import net.sf.orcc.df.State
import net.sf.orcc.df.Transition
import net.sf.orcc.ir.ArgByRef
import net.sf.orcc.ir.ArgByVal
import net.sf.orcc.ir.BlockBasic
import net.sf.orcc.ir.BlockIf
import net.sf.orcc.ir.BlockWhile
import net.sf.orcc.ir.Expression
import net.sf.orcc.ir.InstAssign
import net.sf.orcc.ir.InstCall
import net.sf.orcc.ir.InstLoad
import net.sf.orcc.ir.InstReturn
import net.sf.orcc.ir.InstStore
import net.sf.orcc.ir.Procedure
import net.sf.orcc.ir.Type
import net.sf.orcc.ir.TypeList
import net.sf.orcc.ir.Var

/*
 * An actor printer.
 *  
 * @author Ghislain Roquier
 * 
 */
	
	def compileInstance(Instance instance)  {
		var actor = instance.actor
		var connectedOutput = [ Port port | instance.outgoingPortMap.get(port) != null ]
		'''
		#ifndef __«instance.name.toUpperCase»_H__
		#define __«instance.name.toUpperCase»_H__
		
		#include <iostream>
		#include "actor.h"
		#include "port.h"
				
		«FOR proc : actor.procs.filter(p | p.native && !"print".equals(p.name)) SEPARATOR "\n"»«proc.compileNativeProc»«ENDFOR»
		
		class «instance.name»: public Actor {
		public:	
			«instance.name»() {
				«FOR arg : instance.arguments»«compileArg(arg.variable.type, arg.variable.indexedName, arg.value)»«ENDFOR»
				«FOR v : actor.stateVars.filter(v|v.initialValue != null)»«compileArg(v.type, v.indexedName, v.initialValue)»«ENDFOR»
				«IF actor.fsm != null»state_ = state_«actor.fsm.initialState.name»;«ENDIF»
			}
		
			«FOR port : actor.inputs»
			«IF instance.incomingPortMap.get(port) != null»
			«port.compilePort(instance.incomingPortMap.get(port).size)»
			«ELSE»
			NullPort<«port.type.doSwitch»> port_«port.name»;
			«ENDIF»
			«ENDFOR»			
			
			«FOR port : actor.outputs.filter(connectedOutput)»«port.compilePort(instance.outgoingPortMap.get(port).get(0).size, instance.outgoingPortMap.get(port).size)»«ENDFOR»
			
			«FOR proc : actor.procs.filter(p | !p.native) SEPARATOR "\n"»«proc.compileProcedure»«ENDFOR»
			
			«FOR action : actor.initializes SEPARATOR "\n"»«action.compileAction»«ENDFOR»
		
			void initialize() {
				«FOR action : actor.initializes SEPARATOR "\n"»«action.name»();«ENDFOR»
			}
		
			«FOR action : actor.actions SEPARATOR "\n"»«action.compileAction»«ENDFOR»
		
			«actor.compileScheduler»
			
		
		private:	
			«FOR param : actor.parameters SEPARATOR "\n"»«param.varDecl»;«ENDFOR»
			«FOR variable: actor.stateVars SEPARATOR "\n"»«variable.varDecl»;«ENDFOR»
			«FOR port : actor.inputs SEPARATOR "\n"»int status_«port.name»_;«ENDFOR»
			«FOR port : actor.outputs SEPARATOR "\n"»int status_«port.name»_;«ENDFOR»
			«IF actor.fsm != null»
			int state_;
			enum states {«FOR state : actor.fsm.states SEPARATOR ", "»state_«state.name»«ENDFOR»};
			«ENDIF»
		};
		#endif
	'''
	}
	
	def dispatch compileArg(Type type, String name, Expression expr) '''
		«name» = «expr.doSwitch»;
	'''

	def dispatch compileArg(TypeList type, String name, Expression expr) '''
		// C++11 allows array initializer in initialization list but not yet implemented in VS10... use that instead.
		«type.doSwitch» tmp_«name»«FOR dim:type.dimensions»[«dim»]«ENDFOR»= «expr.doSwitch»;
		memcpy(«name», tmp_«name», «FOR dim : type.dimensions SEPARATOR "*"»«dim»«ENDFOR»*sizeof(«type.innermostType.doSwitch»));
	'''
	
	override caseProcedure(Procedure procedure) '''
		«FOR variable : procedure.locals SEPARATOR "\n"»«variable.varDecl»;«ENDFOR»
		«FOR node : procedure.blocks»«node.doSwitch»«ENDFOR»
	'''

	override caseBlockBasic(BlockBasic node) '''
		«FOR inst : node.instructions»«inst.doSwitch»«ENDFOR»
	'''

	override caseBlockIf(BlockIf node) '''
		if(«node.condition.doSwitch») {
			«FOR then : node.thenBlocks»«then.doSwitch»«ENDFOR»	
		} «IF node.elseBlocks != null»else {
			«FOR els : node.elseBlocks»«els.doSwitch»«ENDFOR»
		}«ENDIF»
		«node.joinBlock.doSwitch»
	'''

	override caseBlockWhile(BlockWhile node) '''
		while(«node.condition.doSwitch») {
			«FOR whil : node.blocks»«whil.doSwitch»«ENDFOR»
		}
		«node.joinBlock.doSwitch»
	'''
	
	override caseInstAssign(InstAssign inst) '''
		«inst.target.variable.name» = «inst.value.doSwitch»;
	'''
	override caseInstCall(InstCall inst) {
	if(inst.print) {
	'''
		std::cout << «FOR arg : inst.parameters SEPARATOR " << "»«arg.compileArg»«ENDFOR»;
	'''
	} else {
	'''
		«IF inst.target!=null»«inst.target.variable.indexedName» = «ENDIF»«inst.procedure.name»(«FOR arg : inst.parameters SEPARATOR ", "»«arg.compileArg»«ENDFOR»);
	'''
	}
}
	override caseInstLoad(InstLoad inst) '''
		«inst.target.variable.indexedName» = «inst.source.variable.name»«FOR index : inst.indexes»[«index.doSwitch»]«ENDFOR»;
	'''

	override caseInstReturn(InstReturn inst) '''
		«IF inst.value != null»return «inst.value.doSwitch»;«ENDIF»
	'''

	override caseInstStore(InstStore inst) '''
		«inst.target.variable.name»«FOR index : inst.indexes»[«index.doSwitch»]«ENDFOR» = «inst.value.doSwitch»;
	'''
	
	
	def compilePort(Port port, Integer size) '''
		PortIn<«port.type.doSwitch»«IF size != null», «size»«ENDIF»> port_«port.name»;
	'''
	
	def compilePort(Port port, Integer size, Integer fanout) '''
		PortOut<«port.type.doSwitch», «fanout»«IF size != null», «size»«ENDIF»> port_«port.name»;
	'''

	def compileNativeProc(Procedure proc) '''
		extern «proc.returnType.doSwitch» «proc.name» («FOR param : proc.parameters SEPARATOR ", "»«param.variable.varDecl»«ENDFOR»);
	'''

	def compileProcedure(Procedure proc) '''
		«proc.returnType.doSwitch» «proc.name» («FOR param : proc.parameters SEPARATOR ", "»«param.variable.varDecl»«ENDFOR») {
			«proc.doSwitch»
		}
	'''
	
	def compileAction(Action action) '''
		«action.scheduler.returnType.doSwitch» «action.scheduler.name» () {	
			«FOR e : action.peekPattern.numTokensMap»
			«e.key.type.doSwitch»* «e.key.name» = port_«e.key.name».getRdPtr(«IF e.value > 1»«e.value»«ENDIF»);
			«ENDFOR»
			«action.scheduler.doSwitch»
		}

		«action.body.returnType.doSwitch» «action.body.name» () {	
			«FOR e : action.inputPattern.numTokensMap»
			«e.key.type.doSwitch»* «e.key.name» = port_«e.key.name».getRdPtr(«IF e.value > 1»«e.value»«ENDIF»);
			«ENDFOR»
			«FOR port : action.outputPattern.ports»
			«port.type.doSwitch»* «port.name» = port_«port.name».getWrPtr();
			«ENDFOR»
			«action.body.doSwitch»
			«FOR e : action.inputPattern.numTokensMap»
			port_«e.key.name».incRdPtr(«IF e.value > 1»«e.value»«ENDIF»);
			status_«e.key.name»_ -= «e.value»;
			«ENDFOR»
			«FOR e : action.outputPattern.numTokensMap»
			port_«e.key.name».incWrPtr(«e.key.name»«IF e.value > 1», «e.value»«ENDIF»);
			status_«e.key.name»_ -= «e.value»;
		«ENDFOR»
		}
	'''
	
	def private varDecl(Var v) {
		'''«v.type.doSwitch» «v.name»«FOR dim : v.type.dimensions»[«dim»]«ENDFOR»'''
	}
	
	def dispatch compileArg(ArgByRef arg) {
		'''&«arg.use.variable.doSwitch»«FOR index : arg.indexes»[«index.doSwitch»]«ENDFOR»'''
	}
	
	def dispatch compileArg(ArgByVal arg) {
		arg.value.doSwitch
	}
	
	def compileScheduler(Actor actor) '''	
		void schedule(EStatus& status) {	
			«FOR port : actor.inputs SEPARATOR "\n"»status_«port.name»_=port_«port.name».getCount();«ENDFOR»
			«FOR port : actor.outputs SEPARATOR "\n"»status_«port.name»_=port_«port.name».getRooms();«ENDFOR»
		
			bool res = true;
			while (res) {
				«IF actor.fsm!=null»
				res = false;
				«FOR action : actor.actionsOutsideFsm BEFORE "if" SEPARATOR " else if"»«action.compileScheduler(null)»«ENDFOR»
				if(!res) {
					switch(state_) {
						«actor.fsm.compilerScheduler»
					}
				}
				«ELSE»
				res = false;
				«FOR action : actor.actions BEFORE "if" SEPARATOR " else if"»«action.compileScheduler(null)»«ENDFOR»
				«ENDIF»
			}
		}

	'''
	
	def compileScheduler(Action action, State state) '''
		(«FOR e : action.inputPattern.numTokensMap»status_«e.key.name»_ >= «e.value» && «ENDFOR»isSchedulable_«action.name»()) {
			«IF !action.outputPattern.empty»
			if(«FOR e : action.outputPattern.numTokensMap SEPARATOR " && "»status_«e.key.name»_ >= «e.value»«ENDFOR») {
				«action.body.name»();
				res = true;
				status = hasExecuted;
				«IF state != null»state_ = state_«state.name»;«ENDIF»
			}
			«ELSE»
			«action.body.name»();
			res = true;
			status = hasExecuted;
			«IF state != null»state_ = state_«state.name»;«ENDIF»
			«ENDIF»
		}'''

	def compilerScheduler(FSM fsm) '''
		«FOR state : fsm.states»
		case state_«state.name»:
			«FOR edge : state.outgoing BEFORE "if" SEPARATOR " else if"»«(edge as Transition).action.compileScheduler(edge.target as State)»«ENDFOR»
			break;
		«ENDFOR»
	'''

}