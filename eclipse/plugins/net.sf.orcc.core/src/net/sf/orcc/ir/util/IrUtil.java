/*
 * Copyright (c) 2011, IETR/INSA of Rennes
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
package net.sf.orcc.ir.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Node;
import net.sf.orcc.ir.NodeBlock;
import net.sf.orcc.ir.NodeWhile;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.impl.IrResourceFactoryImpl;
import net.sf.orcc.util.EcoreHelper;
import net.sf.orcc.util.OrccUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;

/**
 * This class contains several methods to help the manipulation of EMF models.
 * 
 * @author Matthieu Wipliez
 * @author Herve Yviquel
 * 
 */
public class IrUtil {

	/**
	 * Add the given instruction before the given expression. If the expression
	 * is contained by an instruction then the instruction to add is put
	 * directly before, else the instruction is put to the previous nodeblock
	 * which is created if needed. Return <code>true</code> if the given
	 * instruction is added in the current block.
	 * 
	 * @param expression
	 *            an expression
	 * @param instruction
	 *            the instruction to add before the given expression
	 * @param usePreviousJoinNode
	 *            <code>true</code> if the current IR form has join node before
	 *            while node
	 * @return <code>true</code> if the given instruction is added in the
	 *         current block
	 */
	public static boolean addInstBeforeExpr(Expression expression,
			Instruction instruction, boolean usePreviousJoinNode) {
		Instruction instContainer = EcoreHelper.getContainerOfType(expression,
				Instruction.class);
		Node nodeContainer = EcoreHelper.getContainerOfType(expression,
				Node.class);
		if (instContainer != null) {
			if (usePreviousJoinNode && instContainer.isPhi()
					&& isWhileJoinNode(nodeContainer)) {
				NodeWhile nodeWhile = EcoreHelper.getContainerOfType(
						nodeContainer, NodeWhile.class);
				addToPreviousNodeBlock(nodeWhile, instruction);
				return false;
			} else {
				List<Instruction> instructions = EcoreHelper
						.getContainingList(instContainer);
				instructions.add(instructions.indexOf(instContainer),
						instruction);
				return true;
			}
		} else {
			if (usePreviousJoinNode && nodeContainer.isWhileNode()) {
				NodeBlock joinNode = ((NodeWhile) nodeContainer).getJoinNode();
				joinNode.add(instruction);
				return false;
			} else {
				addToPreviousNodeBlock(nodeContainer, instruction);
				return false;
			}
		}
	}

	private static void addToPreviousNodeBlock(Node node,
			Instruction instruction) {
		List<Node> nodes = EcoreHelper.getContainingList(node);
		NodeBlock nodeBlock = IrFactory.eINSTANCE.createNodeBlock();
		nodeBlock.add(instruction);
		nodes.add(nodes.indexOf(node), nodeBlock);
	}

	/**
	 * Returns a deep copy of the given objects, and updates def/use chains.
	 * 
	 * @param eObjects
	 *            a list of objects
	 * @return a deep copy of the given objects with def/use chains correctly
	 *         updated
	 */
	public static <T extends EObject> Collection<T> copy(Collection<T> eObjects) {
		Copier copier = new Copier();
		Collection<T> result = copier.copyAll(eObjects);
		copier.copyReferences();

		TreeIterator<EObject> it = EcoreUtil.getAllContents(eObjects);
		while (it.hasNext()) {
			EObject object = it.next();

			if (object instanceof Def) {
				Def def = (Def) object;
				Def copyDef = (Def) copier.get(def);
				copyDef.setVariable(def.getVariable());
			} else if (object instanceof Use) {
				Use use = (Use) object;
				Use copyUse = (Use) copier.get(use);
				copyUse.setVariable(use.getVariable());
			}
		}

		return result;
	}

	/**
	 * Returns a deep copy of the given expression, and updates uses.
	 * 
	 * @param expression
	 *            an expression
	 * @return a deep copy of the given expression with uses correctly updated
	 */
	public static <T extends EObject> T copy(T eObject) {
		Copier copier = new Copier();
		@SuppressWarnings("unchecked")
		T result = (T) copier.copy(eObject);
		copier.copyReferences();

		TreeIterator<EObject> it = EcoreUtil.getAllContents(eObject, true);
		while (it.hasNext()) {
			EObject object = it.next();

			if (object instanceof Def) {
				Def def = (Def) object;
				Def copyDef = (Def) copier.get(def);
				copyDef.setVariable(def.getVariable());
			} else if (object instanceof Use) {
				Use use = (Use) object;
				Use copyUse = (Use) copier.get(use);
				copyUse.setVariable(use.getVariable());
			}
		}

		return result;
	}

