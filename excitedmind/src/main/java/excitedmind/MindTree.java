package excitedmind;

import java.util.ArrayList;

import java.util.Iterator;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Graph;
import prefuse.data.Tuple;
import prefuse.util.PrefuseLib;
import prefuse.util.collections.IntIterator;

import com.tinkerpop.blueprints.Vertex;

import excitedmind.DBTree.EdgeVertex;
import excitedmind.DBTree.RefLinkInfo;



public class MindTree {

	private final static String sm_dbIdColumnName = "dbElement";

	public final static String sm_textPropName = "text";
	public final static String sm_fontFamilyPropName = "fontFamily";
	public final static String sm_fontSizePropName = "fontSize";
	public final static String sm_boldPropName = "bold";
	public final static String sm_italicPropName = "italic";
	public final static String sm_underlinedPropName = "underlined";
	public final static String sm_nodeColorPropName = "nodeColor";
	public final static String sm_textColorPropName = "textColor";

    public final static String sm_nodePropNames [] = {
            sm_textPropName,

            sm_fontFamilyPropName,
            sm_fontSizePropName,

            sm_boldPropName,
            sm_italicPropName,
            sm_underlinedPropName,

            sm_nodeColorPropName,
            sm_textColorPropName,
    };

	public final static String sm_edgeTypePropName = DBTree.EDGE_TYPE_PROP_NAME;
	public final static String sm_edgeColorPropName = "edgeColor";
    public final static String sm_edgePropNames [] = {sm_edgeTypePropName, sm_edgeColorPropName};

	public Tree m_tree;
	Table m_nodeTable;
	Table m_edgeTable;
	
	private String m_edgeTypePropName;
	
	DBTree m_dbTree;


	//return sorted copy of propName
	private void addTableProperties(String[] propNames, Table t)
	{
		t.addColumn(sm_dbIdColumnName, Object.class, null);
		
		for (String propName : propNames)
		{
			t.addColumn(propName, Object.class, null);
		}
	}
	
	
	public MindTree(String dbPath, Object rootId)
	{
		m_tree = new Tree();
		
		m_dbTree = new DBTree (dbPath);

		m_nodeTable = m_tree.getNodeTable();
		m_edgeTable = m_tree.getEdgeTable();
		

		addTableProperties(sm_nodePropNames, m_nodeTable);
		addTableProperties(sm_edgePropNames, m_edgeTable);
		
		Node root = m_tree.addRoot();
		loadNodeProperties(m_dbTree.getVertex(rootId), root);

        final int initialLevel = 2;
		m_tree.deepTraverse (root, new Tree.Processor () {
			public boolean run (Node node, int level) {
				attachChildren (node);
				return level < initialLevel;
			}
			
		}, 0);

	}

	private static void loadElementProperties(com.tinkerpop.blueprints.Element dbElement, Tuple tuple, String keys[])
	{
		tuple.set(sm_dbIdColumnName, dbElement.getId());
		for (String key : keys)
		{
			tuple.set(key, dbElement.getProperty(key));
		}
	}
	
	private void loadNodeProperties (Vertex vertex, Node node)
	{
		loadElementProperties(vertex, node, sm_nodePropNames);
	}
	
	private void loadEdgeProperties (com.tinkerpop.blueprints.Edge dbEdge, Edge edge)
	{
		loadElementProperties(dbEdge, edge, sm_edgePropNames);
	}
	
	private Vertex getDBVertex(Node node)
	{
		Object dbId = node.get(sm_dbIdColumnName);
		return m_dbTree.getVertex(dbId);
	}
	
	private com.tinkerpop.blueprints.Edge getDBEdge (Edge edge)
	{
		return m_dbTree.getEdge(edge.get(sm_dbIdColumnName));
	}

	public void attachChildren (Node parent)
	{
		ArrayList<EdgeVertex> edgeVertexArray = m_dbTree.getChildrenAndReferees(getDBVertex(parent));
		
		if (edgeVertexArray == null || edgeVertexArray.size() == 0)
		{
			return;
		}

		for (EdgeVertex edgeVertex : edgeVertexArray)
		{
			Node child = m_tree.addChild(parent);
			Edge edge = m_tree.getEdge(parent, child);

			System.out.println (getDBVertex(parent)+ "->" + edgeVertex.m_vertex+ "   :  " + edgeVertex.m_edge);
			loadNodeProperties(edgeVertex.m_vertex, child);
			loadEdgeProperties(edgeVertex.m_edge, edge);
		}
	}

