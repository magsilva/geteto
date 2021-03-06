/*  Copyright 2003  Auri Marcelo Rizzo Vicenzi, Marcio Eduardo Delamaro, 			    Jose Carlos Maldonado

    This file is part of Jabuti.

    Jabuti is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of the      
    License, or (at your option) any later version.

    Jabuti is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Jabuti.  If not, see <http://www.gnu.org/licenses/>.
 */

package br.jabuti.graph.datastructure;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import br.jabuti.graph.datastructure.reducetree.RoundRobinExecutor;

/**
 * This is an abstract class that represents a program graph. The basic operations like
 * including/removing a node or an edge are implemented here. The graph may have 2 types of edges, a
 * "primary" and a "secondary". The most imediate use of them is to use primery edge to indicate
 * ordinary flow transfer and secondary to indicate exception transfer.
 */
public abstract class ListGraph extends Vector implements Graph
{
	private Vector entry;

	private Vector exit;

	/** Creates an empty program graph */
	public ListGraph()
	{
		super();
		entry = new Vector();
		exit = new Vector();
	}

	public void setDefaultNumbering()
	{
		for (int i = size() - 1; i >= 0; i--) {
			GraphNode gfcn = (GraphNode) elementAt(i);

			gfcn.setNumber(i);
		}
	}

	/**
	 * Prints information about each graph node. Uses the method {@link GraphNode#toString} to
	 * obtain the information to be displaied
	 * 
	 * @param f Where to print
	 */
	public void print(PrintStream f)
	{
		for (int i = 0; i < size(); i++) {
			GraphNode gfcn = (GraphNode) elementAt(i);

			f.println("" + gfcn.toString());
		}
	}

	/**
	 * Marks a node as an entry node. The graph can have several entry nodes. The method does not
	 * add the node to the graph, it should be added before calling this method
	 * 
	 * @param x The node that will be an entry.
	 */

	public void setEntryNode(GraphNode x)
	{
		entry.add(x);
	}

	/**
	 * Marks a node as an exit node. The graph can have several exit nodes. The method does not add
	 * the node to the graph, it should be added before calling this method
	 * 
	 * @param x The node that will be the exit.
	 */
	public void setExitNode(GraphNode x)
	{
		exit.add(x);
	}

	/**
	 * Removes an node as entry.
	 * 
	 * @param x - The node to be removed from the entry set
	 */
	public void removeEntryNode(GraphNode x)
	{
		entry.removeElement(x);
	}

	/**
	 * Removes all entries.
	 */
	public void removeEntryNodes()
	{
		entry = new Vector();
	}

	/**
	 * Removes an node as exit.
	 * 
	 * @param x - The node to be removed from the exit set
	 */
	public void removeExitNode(GraphNode x)
	{
		exit.removeElement(x);
	}

	/**
	 * Removes all exit nodes.
	 */
	public void removeExitNodes()
	{
		exit = new Vector();
	}

	/**
	 * Returns the first entry node of the graph. The one that was first set using {@link #setEntry}
	 * .
	 * 
	 * @return The first entry node of this graph. <code>null</code> if none has been set.
	 */
	public GraphNode getFirstEntryNode()
	{
		if (entry.size() == 0) {
			return null;
		}
		return (GraphNode) entry.elementAt(0);
	}

	/**
	 * Returns the complete set of entry nodes for this graph.
	 * 
	 * @return An array with each of the entry nodes.
	 */

	public Set<GraphNode> getEntryNodes()
	{
		Set<GraphNode> entries = new HashSet<GraphNode>();
		entries.addAll(entry);
		return entries;
	}

	/**
	 * Returns the first exit node of the graph. The one that was first set using {@link #setExit}.
	 * 
	 * @return The exit node of this graph. <code>null</code> if none has been set.
	 */
	public GraphNode getFirstExitNode()
	{
		if (exit.size() == 0) {
			return null;
		}
		return (GraphNode) exit.elementAt(0);
	}

	/**
	 * Returns the complete set of exit nodes for this graph.
	 * 
	 * @return An array with each of the entry nodes.
	 */

	public Set<GraphNode> getExitNodes()
	{
		Set<GraphNode> exitNodes = new HashSet<GraphNode>();
		exitNodes.addAll(exit);
		return exitNodes;
	}

	/**
	 * Checks whether a node is an exit node.
	 * 
	 * @param n the noce to be checked
	 * @return true if the node is an exit node
	 */
	public boolean isExitNode(GraphNode n)
	{
		return exit.contains(n);
	}