	/**
	 * Removes the def/use chains of the given object, and then removes the
	 * object itself from its container.
	 * 
	 * @param eObject
	 *            an EObject
	 */
	public static void delete(EObject eObject) {
		removeUses(eObject);
		removeDefs(eObject);
		EcoreUtil.remove(eObject);
	}

	/**
	 * Deletes the given objects, and updates the def/use chains.
	 * 
	 * @param objects
	 *            a list of objects
	 */
	public static void delete(List<? extends EObject> eObjects) {
		while (!eObjects.isEmpty()) {
			delete(eObjects.get(0));
		}
	}

	/**
	 * Deserializes the XMI representation of an actor stored in the given file,
	 * and returns this actor.
	 * 
	 * @param file
	 *            a .ir file
	 * @return the actor serialized with XMI in the given file
	 */
	public static Actor deserializeActor(IFile file) {
		ResourceSet set = new ResourceSetImpl();
		Resource resource = set.getResource(URI.createPlatformResourceURI(file
				.getFullPath().toString(), true), true);
		Actor actor = (Actor) resource.getContents().get(0);
		return actor;
	}

	public static List<Use> getUses(EObject eObject) {
		List<Use> uses = new ArrayList<Use>();
		TreeIterator<EObject> it = eObject.eAllContents();
		while (it.hasNext()) {
			EObject descendant = it.next();
			if (descendant instanceof Use) {
				uses.add((Use) descendant);
			}
		}

		return uses;
	}

	private static boolean isWhileJoinNode(Node node) {
		if (node.isBlockNode()) {
			NodeWhile nodeWhile = EcoreHelper.getContainerOfType(node,
					NodeWhile.class);
			return (nodeWhile != null && nodeWhile.getJoinNode() == node);
		}
		return false;
	}

	/**
	 * Removes the defs present in the given object.
	 * 
	 * @param eObject
	 *            an EObject
	 */
	public static void removeDefs(EObject eObject) {
		TreeIterator<EObject> it = eObject.eAllContents();
		while (it.hasNext()) {
			EObject descendant = it.next();
			if (descendant instanceof Def) {
				Def def = (Def) descendant;
				def.setVariable(null);
			}
		}
	}

	/**
	 * Removes the uses present in the given object.
	 * 
	 * @param eObject
	 *            an EObject
	 */
	public static void removeUses(EObject eObject) {
		TreeIterator<EObject> it = eObject.eAllContents();
		while (it.hasNext()) {
			EObject descendant = it.next();
			if (descendant instanceof Use) {
				Use use = (Use) descendant;
				use.setVariable(null);
			}
		}
	}

	/**
	 * Serializes the given actor to the given output folder.
	 * 
	 * @param outputFolder
	 *            an IFolder of the workspace
	 * @param actor
	 *            an actor
	 * @return <code>true</code> if the serialization succeeded
	 */
	public static boolean serializeActor(IFolder outputFolder, Actor actor) {
		URI uri = URI.createPlatformResourceURI(outputFolder.getFullPath()
				.append(OrccUtil.getFile(actor)).addFileExtension("ir")
				.toString(), true);
		return serializeActor(uri, actor);
	}

	/**
	 * Serializes the given actor to the given output folder.
	 * 
	 * @param outputFolder
	 *            output folder
	 * @param actor
	 *            an actor
	 * @return <code>true</code> if the serialization succeeded
	 */
	public static boolean serializeActor(String outputFolder, Actor actor) {
		String pathName = outputFolder + File.separator
				+ OrccUtil.getFile(actor) + ".ir";
		URI uri = URI.createFileURI(pathName);
		return serializeActor(uri, actor);
	}

	/**
	 * Serializes the given actor to the given URI.
	 * 
	 * @param uri
	 *            URI
	 * @param actor
	 *            an actor
	 * @return <code>true</code> if the serialization succeeded
	 */
	private static boolean serializeActor(URI uri, Actor actor) {
		// check that the factory is registered
		// (only happens in command-line mode)
		// ...
		// duck you command line :)
		Map<String, Object> extToFactoryMap = Resource.Factory.Registry.INSTANCE
				.getExtensionToFactoryMap();
		Object instance = extToFactoryMap.get("ir");
		if (instance == null) {
			instance = new IrResourceFactoryImpl();
			extToFactoryMap.put("ir", instance);
		}

		// serialization
		ResourceSet set = new ResourceSetImpl();
		Resource resource = set.createResource(uri);
		resource.getContents().add(actor);
		try {
			resource.save(null);
			return true;
		} catch (IOException e) {
			// uncomment to see details of exception
			e.printStackTrace();
			return false;
		}
	}

}