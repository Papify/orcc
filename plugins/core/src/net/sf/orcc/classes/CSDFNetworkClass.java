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
package net.sf.orcc.classes;

import net.sf.orcc.ir.Pattern;
import net.sf.orcc.ir.Port;
import net.sf.orcc.network.Network;

/**
 * This class defines the SDF class. A SDF network has fixed
 * production/consumption rates.
 *
 * @author Matthieu Wipliez
 *
 */
public class CSDFNetworkClass extends AbstractNetworkClass implements StaticClass {

	private Pattern inputPattern;

	private Pattern outputPattern;

	protected int numberOfPhases;
	/**
	 * Creates a new static class.
	 */
	public CSDFNetworkClass() {
		inputPattern = new Pattern();
		outputPattern = new Pattern();
	}

	/**
	 * Returns the input pattern of this static class.
	 * 
	 * @return the input pattern of this static class
	 */
	public Pattern getInputPattern() {
		return inputPattern;
	}

	/**
	 * Returns the number of tokens consumed by this port.
	 * 
	 * @param port
	 *            an input port
	 * @return the number of tokens consumed by this port.
	 */
	public int getNumTokensConsumed(Port port) {
		Integer numTokens = inputPattern.get(port);
		if (numTokens == null) {
			return 0;
		}
		return numTokens;
	}

	/**
	 * Returns the number of tokens written to this port.
	 * 
	 * @param port
	 *            an output port
	 * @return the number of tokens written to this port.
	 */
	public int getNumTokensProduced(Port port) {
		Integer numTokens = outputPattern.get(port);
		if (numTokens == null) {
			return 0;
		}
		return numTokens;
	}

	/**
	 * Returns the output pattern of this static class.
	 * 
	 * @return the output pattern of this static class
	 */
	public Pattern getOutputPattern() {
		return outputPattern;
	}

	@Override
	public boolean isSDF() {
		return true;
	}

	/**
	 * Saves the number of tokens consumed by input ports of the given actor.
	 * 
	 * @param actor
	 *            an actor
	 */
	public void setTokenConsumptions(Network network) {
		for (Port port : network.getInputs()) {
			inputPattern.put(port, port.getNumTokensConsumed());
		}
	}

	/**
	 * Saves the number of tokens written to output ports of the given actor.
	 * 
	 * @param actor
	 *            an actor
	 */
	public void setTokenProductions(Network network) {
		for (Port port : network.getOutputs()) {
			outputPattern.put(port, port.getNumTokensProduced());
		}
	}

	public void setNumberOfPhases(int numberOfPhases) {
		this.numberOfPhases = numberOfPhases;
	}

	public int getNumberOfPhases() {
		return numberOfPhases;
	}

}