	/**
	 * Checks whether a node is an entry node.
	 * 
	 * @param n the noce to be checked
	 * @return true if the node is an entry node
	 */
	public boolean isEntryNode(GraphNode n)
	{
		return entry.contains(n);
	}

	/**
	 * Finds the set of nodes without successors and then set them as exit nodes.
	 * 
	 * @param ex - Indicates whether secondary edges should be used to find exit nodes.
	 */

	public void computeExit(boolean ex)
	{
		for (int i = 0; i < size(); i++) {
			GraphNode gn = (GraphNode) elementAt(i);
			Set<GraphNode> v = getLeavingNodes(gn, ex);
			if (v.size() == 0) {
				setExitNode(gn);
			}
		}
	}

	/**
	 * Finds the largest set of connected nodes begining at a given node.
	 * 
	 * @param gn The node from which to calculate the subgraph.
	 * @param subg Is the place where to place the nodes of the subgraph
	 * @param ex Indicates whether to use secondary edges to calculate the subgraph
	 */
	void getSubgraph(GraphNode gn, Vector subg, boolean ex)
	{
		Set<GraphNode> next = getLeavingNodes(gn, ex);

		if (!subg.contains(gn)) {
			subg.add(gn);
		}

		Iterator<GraphNode> i = next.iterator();
		while (i.hasNext()) {
			GraphNode gfcn = i.next();
			getSubgraph(gfcn, subg, ex);
		}
	}

	/**
	 * Adds an directed edge between two nodes
	 * 
	 * @param x The source node
	 * @para y The destination node
	 */
	public void addPrimaryEdge(GraphNode x, GraphNode y)
	{
		x.addPrimNext(y);
	}

	/**
	 * Adds an directed secondary edge between two nodes
	 * 
	 * @param x The source node
	 * @para y The destination node
	 */
	public void addSecondaryEdge(GraphNode x, GraphNode y)
	{
		x.addSecNext(y);
	}

	/**
	 * Remove an directed edge between two nodes
	 * 
	 * @param x The source node
	 * @para y The destination node
	 */
	public void removePrimaryEdge(GraphNode x, GraphNode y)
	{
		x.deletePrimNext(y);
	}

	/**
	 * Adds an directed secondary edge between two nodes
	 * 
	 * @param x The source node
	 * @para y The destination node
	 */
	public void removeSecondaryEdge(GraphNode x, GraphNode y)
	{
		x.deleteSecNext(y);
	}

	/**
	 * Returns the set of nodes for wich there exist edges from a given node.
	 * 
	 * @param x The source node.
	 * @param us Indicates whether or no to consider secondary edges two.
	 * @return The set of nodes for wich the node of interest has an edge (or a secondary edge).
	 */
	public Set<GraphNode> getLeavingNodes(GraphNode x, boolean us)
	{
		Set<GraphNode> ret = new HashSet<GraphNode>();
		ret.addAll(getLeavingNodesByPrimaryEdge(x));

		if (us) {
			ret.addAll(getLeavingNodesBySecondaryEdge(x));
		}
		return ret;
	}

	public Set<GraphNode> getLeavingNodesByPrimaryEdge(GraphNode node)
	{
		return node.getPrimNext();
	}

	/**
	 * Returns the set of primary nodes for wich there exist edges from a given node.
	 * 
	 * @param x The source node.
	 * @return The set of secondary nodes for wich the node of interest has an edge.
	 */
	public Set<GraphNode> getLeavingNodesBySecondaryEdge(GraphNode x)
	{
		return x.getSecNext();
	}

	public Set<GraphNode> getArrivingNodes(GraphNode x, boolean us)
	{
		Set<GraphNode> ret = new HashSet<GraphNode>();
		ret.addAll(getArrivingNodesByPrimaryEdge(x));
		
		if (us) {
			ret.addAll(getArrivingNodesBySecondaryEdge(x));
		}
		return ret;
	}

	/**
	 * Returns the set of primary nodes for wich there exist edges to a given node.
	 * 
	 * @param x The destinatio node.
	 * @return The set of nodes from wich the node of interest has a primary edge.
	 */
	public Set<GraphNode> getArrivingNodesByPrimaryEdge(GraphNode x)
	{
		return x.getPrimArriving();
	}

	/**
	 * Returns the set of secondary nodes for wich there exist edges to a given node.
	 * 
	 * @param x The destination node.
	 * @return The set of nodes from wich the node of interest has a secondary edge.
	 */
	public Set<GraphNode> getArrivingNodesBySecondaryEdge(GraphNode x)
	{
		return x.getSecArriving();
	}

