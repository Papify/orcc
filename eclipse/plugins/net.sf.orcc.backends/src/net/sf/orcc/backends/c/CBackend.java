/*
 * Copyright (c) 2012, IETR/INSA of Rennes
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
package net.sf.orcc.backends.c;

import static net.sf.orcc.backends.BackendsConstants.ADDITIONAL_TRANSFOS;
import static net.sf.orcc.backends.BackendsConstants.BXDF_FILE;
import static net.sf.orcc.backends.BackendsConstants.IMPORT_BXDF;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.AbstractBackend;
import net.sf.orcc.backends.c.transform.CBroadcastAdder;
import net.sf.orcc.backends.transform.CastAdder;
import net.sf.orcc.backends.transform.DeadVariableRemoval;
import net.sf.orcc.backends.transform.DisconnectedOutputPortRemoval;
import net.sf.orcc.backends.transform.DivisionSubstitution;
import net.sf.orcc.backends.transform.EmptyBlockRemover;
import net.sf.orcc.backends.transform.Inliner;
import net.sf.orcc.backends.transform.InlinerByAnnotation;
import net.sf.orcc.backends.transform.InstPhiTransformation;
import net.sf.orcc.backends.transform.InstTernaryAdder;
import net.sf.orcc.backends.transform.ListFlattener;
import net.sf.orcc.backends.transform.LoopUnrolling;
import net.sf.orcc.backends.transform.Multi2MonoToken;
import net.sf.orcc.backends.transform.ParameterImporter;
import net.sf.orcc.backends.transform.StoreOnceTransformation;
import net.sf.orcc.backends.util.Alignable;
import net.sf.orcc.backends.util.Mapping;
import net.sf.orcc.backends.util.Validator;
import net.sf.orcc.df.Actor;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.transform.ArgumentEvaluator;
import net.sf.orcc.df.transform.BroadcastAdder;
import net.sf.orcc.df.transform.Instantiator;
import net.sf.orcc.df.transform.NetworkFlattener;
import net.sf.orcc.df.transform.TypeResizer;
import net.sf.orcc.df.transform.UnitImporter;
import net.sf.orcc.df.util.DfSwitch;
import net.sf.orcc.df.util.DfVisitor;
import net.sf.orcc.ir.CfgNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.transform.BlockCombine;
import net.sf.orcc.ir.transform.ControlFlowAnalyzer;
import net.sf.orcc.ir.transform.DeadCodeElimination;
import net.sf.orcc.ir.transform.DeadGlobalElimination;
import net.sf.orcc.ir.transform.PhiRemoval;
import net.sf.orcc.ir.transform.RenameTransformation;
import net.sf.orcc.ir.transform.SSATransformation;
import net.sf.orcc.ir.transform.SSAVariableRenamer;
import net.sf.orcc.ir.transform.TacTransformation;
import net.sf.orcc.tools.classifier.Classifier;
import net.sf.orcc.tools.mapping.XmlBufferSizeConfiguration;
import net.sf.orcc.tools.merger.action.ActionMerger;
import net.sf.orcc.tools.merger.actor.ActorMerger;
import net.sf.orcc.tools.stats.StatisticsPrinter;
import net.sf.orcc.util.FilesManager;
import net.sf.orcc.util.OrccLogger;
import net.sf.orcc.util.OrccUtil;
import net.sf.orcc.util.Result;
import net.sf.orcc.util.Void;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * C back-end.
 * 
 * @author Matthieu Wipliez
 * @author Herve Yviquel
 * @author Antoine Lorence
 * 
 */
public class CBackend extends AbstractBackend {

	/**
	 * Path to target "src" folder
	 */
	protected String srcPath;

	@Override
	protected void doInitializeOptions() {
		// Create empty folders
		new File(path + File.separator + "build").mkdirs();
		new File(path + File.separator + "bin").mkdirs();

		srcPath = path + File.separator + "src";
	}

