package net.sf.orcc.tools.merger;

import java.util.HashMap;
import java.util.Map;

import net.sf.orcc.OrccException;
import net.sf.orcc.df.Connection;
import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Network;
import net.sf.orcc.df.Vertex;
import net.sf.orcc.df.transformations.INetworkTransformation;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.Port;
import net.sf.orcc.ir.util.IrUtil;

import org.jgrapht.DirectedGraph;

public class UniqueInstantiator implements INetworkTransformation {

	private Map<Actor, Integer> actors = new HashMap<Actor, Integer>();
	private DirectedGraph<Vertex, Connection> graph;

	@Override
	public void transform(Network network) throws OrccException {
		this.transform(network.getGraph());
	}

	public void transform(DirectedGraph<Vertex, Connection> graph)
			throws OrccException {
		this.graph = graph;
		for (Vertex vertex : graph.vertexSet()) {
			if (vertex.isInstance()) {
				Instance instance = vertex.getInstance();
				Actor actor = instance.getActor();
				if (actors.containsKey(actor)) {
					Actor copy = IrUtil.copy(actor);
					instance.setContents(copy);
					int val = actors.get(actor);
					copy.setName(actor.getName() + "_" + val);
					actors.put(actor, val++);
				} else {
					actors.put(actor, 0);
				}
			}
		}

		for (Connection connection : graph.edgeSet()) {
			updateConnection(connection);
		}

	}

	private void updateConnection(Connection connection) {
		Vertex srcVertex = graph.getEdgeSource(connection);
		Vertex tgtVertex = graph.getEdgeTarget(connection);

		if (srcVertex.isInstance()) {
			Instance source = srcVertex.getInstance();
			String srcPortName = connection.getSourcePort().getName();
			Port srcPort = source.getActor().getOutput(srcPortName);
			connection.setSourcePort(srcPort);
		}

		if (tgtVertex.isInstance()) {
			Instance target = tgtVertex.getInstance();
			String dstPortName = connection.getTargetPort().getName();
			Port dstPort = target.getActor().getInput(dstPortName);
			connection.setTargetPort(dstPort);
		}
	}

}