	/**
	 * Removes a node from the graph. Deals with all the details of such operation as removing edges
	 * entering and exiting the node, removing from the set of entry nodes and unseting the exit
	 * node (if those are the cases).
	 * 
	 * @param x The node to be removed
	 */
	public void removeNode(GraphNode x)
	{
		// Remove leaving nodes
		Set<GraphNode> v = getLeavingNodes(x, true);
		Iterator<GraphNode> i = v.iterator();
		for (int j = v.size() - 1; j >= 0; j--) {
			GraphNode y = i.next();
			x.deletePrimNext(y);
			x.deleteSecNext(y);
		}

		// Remove arriving nodes
		v = getArrivingNodes(x, true);
		i = v.iterator();
		for (int j = v.size() - 1; j >= 0; j--) {
			GraphNode y = i.next();
			y.deletePrimNext(x);
			y.deleteSecNext(x);
		}

		// remove as entry and exit node
		exit.remove(x);
		entry.remove(x);

		// remove the node from the graph
		remove(x);
	}

	/**
	 * This method removes a node but makes the links from its previuos and next nodes.
	 * 
	 * @param x The node to remove
	 * @param ex If it should consider also the secondary links
	 */
	public void jumpOverNode(GraphNode x, boolean ex)
	{
		if (isEntryNode(x)) {
			// nao mexer se for no inicial pois podem existir
			// variaveis definidas no noh...
			return;
		}

		Set<GraphNode> nx = getLeavingNodesByPrimaryEdge(x);
		Set<GraphNode> ar = getArrivingNodesByPrimaryEdge(x);
		Iterator<GraphNode> i = ar.iterator();
		while (i.hasNext()) {
			GraphNode f = i.next();
			Iterator<GraphNode> j = nx.iterator();
			while (j.hasNext()) {
				GraphNode t = j.next();
				addPrimaryEdge(f, t);
			}
		}

		nx = getLeavingNodesByPrimaryEdge(x);
		ar = getArrivingNodesByPrimaryEdge(x);
		i = ar.iterator();
		while (i.hasNext()) {
			GraphNode f = i.next();
			Iterator<GraphNode> j = nx.iterator();
			while (j.hasNext()) {
				GraphNode t = j.next();
				addSecondaryEdge(f, t);
			}
		}

		removeNode(x);
	}

	private GraphNode[] fdtf;
	private int ctdtf;

	/**
	 * Construct a Depth First Tree sequence of the nodes.
	 * 
	 * @param ex Whether or not to use secondary edges when calculating "next" nodes.
	 * 
	 */
	public GraphNode[] findDFTNodes(boolean ex)
	{
		fdtf = new GraphNode[size()];
		for (int i = 0; i < size(); i++) {
			GraphNode x = (GraphNode) elementAt(i);

			x.setMark(false);
		}
		ctdtf = 0;
		Set<GraphNode> entr = getEntryNodes();
		Iterator<GraphNode> i = entr.iterator();
		while (i.hasNext()) {
			DFS(i.next(), ex);
		}
		GraphNode[] ret = new GraphNode[ctdtf];

		System.arraycopy(fdtf, 0, ret, 0, ctdtf);
		return ret;
	}

	private void DFS(GraphNode x, boolean ex)
	{
		do {
			if (x.getMark()) {
				return;
			}
			Set<GraphNode> next = getLeavingNodes(x, ex);
			int k = next.size();

			x.setMark(true);
			fdtf[ctdtf++] = x;
			if (k <= 0) {
				return;
			}

			Iterator<GraphNode> i = next.iterator();
			while (i.hasNext()) {
				GraphNode nexti = i.next();
				if (i.hasNext()) {
					DFS(nexti, ex);
				}
			}
			x = i.next();
		} while (true);
	}

	/**
	 * This method implements the framework for the "round robin" algorithm.
	 * 
	 * @param x This object will take care of computing the new set for a given node
	 * @param reverse If true, the depth first tree is used in the opposite order, i.e., from 0 up
	 *        (the normal order is from the top elemen down).
	 */

