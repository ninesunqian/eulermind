package eulermind;

import com.tinkerpop.blueprints.*;
import eulermind.MindDB.EdgeVertex;
import eulermind.MindDB.RefLinkInfo;
import eulermind.importer.FreemindImporter;
import eulermind.importer.Importer;
import eulermind.importer.TikaPlainTextImporter;
import prefuse.data.*;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.util.TypeLib;
import prefuse.util.collections.IntIterator;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.VisualTree;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
The MIT License (MIT)
Copyright (c) 2012-2014 wangxuguang ninesunqian@163.com

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

public class MindModel {
    static Logger s_logger = LoggerFactory.getLogger(MindModel.class);

	final static String sm_dbIdColumnName = "dbElementId";

    final static private String sm_outEdgeInnerIdsPropName = MindDB.OUT_EDGES_PROP_NAME;
    final static private String sm_outEdgeInnerIdPropName = MindDB.OUT_EDGE_INNER_ID_PROP_NAME;

	final static public String sm_textPropName = "text";
    final static public String sm_iconPropName = "icon";
	final static public String sm_fontFamilyPropName = "fontFamily";
	final static public String sm_fontSizePropName = "fontSize";
	final static public String sm_boldPropName = "bold";
	final static public String sm_italicPropName = "italic";
	final static public String sm_underlinedPropName = "underlined";
	final static public String sm_nodeColorPropName = "nodeColor";
	final static public String sm_textColorPropName = "textColor";

    private final static String FAVORITE_INDEX_NAME = "favoriteIndex";
    private final static String FAVORITE_KEY_NAME = "favorite";


    private Index<Vertex> m_favoriteIndex;

    class VertexBasicInfo {
        Object m_dbId;

        //TODO: remove this
        List m_inheritPath;
        //TODO: remove this
        String m_contextText;

        VertexBasicInfo(Vertex vertex) {
            m_dbId = vertex.getId();
            m_inheritPath = m_mindDb.getInheritPath(vertex.getId());
            m_contextText = getContextText(m_dbId);
        }
    }

    ArrayList<VertexBasicInfo> m_favoriteInfoes = new ArrayList<VertexBasicInfo>();

