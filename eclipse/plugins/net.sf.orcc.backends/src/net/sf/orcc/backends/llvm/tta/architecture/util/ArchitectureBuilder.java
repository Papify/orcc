/*
 * Copyright (c) 2012, IRISA
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
package net.sf.orcc.backends.llvm.tta.architecture.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.backends.llvm.tta.architecture.ArchitectureFactory;
import net.sf.orcc.backends.llvm.tta.architecture.Component;
import net.sf.orcc.backends.llvm.tta.architecture.Design;
import net.sf.orcc.backends.llvm.tta.architecture.FunctionUnit;
import net.sf.orcc.backends.llvm.tta.architecture.Memory;
import net.sf.orcc.backends.llvm.tta.architecture.Port;
import net.sf.orcc.backends.llvm.tta.architecture.Processor;
import net.sf.orcc.backends.llvm.tta.architecture.ProcessorConfiguration;
import net.sf.orcc.backends.llvm.tta.architecture.Signal;
import net.sf.orcc.backends.util.Mapping;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Argument;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.util.DfSwitch;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.ir.Var;
import net.sf.orcc.util.OrccLogger;

/**
 * This class contains several methods to build a hardware design.
 * 
 * @author Herve Yviquel
 * 
 */
public class ArchitectureBuilder extends DfSwitch<Design> {

	private int bufferId = 0;
	private int signalId = 0;

	private Map<Vertex, Component> componentMap;

	private Design design;
	private ArchitectureFactory factory = ArchitectureFactory.eINSTANCE;
	private boolean reduceConnections;

	/**
	 * Add a simple signal to the design. The signal is the translation of
	 * native connection. External ports and functional units are automatically
	 * added to the design and their processor.
	 * 
	 * @param connection
	 *            the native connection which represents the signal
	 */
	private void addSignal(Connection connection) {
		Vertex source = componentMap.get(connection.getSource());
		Vertex target = componentMap.get(connection.getTarget());
		Port sourcePort = null;
		Port targetPort = null;
		int size;

		if (source == null) {
			// The signal comes from an external port
			Port port = factory.createPort((net.sf.orcc.df.Port) connection
					.getSource());
			design.addInput(port);
			source = port;
		} else {
			if (source instanceof Processor) {
				Processor processor = (Processor) source;
				FunctionUnit fu = factory.createOutputSignalUnit(processor,
						connection.getSourcePort().getName());
				processor.getFunctionUnits().add(fu);
				sourcePort = fu;
			} else {
				Component component = (Component) source;
				sourcePort = factory.createPort(connection.getSourcePort());
				component.addOutput(sourcePort);
			}
		}

		if (target == null) {
			// The signal targets an external port
			Port port = factory.createPort((net.sf.orcc.df.Port) connection
					.getTarget());
			design.addOutput(port);
			size = port.getSize();
			target = port;
		} else {
			if (target instanceof Processor) {
				throw new OrccRuntimeException("Unsupported input signal.");
			} else {
				Component component = (Component) target;
				targetPort = factory.createPort(connection.getTargetPort());
				component.addInput(targetPort);
				size = targetPort.getSize();
			}
		}

		Signal signal = factory.createSignal("" + signalId++, size, source,
				target, sourcePort, targetPort);

		design.add(signal);
	}

