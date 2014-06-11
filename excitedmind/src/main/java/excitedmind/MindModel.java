package excitedmind;

import com.tinkerpop.blueprints.*;
import excitedmind.MindDB.EdgeVertex;
import excitedmind.MindDB.RefLinkInfo;
import prefuse.data.*;
import prefuse.data.Edge;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.util.TypeLib;
import prefuse.util.collections.IntIterator;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.VisualTree;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MindModel {
    static Logger s_logger = Logger.getLogger("MindModel");

	final static String sm_dbIdColumnName = "dbElementId";

    final static private String sm_outEdgeDBIdsPropName = MindDB.CHILD_EDGES_PROP_NAME;
    final static private String sm_inheritPathPropName = MindDB.INHERIT_PATH_PROP_NAME;

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

    static private Hashtable<String, Class> sm_propertyClassMap = new Hashtable<String, Class>();

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

    private static void fillPropertyClassMap()
    {
        if (sm_propertyClassMap.size() > 0) {
            return;
        }

        sm_propertyClassMap.put(sm_textPropName, String.class);
        sm_propertyClassMap.put(sm_textPropName, String.class);

        sm_propertyClassMap.put(sm_iconPropName, String.class);

        sm_propertyClassMap.put(sm_fontFamilyPropName, String.class);
        sm_propertyClassMap.put(sm_fontSizePropName, Integer.class);

        sm_propertyClassMap.put(sm_boldPropName, Boolean.class);
        sm_propertyClassMap.put(sm_italicPropName, Boolean.class);
        sm_propertyClassMap.put(sm_underlinedPropName, Boolean.class);

        sm_propertyClassMap.put(sm_nodeColorPropName, Integer.class);
        sm_propertyClassMap.put(sm_textColorPropName, Integer.class);
    }

	public MindModel(String dbPath)
	{
        s_logger.setLevel(Level.WARNING);

        fillPropertyClassMap();

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

    private static void verifyElementProperties(com.tinkerpop.blueprints.Element dbElement,
                                                   Tuple tuple, String keys[])
    {

        assert dbElement.getId().equals(tuple.get(sm_dbIdColumnName));

        for (String key : keys)
        {
            Object tupleValue = tuple.get(key);
            Object dbElementValue = dbElement.getProperty(key);

            if (key == sm_inheritPathPropName || key == sm_outEdgeDBIdsPropName) {
                assert tupleValue == dbElementValue || arrayEqual((ArrayList)tupleValue, (ArrayList)dbElementValue);
            } else {
                assert tupleValue == dbElementValue || tupleValue.equals(dbElementValue);
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

        if (childrenAttached(parent)) {
            return;
        }


		ArrayList<EdgeVertex> edgeVertexArray = m_mindDb.getChildrenAndReferents(getDBVertex(parent));
		
		if (edgeVertexArray == null || edgeVertexArray.size() == 0)
		{
			return;
		}


		for (EdgeVertex edgeVertex : edgeVertexArray)
		{
			Node child = tree.addChild(parent);
			Edge edge = tree.getEdge(parent, child);

			s_logger.info(getDBVertex(parent) + "->" + edgeVertex.m_vertex + "   :  " + edgeVertex.m_edge);
			loadNodeProperties(edgeVertex.m_vertex, child);
			loadEdgeProperties(edgeVertex.m_edge, edge);

            verfyNode(child, false);
		}

        verfyNode(parent, true);
	}

    public void detachChildern (Node node)
    {
        Tree tree = (Tree)node.getGraph();
        tree.removeDescendants(node);
    }

	interface Visitor {
	 	abstract public void visit(Node node);
	}

    static ArrayList<Integer> getNodeAvatars(Tree tree, Object dbId)
    {
        Table nodeTable = tree.getNodeTable();
        IntIterator allRows = nodeTable.rows();

        ArrayList<Integer> aimRows = new ArrayList<Integer> ();

        //collect the node with the same parentDBId to aimRows
        while (allRows.hasNext()) {
            int curRow = allRows.nextInt();

            assert (curRow >= 0);

            if (nodeTable.get(curRow, sm_dbIdColumnName).equals(dbId)) {
                aimRows.add(curRow);
            }
        }

        return aimRows;
    }
	
	void visitNodeAvatars(Tree tree, Object dbId, Visitor visitor)
	{
        assert(dbId != null);

        Table nodeTable = tree.getNodeTable();

        ArrayList<Integer> aimRows = getNodeAvatars(tree, dbId);

		//attach blueprints node, to each node in aimRows
        for (Integer row : aimRows) {
			if (nodeTable.isValidRow(row))
			{
				Node node = tree.getNode(row);
				visitor.visit(node);
			}
		}
	}


    protected void exposeRelation(Node sourceNode, int pos, com.tinkerpop.blueprints.Edge dbEdge, Vertex target)
    {
        assert (sourceNode != null);
        assert(sourceNode.isValid());

        Tree tree = (Tree)sourceNode.getGraph();

        Node child = tree.addNode();
        Edge edge = tree.addChildEdge(sourceNode, child, pos);

        ArrayList<Object> childrenEdges = (ArrayList<Object>)sourceNode.get(sm_outEdgeDBIdsPropName);
        childrenEdges.add(pos, dbEdge.getId());

        loadNodeProperties(target, child);
        loadEdgeProperties(dbEdge, edge);

        verfyNode(sourceNode, true);
        verfyNode(sourceNode.getChild(pos), false);
    }

    protected void hideRelation(Node sourceNode, int pos)
    {
        assert (sourceNode != null);
        assert (sourceNode.isValid());
        Tree tree = (Tree)sourceNode.getGraph();

        ArrayList<Object> childrenEdges = (ArrayList<Object>)sourceNode.get(sm_outEdgeDBIdsPropName);
        childrenEdges.remove(pos);

        if (sourceNode.getChildCount() > 0) {
            Node child = tree.getChild(sourceNode, pos);
            tree.removeChild(child);
        }
        verfyNode(sourceNode, false);

    }

    //Maybe there are more than one reference edge link source target
    //The callers of exposeUnfoldedNodesRelations has got target and dbEdge, so pass them as argument
    protected void exposeUnfoldedNodesRelations(final Tree tree, final Object sourceId, final int edgePosInSourceNode,
                                                final com.tinkerpop.blueprints.Edge dbEdge, final Vertex target)
	{
        final Vertex sourceVertex = m_mindDb.getVertex(sourceId);

		visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                ArrayList<Object> outEdgeDBIdsInNode = (ArrayList<Object>)sourceNode.get(sm_outEdgeDBIdsPropName);
                ArrayList<Object> outEdgeDBIdsInVertex = m_mindDb.getEdgeIDsToChildren(getDBVertex(sourceNode), true);

                //if this node is updated, skip
                if (arrayEqual(outEdgeDBIdsInNode, outEdgeDBIdsInVertex)) {
                    verfyNode(sourceNode, false);
                    return;
                }

                //if children not attached, skip
                if (sourceNode.getChildCount() == 0 && outEdgeDBIdsInNode.size() > 0) {
                    outEdgeDBIdsInNode.add(edgePosInSourceNode, dbEdge.getId());
                    verfyNode(sourceNode, false);
                    return;
                }

                exposeRelation(sourceNode, edgePosInSourceNode, dbEdge, target);

            }
        });
	}

    private void hideRelations(final Tree tree, final Object sourceId, final int edgePosInSourceNode)
    {
        visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                ArrayList<Object> outEdgeDBIdsInNode = (ArrayList<Object>)sourceNode.get(sm_outEdgeDBIdsPropName);
                ArrayList<Object> outEdgeDBIdsInVertex = m_mindDb.getEdgeIDsToChildren(getDBVertex(sourceNode), true);

                //if this node is updated, skip
                if (arrayEqual(outEdgeDBIdsInNode, outEdgeDBIdsInVertex)) {
                    verfyNode(sourceNode, false);
                    return;
                }

                //its child is not displayed
                if (sourceNode.getChildCount() == 0) {
                    outEdgeDBIdsInNode.remove(edgePosInSourceNode);
                    verfyNode(sourceNode, false);
                    return;
                }

                hideRelation(sourceNode, edgePosInSourceNode);
                verfyNode(sourceNode, false);
            }
        });
    }

    private void hideRelationYes(Object sourceId, int edgePosInSourceNode)
    {
        for (final Tree tree : m_trees) {
            hideRelations(tree, sourceId, edgePosInSourceNode);
        }
    }

    protected void exposeRelationYes(Node node, int pos, com.tinkerpop.blueprints.Edge dbEdge, Vertex vertex)
    {
        exposeRelation(node, pos, dbEdge, vertex);
        verfyNode(node, true);

        for (Tree tree : m_trees) {
            exposeUnfoldedNodesRelations(tree, getDBId(node), pos, dbEdge, vertex);
        }
    }

    //return new child node
	public Object addChild(Node parent, int pos, String text)
    {
        if (! childrenAttached(parent)) {
            attachChildren(parent);
        }

        Object parentDBId = getDBId(parent);
		Vertex dbParent = m_mindDb.getVertex(parentDBId);
		EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);

        edgeVertex.m_vertex.setProperty(sm_textPropName, text);

        exposeRelationYes(parent, pos, edgeVertex.m_edge, edgeVertex.m_vertex);

        return edgeVertex.m_vertex.getId();
	}

	//return the DBid of node
	public Object trashNode(Object parentDBId, int pos)
	{
        Vertex parent = m_mindDb.getVertex(parentDBId);
        final EdgeVertex edgeChild = m_mindDb.getChildOrReferent(parent, pos);

        Object removedDBId = edgeChild.m_vertex.getId();

        m_mindDb.trashSubTree(parent, pos);

        hideRelationYes(parentDBId, pos);

        return removedDBId;
	}
	
	public void restoreNodeFromTrash(Node parent, final Object dbId)
	{
        if (! childrenAttached(parent)) {
            attachChildren(parent);
        }

        final Vertex restoredVertex = m_mindDb.getVertex(dbId);
        final MindDB.TrashedTreeContext context = m_mindDb.getTrashedTreeContext(restoredVertex);
        final Vertex parentVertex = m_mindDb.getVertex(context.m_parentId);

        final EdgeVertex edgeParent = m_mindDb.restoreTrashedSubTree(restoredVertex);

        exposeRelationYes(parent, context.m_pos, edgeParent.m_edge, restoredVertex);

        for (final RefLinkInfo refLinkInfo : context.m_refLinkInfos) {
            final Vertex referrerVertex = m_mindDb.getVertex(refLinkInfo.m_referrer);
            final Vertex referentVertex = m_mindDb.getVertex(refLinkInfo.m_referent);
            final com.tinkerpop.blueprints.Edge refDBEdge = m_mindDb.getEdge (referrerVertex, refLinkInfo.m_pos);

            for (final Tree tree : m_trees) {
                exposeUnfoldedNodesRelations(tree, referrerVertex, refLinkInfo.m_pos, refDBEdge, referentVertex);
            }
        }
	}

    boolean canAddReference(Node referrerNode, Node referentNode)
    {
        assert (referrerNode.getGraph() == referentNode.getGraph());
        return referrerNode != referentNode;
    }

    public void addReference(Node referrerNode, int pos, Object referentDBId) {
        if (! childrenAttached(referrerNode)) {
            attachChildren(referrerNode);
        }

        Object referrerDBId  = getDBId(referrerNode);
        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        Vertex referentVertex = m_mindDb.getVertex(referentDBId);
        com.tinkerpop.blueprints.Edge refEdge = m_mindDb.addRefEdge(referrerVertex, referentVertex, pos);

        exposeRelationYes(referrerNode, pos, refEdge, referentVertex);
    }


    public void removeReference(Object referrerDBId, int pos) {
        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        m_mindDb.removeRefEdge(referrerVertex, pos);
        hideRelationYes(referrerDBId, pos);
    }

    public void changeChildPos(final Object parentDBId, final int oldPos, final int newPos)
    {
        Vertex parent = m_mindDb.getVertex(parentDBId);
        m_mindDb.changeChildPos(parent, oldPos, newPos);

        for (final Tree tree : m_trees) {
            visitNodeAvatars(tree, parentDBId,
                new Visitor() {
                    public void visit(Node parent)
                    {
                        ArrayList<Object> outEdgeDBIds = (ArrayList<Object>)parent.get(sm_outEdgeDBIdsPropName);
                        Object edgeId = outEdgeDBIds.remove(oldPos);
                        if (oldPos < newPos) {
                            outEdgeDBIds.add(newPos - 1, edgeId);
                        } else {
                            outEdgeDBIds.add(newPos, edgeId);
                        }

                        tree.changeChildIndex(parent, oldPos, newPos);
                        verfyNode(parent, false);
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


	public void setProperty(final Object dbId, final String key, final Object value)
	{
		Vertex dbNode = m_mindDb.getVertex(dbId);
        if (value == null) {
            dbNode.removeProperty(key);
        } else {
            assert (TypeLib.typeCheck(sm_propertyClassMap.get(key), value));
            dbNode.setProperty(key, value);
        }

        for (Tree tree : m_trees) {
            visitNodeAvatars(tree, dbId, new Visitor() {
                public void visit(Node node)
                {
                    node.set(key, value);
                    verfyNode(node, false);
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

    static public int getDBChildCount(Node node)
    {
        ArrayList childEdgesDBIds = (ArrayList)node.get(sm_outEdgeDBIdsPropName);
        s_logger.info("getDBChildCount = " + childEdgesDBIds);
        return childEdgesDBIds==null ? 0: childEdgesDBIds.size();
    }

    static public boolean childrenAttached(Node node)
    {
        return getDBChildCount(node) == node.getChildCount();
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
        return node.getString(sm_textPropName);
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

    public boolean isRefEdge(Edge edge)
    {
        return MindDB.EdgeType.values()[(Integer)edge.get(sm_edgeTypePropName)] == MindDB.EdgeType.REFERENCE;
    }

    public boolean isRefNode(Node node)
    {
        Tree tree = (Tree)node.getGraph();
        Node parent = node.getParent();
        Edge edge = tree.getEdge(parent, node);
        return isRefEdge(edge);
    }


    public ArrayList<Integer> getNodePath(Node node)
    {
        Tree tree = (Tree)node.getGraph();
        ArrayList<Integer> path = new ArrayList<Integer>();

        Node climber = node;
        Node root = tree.getRoot();

        assert (climber != null);

        while (climber != root)
        {
            path.add(0, tree.getIndexInSiblings(climber));
            climber = climber.getParent();
            if (climber.getRow()==root.getRow() && climber != root) {
                s_logger.info("aaaaaaaaaaaa");
            }
        }

        return path;
    }

    public Node getNodeByPath(Tree tree, ArrayList<Integer> path)
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

    static final String MIRROR_X = "mirrorX";
    static final String MIRROR_Y = "mirrorY";

    static public void addNodeMirrorXYColumn(Tree tree, VisualTree visualTree)
    {
        final Table nodeTable = tree.getNodeTable();
        final VisualTable nodeItemTable = (VisualTable)visualTree.getNodeTable();

        nodeTable.addColumn(MIRROR_X, double.class, 0.0);
        nodeTable.addColumn(MIRROR_Y, double.class, 0.0);

        nodeItemTable.addTableListener(new TableListener() {
            @Override
            public void tableChanged(Table t, int start, int end, int col, int type)
            {
                if (type == EventConstants.UPDATE) {
                    if (col == nodeItemTable.getColumnNumber(VisualItem.X)) {
                        for (int row = start; row <= end; row++) {
                            nodeTable.setDouble(row, "mirrorX", nodeItemTable.getX(row));
                        }

                    } else if (col == nodeItemTable.getColumnNumber(VisualItem.Y)) {
                        for (int row = start; row <= end; row++) {
                            nodeTable.setDouble(row, "mirrorY", nodeItemTable.getX(row));
                        }

                    }
                }
            }
        });
    }

    private static double getNodeDistanceSquare(Node node1, Node node2)
    {
        double x1 = node1.getDouble(MIRROR_X);
        double y1 = node1.getDouble(MIRROR_Y);
        double x2 = node2.getDouble(MIRROR_X);
        double y2 = node2.getDouble(MIRROR_Y);

        return (x1 - x2) * (x1 - x2)  + (y1 - y2) * (y1 - y2) ;
    }

    private static class NodeAvatarsPairingInfo {
        HashMap<Integer, Integer> m_nodeAvatarPairs;
        ArrayList<Integer> m_nodeAvatars1Alone;
        ArrayList<Integer> m_nodeAvatars2Alone;

    }

    private static NodeAvatarsPairingInfo pairNodeAvatars(Tree tree, Object dbId1, Object dbId2,
                                             int enforceNode1, int enforceNode2)
    {
        final ArrayList<Integer> nodeAvatars1 = getNodeAvatars(tree, dbId1);
        final ArrayList<Integer> nodeAvatars2 = getNodeAvatars(tree, dbId2);
        final HashMap<Integer, Integer> pairs = new HashMap<Integer, Integer>();

        if (enforceNode1 >= 0) {
            assert (nodeAvatars1.contains(enforceNode1));
            assert (nodeAvatars2.contains(enforceNode2));
        }

        class InsertFun {
            void does(int node1, int node2)
            {
                pairs.put(node1, node2);
                nodeAvatars1.remove((Integer)node1);
                nodeAvatars2.remove((Integer)node2);
            }
        };
        InsertFun insert_fun = new InsertFun();

        if (enforceNode1 >= 0 && enforceNode2 >= 0) {
            insert_fun.does(enforceNode1, enforceNode2);
        }

        //sort by x,y
        while (nodeAvatars1.size() > 0 && nodeAvatars2.size() > 0) {
            int node1 = nodeAvatars1.get(0);
            int nearestNode = -1;
            Double minDistanceSquare = Double.MAX_VALUE;

            if (! childrenAttached(tree.getNode(node1))) {
                continue;
            }

            for (int node2 : nodeAvatars2) {

                if (! childrenAttached(tree.getNode(node2))) {
                    continue;
                }

                double distanceSquare = getNodeDistanceSquare(tree.getNode(node1), tree.getNode(node2));
                if (distanceSquare < minDistanceSquare) {
                    minDistanceSquare = distanceSquare;
                    nearestNode = node2;
                }
            }

            insert_fun.does(node1, nearestNode);
        }

        NodeAvatarsPairingInfo pairingInfo = new NodeAvatarsPairingInfo();
        pairingInfo.m_nodeAvatarPairs = pairs;
        pairingInfo.m_nodeAvatars1Alone = nodeAvatars1;
        pairingInfo.m_nodeAvatars2Alone = nodeAvatars2;

        return pairingInfo;
    }

    private void rebuildChildEdge(Tree tree, NodeAvatarsPairingInfo oldNewParentPairingInfo,
                                  int oldChildPos, int newChildPos,
                                  EdgeVertex childEdgeVertex)
    {
        HashMap<Integer, Integer> pairs = oldNewParentPairingInfo.m_nodeAvatarPairs;
        for (int node1 : pairs.keySet())
        {
            int node2 = pairs.get(node1);
            Node oldParent = tree.getNode(node1);
            Node newParent = tree.getNode(node2);
            Node child = oldParent.getChild(oldChildPos);

            ArrayList<Object> outEdgeDBIds;
            tree.removeEdge(tree.getEdge(oldParent, child));
            outEdgeDBIds = (ArrayList<Object>)oldParent.get(sm_outEdgeDBIdsPropName);
            outEdgeDBIds.remove(oldChildPos);

            Edge newEdge = tree.addChildEdge(newParent, child, newChildPos);
            outEdgeDBIds = (ArrayList<Object>)newParent.get(sm_outEdgeDBIdsPropName);
            outEdgeDBIds.add(newChildPos, childEdgeVertex.m_edge.getId());
            loadEdgeProperties(childEdgeVertex.m_edge, newEdge);

            verfyNode(oldParent, true);
            verfyNode(newParent, true);
        }

        for (int node1 : oldNewParentPairingInfo.m_nodeAvatars1Alone) {
            if (tree.getNodeTable().isValidRow(node1)) {
                Node oldParent = tree.getNode(node1);
                if (childrenAttached(oldParent)) {
                    hideRelation(oldParent, oldChildPos);
                }
            }
        }

        for (int node2 : oldNewParentPairingInfo.m_nodeAvatars2Alone) {
            if (tree.getNodeTable().isValidRow(node2)) {
                Node newParent = tree.getNode(node2);
                if (childrenAttached(newParent)) {
                    exposeRelation(newParent, newChildPos, childEdgeVertex.m_edge, childEdgeVertex.m_vertex);
                }
            }
        }
    }

    public void moveChild(Node oldParent, int oldPos, Node newParent, int newPos)
    {
        assert (oldParent.getGraph() == newParent.getGraph());

        if (! childrenAttached(newParent)) {
            attachChildren(newParent);
        }

        Object oldParentDBId = getDBId(oldParent);
        Object newParentDBId = getDBId(newParent);
        assert (! oldParentDBId.equals(newParentDBId));

        Vertex oldParentVertex = m_mindDb.getVertex(oldParentDBId);
        Vertex newParentVertex = m_mindDb.getVertex(newParentDBId);
        EdgeVertex edgeVertex = m_mindDb.handoverChild(oldParentVertex, oldPos, newParentVertex, newPos);

        for (Tree tree : m_trees) {
            NodeAvatarsPairingInfo oldNewParentPairingInfo;
            if(oldParent.getGraph() == tree)
            {
                 oldNewParentPairingInfo =  pairNodeAvatars(tree, oldParentDBId, newParentDBId,
                        oldParent.getRow(), newParent.getRow());
            } else {
                oldNewParentPairingInfo =  pairNodeAvatars(tree, oldParentDBId, newParentDBId,
                        -1, -1);
            }

            rebuildChildEdge(tree, oldNewParentPairingInfo, oldPos, newPos, edgeVertex);
        }

    }


    private static boolean arrayEqual(ArrayList<Object> array1, ArrayList<Object> array2)
    {
        if (array1.size() != array2.size()) {
            return false;
        }

        for (int i = 0; i<array1.size(); i++) {
            if (! array1.get(i).equals(array2.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isParentChildRelation(ArrayList<Object> parentInheritPath, Object parentDBId,
                                                 ArrayList<Object> childInheritPath)
    {
        ArrayList<Object> parentInheritPathClone = (ArrayList<Object>)parentInheritPath.clone();
        parentInheritPathClone.add(parentDBId);

        return arrayEqual(parentInheritPathClone, childInheritPath);
    }

    private ArrayList<Object> getDBIdsInOutEdges(Node node)
    {
        Iterator outEdges = node.outEdges();
        ArrayList arrayList = new ArrayList();
        while(outEdges.hasNext())
        {
            Edge edge = (Edge)outEdges.next();
            arrayList.add(getDBId(edge));
        }

        return arrayList;
    }

    private ArrayList<Object> getDBIdsInOutDBEdges(Vertex vertex)
    {
        ArrayList arrayList = new ArrayList();
        for (com.tinkerpop.blueprints.Edge edge : vertex.getEdges(Direction.OUT))
        {
            arrayList.add(edge.getId());
        }
        return arrayList;
    }

    boolean isParentChildRelation(Node parent, Node child)
    {
        return MindDB.isParentChildRelation((ArrayList<Object>)parent.get(sm_inheritPathPropName),
                getDBId(parent), (ArrayList<Object>)child.get(sm_inheritPathPropName));
    }

    void verfyNode(Node node, boolean forceChildAttached)
    {
        Vertex vertex = getDBVertex(node);

        m_mindDb.verifyVertex(vertex);

        verifyElementProperties(vertex, node, sm_nodePropNames);

        ArrayList<Object> outEdgeDBIds = (ArrayList<Object>)node.get(sm_outEdgeDBIdsPropName);
        if (outEdgeDBIds.size() > 0 && forceChildAttached) {
            assert node.getChildCount() == outEdgeDBIds.size();
        }

        for (int i=0; i<node.getChildCount(); i++) {
            Node childOrReferenceNode = node.getChild(i);
            Edge outEdge = (node.getGraph()).getEdge(node, childOrReferenceNode);

            assert(getDBId(outEdge).equals(outEdgeDBIds.get(i)));
            verifyElementProperties(getDBEdge(outEdge), outEdge, sm_edgePropNames);

            Integer outEdgeType = (Integer)outEdge.get(sm_edgeTypePropName);
            if (MindDB.EdgeType.values()[outEdgeType] == MindDB.EdgeType.INCLUDE) {
                isParentChildRelation(node, childOrReferenceNode);
            } else {
                assert MindDB.EdgeType.values()[outEdgeType] == MindDB.EdgeType.REFERENCE;
            }
        }

        Edge inEdge = node.getParentEdge();
        if (inEdge != null) {
            verifyElementProperties(getDBEdge(inEdge), inEdge, sm_edgePropNames);

            Integer inEdgeType = (Integer)inEdge.get(sm_edgeTypePropName);
            Node parentOrReferrerNode = inEdge.getSourceNode();

            ArrayList<Object> parentOrReferrerOutEdgeArray = (ArrayList<Object>)parentOrReferrerNode.get(sm_outEdgeDBIdsPropName);
            assert parentOrReferrerOutEdgeArray.contains(getDBId(inEdge));

            if (MindDB.EdgeType.values()[inEdgeType] == MindDB.EdgeType.INCLUDE) {
                assert isParentChildRelation(parentOrReferrerNode, node);
            } else {
                assert MindDB.EdgeType.values()[inEdgeType] == MindDB.EdgeType.REFERENCE;
            }
        }
    }
}
