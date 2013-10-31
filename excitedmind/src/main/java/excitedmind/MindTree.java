package excitedmind;

import java.util.ArrayList;

import java.util.Iterator;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.util.collections.IntIterator;

import com.tinkerpop.blueprints.Vertex;

import excitedmind.DBTree.EdgeVertex;
import excitedmind.DBTree.RefLinkInfo;



public class MindTree {

	private final static String sm_dbIdColumnName = "dbElement";

    public final static String sm_outEdgeDBIdsPropName = DBTree.CHILD_EDGES_PROP_NAME;
    public final static String sm_inheritPathPropName = DBTree.INHERIT_PATH_PROP_NAME;

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
            sm_outEdgeDBIdsPropName,
            sm_inheritPathPropName,

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

    //Maybe there are more than one reference edge link source ant target
    //The callers of exposeRelation has getted target and dbEdge, so pass them as argument
	private void exposeRelation(final Object sourceId, final int edgePosInSourceNode,
                                final com.tinkerpop.blueprints.Edge dbEdge, final Vertex target)
	{
		visitNodeAvatares(sourceId, new Visitor() {
            public void visit(Node sourceNode) {
                Node child = m_tree.addNode();
                Edge edge = m_tree.addChildEdge(sourceNode, child, edgePosInSourceNode);

                loadNodeProperties(target, child);
                loadEdgeProperties(dbEdge, edge);
            }
        });
	}

    private void hideRelation(final Object sourceId, final int edgePosInSourceNode)
    {
        visitNodeAvatares(sourceId, new Visitor() {
            public void visit(Node sourceNode) {
                //its child is not displayed
                if (sourceNode.getChildCount() == 0)
                    return;

                Node child = m_tree.getChild(sourceNode, edgePosInSourceNode);
                m_tree.removeChild(child);
            }
        });

    }

    //return new child node
	public Object addChild(Object  parentDBId, int pos) {
		Vertex dbParent = m_dbTree.getVertex(parentDBId);
		EdgeVertex edgeVertex = m_dbTree.addChild(dbParent, pos);

        edgeVertex.m_vertex.setProperty(sm_textPropName, "");

		exposeRelation(parentDBId, pos, edgeVertex.m_edge, edgeVertex.m_vertex);

        return edgeVertex.m_vertex.getId();

        //TODO add to VisualMindTree return m_tree.getChild(parent, pos);
	}

	//return the DBid of node
	public Object moveNodeToTrash (Object parentDBId, int pos)
	{
        Vertex parent = m_dbTree.getVertex(parentDBId);
        EdgeVertex edgeChild = m_dbTree.getChildOrReferee(parent, pos);
        Object removedDBId = edgeChild.m_vertex.getId();

        hideRelation(parent, pos);

		ArrayList<Object> refLinkInfoes = m_dbTree.getContainerProperty (edgeChild.m_vertex, DBTree.SAVED_REFERER_INFO_PROP_NAME, true);
		for (Object obj : refLinkInfoes)
		{
			final RefLinkInfo refLinkInfo = (RefLinkInfo) obj;
            hideRelation(m_dbTree.getVertex(refLinkInfo.m_referee), refLinkInfo.m_pos);
		}

        m_dbTree.trashSubTree(parent, pos);

        return removedDBId;
	}
	
	public void restoreNodeFromTrash(final Object dbId)
	{
        final Vertex restoredVertex = m_dbTree.getVertex(dbId);
        final DBTree.TrashedTreeContext context = m_dbTree.getTrashedTreeContext(restoredVertex);
        final Vertex parentVertex = m_dbTree.getVertex(context.m_parentId);

        final EdgeVertex edgeParent = m_dbTree.restoreTrashedSubTree(restoredVertex);

        exposeRelation(parentVertex, context.m_pos, edgeParent.m_edge, restoredVertex);

        for (final RefLinkInfo refLinkInfo : context.m_refLinkInfos) {
            final Vertex refererVertex = m_dbTree.getVertex(refLinkInfo.m_referer);
            final Vertex refereeVertex = m_dbTree.getVertex(refLinkInfo.m_referee);
            final com.tinkerpop.blueprints.Edge refDBEdge = m_dbTree.getEdge (refereeVertex, refLinkInfo.m_pos);

            exposeRelation(refererVertex, refLinkInfo.m_pos, refDBEdge, refereeVertex);
        }
	}


    public void addReference(Object refererDBId, Object refereeDBId, int pos) {

        //TODO: move to VisualMindTree: Vertex refererVertex = getDBVertex(referer);
        Vertex refererVertex = m_dbTree.getVertex(refererDBId);
        Vertex refereeVertex = m_dbTree.getVertex(refereeDBId);
        com.tinkerpop.blueprints.Edge refEdge = m_dbTree.addRefEdge(refererVertex, refereeVertex, pos);

        exposeRelation(refererVertex, pos, refEdge, refereeVertex);

        /*TODO
        //move to VisualMindTree: Vertex refererVertex = getDBVertex(referer);
            return m_tree.getChild(referer, pos);
            */
    }

    public void removeReference(Object refererDBId, int pos) {
        Vertex refererVertex = m_dbTree.getVertex(refererDBId);
        m_dbTree.removeRefEdge(refererVertex, pos);
        hideRelation(refererDBId, pos);
    }

    public void moveChild (Object oldParentDBId, int oldPos, Object newParentDBId, int newPos)
	{
        if (oldParentDBId.equals(newParentDBId))
            return;

        Vertex oldParent = m_dbTree.getVertex(oldParentDBId);
        Vertex newParent = m_dbTree.getVertex(newParentDBId);

        EdgeVertex edgeVertex = m_dbTree.moveChild(oldParent, oldPos, newParent, newPos);

        hideRelation(oldParentDBId, oldPos);
        exposeRelation(newParentDBId, newPos, edgeVertex.m_edge, edgeVertex.m_vertex);
	}

    public void changeChildPos (final Object parentDBId, final int oldPos, final int newPos)
    {
        Vertex parent = m_dbTree.getVertex(parentDBId);
        m_dbTree.changeChildPos(parent, oldPos, newPos);

        visitNodeAvatares(parentDBId, new Visitor() {
            public void visit(Node parent) {
                m_tree.changeChildIndex(parent, oldPos, newPos);
            }
        });
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

	public Object getDBElementId(final Tuple tuple)
	{
		assert(m_tree.containsTuple(tuple));
		return tuple.get(sm_dbIdColumnName);
	}

    public int getChildCount(Node node)
    {
        if (node.getChildCount() > 0) {
            return node.getChildCount();
        } else {
            ArrayList childEdgesDBIds = (ArrayList)node.get(sm_outEdgeDBIdsPropName);
            System.out.println ("getChildCount = " + childEdgesDBIds);
            return childEdgesDBIds==null ? 0: childEdgesDBIds.size();
        }
    }

    public DBTree.InheritDirection getInheritDirection(Node from, Node to)
    {
        return m_dbTree.getInheritDirection((ArrayList) from.get(sm_inheritPathPropName),
                (ArrayList) to.get(sm_inheritPathPropName));
    }
}