    public final static String sm_nodePropNames [] = {
            sm_textPropName,
            sm_outEdgeInnerIdsPropName,

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

    public final static String sm_edgePropNames [] = {
            sm_edgeTypePropName,
            sm_outEdgeInnerIdPropName
    };

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
        fillPropertyClassMap();

		m_mindDb = new MindDB(dbPath);
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


        /*
        Importer importer =  new TikaPlainTextImporter(m_mindDb);
        try {
            importer.importFile(root.getId(), 0, "/home/wangxuguang/1.txt");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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

        displayNodeTable.setTupleToStringHandler(new Table.TupleToStringHandler() {
            @Override
            public String tupleToString(Table table, Tuple tuple)
            {
                return getNodeDebugInfo((Node)tuple);
            }
        });

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
            if (key == sm_outEdgeInnerIdsPropName) {
                value = m_mindDb.getContainerProperty((Vertex)dbElement, key);
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
                    dbElement.setProperty(key, value);
                }
            }
        }
    }

    private void verifyElementProperties(com.tinkerpop.blueprints.Element dbElement,
                                                   Tuple tuple, String keys[])
    {
        assert dbElement.getId().equals(tuple.get(sm_dbIdColumnName));

        for (String key : keys)
        {
            Object tupleValue = tuple.get(key);

            if (key == sm_outEdgeInnerIdsPropName) {
                ArrayList dbElementValue = m_mindDb.getContainerProperty(dbElement, key);
                assert tupleValue == dbElementValue || tupleValue.equals(dbElementValue);
            } else {
                Object dbElementValue = dbElement.getProperty(key);
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

            verifyNode(child, false);
		}

        verifyNode(parent, true);
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

    protected void exposeNodeRelation(Node sourceNode, int pos, EdgeVertex toTarget)
    {
        assert sourceNode != null;
        assert sourceNode.isValid();

        ArrayList<Short> outEdgeInnerIdsInNode = (ArrayList<Short>)sourceNode.get(sm_outEdgeInnerIdsPropName);
        ArrayList<Short> outEdgeInnerIdsInVertex = m_mindDb.getOutEdgeInnerIds(getDBVertex(sourceNode));

        //if this node is updated, skip
        if (outEdgeInnerIdsInNode.equals(outEdgeInnerIdsInVertex)) {
            verifyNode(sourceNode, false);
            return;
        }

        outEdgeInnerIdsInNode.add(pos, m_mindDb.getOutEdgeInnerId(toTarget.m_edge));

        //if children not attached, skip
        if (sourceNode.getChildCount() == 0 && outEdgeInnerIdsInNode.size() > 1) {
            verifyNode(sourceNode, false);
            return;
        }

        Tree tree = (Tree)sourceNode.getGraph();

        Node child = tree.addNode();
        Edge edge = tree.addChildEdge(sourceNode, child, pos);

        loadNodeProperties(toTarget.m_vertex, child);
        loadEdgeProperties(toTarget.m_edge, edge);

        verifyNode(sourceNode, true);
        verifyNode(sourceNode.getChild(pos), false);
    }

    protected void hideNodeRelation(Node sourceNode, int pos)
    {
        assert (sourceNode != null);
        assert (sourceNode.isValid());

        ArrayList<Short> outEdgeInnerIdsInNode = (ArrayList<Short>)sourceNode.get(sm_outEdgeInnerIdsPropName);
        ArrayList<Short> outEdgeInnerIdsInVertex = m_mindDb.getOutEdgeInnerIds(getDBVertex(sourceNode));

        //if this node is updated, skip
        if (outEdgeInnerIdsInNode.equals(outEdgeInnerIdsInVertex)) {
            verifyNode(sourceNode, false);
            return;
        }

        outEdgeInnerIdsInNode.remove(pos);

        //its child is not displayed
        if (sourceNode.getChildCount() == 0) {
            verifyNode(sourceNode, false);
            return;
        }

        Tree tree = (Tree)sourceNode.getGraph();

        if (sourceNode.getChildCount() > 0) {
            Node child = tree.getChild(sourceNode, pos);
            tree.removeChild(child);
        }
        verifyNode(sourceNode, false);
    }

    protected void exposeNodeRelations(Node sourceNode, int pos, List<EdgeVertex> toTargets)
    {
        assert sourceNode != null;
        assert sourceNode.isValid();

        ArrayList<Short> outEdgeInnerIdsInNode = (ArrayList<Short>)sourceNode.get(sm_outEdgeInnerIdsPropName);
        ArrayList<Short> outEdgeInnerIdsInVertex = m_mindDb.getOutEdgeInnerIds(getDBVertex(sourceNode));

        //if this node is updated, skip
        if (outEdgeInnerIdsInNode.equals(outEdgeInnerIdsInVertex)) {
            verifyNode(sourceNode, false);
            return;
        }

        for (int i=0; i<toTargets.size(); i++) {
            EdgeVertex toTarget = toTargets.get(i);
            outEdgeInnerIdsInNode.add(pos+i, m_mindDb.getOutEdgeInnerId(toTarget.m_edge));
        }

        //if children not attached, skip
        if (sourceNode.getChildCount() == 0 && outEdgeInnerIdsInNode.size() > toTargets.size()) {
            verifyNode(sourceNode, false);
            return;
        }

        for (int i=0; i<toTargets.size(); i++) {
            Tree tree = (Tree)sourceNode.getGraph();

            Node child = tree.addNode();
            Edge edge = tree.addChildEdge(sourceNode, child, pos+i);

            EdgeVertex toTarget = toTargets.get(i);

            loadNodeProperties(toTarget.m_vertex, child);
            loadEdgeProperties(toTarget.m_edge, edge);
        }

        verifyNode(sourceNode, true);
        verifyNode(sourceNode.getChild(pos), false);
    }

    protected void hideNodeRelations(Node sourceNode, int pos, int num)
    {
        assert (sourceNode != null);
        assert (sourceNode.isValid());

        ArrayList<Short> outEdgeInnerIdsInNode = (ArrayList<Short>)sourceNode.get(sm_outEdgeInnerIdsPropName);
        ArrayList<Short> outEdgeInnerIdsInVertex = m_mindDb.getOutEdgeInnerIds(getDBVertex(sourceNode));

        //if this node is updated, skip
        if (outEdgeInnerIdsInNode.equals(outEdgeInnerIdsInVertex)) {
            verifyNode(sourceNode, false);
            return;
        }

        for (int i=0; i<num; i++) {
            outEdgeInnerIdsInNode.remove(pos);
        }

        //its child is not displayed
        if (sourceNode.getChildCount() == 0) {
            verifyNode(sourceNode, false);
            return;
        }

        Tree tree = (Tree)sourceNode.getGraph();

        for (int i=0; i<num; i++) {
            Node child = tree.getChild(sourceNode, pos);
            tree.removeChild(child);
        }

        verifyNode(sourceNode, false);
    }

    //Maybe there are more than one reference edge link source target
    //The callers of exposeTreeRelation has got target and dbEdge, so pass them as argument
    protected void exposeTreeRelation(final Tree tree, final Object sourceId, final int edgePosInSourceNode,
                                      final EdgeVertex toTarget)
	{
        final Vertex sourceVertex = m_mindDb.getVertex(sourceId);

		visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                exposeNodeRelation(sourceNode, edgePosInSourceNode, toTarget);
            }
        });
	}

    private void hideTreeRelation(final Tree tree, final Object sourceId, final int edgePosInSourceNode)
    {
        visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                hideNodeRelation(sourceNode, edgePosInSourceNode);
            }
        });
    }

    protected void exposeTreeRelations(final Tree tree, final Object sourceId, final int edgePosInSourceNode,
                                      final List<EdgeVertex> toTargets)
    {
        final Vertex sourceVertex = m_mindDb.getVertex(sourceId);

        visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                exposeNodeRelations(sourceNode, edgePosInSourceNode, toTargets);
            }
        });
    }

    private void hideTreeRelations(final Tree tree, final Object sourceId, final int edgePosInSourceNode, final int num)
    {
        visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                hideNodeRelations(sourceNode, edgePosInSourceNode, num);
            }
        });
    }

    //opNode: user operated node
    protected void exposeModelRelation(Node opNode, int pos, EdgeVertex edgeVertex)
    {
        for (Tree tree : m_trees) {
            exposeTreeRelation(tree, getDBId(opNode), pos, edgeVertex);
        }

        verifyNode(opNode, true);
    }

    private void hideModelRelation(Object sourceId, int edgePosInSourceNode)
    {
        for (final Tree tree : m_trees) {
            hideTreeRelation(tree, sourceId, edgePosInSourceNode);
        }
    }

    //opNode: user operated node
    protected void exposeModelRelations(Node opNode, int pos, List<EdgeVertex> toTargets)
    {
        for (Tree tree : m_trees) {
            exposeTreeRelations(tree, getDBId(opNode), pos, toTargets);
        }

        verifyNode(opNode, true);
    }

    private void hideModelRelations(Object sourceId, int edgePosInSourceNode, int num)
    {
        for (final Tree tree : m_trees) {
            hideTreeRelations(tree, sourceId, edgePosInSourceNode, num);
        }
    }

    //return new child node
	public Object addChild(Node parent, int pos, String text)
    {
        /*FIXME: 为什么这里会出错
        if (! childrenAttached(parent)) {
            attachChildren(parent);
        }
        */

        Object parentDBId = getDBId(parent);
		Vertex dbParent = m_mindDb.getVertex(parentDBId);
		EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);

