package excitedmind;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.util.collections.IntIterator;

import com.tinkerpop.blueprints.Vertex;

import excitedmind.MindDB.EdgeVertex;
import excitedmind.MindDB.RefLinkInfo;



public class MindTree {
    Logger m_logger = Logger.getLogger(this.getClass().getName());

	final static String sm_dbIdColumnName = "dbElement";

    final static String sm_outEdgeDBIdsPropName = MindDB.CHILD_EDGES_PROP_NAME;
    final static String sm_inheritPathPropName = MindDB.INHERIT_PATH_PROP_NAME;

	final static String sm_textPropName = "text";
	final static String sm_fontFamilyPropName = "fontFamily";
	final static String sm_fontSizePropName = "fontSize";
	final static String sm_boldPropName = "bold";
	final static String sm_italicPropName = "italic";
	final static String sm_underlinedPropName = "underlined";
	final static String sm_nodeColorPropName = "nodeColor";
	final static String sm_textColorPropName = "textColor";

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

	public final static String sm_edgeTypePropName = MindDB.EDGE_TYPE_PROP_NAME;
	public final static String sm_edgeColorPropName = "edgeColor";
    public final static String sm_edgePropNames [] = {sm_edgeTypePropName, sm_edgeColorPropName};

	final public Tree m_displayTree;
	final public Table m_displayNodeTable;
	final public Table m_displayEdgeTable;
	
	private String m_edgeTypePropName;
	
	MindDB m_mindDb;


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
        m_logger.setLevel(Level.INFO);

		m_displayTree = new Tree();
		
		m_mindDb = new MindDB(dbPath);
        m_mindDb.createFullTextVertexKeyIndex(sm_textPropName);

		m_displayNodeTable = m_displayTree.getNodeTable();
		m_displayEdgeTable = m_displayTree.getEdgeTable();
		

		addTableProperties(sm_nodePropNames, m_displayNodeTable);
		addTableProperties(sm_edgePropNames, m_displayEdgeTable);
		
		Node root = m_displayTree.addRoot();
		loadNodeProperties(m_mindDb.getVertex(rootId), root);

        final int initialLevel = 2;
		m_displayTree.deepTraverse(root, new Tree.Processor() {
            public boolean run(Node node, int level) {
                attachChildren(node);
                return level < initialLevel;
            }

        }, 0);
        m_displayNodeTable.addTableListener(new TableListener() {
            @Override
            public void tableChanged(Table t, int start, int end, int col, int type) {
                //To change body of implemented methods use File | Settings | File Templates.
                if (type ==  EventConstants.DELETE) {
                    for (int i=start; i<=end; i++) {
                        Tuple tuple = t.getTuple(i);
                        Object dbId = tuple.get(sm_dbIdColumnName);
                        m_logger.info ("delete : " + dbId.toString());
                    }
                }
            }
        });

