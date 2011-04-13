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
package net.sf.orcc.ir;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import java.io.File;
import java.util.List;

import net.sf.orcc.OrccException;
import net.sf.orcc.util.UniqueEdge;

import org.jgrapht.DirectedGraph;

/**
 * This class defines a Finite State Machine (FSM). A FSM is a directed
 * multi-graph, where a vertex is a state, and an edge is a list of actions.
 * 
 * @author Matthieu Wipliez
 * @model
 */
public interface FSM extends EObject {

	/**
	 * Adds a transition between two states with the given action.
	 * 
	 * @param source
	 *            source state
	 * @param action
	 *            an action
	 * @param target
	 *            target state
	 */
	void addTransition(State source, Action action, State target);

	/**
	 * Creates and returns a graph representation of this FSM. Note that the
	 * graph is built each time this method is called.
	 * 
	 * @return a graph representation of this FSM
	 */
	DirectedGraph<State, UniqueEdge> getGraph();

	/**
	 * Returns the initial state.
	 * 
	 * @return the initial state
	 * @model
	 */
	State getInitialState();

	/**
	 * Returns the list of states sorted by alphabetical order.
	 * 
	 * @return the list of states sorted by alphabetical order
	 * @model containment="true"
	 */
	EList<State> getStates();

	/**
	 * Returns the actions accessible from the given state.
	 * 
	 * @param state
	 *            a state
	 * @return the actions accessible from the given state
	 */
	List<Action> getTargetActions(State state);

	/**
	 * Returns the states accessible from the given state.
	 * 
	 * @param state
	 *            a state
	 * @return the states accessible from the given state
	 */
	List<State> getTargetStates(State state);

	/**
	 * Returns the transition associated with the given state.
	 * 
	 * @param state
	 *            a state
	 * @return the transition associated with the given state
	 */
	Transition getTransition(State state);

	/**
	 * Returns a state to transition map.
	 * 
	 * @return a state to transition map
	 * @model keyType="State" valueType="Transition"
	 */
	EMap<State, Transition> getTransitionsMap();

	/**
	 * Returns the list of transitions of this FSM as a list of
	 * {@link Transition}.
	 * 
	 * @return the list of transitions of this FSM as a list of
	 *         {@link Transition}
	 */
	List<Transition> getTransitions();

	/**
	 * Prints a graph representation of this FSM.
	 * 
	 * @param file
	 *            output file
	 * @throws OrccException
	 *             if something goes wrong (most probably I/O error)
	 */
	void printGraph(File file) throws OrccException;

	/**
	 * Removes the transition from the given <code>source</code> state that is
	 * associated with the given action.
	 * 
	 * @param source
	 *            source state
	 * @param action
	 *            action associated with the transition
	 */
	void removeTransition(State source, Action action);

	/**
	 * Replaces the target of the transition from the <code>source</code> state
	 * and whose action equals to the given action by the given
	 * <code>target</code> state.
	 * 
	 * @param source
	 *            source state
	 * @param action
	 *            action associated with the transition
	 * @param newTargetName
	 *            new target state
	 */
	void replaceTarget(State source, Action action, State target);

	/**
	 * Sets the initial state of this FSM to the given state.
	 * 
	 * @param state
	 *            a state
	 */
	void setInitialState(State state);

}