        edgeVertex.m_vertex.setProperty(sm_textPropName, text);

        exposeModelRelation(parent, pos, edgeVertex);

        return edgeVertex.m_vertex.getId();
	}

    public Importer getImporter(String path)
    {

        if (path.endsWith(".mm")) {
            return new FreemindImporter(m_mindDb);
        } else {
            return new TikaPlainTextImporter(m_mindDb);
        }
    }

    //return new child node
    public List importFile(Node parent, String path) throws Exception
    {
        if (! childrenAttached(parent)) {
            attachChildren(parent);
        }

        int pos = parent.getChildCount();

        Object parentDBId = getDBId(parent);
        Importer importer = getImporter(path);
        List newChildren = importer.importFile(getDBId(parent), pos, path);

        Vertex dbParent = m_mindDb.getVertex(parentDBId);
        ArrayList<EdgeVertex> newToTargets = new ArrayList<EdgeVertex>();
        for (int i=0; i<newChildren.size(); i++) {
            EdgeVertex edgeVertex = m_mindDb.getChildOrReferent(dbParent, pos+i);
            newToTargets.add(edgeVertex);
        }

        exposeModelRelations(parent, pos, newToTargets);
        return newChildren;
    }

	//return the DBid of node
	public Object trashNode(Object parentDBId, int pos)
	{
        Vertex parent = m_mindDb.getVertex(parentDBId);
        final EdgeVertex edgeChild = m_mindDb.getChildOrReferent(parent, pos);

        Object removedDBId = edgeChild.m_vertex.getId();

        m_mindDb.trashSubTree(parent, pos);

        hideModelRelation(parentDBId, pos);

        return removedDBId;
	}
	
	public void restoreNodeFromTrash(Node parent, final Object dbId)
	{
        if (! childrenAttached(parent)) {
            attachChildren(parent);
        }

        final Vertex restoredVertex = m_mindDb.getVertex(dbId);
        final MindDB.TrashedTreeContext context = m_mindDb.getTrashedTreeContext(restoredVertex);

        final EdgeVertex edgeParent = m_mindDb.restoreTrashedSubTree(restoredVertex);

        exposeModelRelation(parent, context.m_pos, new EdgeVertex(edgeParent.m_edge, restoredVertex));

        for (final RefLinkInfo refLinkInfo : context.m_refLinkInfos) {
            final Vertex referrerVertex = m_mindDb.getVertex(refLinkInfo.m_referrer);
            final EdgeVertex toReferent = m_mindDb.getChildOrReferent(referrerVertex, refLinkInfo.m_pos);

            for (final Tree tree : m_trees) {
                exposeTreeRelation(tree, referrerVertex, refLinkInfo.m_pos, toReferent);
            }
        }
	}

    public void addReference(Node referrerNode, int pos, Object referentDBId) {
        if (! childrenAttached(referrerNode)) {
            attachChildren(referrerNode);
        }

        s_logger.info(String.format("addReference : %s -- %s", getText(referrerNode), referentDBId.toString()));
        Object referrerDBId  = getDBId(referrerNode);
        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        Vertex referentVertex = m_mindDb.getVertex(referentDBId);
        com.tinkerpop.blueprints.Edge refEdge = m_mindDb.addRefEdge(referrerVertex, referentVertex, pos);

        exposeModelRelation(referrerNode, pos, new EdgeVertex(refEdge, referentVertex));
    }


    public void removeReference(Object referrerDBId, int pos) {
        Vertex referrerVertex = m_mindDb.getVertex(referrerDBId);
        m_mindDb.removeRefEdge(referrerVertex, pos);
        hideModelRelation(referrerDBId, pos);
    }

    public void changeChildPos(final Object parentDBId, final int oldPos, final int newPos)
    {
        s_logger.info("arg: parentDBID:{}", parentDBId);
        s_logger.info("arg: oldPos:{}", oldPos);
        s_logger.info("arg: newPos:{}", newPos);

        Vertex parent = m_mindDb.getVertex(parentDBId);
        m_mindDb.changeChildOrReferentPos(parent, oldPos, newPos);

        for (final Tree tree : m_trees) {
            visitNodeAvatars(tree, parentDBId,
                new Visitor() {
                    public void visit(Node parent)
                    {
                        ArrayList<Short> outEdgeInnerIds = (ArrayList<Short>)parent.get(sm_outEdgeInnerIdsPropName);

                        s_logger.info("before change: outEdgeInnerIds:{}", outEdgeInnerIds);
                        Short edgeInnerId = outEdgeInnerIds.remove(oldPos);
                        outEdgeInnerIds.add(newPos, edgeInnerId);

                        s_logger.info("after change: outEdgeInnerIds:{}", outEdgeInnerIds);
                        if (parent.getChildCount() > 0) {
                            tree.changeChildIndex(parent, oldPos, newPos);
                        }
                        verifyNode(parent, false);
                    }
                });
        }

        s_logger.info("ret:");
    }

    public boolean canResetParent(Node node, Node newParent)
    {
        assert(node.getGraph() == newParent.getGraph());
        assert(node.getParent() != null);

        Node oldParent = node.getParent();
        Graph graph = node.getGraph();

        if (isRefEdge(graph.getEdge(oldParent, node))) {
            return true;
        } else {
            return (! isSelfInDB(node, newParent)) && (! isDescendantInDB(node, newParent));
        }
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
                    verifyNode(node, false);
                }
            });
        }
	}

    public Object getProperty (final Object dbId, final String key)
    {
        Vertex dbNode = m_mindDb.getVertex(dbId);
        return dbNode.getProperty(key);
    }

	static public Object getDBId(Tuple tuple)
	{
		return tuple.get(sm_dbIdColumnName);
	}

    static public Short getOutEdgeInnerId(Edge edge)
    {
        return (Short)edge.get(sm_outEdgeInnerIdPropName);
    }

    static public int getDBChildCount(Node node)
    {
        ArrayList childEdgesInnerIds = (ArrayList)node.get(sm_outEdgeInnerIdsPropName);
        return childEdgesInnerIds==null ? 0: childEdgesInnerIds.size();
    }

    static public boolean childrenAttached(Node node)
    {
        return getDBChildCount(node) == node.getChildCount();
    }

    public boolean isSelfInDB(Node n1, Node n2)
    {
        return m_mindDb.isVertexIdSelf(getDBId(n1), getDBId(n2));
    }

    public boolean isChildInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdChild(getDBId(thiz), getDBId(that));
    }

    public boolean isParentInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdParent(getDBId(thiz), getDBId(that));
    }

    public boolean isSiblingInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdSibling(getDBId(thiz), getDBId(that));
    }

    public boolean isDescendantInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdDescendant(getDBId(thiz), getDBId(that));
    }

    public boolean isAncestorInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdAncestor(getDBId(thiz), getDBId(that));
    }

    private String objectToString(Object object)
    {
        return object == null ? "null" : object.toString();
    }
    public String getNodeDebugInfo(Node node) {
        int row = node.getRow();
        ArrayList<Integer> nodePath = getNodePath(node);
        Object rootId = getDBId(((Tree)node.getGraph()).getRoot());


        Object dbId = getDBId(node);
        if (dbId != null) {
            List inheritPath = m_mindDb.getInheritPath(dbId);
            String infoFmt = "row:%d, rootId:%s, nodePath:%s, inheritPath:%s, id:%s, text:%s";
            return String.format(infoFmt,
                    row,
                    objectToString(rootId),
                    objectToString(nodePath),
                    objectToString(inheritPath),
                    objectToString(getDBId(node)),
                    objectToString(getText(node)));
        } else {
            String infoFmt = "row:%d, rootId:%s, nodePath:%s, placeholder";
            return String.format(infoFmt,
                    row,
                    objectToString(rootId),
                    objectToString(nodePath));
        }
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
        Vertex parent = m_mindDb.getParent(vertex);

        if (parent == null) {
            return text;
        } else {
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

        for (int i=0; i<path.size(); i++) {

            if (node.getChildCount() == 0 && getDBChildCount(node) > 0) {
                attachChildren(node);
            }

            int pos = path.get(i);
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

    static void addNodeMirrorXYColumn(Tree tree, VisualTree visualTree)
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
        s_logger.info("arg: tree:{}", getDBId(tree.getRoot()));
        s_logger.info("arg: dbID1:{}", dbId1);
        s_logger.info("arg: dbID2:{}", dbId2);
        s_logger.info("arg: enforceNode1:{}", enforceNode1);
        s_logger.info("arg: enforceNode2:{}", enforceNode2);

        final ArrayList<Integer> nodeAvatars1 = getNodeAvatars(tree, dbId1);
        final ArrayList<Integer> nodeAvatars2 = getNodeAvatars(tree, dbId2);

        final HashMap<Integer, Integer> pairs = new HashMap<Integer, Integer>();

        if (enforceNode1 >= 0) {
            assert (nodeAvatars1.contains(enforceNode1));
            assert (nodeAvatars2.contains(enforceNode2));
        }

        if (enforceNode1 >= 0 && enforceNode2 >= 0) {
            pairs.put(enforceNode1, enforceNode2);
            nodeAvatars1.remove((Integer)enforceNode1);
            nodeAvatars2.remove((Integer)enforceNode2);
        }

        //sort by x,y
        Iterator<Integer> iterator1 = nodeAvatars1.iterator();
        while (iterator1.hasNext() && nodeAvatars2.size() > 0) {
            int node1 = iterator1.next();

            if (! childrenAttached(tree.getNode(node1))) {
                continue;
            }

            int nearestNode2 = -1;
            Double minDistanceSquare = Double.MAX_VALUE;

            for (int node2 : nodeAvatars2) {

                if (! childrenAttached(tree.getNode(node2))) {
                    continue;
                }

                double distanceSquare = getNodeDistanceSquare(tree.getNode(node1), tree.getNode(node2));
                if (distanceSquare < minDistanceSquare) {
                    minDistanceSquare = distanceSquare;
                    nearestNode2 = node2;
                }
            }

            pairs.put(node1, nearestNode2);
            iterator1.remove();
            nodeAvatars2.remove((Integer)nearestNode2);
        }

        s_logger.info("QQQQQQQQQQQQQQQQQQQQQQQqq");
        NodeAvatarsPairingInfo pairingInfo = new NodeAvatarsPairingInfo();
        s_logger.info("llllllllllllllllllQQQQQQQQQQQQQQQQQQQQQQqq");
        pairingInfo.m_nodeAvatarPairs = pairs;
        pairingInfo.m_nodeAvatars1Alone = nodeAvatars1;
        pairingInfo.m_nodeAvatars2Alone = nodeAvatars2;

        s_logger.info("ret: {}", pairingInfo);

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

            ArrayList<Short> outEdgeInnerIds;
            outEdgeInnerIds = (ArrayList<Short>)oldParent.get(sm_outEdgeInnerIdsPropName);
            outEdgeInnerIds.remove(oldChildPos);
            tree.removeEdge(tree.getEdge(oldParent, child));

            outEdgeInnerIds = (ArrayList<Short>)newParent.get(sm_outEdgeInnerIdsPropName);
            outEdgeInnerIds.add(newChildPos, m_mindDb.getOutEdgeInnerId(childEdgeVertex.m_edge));
            Edge newEdge = tree.addChildEdge(newParent, child, newChildPos);
            loadEdgeProperties(childEdgeVertex.m_edge, newEdge);

            verifyNode(oldParent, true);
            verifyNode(newParent, true);
        }

        for (int node1 : oldNewParentPairingInfo.m_nodeAvatars1Alone) {
            if (tree.getNodeTable().isValidRow(node1)) {
                Node oldParent = tree.getNode(node1);
                hideNodeRelation(oldParent, oldChildPos);
                verifyNode(oldParent, false);
            }
        }

        for (int node2 : oldNewParentPairingInfo.m_nodeAvatars2Alone) {
            if (tree.getNodeTable().isValidRow(node2)) {
                Node newParent = tree.getNode(node2);
                exposeNodeRelation(newParent, newChildPos, childEdgeVertex);
                verifyNode(newParent, false);
            }
        }
    }

    private void handoverNode(Node oldParent, int oldPos, Node newParent, int newPos,
                              EdgeVertex newEdgeVertex)
    {
        Object oldParentDBId = getDBId(oldParent);
        Object newParentDBId = getDBId(newParent);

        for (Tree tree : m_trees) {
            NodeAvatarsPairingInfo oldNewParentPairingInfo;
            if(tree == oldParent.getGraph())
            {
                oldNewParentPairingInfo =  pairNodeAvatars(tree, oldParentDBId, newParentDBId,
                        oldParent.getRow(), newParent.getRow());
            } else {
                oldNewParentPairingInfo =  pairNodeAvatars(tree, oldParentDBId, newParentDBId,
                        -1, -1);
            }

            rebuildChildEdge(tree, oldNewParentPairingInfo, oldPos, newPos, newEdgeVertex);
        }
    }

    public void handoverChild(Node oldParent, int oldPos, Node newParent, int newPos)
    {
        s_logger.info("arg: oldParent:{}", oldParent);
        s_logger.info("arg: oldPos:{}", oldPos);
        s_logger.info("arg: newParent:{}", newParent);
        s_logger.info("arg: newPos:{}", newPos);

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

        handoverNode(oldParent, oldPos, newParent, newPos, edgeVertex);

        s_logger.info("ret:");

    }

    public void handoverReferent(Node oldReferrer, int oldPos, Node newReferrer, int newPos)
    {
        s_logger.info("arg: oldReferrer:{}", oldReferrer);
        s_logger.info("arg: oldPos:{}", oldPos);
        s_logger.info("arg: newReferrer:{}", newReferrer);
        s_logger.info("arg: newPos:{}", newPos);

        assert (oldReferrer.getGraph() == newReferrer.getGraph());

        if (! childrenAttached(newReferrer)) {
            attachChildren(newReferrer);
        }

        Object oldReferrerDBId = getDBId(oldReferrer);
        Object newReferrerDBId = getDBId(newReferrer);
        assert (! oldReferrerDBId.equals(newReferrerDBId));

        Vertex oldReferrerVertex = m_mindDb.getVertex(oldReferrerDBId);
        Vertex newReferrerVertex = m_mindDb.getVertex(newReferrerDBId);

        EdgeVertex edgeVertex = m_mindDb.handoverReferent(oldReferrerVertex, oldPos, newReferrerVertex, newPos);
        handoverNode(oldReferrer, oldPos, newReferrer, newPos, edgeVertex);

        s_logger.info("ret:");

    }

    void verifyNode(Node node, boolean forceChildAttached)
    {
        Vertex vertex = getDBVertex(node);

        m_mindDb.verifyVertex(vertex);

        verifyElementProperties(vertex, node, sm_nodePropNames);

        ArrayList<Short> outEdgeInnerIds = (ArrayList<Short>)node.get(sm_outEdgeInnerIdsPropName);
        if (outEdgeInnerIds.size() > 0 && forceChildAttached) {
            assert node.getChildCount() == outEdgeInnerIds.size();
        }

        for (int i=0; i<node.getChildCount(); i++) {
            Node childOrReferenceNode = node.getChild(i);
            Edge outEdge = (node.getGraph()).getEdge(node, childOrReferenceNode);

            assert(getOutEdgeInnerId(outEdge).equals(outEdgeInnerIds.get(i)));
            verifyElementProperties(getDBEdge(outEdge), outEdge, sm_edgePropNames);

            Integer outEdgeType = (Integer)outEdge.get(sm_edgeTypePropName);
            if (MindDB.EdgeType.values()[outEdgeType] == MindDB.EdgeType.INCLUDE) {
                assert m_mindDb.isVertexIdParent(getDBId(childOrReferenceNode), getDBId(node));
            } else {
                assert MindDB.EdgeType.values()[outEdgeType] == MindDB.EdgeType.REFERENCE;
            }
        }

        Edge inEdge = node.getParentEdge();
        if (inEdge != null) {
            verifyElementProperties(getDBEdge(inEdge), inEdge, sm_edgePropNames);

            Integer inEdgeType = (Integer)inEdge.get(sm_edgeTypePropName);
            Node parentOrReferrerNode = inEdge.getSourceNode();

            ArrayList<Short> parentOrReferrerOutEdgeInnerIds =
                    (ArrayList<Short>)parentOrReferrerNode.get(sm_outEdgeInnerIdsPropName);

            assert parentOrReferrerOutEdgeInnerIds.get(node.getIndex()).equals(getOutEdgeInnerId(inEdge));

            if (MindDB.EdgeType.values()[inEdgeType] == MindDB.EdgeType.INCLUDE) {
                assert m_mindDb.isVertexIdChild(getDBId(parentOrReferrerNode), getDBId(node));
            } else {
                assert MindDB.EdgeType.values()[inEdgeType] == MindDB.EdgeType.REFERENCE;
            }
        }
    }

}