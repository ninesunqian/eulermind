package excitedmind;

import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import excitedmind.MindDB.EdgeVertex;
import excitedmind.MindDB.RefLinkInfo;
import prefuse.data.*;
import prefuse.util.collections.IntIterator;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class MindModel {
    Logger m_logger = Logger.getLogger(this.getClass().getName());

	final static String sm_dbIdColumnName = "dbElementId";

    final static String sm_outEdgeDBIdsPropName = MindDB.CHILD_EDGES_PROP_NAME;
    final static String sm_inheritPathPropName = MindDB.INHERIT_PATH_PROP_NAME;

	final static String sm_textPropName = "text";
    final static String sm_iconPropName = "icon";
	final static String sm_fontFamilyPropName = "fontFamily";
	final static String sm_fontSizePropName = "fontSize";
	final static String sm_boldPropName = "bold";
	final static String sm_italicPropName = "italic";
	final static String sm_underlinedPropName = "underlined";
	final static String sm_nodeColorPropName = "nodeColor";
	final static String sm_textColorPropName = "textColor";

    private final static String FAVORITE_INDEX_NAME = "favoriteIndex";
    private final static String FAVORITE_KEY_NAME = "favorite";


    private Index<Vertex> m_favoriteIndex;

    class VertexBasicInfo {
        String m_contextText;
        Object m_dbId;
        ArrayList<Object> m_inheritPath;

        VertexBasicInfo(Vertex vertex) {
            m_dbId = vertex.getId();
            m_inheritPath = m_mindDb.getInheritPath(vertex);
            m_contextText = getContextText(m_dbId);
        }
    }

    ArrayList<VertexBasicInfo> m_favoriteInfoes = new ArrayList<VertexBasicInfo>();

    public final static String sm_nodePropNames [] = {
            sm_textPropName,
            sm_outEdgeDBIdsPropName,
            sm_inheritPathPropName,

            sm_iconPropName,

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

	MindDB m_mindDb;
    ArrayList<Tree> m_trees = new ArrayList<Tree>();

	//return sorted copy of propName
	private void addTableProperties(String[] propNames, Table t)
	{
		t.addColumn(sm_dbIdColumnName, Object.class, null);

		for (String propName : propNames)
		{
			t.addColumn(propName, Object.class, null);
		}
	}

    boolean m_isDebuging;

	public MindModel(String dbPath)
	{
        m_logger.setLevel(Level.WARNING);

		m_mindDb = new MindDB(dbPath);
        m_mindDb.createFullTextVertexKeyIndex(sm_textPropName);
        m_favoriteIndex = m_mindDb.getOrCreateIndex(FAVORITE_INDEX_NAME);

        Vertex root = m_mindDb.getVertex(m_mindDb.getRootId());

        if (! m_favoriteIndex.get(FAVORITE_KEY_NAME, FAVORITE_KEY_NAME).iterator().hasNext()) {
            root.setProperty(sm_textPropName, "root");

            EdgeVertex edgeVertex = m_mindDb.addChild(root, 0);
            edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, "child_1");

            edgeVertex = m_mindDb.addChild(root, 1);
            edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, "child_2");

            m_mindDb.addRefEdge(root, root, 2);

            m_favoriteIndex.put(FAVORITE_KEY_NAME, FAVORITE_KEY_NAME, root);
        }

        for (Vertex vertex : m_favoriteIndex.get(FAVORITE_KEY_NAME, FAVORITE_KEY_NAME))  {
            m_favoriteInfoes.add(new VertexBasicInfo(vertex));
        }

        m_isDebuging = false;
        /*
        Pattern debugPattern = Pattern.compile("-Xdebubg|jdwp");
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (debugPattern.matcher(arg).find()) {
                m_isDebuging = true;
            }
        }
        */
	}

    public Tree findTree(Object rootId)
    {
        for (Tree tree : m_trees) {
            if (tree.getRoot().get(sm_dbIdColumnName).equals(rootId)) {
                return tree;
            }
        }
        return null;
    }

    public prefuse.data.Tree findOrPutTree(Object rootId, int depth)
    {
        Tree tree;

        tree = findTree(rootId);

        if (tree != null) {
            return tree;
        }

        tree = new Tree();
        Table displayNodeTable = tree.getNodeTable();
        Table displayEdgeTable = tree.getEdgeTable();

        addTableProperties(sm_nodePropNames, displayNodeTable);
        addTableProperties(sm_edgePropNames, displayEdgeTable);

        m_trees.add(tree);

        Node root = tree.addRoot();
        loadNodeProperties(m_mindDb.getVertex(rootId), root);

        final int initialLevel = depth;
        tree.deepTraverse(root, new Tree.Processor() {
            public boolean run(Node node, int level) {
                attachChildren(node);
                return level < initialLevel;
            }
        });

        return tree;
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

    private void storeElementProperties(com.tinkerpop.blueprints.Element dbElement, Tuple tuple, String keys[])
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
        Tree tree = (Tree)parent.getGraph();
		ArrayList<EdgeVertex> edgeVertexArray = m_mindDb.getChildrenAndReferents(getDBVertex(parent));
		
		if (edgeVertexArray == null || edgeVertexArray.size() == 0)
		{
			return;
		}

		for (EdgeVertex edgeVertex : edgeVertexArray)
		{
			Node child = tree.addChild(parent);
			Edge edge = tree.getEdge(parent, child);

			m_logger.info (getDBVertex(parent)+ "->" + edgeVertex.m_vertex+ "   :  " + edgeVertex.m_edge);
			loadNodeProperties(edgeVertex.m_vertex, child);
			loadEdgeProperties(edgeVertex.m_edge, edge);
		}
	}

    public void detachChildern (Node node)
    {
        Tree tree = (Tree)node.getGraph();
        tree.removeDescendants(node);
    }

	public void ascendRoot (Tree tree)
	{
		Node root = tree.getRoot();
		EdgeVertex edgeVertex = m_mindDb.getParent(getDBVertex(root));
		
		if (edgeVertex == null)
		{
			return;
		}
		
		Node newRoot = tree.addNode();
		Edge edge = tree.addEdge(newRoot, root);
		
		loadEdgeProperties(edgeVertex.m_edge, edge);
		loadNodeProperties(edgeVertex.m_vertex, newRoot);
		
		tree.setRoot(newRoot);
	}
	
	interface Visitor {
	 	abstract public void visit(Node node);
	}
	
	void visitNodeAvatares(Tree tree, Object dbId, Visitor visitor)
	{
        assert(dbId != null);

        Table nodeTable = tree.getNodeTable();

		IntIterator allRows = nodeTable.rows();

		ArrayList<Integer> aimRows = new ArrayList<Integer> ();
		m_logger.info ("need node's dbId is" + dbId);

		//collect the node with the same parentDBId to aimRows
		while (allRows.hasNext()) {
			int curRow = allRows.nextInt();

			if (nodeTable.get(curRow, sm_dbIdColumnName).equals(dbId)) {
				aimRows.add(curRow);
			}
		}

		Iterator<Integer> aimRowIter = aimRows.iterator();

		//attach blueprints node, to each node in aimRows
		while (aimRowIter.hasNext()) {
			int row = aimRowIter.next();
			
			if (nodeTable.isValidRow(row))
			{
				Node node = tree.getNode(row);
				visitor.visit(node);
			}
		}
	}

    //Maybe there are more than one reference edge link source target
    //The callers of exposeRelation has got target and dbEdge, so pass them as argument
    protected void exposeRelation(final Tree tree, final Object sourceId, final int edgePosInSourceNode,
                                  final com.tinkerpop.blueprints.Edge dbEdge, final Vertex target)
	{
        final Vertex sourceVertex = m_mindDb.getVertex(sourceId);

		visitNodeAvatares(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode) {

                //if children not attached, skip
                if (sourceNode.getChildCount() == 0 && getChildCount(sourceNode) > 0) {
                    return;
                }

                Node child = tree.addNode();
                Edge edge = tree.addChildEdge(sourceNode, child, edgePosInSourceNode);

                loadNodeProperties(target, child);
                loadEdgeProperties(dbEdge, edge);
            }
        });
	}

    private void hideRelation(final Tree tree, final Object sourceId, final int edgePosInSourceNode)
    {
        visitNodeAvatares(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode) {
                //its child is not displayed
                if (sourceNode.getChildCount() == 0)
                    return;

                Node child = tree.getChild(sourceNode, edgePosInSourceNode);
                tree.removeChild(child);
            }
        });

    }

    //return new child node
	public Object addChild(Object parentDBId, int pos, String text) {
		Vertex dbParent = m_mindDb.getVertex(parentDBId);
		EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);

        edgeVertex.m_vertex.setProperty(sm_textPropName, text);

        for (Tree tree : m_trees) {
            exposeRelation(tree, parentDBId, pos, edgeVertex.m_edge, edgeVertex.m_vertex);
        }

        return edgeVertex.m_vertex.getId();
	}

	//return the DBid of node
	public Object trashNode(Object parentDBId, int pos)
	{
        Vertex parent = m_mindDb.getVertex(parentDBId);
        final EdgeVertex edgeChild = m_mindDb.getChildOrReferent(parent, pos);
        Object removedDBId = edgeChild.m_vertex.getId();

        final ArrayList<Object> inheritPathOfTrashedNode =
                m_mindDb.getContainerProperty(edgeChild.m_vertex, sm_inheritPathPropName, true);

        final Object trashedId = edgeChild.m_vertex.getId();

        for (final Tree tree : m_trees) {
            tree.deepTraverse(tree.getRoot(),  new Tree.Processor() {
                public boolean run(Node node, int level) {
                    m_logger.info ("remove traverse: " + node.getString(sm_textPropName));

                    ArrayList inheritPathOfTreeNode = (ArrayList) node.get(sm_inheritPathPropName);

                    MindDB.InheritDirection inheritDirection = m_mindDb.getInheritDirection(
                            inheritPathOfTrashedNode, trashedId,
                            inheritPathOfTreeNode, getDBId(node));

                    System.out.println (edgeChild.m_vertex.getProperty(sm_textPropName) +"-->" + node.getString(sm_textPropName)
                            + ": " + inheritDirection);

                    if (node.get(sm_dbIdColumnName).equals(edgeChild.m_vertex.getId()) ||
                            inheritDirection == MindDB.InheritDirection.LINEAL_DESCENDANT ) {
                        tree.removeChild(node);
                        return false;
                    }
                    return true;
                }

            });
        }

        m_mindDb.trashSubTree(parent, pos);

        return removedDBId;
	}
	
	public void restoreNodeFromTrash(final Object dbId)
	{
        final Vertex restoredVertex = m_mindDb.getVertex(dbId);
        final MindDB.TrashedTreeContext context = m_mindDb.getTrashedTreeContext(restoredVertex);
        final Vertex parentVertex = m_mindDb.getVertex(context.m_parentId);

        final EdgeVertex edgeParent = m_mindDb.restoreTrashedSubTree(restoredVertex);

        for (Tree tree : m_trees) {
            exposeRelation(tree, parentVertex, context.m_pos, edgeParent.m_edge, restoredVertex);

            for (final RefLinkInfo refLinkInfo : context.m_refLinkInfos) {
                final Vertex referrerVertex = m_mindDb.getVertex(refLinkInfo.m_referrer);
                final Vertex referentVertex = m_mindDb.getVertex(refLinkInfo.m_referent);
                final com.tinkerpop.blueprints.Edge refDBEdge = m_mindDb.getEdge (referrerVertex, refLinkInfo.m_pos);

                exposeRelation(tree, referrerVertex, refLinkInfo.m_pos, refDBEdge, referentVertex);
            }
        }
	}

    boolean canAddReference(Node referrerNode, Node referentNode)
    {
        assert (referrerNode.getGraph() == referentNode.getGraph());
        return referrerNode != referentNode;
    }

    public void addReference(Object referrerDBId, int pos, Object referentDBId) {

        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        Vertex referentVertex = m_mindDb.getVertex(referentDBId);
        com.tinkerpop.blueprints.Edge refEdge = m_mindDb.addRefEdge(referrerVertex, referentVertex, pos);

        for (Tree tree : m_trees) {
            exposeRelation(tree, referrerVertex, pos, refEdge, referentVertex);
        }
    }


    public void removeReference(Object referrerDBId, int pos) {
        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        m_mindDb.removeRefEdge(referrerVertex, pos);

        for (Tree tree : m_trees) {
            hideRelation(tree, referrerDBId, pos);
        }
    }

    public void changeChildPos (final Object parentDBId, final int oldPos, final int newPos)
    {
        Vertex parent = m_mindDb.getVertex(parentDBId);
        m_mindDb.changeChildPos(parent, oldPos, newPos);

        for (final Tree tree : m_trees) {
            visitNodeAvatares(tree, parentDBId, new Visitor() {
                                              public void visit(Node parent) {
                    tree.changeChildIndex(parent, oldPos, newPos);
            }
            });
        }
    }

    public boolean canResetParent(Node node, Node newParent)
    {
        assert(node.getGraph() == newParent.getGraph());
        assert(node.getParent() != null);
        return (! sameDBNode(newParent, node))
                && (!isInDBSubTree(newParent, node));
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

        EdgeVertex edgeVertex = m_mindDb.handoverChild(oldParent, oldPos, newParent, newPos);

        for (Tree tree : m_trees) {
            hideRelation(tree, oldParentDBId, oldPos);
            exposeRelation(tree, newParentDBId, newPos, edgeVertex.m_edge, edgeVertex.m_vertex);
        }
	}

	public void setProperty(final Object dbId, final String key, final Object value)
	{
		Vertex dbNode = m_mindDb.getVertex(dbId);
        if (value == null) {
            dbNode.removeProperty(key);
        } else {
            dbNode.setProperty(key, value);
        }

        for (Tree tree : m_trees) {
            visitNodeAvatares(tree, dbId, new Visitor() {
                public void visit(Node node) {
                    node.set(key, value);
                }
            });
        }
	}

    public Object getProperty (final Object dbId, final String key)
    {
        Vertex dbNode = m_mindDb.getVertex(dbId);
        return dbNode.getProperty(key);
    }

	public Object getDBId(Tuple tuple)
	{
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
        return m_mindDb.getInheritDirection(fromInheritPath, getDBId(from), toInheritPath, getDBId(to));
    }

    public MindDB.InheritDirection getInheritDirection(Object fromDBId, Object toDBId)
    {
        Vertex fromVertex = m_mindDb.getVertex(fromDBId);
        Vertex toVertex = m_mindDb.getVertex(toDBId);
        return m_mindDb.getInheritRelation (fromVertex, toVertex);
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

    public String getNodeDebugInfo(Node node) {
        int row = node.getRow();
        String info = ((Integer)row).toString();
        info += "  ";

        Object dbId = getDBId(node);
        if (dbId != null) {
            ArrayList inheritPath = m_mindDb.getInheritPath(m_mindDb.getVertex(dbId));
            info += inheritPath.toString();
            info += " ";
            info += dbId.toString();
        } else {
            info += "placeholder";
        }
        return info;
    }

    //not use dbId for argument, becase the node saved the propperty
    public String getText(Node node)
    {
        String text = node.getString(sm_textPropName);
        if (m_isDebuging) {
            text += " D{" + getNodeDebugInfo(node) + "}";
        }
        return text;
    }

    public String getContextText(Object dbId)
    {
        Vertex vertex = m_mindDb.getVertex(dbId);
        String text = vertex.getProperty(sm_textPropName);
        EdgeVertex toParent = m_mindDb.getParent(vertex);

        if (toParent == null) {
            return text;
        } else {
            Vertex parent = toParent.m_vertex;
            String parentText = parent.getProperty(sm_textPropName);
            return parentText + " -> " + text;
        }
    }

    public void setText(Object dbId, String text)
    {
        setProperty(dbId, sm_textPropName, text);
    }

    public int getNodeColor (Node node)
    {
        return node.getInt(sm_nodeColorPropName);
    }

    public void setNodeColor (Object dbId, int rgba)
    {
        setProperty(dbId, sm_nodeColorPropName, rgba);
    }

    public String getFontFamily (Node node)
    {
        return node.getString(sm_fontFamilyPropName);
    }

    public void setFontFamily (Object dbId, String fontFamily)
    {
        setProperty(dbId, sm_fontFamilyPropName, fontFamily);
    }

    public int getFontSize (Node node)
    {
        return node.getInt(sm_fontSizePropName);
    }

    public void setFontSize (Object dbId, int size)
    {
        setProperty(dbId, sm_fontSizePropName, size);
    }

    public boolean isRefEdge (Edge edge)
    {
        return MindDB.EdgeType.values()[(Integer)edge.get(sm_edgeTypePropName)] == MindDB.EdgeType.REFERENCE;
    }


    public Stack<Integer> getNodePath(Node node)
    {
        Tree tree = (Tree)node.getGraph();
        Stack<Integer> path = new Stack<Integer>();

        Node climber = node;
        Node root = tree.getRoot();

        assert (climber != null);

        while (climber != root)
        {
            path.add(0, tree.getIndexInSiblings(climber));
            climber = climber.getParent();
            if (climber.getRow()==root.getRow() && climber != root) {
                m_logger.info("aaaaaaaaaaaa");
            }
        }

        return path;
    }

    public Node getNodeByPath(Tree tree, Stack<Integer> path)
    {
        Node node = tree.getRoot();

        for (int pos : path) {

            if (node.getChildCount() == 0) {
                attachChildren(node);
            }

            node = node.getChild(pos);

            if (node == null) {
                return null;
            }
        }

        return node;
    }

    public boolean isInFavorite(Object dbId) {
        for (VertexBasicInfo info: m_favoriteInfoes) {
            if (info.m_dbId.equals(dbId)) {
                return true;
            }
        }
        return false;
    }

    public void addToFavorite(Object dbId) {
        m_favoriteIndex.put(FAVORITE_KEY_NAME, FAVORITE_KEY_NAME, m_mindDb.getVertex(dbId));

        assert(!isInFavorite(dbId));
        m_favoriteInfoes.add(new VertexBasicInfo(m_mindDb.getVertex(dbId)));
    }

    public void removeFromFavorite(Object dbId) {
        m_favoriteIndex.remove(FAVORITE_KEY_NAME, FAVORITE_KEY_NAME, m_mindDb.getVertex(dbId));

        for (VertexBasicInfo info: m_favoriteInfoes) {
            if (info.m_dbId.equals(dbId)) {
                m_favoriteInfoes.remove(info);
                break;
            }
        }
    }

    public VertexBasicInfo getVertexBasicInfo(Object dbId) {
        return new VertexBasicInfo(m_mindDb.getVertex(dbId));
    }
}