        m_displayTree.removeChild(m_displayTree.getRoot().getFirstChild());

	}

	private void loadElementProperties(com.tinkerpop.blueprints.Element dbElement, Tuple tuple, String keys[])
	{
        assert(dbElement != null && dbElement.getId() != null);

		tuple.set(sm_dbIdColumnName, dbElement.getId());
		for (String key : keys)
		{
            Object value;
            if (key == sm_inheritPathPropName || key == sm_outEdgeDBIdsPropName) {
                value = m_mindDb.getContainerProperty((Vertex)dbElement, key, true);
                assert(((ArrayList)value).size() != 0);
            } else {
                value = dbElement.getProperty(key);
            }
            tuple.set(key, value);

		}
	}

    private static void storeElementProperties(com.tinkerpop.blueprints.Element dbElement, Tuple tuple, String keys[])
    {
        for (String key : keys)
        {
            if (key != sm_dbIdColumnName) {
                Object value = tuple.get(key);
                if (value == null) {
                    dbElement.removeProperty(key);
                } else {
                    if (key == sm_inheritPathPropName) {
                        assert(((ArrayList)value).size() != 0);
                    }
                    dbElement.setProperty(key, value);
                }
            }
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

    protected void storeNodeProperties(Vertex vertex, Node node)
    {
        storeElementProperties(vertex, node, sm_nodePropNames);
    }

    private void storeEdgeProperties (com.tinkerpop.blueprints.Edge dbEdge, Edge edge)
    {
        storeElementProperties(dbEdge, edge, sm_edgePropNames);
    }

	private Vertex getDBVertex(Node node)
	{
		Object dbId = node.get(sm_dbIdColumnName);
		return m_mindDb.getVertex(dbId);
	}
	
	private com.tinkerpop.blueprints.Edge getDBEdge (Edge edge)
	{
		return m_mindDb.getEdge(edge.get(sm_dbIdColumnName));
	}

	public void attachChildren (Node parent)
	{
		ArrayList<EdgeVertex> edgeVertexArray = m_mindDb.getChildrenAndReferents(getDBVertex(parent));
		
		if (edgeVertexArray == null || edgeVertexArray.size() == 0)
		{
			return;
		}

		for (EdgeVertex edgeVertex : edgeVertexArray)
		{
			Node child = m_displayTree.addChild(parent);
			Edge edge = m_displayTree.getEdge(parent, child);

			m_logger.info (getDBVertex(parent)+ "->" + edgeVertex.m_vertex+ "   :  " + edgeVertex.m_edge);
			loadNodeProperties(edgeVertex.m_vertex, child);
			loadEdgeProperties(edgeVertex.m_edge, edge);
		}
	}

    public void detachChildern (Node node)
    {
        m_displayTree.removeDescendants(node);
    }

	public void setRoot (Node node)
	{
		if (node == m_displayTree.getRoot())
		{
			return;
		}
		else
		{
			m_displayTree.setRoot(node);
			//FIXME: update the visualTree;
		}
	}
	
	public void ascendRoot ()
	{
		Node root = m_displayTree.getRoot();
		EdgeVertex edgeVertex = m_mindDb.getParent(getDBVertex(root));
		
		if (edgeVertex == null)
		{
			return;
		}
		
		Node newRoot = m_displayTree.addNode();
		Edge edge = m_displayTree.addEdge(newRoot, root);
		
		loadEdgeProperties(edgeVertex.m_edge, edge);
		loadNodeProperties(edgeVertex.m_vertex, newRoot);
		
		m_displayTree.setRoot(newRoot);
	}
	
	interface Visitor {
	 	abstract public void visit (Node node);
	}
	
	void visitNodeAvatares(Object dbId, Visitor visiter)
	{
        assert(dbId != null);

		IntIterator allRows = m_displayNodeTable.rows();

		ArrayList<Integer> aimRows = new ArrayList<Integer> ();
		m_logger.info ("need node's dbId is" + dbId);

		//collect the node with the same parentDBId to aimRows
		while (allRows.hasNext()) {
			int curRow = allRows.nextInt();

			if (m_displayNodeTable.get(curRow, sm_dbIdColumnName).equals(dbId)) {
				aimRows.add(curRow);
			}
		}

		Iterator<Integer> aimRowIter = aimRows.iterator();

		//attach blueprints node, to each node in aimRows
		while (aimRowIter.hasNext()) {
			int row = aimRowIter.next();
			
			if (m_displayNodeTable.isValidRow(row))
			{
				Node node = m_displayTree.getNode(row);
				visiter.visit (node);
			}
		}
	}

    //Maybe there are more than one reference edge link source target
    //The callers of exposeRelation has got target and dbEdge, so pass them as argument
    protected void exposeRelation(final Object sourceId, final int edgePosInSourceNode,
                                  final com.tinkerpop.blueprints.Edge dbEdge, final Vertex target)
	{
        final Vertex sourceVertex = m_mindDb.getVertex(sourceId);

		visitNodeAvatares(sourceId, new Visitor() {
            public void visit(Node sourceNode) {

                //if children not attached, skip
                if (sourceNode.getChildCount() == 0 && getChildCount(sourceNode) > 0) {
                    return;
                }

                Node child = m_displayTree.addNode();
                Edge edge = m_displayTree.addChildEdge(sourceNode, child, edgePosInSourceNode);

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

                Node child = m_displayTree.getChild(sourceNode, edgePosInSourceNode);
                m_displayTree.removeChild(child);
            }
        });

    }

    //return new child node
	public Object addChild(Object parentDBId, int pos, String text) {
		Vertex dbParent = m_mindDb.getVertex(parentDBId);
		EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);

        edgeVertex.m_vertex.setProperty(sm_textPropName, text);

		exposeRelation(parentDBId, pos, edgeVertex.m_edge, edgeVertex.m_vertex);

        return edgeVertex.m_vertex.getId();

        //TODO add to MindTreeController return m_displayTree.getChild(parent, pos);
	}

	//return the DBid of node
	public Object trashNode(Object parentDBId, int pos)
	{
        Vertex parent = m_mindDb.getVertex(parentDBId);
        final EdgeVertex edgeChild = m_mindDb.getChildOrReferent(parent, pos);
        Object removedDBId = edgeChild.m_vertex.getId();

        final ArrayList<Object> inheritPathOfTrashedNode =
                m_mindDb.getContainerProperty(edgeChild.m_vertex, sm_inheritPathPropName, true);

        m_displayTree.deepTraverse(m_displayTree.getRoot(),  new Tree.Processor() {
            public boolean run(Node node, int level) {

                m_logger.info ("remove traverse: " + node.getString(sm_textPropName));

                ArrayList inheritPathOfTreeNode = (ArrayList) node.get(sm_inheritPathPropName);

                MindDB.InheritDirection inheritDirection = m_mindDb.getInheritDirection(inheritPathOfTrashedNode,
                        inheritPathOfTreeNode);

                System.out.println (edgeChild.m_vertex.getProperty(sm_textPropName) +"-->" + node.getString(sm_textPropName)
                        + ": " + inheritDirection);

                if (node.get(sm_dbIdColumnName).equals(edgeChild.m_vertex.getId()) ||
                        inheritDirection == MindDB.InheritDirection.LINEAL_DESCENDANT ) {
                    m_displayTree.removeChild(node);
                    return false;
                }
                return true;
            }

        });

        m_mindDb.trashSubTree(parent, pos);

        return removedDBId;
	}
	
	public void restoreNodeFromTrash(final Object dbId)
	{
        final Vertex restoredVertex = m_mindDb.getVertex(dbId);
        final MindDB.TrashedTreeContext context = m_mindDb.getTrashedTreeContext(restoredVertex);
        final Vertex parentVertex = m_mindDb.getVertex(context.m_parentId);

        final EdgeVertex edgeParent = m_mindDb.restoreTrashedSubTree(restoredVertex);

        exposeRelation(parentVertex, context.m_pos, edgeParent.m_edge, restoredVertex);

        for (final RefLinkInfo refLinkInfo : context.m_refLinkInfos) {
            final Vertex referrerVertex = m_mindDb.getVertex(refLinkInfo.m_referrer);
            final Vertex referentVertex = m_mindDb.getVertex(refLinkInfo.m_referent);
            final com.tinkerpop.blueprints.Edge refDBEdge = m_mindDb.getEdge (referrerVertex, refLinkInfo.m_pos);

            exposeRelation(referrerVertex, refLinkInfo.m_pos, refDBEdge, referentVertex);
        }
	}


    public void addReference(Object referrerDBId, int pos, Object referentDBId) {

        //TODO: move to MindTreeController: Vertex referrerVertex = getDBVertex(referrer);
        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        Vertex referentVertex = m_mindDb.getVertex(referentDBId);
        com.tinkerpop.blueprints.Edge refEdge = m_mindDb.addRefEdge(referrerVertex, referentVertex, pos);

        exposeRelation(referrerVertex, pos, refEdge, referentVertex);

        /*TODO
        //move to MindTreeController: Vertex referrerVertex = getDBVertex(referrer);
            return m_displayTree.getChild(referrer, pos);
            */
    }

    public void removeReference(Object referrerDBId, int pos) {
        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        m_mindDb.removeRefEdge(referrerVertex, pos);
        hideRelation(referrerDBId, pos);
    }

    public void changeChildPos (final Object parentDBId, final int oldPos, final int newPos)
    {
        Vertex parent = m_mindDb.getVertex(parentDBId);
        m_mindDb.changeChildPos(parent, oldPos, newPos);

        visitNodeAvatares(parentDBId, new Visitor() {
            public void visit(Node parent) {
                m_displayTree.changeChildIndex(parent, oldPos, newPos);
            }
        });
    }

    public void moveChild(Object oldParentDBId, int oldPos, Object newParentDBId, int newPos)
	{
        if (oldParentDBId.equals(newParentDBId)) {
            if (oldPos != newPos) {
                changeChildPos(oldParentDBId, oldPos, newPos);
            }

            return;
        }

        Vertex oldParent = m_mindDb.getVertex(oldParentDBId);
        Vertex newParent = m_mindDb.getVertex(newParentDBId);

        EdgeVertex edgeVertex = m_mindDb.moveChild(oldParent, oldPos, newParent, newPos);

        hideRelation(oldParentDBId, oldPos);
        exposeRelation(newParentDBId, newPos, edgeVertex.m_edge, edgeVertex.m_vertex);
	}

	public void setNodeProperty (final Object dbId, final String key, final Object value)
	{
		Vertex dbNode = m_mindDb.getVertex(dbId);
		dbNode.setProperty(key, value);
		
		visitNodeAvatares(dbId, new Visitor() {
            public void visit(Node node) {
                node.set(key, value);
            }
        });
	}

	public Object getDBId(final Tuple tuple)
	{
		assert(m_displayTree.containsTuple(tuple));
		return tuple.get(sm_dbIdColumnName);
	}

    public int getChildCount(Node node)
    {
        if (node.getChildCount() > 0) {
            return node.getChildCount();
        } else {
            ArrayList childEdgesDBIds = (ArrayList)node.get(sm_outEdgeDBIdsPropName);
            m_logger.info ("getChildCount = " + childEdgesDBIds);
            return childEdgesDBIds==null ? 0: childEdgesDBIds.size();
        }
    }

    public MindDB.InheritDirection getInheritDirection(Node from, Node to)
    {
        ArrayList fromInheritPath = (ArrayList) from.get(sm_inheritPathPropName);
        ArrayList toInheritPath = (ArrayList) to.get(sm_inheritPathPropName);
        return m_mindDb.getInheritDirection(fromInheritPath, toInheritPath);
    }

    public boolean sameDBNode(Node n1, Node n2)
    {
        return getDBId(n1) == getDBId(n2);
    }

    public boolean isInDBSubTree(Node node, Node treeRoot)
    {
        MindDB.InheritDirection inheritDirection = getInheritDirection(node, treeRoot);
        return inheritDirection == MindDB.InheritDirection.LINEAL_ANCESTOR ||
                inheritDirection == MindDB.InheritDirection.SELF;

    }

    public String getText(Node node)
    {
        return node.getString(sm_textPropName);
    }

    public void setText(Node node, String text)
    {
        setNodeProperty (getDBId(node), sm_textPropName, text);
    }

    public int getNodeColor (Node node)
    {
        return node.getInt(sm_nodeColorPropName);
    }

    public void setNodeColor (Node node, int rgba)
    {
        setNodeProperty (getDBId(node), sm_nodeColorPropName, rgba);
    }

    public String getFontFamily (Node node)
    {
        return node.getString(sm_fontFamilyPropName);
    }

    public void setFontFamily (Node node, String fontFamily)
    {
        setNodeProperty (getDBId(node), sm_fontFamilyPropName, fontFamily);
    }

    public int getFontSize (Node node)
    {
        return node.getInt(sm_fontSizePropName);
    }

    public void getFontSize (Node node, int size)
    {
        setNodeProperty (getDBId(node), sm_fontSizePropName, size);
    }

    public boolean isRefEdge (Edge edge)
    {
        return MindDB.EdgeType.values()[(Integer)edge.get(sm_edgeTypePropName)] == MindDB.EdgeType.REFERENCE;
    }
}