    public void detachChildern (Node node)
    {
        m_tree.removeDescendants(node);
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
	
	public void ascendRoot ()
	{
		Node root = m_tree.getRoot();
		EdgeVertex edgeVertex = m_dbTree.getParent(getDBVertex(root));
		
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
	
	interface Visitor {
	 	abstract public void visit (Node node);
	}
	
	private void visitNodeAvatares(Object dbId, Visitor visiter)
	{
		IntIterator allRows = m_nodeTable.rows();

		ArrayList<Integer> aimRows = new ArrayList<Integer> ();
		System.out.println ("need node's dbId is" + dbId);

		//collect the node with the same parentDBId to aimRows
		while (allRows.hasNext()) {
			int curRow = allRows.nextInt();

			if (dbId == null || m_nodeTable.get(curRow, sm_dbIdColumnName).equals(dbId)) {
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

	private void exposeDBEdge(final Vertex source,
                              final Vertex target,
                              final com.tinkerpop.blueprints.Edge dbEdge,
                              final int edgePosInSourceNode)
	{
        final Object sourceId = source.getId();
		visitNodeAvatares(source, new Visitor() {
            public void visit(Node node) {
                Node child = m_tree.addNode();
                Edge edge = m_tree.addChildEdge(node, child, edgePosInSourceNode);

                loadNodeProperties(target, child);
                loadEdgeProperties(dbEdge, edge);
            }
        });
	}

    //return new child node
	public Node addChild(Node parent, int pos) {
		Vertex dbParent = getDBVertex(parent);
		EdgeVertex edgeVertex = m_dbTree.addChild(dbParent, pos);
		
		exposeDBEdge(dbParent, edgeVertex.m_vertex, edgeVertex.m_edge, pos);
		
        return m_tree.getChild(parent, pos);
	}

    //return the next or previous sibling, or parent
    public Node removeLeafNode(Node node) {

        /* TODO
        Node parent = node
        Vertex dbParent = getDBVertex(parent);
        EdgeVertex edgeVertex = m_dbTree.addChild(dbParent, pos);

        exposeDBEdge(dbParent, edgeVertex.m_vertex, edgeVertex.m_edge, pos);

        return m_tree.getChild(parent, pos);
        */
        return null;
    }

	//return the DBid of node
	public Object moveNodeToTrash (Node node)
	{
		Node parent = m_tree.getParent (node);
		int index = m_tree.getChildIndex(parent, node);
		Vertex vertex = getDBVertex(node);
        Object dbId = vertex.getId();
		
		m_dbTree.moveSubTreeToTrash(getDBVertex(parent), index);
		
		visitNodeAvatares(getDBElementId(node), new Visitor() {
            public void visit(Node node) {
                System.out.println("remove node :" + node.getRow() + "---" + node.getString(sm_textPropName));
                m_tree.removeChild(node);
            }
        });
		
		ArrayList<Object> refLinkInfoes = m_dbTree.getContainerProperty (vertex, DBTree.SAVED_REFERER_INFO_PROP_NAME, false);
		for (Object obj : refLinkInfoes)
		{
			final RefLinkInfo refLinkInfo = (RefLinkInfo) obj;
			visitNodeAvatares(refLinkInfo.m_referee, new Visitor() {
                public void visit(Node node) {
                    m_tree.removeNode(node);
                }
            });
		}
        return dbId;
	}
	
	public void restoreNode (Object dbId)
	{
        //TODO
		
		
	}

    public Node addReference(Node referer, Node referee, int pos) {

        Vertex refererVertex = getDBVertex(referer);
        Vertex refereeVertex = getDBVertex(referee);
        com.tinkerpop.blueprints.Edge refEdge = m_dbTree.addRefEdge(refererVertex, refereeVertex, pos);

        if (refEdge == null) {
            return null;
        }
        else {
            exposeDBEdge(refererVertex, refereeVertex, refEdge, pos);
            return m_tree.getChild(referer, pos);
        }
    }

    public void removeReference(Node referer, Node referee) {
        /* TODO

        Vertex refererVertex = getDBVertex(referer);
        Vertex refereeVertex = getDBVertex(referee);
        Edge edge = m_tree.getEdge (referer, referee);
        edge.get

        com.tinkerpop.blueprints.Edge refEdge = m_dbTree.getRef(refererVertex, refereeVertex, pos);

        if (refEdge == null) {
            return null;
        }
        else {
            exposeDBEdge(refererVertex, refereeVertex, refEdge, pos);
            return m_tree.getChild(referer, pos);
        }
        */
    }


    public void reconnectNode (Node target, Node source1, Node source2, int pos)
	{
		//source1 // OUTDEGREE, OUTEDGES
		//source2 //OUTDEGEEE, OUTEDGES
	}
	
	public void setNodeProperty (final Object dbId, final String key, final Object value)
	{
		Vertex dbNode = m_dbTree.getVertex(dbId);
		dbNode.setProperty(key, value);
		
		visitNodeAvatares(dbId, new Visitor() {
            public void visit(Node node) {
                node.set(key, value);
            }
        });
		
	}
	
	public void setNodeProperty (final Node node, final String key, final Object value)
	{
		setNodeProperty (node.get(sm_dbIdColumnName), key, value);
	}
	
	public Object getDBElementId(final Tuple tuple)
	{
		assert(m_tree.containsTuple(tuple));
		return tuple.get(sm_dbIdColumnName);
	}

    public String getText (Node node)
    {
        return node.getString(sm_textColorPropName);
    }

    public void setText (Node node, String text)
    {
        setNodeProperty(node, sm_textPropName, text);
    }
}
