package excitedmind;

import java.rmi.UnexpectedException;
import java.util.ArrayList;

import prefuse.Visualization;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.data.event.TreeRootChangeListener;
import prefuse.util.PrefuseLib;
import prefuse.util.collections.IntIterator;

import com.tinkerpop.blueprints.CloseableIterable;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;

import excitedmind.DBTree.EdgeVertex;
import excitedmind.DBTree.RefLinkInfo;



public class MindTree {

	private final static String BP_ID_COL_KEY = PrefuseLib.FIELD_PREFIX + "bpElement";
	private final static String EDGE_TYPE_COL_KEY = PrefuseLib.FIELD_PREFIX + "edgeType";
	
	public final static String sm_textPropName = "text";
	
	public final static String sm_fontFamilyPropName = "fontFamily";
	public final static String sm_fontSizePropName = "fontSize";
	
	public final static String sm_boldPropName = "bold";
	public final static String sm_italicPropName = "italic";
	public final static String sm_underlinedPropName = "underlined";
	
	public final static String sm_nodeColorPropName = "nodeColor";
	public final static String sm_textColorPropName = "textColor";
	public final static String sm_outlineColorPropName = "outlineColor";
	
	public final static String sm_edgeTypePropName = DBTree.EDGE_TYPE_PROP_NAME;
	public final static String sm_edgeColorPropName = "edgeColor";

	public Tree m_tree;
	Table m_nodeTable;
	Table m_edgeTable;
	
	private HashSet<String> m_nodePropNames;
	private HashSet<String> m_edgePropNames;
	
	private String m_edgeTypePropName;
	
	DBTree m_dbTree;
	private int m_maxLevel;
	
	private boolean m_tableListenerEnabled = true;
	
    private LinkedHashSet<Node> m_foldedNodes = new LinkedHashSet<Node>();
    
	TableListener m_nodeTableListener = new TableListener() {
		
		@Override
		public void tableChanged(Table t, int start, int end, int col, int type) {
			//only update the propNames, not include m_edgeIdArrayPropName
			if (type == EventConstants.UPDATE && m_tableListenerEnabled)
			{
				String colName = t.getColumnName(col);
				if (m_nodePropNames.contains(colName))
				{
					for (int i=start; i<=end; i++)
					{
						Tuple tuple = t.getTuple(i);

						Object bpId = tuple.get(BP_ID_COL_KEY);
						com.tinkerpop.blueprints.Vertex bpNode = m_dbTree.getVertex (bpId);
						bpNode.setProperty(colName, tuple.get(col));
					}
				}

			}
		}
	};
	
	TableListener m_edgeTableListener = new TableListener() {

		@Override
		public void tableChanged(Table t, int start, int end, int col, int type) {
			if (type == EventConstants.UPDATE && m_tableListenerEnabled)
			{
				String colName = t.getColumnName(col);
				if (m_edgePropNames.contains(colName))
				{
					for (int i=start; i<=end; i++)
					{
						Tuple tuple = t.getTuple(i);
						
						Object bpId = tuple.get(BP_ID_COL_KEY);
						com.tinkerpop.blueprints.Edge bpEdge = m_dbTree.getEdge (bpId);
						bpEdge.setProperty(colName, tuple.get(col));
					}
				}
			}
		}
	};
	
