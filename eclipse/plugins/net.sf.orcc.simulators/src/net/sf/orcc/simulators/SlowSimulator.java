/*
 * Copyright (c) 2010-2011, IETR/INSA of Rennes
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
package net.sf.orcc.simulators;

import static net.sf.orcc.OrccLaunchConstants.DEFAULT_FIFO_SIZE;
import static net.sf.orcc.OrccLaunchConstants.FIFO_SIZE;
import static net.sf.orcc.OrccLaunchConstants.INPUT_STIMULUS;
import static net.sf.orcc.OrccLaunchConstants.PROJECT;
import static net.sf.orcc.OrccLaunchConstants.XDF_FILE;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.OrccException;
import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.interpreter.ActorInterpreter;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Port;
import net.sf.orcc.ir.transformations.DeadCodeElimination;
import net.sf.orcc.ir.transformations.DeadGlobalElimination;
import net.sf.orcc.ir.transformations.DeadVariableRemoval;
import net.sf.orcc.ir.util.ActorVisitor;
import net.sf.orcc.ir.util.ExpressionEvaluator;
import net.sf.orcc.network.Connection;
import net.sf.orcc.network.Instance;
import net.sf.orcc.network.Network;
import net.sf.orcc.network.Vertex;
import net.sf.orcc.network.attributes.IAttribute;
import net.sf.orcc.network.attributes.IValueAttribute;
import net.sf.orcc.network.serialize.XDFParser;
import net.sf.orcc.network.transformations.BroadcastAdder;
import net.sf.orcc.runtime.Fifo;
import net.sf.orcc.util.OrccUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jgrapht.DirectedGraph;

import std.io.impl.Source;

/**
 * This class implements a simulator using a slow, visitor-based approach.
 * 
 * @author Matthieu Wipliez
 * @author Pierre-Laurent Lagalaye
 * 
 */
public class SlowSimulator extends AbstractSimulator {

	private Map<Instance, BroadcastInterpreter> bcastInterpreters;

	protected List<Fifo> fifoList;

	private Map<Port, Fifo> fifos;

	private int fifoSize;

	private Map<Instance, ActorInterpreter> interpreters;

	private IProject project;

	private String stimulusFile;

	private List<IFolder> vtlFolders;

	private String xdfFile;

	protected void connectActors(Port srcPort, Port tgtPort, int fifoSize) {
		Fifo fifo = new Fifo(srcPort.getType(), fifoSize);
		fifos.put(srcPort, fifo);
		fifos.put(tgtPort, fifo);
	}

	/**
	 * Visit the network graph for building the required topology. Edges of the
	 * graph correspond to the connections between the actors. These connections
	 * should be implemented as FIFOs of specific size as defined in the CAL
	 * model or a common default size.
	 * 
	 * @param graph
	 */
	public void connectNetwork(DirectedGraph<Vertex, Connection> graph) {
		// Get edges from the graph has actors point-to-point connections.
		Set<Connection> connections = graph.edgeSet();
		// Loop over the connections and ask for the source and target actors
		// connection through specified I/O ports.
		for (Connection connection : connections) {
			Vertex srcVertex = graph.getEdgeSource(connection);
			Vertex tgtVertex = graph.getEdgeTarget(connection);

			if (srcVertex.isInstance() && tgtVertex.isInstance()) {
				// get FIFO size (user-defined nor default)
				int size;
				IAttribute attr = connection
						.getAttribute(Connection.BUFFER_SIZE);
				if (attr != null && attr.getType() == IAttribute.VALUE) {
					Expression expr = ((IValueAttribute) attr).getValue();
					size = new ExpressionEvaluator().evaluateAsInteger(expr) + 1;
				} else {
					size = fifoSize;
				}

				// create the communication FIFO between source and target
				// actors
				Port srcPort = connection.getSource();
				Port tgtPort = connection.getTarget();
				// connect source and target actors
				if ((srcPort != null) && (tgtPort != null)) {
					connectActors(srcPort, tgtPort, size);
				}
			}
		}
	}

	protected void initializeNetwork(Network network) {
		Source.setFileName(stimulusFile);

		for (Instance instance : network.getInstances()) {
			if (instance.isActor()) {
				ActorInterpreter interpreter = interpreters.get(instance);
				interpreter.initialize();
			} else {
				BroadcastInterpreter interpreter = bcastInterpreters
						.get(instance);
				interpreter.initialize();
			}
		}
	}

	@Override
	protected void initializeOptions() {
		fifoSize = getAttribute(FIFO_SIZE, DEFAULT_FIFO_SIZE);
		stimulusFile = getAttribute(INPUT_STIMULUS, "");
		xdfFile = getAttribute(XDF_FILE, "");

		String name = getAttribute(PROJECT, "");
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject(name);

		vtlFolders = OrccUtil.getOutputFolders(project);
	}