	@Override
	protected void doTransformActor(Actor actor) {
		Map<String, String> replacementMap = new HashMap<String, String>();
		replacementMap.put("abs", "abs_replaced");
		replacementMap.put("getw", "getw_replaced");
		replacementMap.put("exit", "exit_replaced");
		replacementMap.put("index", "index_replaced");
		replacementMap.put("log2", "log2_replaced");
		replacementMap.put("max", "max_replaced");
		replacementMap.put("min", "min_replaced");
		replacementMap.put("select", "select_replaced");
		replacementMap.put("OUT", "OUT_REPLACED");
		replacementMap.put("IN", "IN_REPLACED");
		replacementMap.put("SIZE", "SIZE_REPLACED");

		if (mergeActions) {
			new ActionMerger().doSwitch(actor);
		}
		if (convertMulti2Mono) {
			new Multi2MonoToken().doSwitch(actor);
		}

		List<DfSwitch<?>> transformations = new ArrayList<DfSwitch<?>>();
		transformations.add(new TypeResizer(true, false, true, false));
		transformations.add(new RenameTransformation(replacementMap));
		transformations.add(new DisconnectedOutputPortRemoval());
		transformations.add(new DfVisitor<Void>(new InlinerByAnnotation()));
		transformations.add(new DfVisitor<Void>(new LoopUnrolling()));

		// If "-t" option is passed to command line, apply additional
		// transformations
		if (getAttribute(ADDITIONAL_TRANSFOS, false)) {
			transformations.add(new StoreOnceTransformation());
			transformations.add(new DfVisitor<Void>(new SSATransformation()));
			transformations.add(new DfVisitor<Void>(new PhiRemoval()));
			transformations.add(new Multi2MonoToken());
			transformations.add(new DivisionSubstitution());
			transformations.add(new ParameterImporter());
			transformations.add(new DfVisitor<Void>(new Inliner(true, true)));

			// transformations.add(new UnaryListRemoval());
			// transformations.add(new GlobalArrayInitializer(true));

			transformations.add(new DfVisitor<Void>(new InstTernaryAdder()));
			transformations.add(new DeadGlobalElimination());

			transformations.add(new DfVisitor<Void>(new DeadVariableRemoval()));
			transformations.add(new DfVisitor<Void>(new DeadCodeElimination()));
			transformations.add(new DfVisitor<Void>(new DeadVariableRemoval()));
			transformations.add(new DfVisitor<Void>(new ListFlattener()));
			transformations.add(new DfVisitor<Expression>(
					new TacTransformation()));
			transformations.add(new DfVisitor<CfgNode>(
					new ControlFlowAnalyzer()));
			transformations
					.add(new DfVisitor<Void>(new InstPhiTransformation()));
			transformations.add(new DfVisitor<Void>(new EmptyBlockRemover()));
			transformations.add(new DfVisitor<Void>(new BlockCombine()));

			transformations.add(new DfVisitor<Expression>(new CastAdder(true,
					true)));
			transformations.add(new DfVisitor<Void>(new SSAVariableRenamer()));
		}

		for (DfSwitch<?> transformation : transformations) {
			transformation.doSwitch(actor);
			if (debug) {
				OrccUtil.validateObject(transformation.toString() + " on "
						+ actor.getName(), actor);
			}
		}

		// update "vectorizable" information
		Alignable.setAlignability(actor);
	}

	protected void doTransformNetwork(Network network) {
		if (mergeActors) {
			new BroadcastAdder().doSwitch(network);
		}
		OrccLogger.traceln("Instantiating...");
		new Instantiator(true).doSwitch(network);
		OrccLogger.traceln("Flattening...");
		new NetworkFlattener().doSwitch(network);
		new UnitImporter().doSwitch(network);

		if (classify) {
			OrccLogger.traceln("Classification of actors...");
			new Classifier().doSwitch(network);
		}
		if (mergeActors) {
			OrccLogger.traceln("Merging of actors...");
			new ActorMerger().doSwitch(network);
		} else {
			new CBroadcastAdder().doSwitch(network);
		}

		new ArgumentEvaluator().doSwitch(network);

		// if required, load the buffer size from the mapping file
		if (getAttribute(IMPORT_BXDF, false)) {
			File f = new File(getAttribute(BXDF_FILE, ""));
			new XmlBufferSizeConfiguration().load(f, network);
		}
	}

	@Override
	protected void doVtlCodeGeneration(List<IFile> files) {
		// do not generate a C VTL
	}

	@Override
	protected void doXdfCodeGeneration(Network network) {
		Validator.checkTopLevel(network);
		Validator.checkMinimalFifoSize(network, fifoSize);

		doTransformNetwork(network);

		if (debug) {
			// Serialization of the actors will break proxy link
			EcoreUtil.resolveAll(network);
		}
		transformActors(network.getAllActors());

		network.computeTemplateMaps();

		// print instances
		printChildren(network);

		// print network
		OrccLogger.trace("Printing network... ");
		if (new NetworkPrinter(network, options).print(srcPath) > 0) {
			OrccLogger.traceRaw("Cached\n");
		} else {
			OrccLogger.traceRaw("Done\n");
		}

		printCMake(network);

		CharSequence content = new StatisticsPrinter().getContent(network);
		FilesManager.writeFile(content, srcPath, network.getSimpleName() + ".csv");

		content = new Mapping(network, mapping).getContentFile();
		FilesManager.writeFile(content, srcPath, network.getSimpleName() + ".xcf");
	}

	protected void printCMake(Network network) {
		// print CMakeLists
		OrccLogger.traceln("Printing CMake project files");
		new CMakePrinter(network).printCMakeFiles(path);
	}

	@Override
	protected Result extractLibraries() {
		Result result = FilesManager.extract("/runtime/C/README.txt", path);

		// Copy specific windows batch file
		if (FilesManager.getCurrentOS() == FilesManager.OS_WINDOWS) {
			result.merge(FilesManager.extract(
					"/runtime/C/run_cmake_with_VS_env.bat", path));
		}

		OrccLogger.traceln("Export libraries sources");
		result.merge(FilesManager.extract("/runtime/C/libs", path));

		String scriptsPath = path + File.separator + "scripts";

		OrccLogger.traceln("Export scripts into " + scriptsPath + "... ");

		result.merge(FilesManager.extract("/runtime/common/scripts", path));
		result.merge(FilesManager.extract("/runtime/C/scripts", path));

		// Fix some permissions on scripts
		new File(scriptsPath + File.separator + "profilingAnalyse.py")
				.setExecutable(true);
		new File(scriptsPath + File.separator + "benchAutoMapping.py")
				.setExecutable(true);

		return result;
	}

	@Override
	protected boolean printInstance(Instance instance) {
		return new InstancePrinter(options).print(srcPath, instance) > 0;
	}

	@Override
	protected boolean printActor(Actor actor) {
		return new InstancePrinter(options).print(srcPath, actor) > 0;
	}

}