	//return sorted copy of propName
	private HashSet<String> addProperties (String [] propNames, Table t)
	{
		HashSet<String> hashSet = new HashSet<String> ();
		
		t.addColumn(BP_ID_COL_KEY, Object.class, null);
		
		for (String propName : propNames)
		{
			t.addColumn(propName, Object.class, null);
			hashSet.add(propName);
		}
		return hashSet;
	}
	
	
	public MindTree(String dbPath, Object rootId) 
	{
		m_tree = new Tree();
		
		m_dbTree = new DBTree (dbPath);
		m_maxLevel = 2;
		
		m_nodeTable = m_tree.getNodeTable();
		m_edgeTable = m_tree.getEdgeTable();
		
		String nodePropNames [] = {
			sm_textPropName,
			
			sm_fontFamilyPropName,
			sm_fontSizePropName,
			
			sm_boldPropName,
			sm_italicPropName,
			sm_underlinedPropName,
			
			sm_nodeColorPropName,
			sm_textColorPropName,
			sm_outlineColorPropName
		};

		String edgePropNames [] = {sm_edgeTypePropName, sm_edgeColorPropName};
		
		m_nodePropNames = addProperties(nodePropNames, m_nodeTable);
		m_edgePropNames = addProperties(edgePropNames, m_edgeTable);
		
		Node root = m_tree.addRoot();
		loadNodeProperties(m_dbTree.getVertex(rootId), root);
		
		deepTraverse (root, new Processor () {
			public boolean run (Node node, int level) {
				attachChildren (node);
				return level < m_maxLevel;
			}
			
		}, 0);
		
		m_nodeTable.addTableListener(m_nodeTableListener);
		m_edgeTable.addTableListener(m_edgeTableListener);
		
		m_tableListenerEnabled = true;
	}
	
	private static void blueprints2prefuse(com.tinkerpop.blueprints.Element bpElement, Tuple tuple, HashSet<String> keys)
	{
		tuple.set(BP_ID_COL_KEY, bpElement.getId());
		for (String key : keys)
		{
			tuple.set(key, bpElement.getProperty(key));
		}
	}
	
	private void loadNodeProperties (Vertex vertex, Node node)
	{
		blueprints2prefuse(vertex, node, m_nodePropNames);
	}
	
	private void loadEdgeProperties (com.tinkerpop.blueprints.Edge dbEdge, Edge edge)
	{
		blueprints2prefuse(dbEdge, edge, m_edgePropNames);
	}
	

	/*
	private static void prefuse2blueprints(Tuple tuple, com.tinkerpop.blueprints.Element bpElement, String [] keys) 
	{
		for (String key : keys)
		{
			bpElement.setProperty(key, tuple.get(key));
		}
	}
	*/
	
	private Vertex getVertex (Node node)
	{
		Object bpId = node.get(BP_ID_COL_KEY);
		return m_dbTree.getVertex(bpId);
	}
	
	private com.tinkerpop.blueprints.Edge getDBEdge (Edge edge)
	{
		return m_dbTree.getEdge(edge.get(BP_ID_COL_KEY));
	}

	private void attachChildren (Node parent)
	{
		ArrayList<EdgeVertex> edgeVertexArray = m_dbTree.getChildrenAndReferees(getVertex(parent));
		
		if (edgeVertexArray == null || edgeVertexArray.size() == 0)
		{
			return;
		}

		for (EdgeVertex edgeVertex : edgeVertexArray)
		{
			Node child = m_tree.addChild(parent);
			Edge edge = m_tree.getEdge(parent, child);

			System.out.println (getVertex(parent)+ "->" + edgeVertex.m_vertex+ "   :  " + edgeVertex.m_edge);
			loadNodeProperties(edgeVertex.m_vertex, child);
			loadEdgeProperties(edgeVertex.m_edge, edge);
		}
	}

	private interface Processor 
	{
		//return: true: continue deeper, false stop
	 	abstract public boolean run (Node node, int level);
	}
	
	private void deepTraverse (Node node, Processor proc, int level)
	{
		if (proc.run(node, level))
		{
			Iterator children_iter = m_tree.children(node);
			while (children_iter.hasNext())
			{
				deepTraverse((Node)children_iter.next(), proc, level+1);
			}
		}
	}
	
	void deepTraverse (Node node, Processor proc)
	{
		deepTraverse(node, proc, 0);
	}
	
