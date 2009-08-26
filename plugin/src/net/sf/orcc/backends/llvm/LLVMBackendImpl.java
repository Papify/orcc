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
package net.sf.orcc.backends.llvm;

import java.io.File;
import java.io.FileInputStream;

import net.sf.orcc.backends.IBackend;
import net.sf.orcc.ir.network.Network;
import net.sf.orcc.ir.parser.NetworkParser;
import net.sf.orcc.ir.transforms.BroadcastAdder;

/**
 * LLVM back-end.
 * 
 * @author J�r�me GORIN
 * 
 */
public class LLVMBackendImpl implements IBackend {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			try {
				new LLVMBackendImpl().generateCode(args[0], 10000);
			} catch (Exception e) {
				System.err.println("Could not print \"" + args[0] + "\"");
				e.printStackTrace();
			}
		} else {
			System.err.println("Usage: CBackendImpl <flattened XDF network>");
		}
	}

	@Override
	public void generateCode(String fileName, int fifoSize) throws Exception {
		File file = new File(fileName);
		String path = file.getParent();

		Network network = new NetworkParser().parseNetwork(path,
				new FileInputStream(file));
//		ActorPrinterTemplate printer = new ActorPrinterTemplate();

/*		Set<Instance> instances = network.getGraph().vertexSet();
		for (Instance instance : instances) {
			if (instance.hasActor()) {
				String outputName = path + File.separator + instance.getId()
						+ ".c";
				File out = new File(outputName);
				if (instance.getFile().lastModified() > out.lastModified()) {
					// only goes through the whole code generation process if
					// the source JSON file is newer than the target C file
					Actor actor = instance.getActor();

					new PhiRemoval(actor);
					new IncrementPeephole(actor);
					new MoveWritesTransformation(actor);
					// new ActorPrinter(outputName, actor);
					printer.printActor(outputName, actor);
				}
			}
		}
*/
		// add broadcasts
		new BroadcastAdder(network);

		// print network
		NetworkPrinter networkPrinter = new NetworkPrinter();
		String outputName = path + File.separator + network.getName() + ".cpp";
		networkPrinter.printNetwork(outputName, network, false, fifoSize);
	}
}
