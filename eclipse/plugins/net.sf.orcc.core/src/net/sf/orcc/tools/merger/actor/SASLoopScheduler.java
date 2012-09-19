/*
 * Copyright (c) 2010, EPFL
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
 *   * Neither the name of the EPFL nor the names of its contributors may be used 
 *     to endorse or promote products derived from this software without specific 
 *     prior written permission.
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

package net.sf.orcc.tools.merger.actor;

import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Network;
import net.sf.orcc.graph.Vertex;
import net.sf.orcc.graph.visit.ReversePostOrder;

/**
 * This class computes a single appearance schedule (SAS) with 1-level nested
 * loop from the given SDF graph.
 * 
 * @author Ghislain Roquier
 * 
 */
public class SASLoopScheduler extends AbstractScheduler {

	public SASLoopScheduler(Network network) {
		super(network);
	}

	@Override
	public void schedule() {
		schedule = new Schedule();

		schedule.setIterationCount(1);

		for (Vertex vertex : new ReversePostOrder(network, network.getInputs())) {
			Actor actor = vertex.getAdapter(Actor.class);
			if (actor != null) {
				int rep = repetitions.get(actor);
				Iterand iterand = null;
				if (rep > 1) {
					Schedule subSched = new Schedule();
					subSched.setIterationCount(repetitions.get(actor));
					subSched.add(new Iterand(actor));
					iterand = new Iterand(subSched);
				} else {
					iterand = new Iterand(actor);
				}
				schedule.add(iterand);
			}
		}
	}

}