	public void unfoldNode (VisualItem visualItem)
	{
		
		final Visualization vis = visualItem.getVisualization();
		Node node = (Node)visualItem.getSourceTuple();
		final String group = visualItem.getGroup();
		
		if (visualItem.isExpanded())
		{
			return;
		}
		
		visualItem.setExpanded(true);
		
		if (m_tree.getChildCount(node.getRow()) > 0) // node is not a leaf node
		{
			assert (m_foldedNodes.contains(node));
			
			m_foldedNodes.remove(node);
			final Node unfoldTreeRoot = node;
			
			//unfold descendants deeply, to the folded descendants
			deepTraverse(node,new Processor() {
				public boolean run(Node node, int level) {
					
					if (node == unfoldTreeRoot) {
						return true;
					}
					
					TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
					TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();
					
					PrefuseLib.updateVisible(visualNode, true);
					PrefuseLib.updateVisible(visualEdge, true);
					
					if (m_foldedNodes.contains(node)) {
						return false;
					} else {
						return true;
					}
				}
			}, 0);
		}
		else // node  is the leaf of prefuse Tree
		{
			attachChildren(node);
		}
	}
	
	public void foldNode (VisualItem visualItem)
	{
		final Visualization vis = visualItem.getVisualization();
		Node node = (Node)visualItem.getSourceTuple();
		final String group = visualItem.getGroup();
	
		if (! visualItem.isExpanded())
		{
			return;
		}
		
		visualItem.setExpanded(false);
		
		m_foldedNodes.add(node);
		
		final Node foldTreeRoot = node;
		
		//set descendants unvisible deeply, to the folded descendants
		deepTraverse(node,new Processor() {
			public boolean run(Node node, int level) {
				if (node == foldTreeRoot)
				{
					return true;
				}
				
				TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
				TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();
				
				PrefuseLib.updateVisible(visualNode, false);
				PrefuseLib.updateVisible(visualEdge, false);
					
				System.out.println ("invisable node: " + node.getString("text"));
				
				if (m_foldedNodes.contains(node)) {
					return false;
				} else {
					return true;
				}
			}
		}, 0);
		
		// detach the descendants of the earliest unfold node
		if (m_foldedNodes.size() > 100)
		{
			Iterator<Node> iter = m_foldedNodes.iterator();
			Node toRemovedNode = iter.next();
			iter = null;
			m_foldedNodes.remove(toRemovedNode);
			
			m_tree.removeDescendants(toRemovedNode);
		}
	}
	
	public void ToggleFoldNode (VisualItem visualItem )
	{
		if (visualItem.isExpanded())
		{
			foldNode(visualItem);
		}
		else
		{
			unfoldNode(visualItem);
		}
	}
	
	public void setRoot (Node node)
	{
		if (node == m_tree.getRoot())
		{
			return;
		}
		else
		{
			m_tree.setRoot(node);
			//FIXME: update the visualTree;
		}
	}
	
	public void ascendRootParent ()
	{
		Node root = m_tree.getRoot();
		EdgeVertex edgeVertex = m_dbTree.getParent(getVertex(root));
		
		if (edgeVertex == null)
		{
			return;
		}
		
		Node newRoot = m_tree.addNode();
		Edge edge = m_tree.addEdge(newRoot, root);
		
		loadEdgeProperties(edgeVertex.m_edge, edge);
		loadNodeProperties(edgeVertex.m_vertex, newRoot);
		
		m_tree.setRoot(newRoot);
	}
	
	interface Visiter  {
	 	abstract public void visit (Node node);
	}
	
	private void visitAllAvataresOfNode (Object bpId, Visiter visiter)
	{
		IntIterator allRows = m_nodeTable.rows();

		ArrayList<Integer> aimRows = new ArrayList<Integer> ();
		System.out.println ("need node's bpId is" + bpId);

		//collect the node with the same parentBpId to aimRows
		while (allRows.hasNext()) {
			int curRow = allRows.nextInt();

			if (bpId == null || m_nodeTable.get(curRow, BP_ID_COL_KEY).equals(bpId)) {
				aimRows.add(curRow);
			}
		}

		Iterator<Integer> aimRowIter = aimRows.iterator();

		//attach blueprints node, to each node in aimRows
		while (aimRowIter.hasNext()) {
			int row = aimRowIter.next();
			
			if (m_nodeTable.isValidRow(row))
			{
				Node node = m_tree.getNode(row);
				visiter.visit (node);
			}
		}
	}