	/**
	 * Visit the network graph for instantiating the vertexes (actors we want to
	 * simulate). Created actor instances are stored in the simuActorsMap.
	 * 
	 * @param graph
	 * @throws OrccException
	 * @throws FileNotFoundException
	 */
	private void instantiateNetwork(DirectedGraph<Vertex, Connection> graph)
			throws OrccException, FileNotFoundException {
		// Loop over the graph vertexes and get instances definition for
		// instantiating the network to simulate.
		for (Vertex vertex : graph.vertexSet()) {
			if (vertex.isInstance()) {
				Instance instance = vertex.getInstance();
				if (instance.isActor()) {
					Actor clonedActor = EcoreUtil.copy(instance.getActor());
					instance.setContents(clonedActor);

					ActorVisitor<?>[] transformations = {
							new DeadGlobalElimination(),
							new DeadCodeElimination(),
							new DeadVariableRemoval() };
					for (ActorVisitor<?> transformation : transformations) {
						transformation.doSwitch(clonedActor);
					}

					ConnectedActorInterpreter interpreter = new ConnectedActorInterpreter(
							clonedActor, instance.getParameters());

					interpreters.put(instance, interpreter);
					interpreter.setFifos(fifos);

				} else if (instance.isBroadcast()) {
					BroadcastInterpreter bcastInterpreter = new BroadcastInterpreter(
							instance.getBroadcast());
					bcastInterpreters.put(instance, bcastInterpreter);
					bcastInterpreter.setFifos(fifos);
				}
			}
		}
	}

	protected void runNetwork(Network network) {
		boolean isAlive = true;
		do {
			boolean hasExecuted = false;
			for (Instance instance : network.getInstances()) {
				int nbFiring = 0;
				if (instance.isActor()) {
					ActorInterpreter interpreter = interpreters.get(instance);
					while (interpreter.schedule()) {
						nbFiring++;
					}
				} else {
					BroadcastInterpreter interpreter = bcastInterpreters
							.get(instance);
					while (interpreter.schedule()) {
						nbFiring++;
					}
				}
				
				hasExecuted |= (nbFiring > 0);
				
				// check for cancelation
				if (isCanceled()) {
					return;
				}
			}
			isAlive = hasExecuted;
		} while (isAlive);
	}

	@Override
	public void start(String mode) {
		try {
			interpreters = new HashMap<Instance, ActorInterpreter>();

			bcastInterpreters = new HashMap<Instance, BroadcastInterpreter>();

			fifos = new HashMap<Port, Fifo>();

			IFile file = OrccUtil.getFile(project, xdfFile, "xdf");

			Network network = new XDFParser(file).parseNetwork();

			// Instantiate the network
			network.instantiate(vtlFolders);
			Network.clearActorPool();

			// Flatten the hierarchical network
			network.flatten();

			// Add broadcasts before connecting actors
			new BroadcastAdder().transform(network);

			// Parse XDF file, do some transformations and return the
			// graph corresponding to the flat network instantiation.
			DirectedGraph<Vertex, Connection> graph = network.getGraph();
			instantiateNetwork(graph);
			updateConnections(graph);
			connectNetwork(graph);
			initializeNetwork(network);
			runNetwork(network);
		} catch (OrccException e) {
			throw new OrccRuntimeException(e.getMessage());
		} catch (FileNotFoundException e) {
			throw new OrccRuntimeException(e.getMessage());
		}
	}

	private void updateConnections(DirectedGraph<Vertex, Connection> graph)
			throws OrccException {
		for (Connection connection : graph.edgeSet()) {
			Vertex srcVertex = graph.getEdgeSource(connection);
			Vertex tgtVertex = graph.getEdgeTarget(connection);

			if (srcVertex.isInstance()) {
				Instance source = srcVertex.getInstance();
				String srcPortName = connection.getSource().getName();

				Port srcPort;
				if (source.isActor()) {
					srcPort = source.getActor().getOutput(srcPortName);
				} else {
					srcPort = source.getBroadcast().getOutput(srcPortName);
				}
				connection.setSource(srcPort);
			}

			if (tgtVertex.isInstance()) {
				Instance target = tgtVertex.getInstance();
				String dstPortName = connection.getTarget().getName();

				Port dstPort;
				if (target.isActor()) {
					dstPort = target.getActor().getInput(dstPortName);
				} else {
					dstPort = target.getBroadcast().getInput();
				}

				connection.setTarget(dstPort);
			}
		}
	}

}