	/**
	 * Build a design from a network of actors, a predefined configuration of
	 * the processors and finally the mapping of the actors on a set of
	 * processors. The processors and their interconnections are instantiated
	 * during the visit of a given process network.
	 * 
	 * @param network
	 *            The dataflow network of the current description to compile
	 * @param configuration
	 *            A predefined configuration of the processors
	 * @param mapping
	 *            The mapping of the actors contained by the network on a set of
	 *            processors
	 * @return A new design
	 */
	public Design build(Network network, ProcessorConfiguration configuration,
			Mapping mapping, boolean reduceConnections, int fifosize) {
		this.componentMap = new HashMap<Vertex, Component>();
		this.reduceConnections = reduceConnections;
		this.design = factory.createDesign();

		// Map all unmapped component to its own processor
		for (Vertex unmapped : new ArrayList<Vertex>(mapping.getUnmapped())) {
			mapping.map("processor_" + unmapped.getLabel(), unmapped);
		}

		// Build processors
		for (String name : mapping.getComponents()) {
			design.add(factory.createProcessor(name, configuration, 0));
		}

		// Map Actors
		for (Vertex vertex : network.getChildren()) {
			Instance instance = vertex.getAdapter(Instance.class);
			Actor actor = vertex.getAdapter(Actor.class);
			if (actor.isNative()) {
				// A native actor describes a VHDL component from the library.
				Component component = factory
						.createComponent(vertex.getLabel());
				// The parameter of this actor is used as generic.
				if (instance != null) {
					for (Argument arg : instance.getArguments()) {
						component.setAttribute(arg.getVariable().getName(),
								arg.getValue());
					}
				} else {
					for (Var param : actor.getParameters()) {
						component.setAttribute(param.getName(),
								param.getInitialValue());
					}
				}
				design.add(component);
				componentMap.put(vertex, component);
			} else {
				Processor processor = design.getProcessor(mapping
						.getComponent(vertex));
				processor.getMappedActors().add(vertex);

				componentMap.put(vertex, processor);
			}
		}

		// Map FIFOs
		for (Connection connection : network.getConnections()) {
			if (isNative(connection)) {
				// Native connection are hardware signals
				addSignal(connection);
			} else {
				// FIFO connection are mapped to a memory
				mapToBuffer(connection);
			}
		}

		new ArchitectureMemoryEstimator(fifosize).doSwitch(design);

		for (Processor processor : design.getProcessors()) {
			if (ArchitectureUtil.needOrccFu(processor.getMappedActors())) {
				processor.getFunctionUnits().add(
						factory.createOrccFU(processor));
			}
			if (ArchitectureUtil.needToPrint(processor.getMappedActors())) {
				processor.getFunctionUnits().add(factory.createIoFU(processor));
			}
		}

		return design;
	}

	private boolean isNative(Connection connection) {
		net.sf.orcc.df.Port source = connection.getSourcePort();
		net.sf.orcc.df.Port target = connection.getTargetPort();

		return (source != null && source.isNative())
				|| (target != null && target.isNative());
	}

	/**
	 * Map a connection on a circular buffer. If the actors source and target of
	 * the connection are mapped on different processors, then the connection is
	 * mapped to their interconnected memory and this memory is created in case
	 * it doesn't yet exist. If both actors are mapped to the same processor,
	 * then the connection is mapped to its internal RAM.
	 * 
	 * @param connection
	 *            the connection to map on a buffer
	 */
	private void mapToBuffer(Connection connection) {
		Processor source = (Processor) componentMap.get(connection.getSource());
		Processor target = (Processor) componentMap.get(connection.getTarget());

		if (source == null || target == null) {
			// One of them is a network port.
			OrccLogger.warnln("External FIFO port: The given application "
					+ "cannot be synthesised.");

			Processor proc = source == null ? target : source;
			Memory ram = factory.createMemory("smem_" + bufferId);
			proc.connect(ram);
			return;
		}

		Memory ram;
		if (reduceConnections && source.getNeighbors().contains(target)) {
			ram = source.getMemorySharedWith(target);
		} else if (source == target) {
			// If both source and target are mapped to the same processor, then
			// the connection will be mapped to a local RAM
			if (reduceConnections && !source.getLocalRAMs().isEmpty()) {
				// An existing local RAM
				ram = source.getLocalRAMs().get(0);
			} else {
				// Or a newly created one
				ram = factory.createMemory("lmem_" + bufferId);
				source.connect(ram);
				source.getLocalRAMs().add(ram);
				ram.setAttribute("id", bufferId++);
			}
		} else {
			// Creation of a new shared memory connected to both processors.
			ram = factory.createMemory("smem_" + bufferId);
			FunctionUnit sourceLSU = source.connect(ram);
			FunctionUnit targetLSU = target.connect(ram);
			ram.setSource(source);
			ram.setTarget(target);
			ram.setSourcePort(sourceLSU);
			ram.setTargetPort(targetLSU);
			design.add(ram);
			ram.setAttribute("id", bufferId++);
		}

		ram.getMappedConnections().add(connection);
	}

}