	private void appendBpChild (final Object parentBpId, 
			final Vertex bpChild,
			final com.tinkerpop.blueprints.Edge bpEdge, 
			final int edgePosInSourceNode)
	{
		visitAllAvataresOfNode(parentBpId, new Visiter () {
			public void visit (Node node) {
				Node child = m_tree.addNode();
				Edge edge = m_tree.addChildEdge(node, child, edgePosInSourceNode);

				loadNodeProperties(bpChild, child);
				loadEdgeProperties(bpEdge, edge);
			}
		});
	}

	public Node addChild(Node parent, int pos) {
		Vertex dbParent = getVertex(parent);
		EdgeVertex edgeVertex = m_dbTree.addChild(dbParent, pos);
		
		appendBpChild(dbParent.getId(), edgeVertex.m_vertex,edgeVertex.m_edge, pos);
		
        return m_tree.getChild(parent, pos);
	}

	public Node addReference(Node node, Vertex referee, int pos) {
		
		Vertex referer = getVertex(node);
		com.tinkerpop.blueprints.Edge refEdge = m_dbTree.addRefEdge(referer, referee, pos);
		
        if (refEdge == null) {
        	return null;
        }
        else {
			appendBpChild(referer.getId(), referee, refEdge, pos);
	        return m_tree.getChild(node, pos);
        }
	}
	
	public Node addReference(Node node, Node refTargetNode, int pos) {
		return addReference(node, getVertex(refTargetNode), pos);
	}
	
	//return the parent of the removed node
	public void moveNodeToTrash (Node node)
	{
		Node parent = m_tree.getParent (node);
		int index = m_tree.getChildIndex(parent, node);
		Vertex vertex = getVertex (node);
		
		m_dbTree.moveSubTreeToTrash(getVertex(parent), index);
		
		visitAllAvataresOfNode(getDBItemId(node), new Visiter () {
			public void visit (Node node) {
				System.out.println ("remove node :" + node.getRow() + "---" + node.getString(sm_textPropName));
				m_tree.removeChild(node);
			}
		});
		
		ArrayList<Object> refLinkInfoes = m_dbTree.getContainerProperty (vertex, DBTree.SAVED_REFERER_INFO_PROP_NAME, false);
		for (Object obj : refLinkInfoes)
		{
			final RefLinkInfo refLinkInfo = (RefLinkInfo) obj;
			visitAllAvataresOfNode(refLinkInfo.m_referee, new Visiter () {
				public void visit (Node node) {
					m_tree.removeNode(node);
				}
			});
		}
	}
	
	public void restoreNode (Node node)
	{
		
		
	}
	
	public void reconnectNode (Node target, Node source1, Node source2, int pos)
	{
		//source1 // OUTDEGREE, OUTEDGES
		//source2 //OUTDEGEEE, OUTEDGES
	}
	
	public void setNodeProperty (final Object bpId, final String key, final Object value)
	{
		assert (m_nodePropNames.contains(key));
		
		Vertex dbNode = m_dbTree.getVertex(bpId);
		dbNode.setProperty(key, value);
		
		visitAllAvataresOfNode(bpId, new Visiter () {
			public void visit (Node node) {
				node.set(key, value);
			}
		});
		
	}
	
	public void setNodeProperty (final Node node, final String key, final Object value)
	{
		setNodeProperty (node.get(BP_ID_COL_KEY), key, value);
	}
	
	public Object getDBItemId (final Tuple tuple)
	{
		assert(m_tree.containsTuple(tuple));
		return tuple.get(BP_ID_COL_KEY);
	}
}