	public void roundRobinAlgorithm(RoundRobinExecutor x, boolean reverse)
	{
		Set<GraphNode> nx, nx2;
		GraphNode[] dft = findDFTNodes(true);
		int cont = 0, nchange;
		int inc = -1, init = dft.length - 1, end = -1;

		if (reverse) {
			end = init + 1;
			init = 0;
			inc = 1;
		}
		for (int i = init; i != end; i += inc) {
			GraphNode in = dft[i];

			if (reverse) // pega anterior
			{
				nx = in.getPrimArriving();
				nx2 = in.getSecArriving();
			} else {
				nx = in.getPrimNext();
				nx2 = in.getSecNext();
			}
			x.init(in, nx, nx2);
		}
		do {
			cont++;
			nchange = 0;

			for (int i = init; i != end; i += inc) {
				GraphNode in = dft[i];

				if (reverse) // pega anterior
				{
					nx = in.getPrimArriving();
					nx2 = in.getSecArriving();
				} else {
					nx = in.getPrimNext();
					nx2 = in.getSecNext();
				}
				Object bs = x.calcNewSet(in, nx, nx2);

				if (!x.compareEQ(in, bs)) // compare current and last
				{
					nchange++;
					x.setNewSet(in, bs);
				}
			}

		} while (nchange > 0);
	}

	/**
	 * Construct an Inverse Depth First Tree sequence of the nodes.
	 * 
	 * @param ex Whether or not to use secondary edges when calculating "next" nodes.
	 * 
	 */
	public GraphNode[] findIDFTNodes(boolean ex)
	{
		fdtf = new GraphNode[size()];
		for (int i = 0; i < size(); i++) {
			GraphNode x = (GraphNode) elementAt(i);

			x.setMark(false);
		}
		Set<GraphNode> ext = getExitNodes();
		Iterator<GraphNode> i = ext.iterator();

		ctdtf = 0;
		while (i.hasNext()) {
			IDFS(i.next(), ex);
		}
		GraphNode[] ret = new GraphNode[ctdtf];

		System.arraycopy(fdtf, 0, ret, 0, ctdtf);
		return ret;
	}

	/**
	 * Construct an Inverse Depth First Tree sequence of the nodes, from a given node
	 * 
	 * @param ex Whether or not to use secondary edges when calculating "next" nodes.
	 * @param node The node from where to start
	 * 
	 */
	public GraphNode[] findIDFT(boolean ex, GraphNode node)
	{
		fdtf = new GraphNode[size()];
		for (int i = 0; i < size(); i++) {
			GraphNode x = (GraphNode) elementAt(i);

			x.setMark(false);
		}
		GraphNode ext = node;

		ctdtf = 0;
		IDFS(ext, ex);
		GraphNode[] ret = new GraphNode[ctdtf];

		System.arraycopy(fdtf, 0, ret, 0, ctdtf);
		return ret;
	}

	private void IDFS(GraphNode x, boolean ex)
	{
		do {
			if (x.getMark()) {
				return;
			}
			Set<GraphNode> next = getArrivingNodes(x, ex);
			int k = next.size();

			x.setMark(true);
			fdtf[ctdtf++] = x;
			if (k <= 0) {
				return;
			}

			Iterator<GraphNode> i = next.iterator();
			GraphNode lastNode = null;
			while (i.hasNext()) {
				lastNode = i.next();
				if (i.hasNext()) {
					IDFS(lastNode, ex);
				}
			}
			x = lastNode;
		} while (true);
	}

	/**
	 * This method implements the framework for the "round robin" algorithm.
	 * 
	 * @param x This object will take care of computing the new set for a given node
	 * @param reverse If true, the depth first tree is used in the opposite order, i.e., from 0 up
	 *        (the normal order is from the top elemen down).
	 */

	public void roundIRobinAlgorithm(RoundRobinExecutor x, boolean reverse)
	{
		Set<GraphNode> nx, nx2;
		GraphNode[] dft = findIDFTNodes(true);
		// System.out.println("Tamanho dft " + dft.length);

		// se dft.lnegth == 0 isso significa que metodo nao tem nos
		// de saida. assim, deve fazer apenas a inicializacao.
		if (dft.length == 0) {
			dft = findDFTNodes(true);

			for (int i = 0; i < dft.length; i++) {
				GraphNode in = dft[i];

				x.init(in, null, null);
			}
			return;
		}

		int cont = 0, nchange;
		int inc = -1, init = dft.length - 1, end = -1;

		if (reverse) {
			end = init + 1;
			init = 0;
			inc = 1;
		}
		for (int i = init; i != end; i += inc) {
			GraphNode in = dft[i];

			if (!reverse) // pega anterior
			{
				nx = in.getPrimArriving();
				nx2 = in.getSecArriving();
			} else {
				nx = in.getPrimNext();
				nx2 = in.getSecNext();
			}
			// System.out.println("Chamando init para no: " + in.getLabel());
			x.init(in, nx, nx2);
		}
		do {
			cont++;
			nchange = 0;

			for (int i = init; i != end; i += inc) {
				GraphNode in = dft[i];

				if (!reverse) // pega anterior
				{
					nx = in.getPrimArriving();
					nx2 = in.getSecArriving();
				} else {
					nx = in.getPrimNext();
					nx2 = in.getSecNext();
				}
				Object bs = x.calcNewSet(in, nx, nx2);

				if (!x.compareEQ(in, bs)) // compare current and last
				{
					nchange++;
					x.setNewSet(in, bs);
				}
			}

		} while (nchange > 0);
	}

