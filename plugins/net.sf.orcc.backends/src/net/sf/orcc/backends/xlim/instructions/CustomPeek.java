/*
 * Copyright (c) 2010, IRISA
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
 *   * Neither the name of IRISA nor the names of its
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
package net.sf.orcc.backends.xlim.instructions;

import net.sf.orcc.ir.Location;
import net.sf.orcc.ir.Port;
import net.sf.orcc.ir.TargetContainer;
import net.sf.orcc.ir.Variable;
import net.sf.orcc.ir.instructions.SpecificInstruction;
import net.sf.orcc.ir.util.CommonNodeOperations;

/**
 * This class defines a custom Peek instruction. This Peek instruction doesn't
 * take a group of tokens but a precise token defined by its index in the FIFO.
 * 
 * @author Herve Yviquel
 * 
 */
public class CustomPeek extends SpecificInstruction implements TargetContainer {

	private int tokenIndex;

	private Port port;

	private Variable target;

	public CustomPeek(Location location, Port port, int tokenIndex,
			Variable target) {
		super(location);
		this.port = port;
		this.tokenIndex = tokenIndex;
		this.target = target;
	}

	/**
	 * Returns the port used by this FIFO operation.
	 * 
	 * @return the port used by this FIFO operation
	 */
	public Port getPort() {
		return port;
	}

	@Override
	public Variable getTarget() {
		return target;
	}

	public int getTokenIndex() {
		return tokenIndex;
	}

	@Override
	public void internalSetTarget(Variable target) {
		this.target = target;
	}

	/**
	 * Sets the port used by this FIFO operation.
	 * 
	 * @param port
	 *            the port used by this FIFO operation
	 */
	public void setPort(Port port) {
		if (this.port != null) {
			this.port.removeUse(this);
		}
		this.port = port;
		if (port != null) {
			port.addUse(this);
		}
	}

	@Override
	public void setTarget(Variable target) {
		CommonNodeOperations.setTarget(this, target);
	}

	/**
	 * Sets the index of the token used by this FIFO operation.
	 * 
	 * @param numTokens
	 *            the index of the token used by this FIFO operation
	 */
	public void setTokenIndex(int tokenIndex) {
		this.tokenIndex = tokenIndex;
	}
	
	@Override
	public String toString() {
		return getTarget() + " = customPeek(" + getPort() + ", " + getTokenIndex()
				+ ")";
	}

}