	/**
	 * Computes a simple paths from one node to another
	 * 
	 * @param orig - the source node
	 * @param dest - the destination node
	 * @param sec - indecates whether secondary edges can be used in the paths
	 * 
	 * @return - an array of {@link GraphNode}'s. If one of the nodes is not part of this graph,
	 *         returns null. If theres is no such path, returns an array of size 0.
	 */
	public GraphNode[] computeSimplePath(GraphNode orig, GraphNode dest, boolean sec)
	{
		if (!contains(orig) || !contains(dest)) {
			return null;
		}
		Set<GraphNode> v = compAP(new HashSet(), orig, dest, sec);

		if (v == null) {
			return null;
		}
		GraphNode gnRet[] = (GraphNode[]) v.toArray(new GraphNode[0]);

		return gnRet;
	}

	private Set<GraphNode> compAP(Set<GraphNode> forb, GraphNode f, GraphNode t, boolean sec)
	{
		if (forb.contains(f)) {

			return null;
		}

		forb.add(f);
		if (f == t) {
			Set<GraphNode> v = new HashSet<GraphNode>();
			v.add(f);
			return v;
		}
		Set<GraphNode> ret = null;
		Set<GraphNode> nx = getLeavingNodes(f, sec);

		Iterator<GraphNode> i = nx.iterator();
		while (ret == null & i.hasNext()) {
			GraphNode ngn = i.next();

			ret = compAP(forb, ngn, t, sec);
			if (ret != null) {
				ret.add(f);
			} else {
				forb.add(ngn);
			}
		}
		return ret;
	}

	/**
	 * Compute the set of Strongly Connected Components of this graph.
	 * 
	 * @return An array where each element corresponds ta a SC component
	 * @param sec - indicates if secondary edges should be considered
	 */
	public HashSet[] computeSCC(boolean sec)
	{
		Vector v = (Vector) clone();
		Vector ret = new Vector();

		while (v.size() > 0) {
			HashSet hs = new HashSet();
			GraphNode prim = (GraphNode) v.firstElement();

			for (int i = 0; i < v.size(); i++) {
				GraphNode gn = (GraphNode) v.elementAt(i);
				GraphNode vem[] = computeSimplePath(gn, prim, sec);

				if (vem == null || vem.length == 0) {
					continue;
				} // no paths from this nodes
				GraphNode vai[] = computeSimplePath(prim, gn, sec);

				if (vai == null || vai.length == 0) {
					continue;
				} // no paths from this nodes
				for (int j = 0; j < vai.length; j++) {
					hs.add(vai[j]);
				}
				for (int j = 0; j < vem.length; j++) {
					hs.add(vem[j]);
				}
			}
			v.removeAll(hs);
			ret.add(hs);
		}
		return (HashSet[]) ret.toArray(new HashSet[0]);
	}

	public void removeComposite(boolean sec)
	{
		Iterator<GraphNode> i = iterator();
		while (i.hasNext()) {
			GraphNode gn = i.next();
			Set<GraphNode> next = getLeavingNodes(gn, sec);

			Iterator<GraphNode> j = next.iterator();
			while (j.hasNext()) {
				GraphNode nx = j.next();

				if (gn == nx) {
					removePrimaryEdge(gn, nx);
					break;
				}

				Iterator<GraphNode> k = next.iterator();
				while (k.hasNext()) {
					GraphNode nx2 = (GraphNode) k.next();

					if (nx.equals(nx2)) {
						continue;
					}
					GraphNode path[] = computeSimplePath(nx2, nx, false);
					if (path != null) {
						removePrimaryEdge(gn, nx);
						break;
					}
				}
			}
		}

	}

}
