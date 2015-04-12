package eulermind;

import com.tinkerpop.blueprints.*;
import eulermind.MindDB.EdgeVertex;
import eulermind.MindDB.RefLinkInfo;
import eulermind.component.SwingWorkerDialog;
import eulermind.importer.DirectoryImporter;
import eulermind.importer.FreemindImporter;
import eulermind.importer.Importer;
import eulermind.importer.TikaPlainTextImporter;
import prefuse.data.*;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.util.FontLib;
import prefuse.util.TypeLib;
import prefuse.util.collections.IntIterator;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.VisualTree;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

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

    private final static String OUT_EDGE_ID_PAIRS_PROP_NAME = "outEdgeIdPairs";
    private final static String EDGE_INNER_ID_PROP_NAME = MindDB.EDGE_INNER_ID_PROP_NAME;

    //这两个属性应用频繁
    public final static String TEXT_PROP_NAME = "x"; //"t" 已经占用了
    public final static String STYLE_PROP_NAME = "s";

    public final static String sm_iconPropName = "ic";
    public final static String sm_textColorPropName = "tc";
    public final static String sm_nodeColorPropName = "nc";
    public final static String sm_italicPropName = "it";
    public final static String sm_boldPropName = "bd";
    public final static String sm_fontSizePropName = "sz";
    public final static String sm_fontFamilyPropName = "ft";

    private final static String FAVORITE_INDEX_NAME = "favoriteIndex";
    private final static String FAVORITE_KEY_NAME = "favorite";

    private final static String LAST_OPENED_INDEX_NAME = "lastOpenedIndex";
    private final static String LAST_OPENED_KEY_NAME = "lastOpened";

    private Index<Vertex> m_favoriteIndex;

    class VertexBasicInfo {
        Object m_dbId;
        String m_text;

        Object m_parentDbId;
        String m_parentText;

        String m_contextText;

        VertexBasicInfo(Vertex vertex) {
            m_dbId = vertex.getId();
            m_text = vertex.getProperty(TEXT_PROP_NAME);
            Vertex parent = m_mindDb.getParent(vertex);

            if (parent == null) {
                m_parentText = null;
                m_parentDbId = null;
            } else {
                m_parentDbId = parent.getId();
                m_parentText = parent.getProperty(TEXT_PROP_NAME);
            }

            //如果m_parentText在前面，需要考虑m_parentText可能过长，总长度过可能长，m_text可能过短等问题
            m_contextText = m_text + " @ " + m_parentText;
        }
    }

    ArrayList<VertexBasicInfo> m_favoriteInfoes = new ArrayList<VertexBasicInfo>();

    public final static String sm_nodePropNames [] = {
            TEXT_PROP_NAME,
            STYLE_PROP_NAME,

            sm_iconPropName,

            sm_fontFamilyPropName,
            sm_fontSizePropName,

            sm_boldPropName,
            sm_italicPropName,

            sm_nodeColorPropName,
            sm_textColorPropName,
    };

    private static Hashtable<String, Class> sm_propertyClassMap = new Hashtable<String, Class>();

	public final static String sm_edgeTypePropName = MindDB.EDGE_TYPE_PROP_NAME;

    public final static String sm_edgePropNames [] = {
    };

	public MindDB m_mindDb;
    ArrayList<Tree> m_trees = new ArrayList<Tree>();

	//return sorted copy of propName
	private void addNodeTableProperties(Table t)
	{
		t.addColumn(sm_dbIdColumnName, Object.class, null);
        t.addColumn(OUT_EDGE_ID_PAIRS_PROP_NAME, Object.class, null);

		for (String propName : sm_nodePropNames)
		{
			t.addColumn(propName, Object.class, null);
		}
	}

    private void addEdgeTableProperties(Table t)
    {
        t.addColumn(sm_dbIdColumnName, Object.class, null);
        t.addColumn(sm_edgeTypePropName, Object.class, null);
        t.addColumn(EDGE_INNER_ID_PROP_NAME, Object.class, null);

        for (String propName : sm_nodePropNames)
        {
            t.addColumn(propName, Object.class, null);
        }

    }

    private void loadNodeProperties (Vertex vertex, Node node)
    {
        assert(vertex != null && vertex.getId() != null);

        node.set(sm_dbIdColumnName, vertex.getId());

        //拷贝一份，自己维护。因为prefuse需要独立操作节点，使用户有操作树的感觉。所以需要自己维护一套状态
        ArrayList<MindDB.OutEdgeIdPair> outEdgeIdPairs = new ArrayList<>();
        outEdgeIdPairs.addAll(m_mindDb.getOutEdgeIdPairs(vertex));

        node.set(OUT_EDGE_ID_PAIRS_PROP_NAME, outEdgeIdPairs);

        loadElementProperties(vertex, node, sm_nodePropNames);
    }

    private void loadEdgeProperties (com.tinkerpop.blueprints.Edge dbEdge, Edge edge)
    {
        assert(dbEdge != null && dbEdge.getId() != null);

        edge.set(sm_dbIdColumnName, dbEdge.getId());
        edge.set(sm_edgeTypePropName, dbEdge.getProperty(MindDB.EDGE_TYPE_PROP_NAME));
        edge.set(EDGE_INNER_ID_PROP_NAME, dbEdge.getProperty(MindDB.EDGE_INNER_ID_PROP_NAME));

        loadElementProperties(dbEdge, edge, sm_edgePropNames);

    }

    //store 仅仅保存与图无关的属性
    protected void storeNodeProperties(Vertex vertex, Node node)
    {
        storeElementProperties(vertex, node, sm_nodePropNames);
    }

    private void storeEdgeProperties (com.tinkerpop.blueprints.Edge dbEdge, Edge edge)
    {
        storeElementProperties(dbEdge, edge, sm_edgePropNames);
    }


    private static void fillPropertyClassMap()
    {
        if (sm_propertyClassMap.size() > 0) {
            return;
        }

        sm_propertyClassMap.put(TEXT_PROP_NAME, String.class);
        sm_propertyClassMap.put(TEXT_PROP_NAME, String.class);
        sm_propertyClassMap.put(STYLE_PROP_NAME, String.class);

        sm_propertyClassMap.put(sm_iconPropName, String.class);

        sm_propertyClassMap.put(sm_fontFamilyPropName, String.class);
        sm_propertyClassMap.put(sm_fontSizePropName, Integer.class);

        sm_propertyClassMap.put(sm_boldPropName, Boolean.class);
        sm_propertyClassMap.put(sm_italicPropName, Boolean.class);

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
            root.setProperty(TEXT_PROP_NAME, "root");

            EdgeVertex edgeVertex = m_mindDb.addChild(root, 0);
            edgeVertex.m_vertex.setProperty(MindModel.TEXT_PROP_NAME, "child_1");

            edgeVertex = m_mindDb.addChild(root, 1);
            edgeVertex.m_vertex.setProperty(MindModel.TEXT_PROP_NAME, "child_2");

            m_mindDb.addRefEdge(root, root, 2);

            m_favoriteIndex.put(FAVORITE_KEY_NAME, FAVORITE_KEY_NAME, root);
        }

        for (Vertex vertex : m_favoriteIndex.get(FAVORITE_KEY_NAME, FAVORITE_KEY_NAME))  {
            m_favoriteInfoes.add(new VertexBasicInfo(vertex));
        }

    }

    ArrayList<Object> getLastOpenedRootId()
    {
        Index<Vertex> lastOpenedIndex = m_mindDb.getOrCreateIndex(LAST_OPENED_INDEX_NAME);

        ArrayList<Object> lastOpenedRootId = new ArrayList<Object>();
        for (int i=0; lastOpenedIndex.get(LAST_OPENED_KEY_NAME, i).iterator().hasNext(); i++) {
            Vertex vertex = lastOpenedIndex.get(LAST_OPENED_KEY_NAME, i).iterator().next();
            lastOpenedRootId.add(vertex.getId());
        }
        return lastOpenedRootId;
    }

    public void close()
    {
        Index<Vertex> lastOpenedIndex = m_mindDb.getOrCreateIndex(LAST_OPENED_INDEX_NAME);

        //FIXME: 此处不能用 dropIndex来代替
        for (int i=0; lastOpenedIndex.get(LAST_OPENED_KEY_NAME, i).iterator().hasNext(); i++) {
            Vertex vertex = lastOpenedIndex.get(LAST_OPENED_KEY_NAME, i).iterator().next();
            lastOpenedIndex.remove(LAST_OPENED_KEY_NAME, i, vertex);
        }

        for (int i=0; i<m_trees.size(); i++) {
            Tree tree = m_trees.get(i);
            Vertex vertex = m_mindDb.getVertex(tree.getRoot().get(sm_dbIdColumnName));
            lastOpenedIndex.put(LAST_OPENED_KEY_NAME, i, vertex);
        }

        m_mindDb.shutdown();
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

        addNodeTableProperties(displayNodeTable);
        addEdgeTableProperties(displayEdgeTable);

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
        tree.deepTraverse(root, new Tree.TraverseProcessor() {
            public boolean run(Node parent, Node node, int level) {
                attachChildren(node);
                return level < initialLevel;
            }
        });


        return tree;
    }

    public void removeTree(Object rootId)
    {
        Tree tree = findTree(rootId);
        if (tree != null) {
            m_trees.remove(tree);
        }
    }

	private void loadElementProperties(com.tinkerpop.blueprints.Element dbElement, Tuple tuple, String keys[])
	{
        assert (keys == sm_nodePropNames || keys == sm_edgePropNames);
		for (String key : keys)
		{
            tuple.set(key, dbElement.getProperty(key));
		}
	}

    private void storeElementProperties(com.tinkerpop.blueprints.Element dbElement, Tuple tuple, String keys[])
    {
        assert (keys == sm_nodePropNames || keys == sm_edgePropNames);

        for (String key : keys)
        {
            Object value = tuple.get(key);
            if (value == null) {
                dbElement.removeProperty(key);
            } else {
                dbElement.setProperty(key, value);
            }
        }
    }

    private void verifyElementProperties(com.tinkerpop.blueprints.Element dbElement,
                                                   Tuple tuple, String keys[])
    {
        if (true) {
            return;
        }

        assert dbElement.getId().equals(tuple.get(sm_dbIdColumnName));

        for (String key : keys)
        {
            Object tupleValue = tuple.get(key);

            //OLD
            if (key == OUT_EDGE_ID_PAIRS_PROP_NAME) {
                ArrayList dbElementValue = m_mindDb.getContainerProperty(dbElement, key);
                assert tupleValue == dbElementValue || tupleValue.equals(dbElementValue);
            } else {
                Object dbElementValue = dbElement.getProperty(key);
                if (!(tupleValue == dbElementValue || tupleValue.equals(dbElementValue))) {
                    int debug = 1;
                }
                assert tupleValue == dbElementValue || tupleValue.equals(dbElementValue);
            }

            //NEW
            Object dbElementValue = dbElement.getProperty(key);
            if (!(tupleValue == dbElementValue || tupleValue.equals(dbElementValue))) {
                int debug = 1;
            }
            assert tupleValue == dbElementValue || tupleValue.equals(dbElementValue);
        }
    }
	private Vertex getDBVertex(Node node)
	{
		Object dbId = node.get(sm_dbIdColumnName);
		return m_mindDb.getVertex(dbId);
	}
	
	private com.tinkerpop.blueprints.Edge getDBEdge (Edge edge)
	{
        if (edge == null || edge.get(sm_dbIdColumnName) == null) {
            int debug = 1;
        }

		return m_mindDb.getEdge(edge.get(sm_dbIdColumnName));
	}

	public void attachChildren (Node parent)
	{
        Tree tree = (Tree)parent.getGraph();

        if (isChildrenAttached(parent)) {
            return;
        }

		ArrayList<EdgeVertex> edgeVertexArray = m_mindDb.getChildrenAndReferents(getDBVertex(parent));
		
		if (edgeVertexArray == null || edgeVertexArray.size() == 0)
		{
			return;
		}

        if (edgeVertexArray.size() > 50) {
            for (EdgeVertex edgeVertex : edgeVertexArray)
            {
                Vertex parentV = getDBVertex(parent);
                Vertex childV = edgeVertex.m_vertex;

                s_logger.info("[{}] -> [{}]", parentV.getProperty(TEXT_PROP_NAME), childV.getProperty(TEXT_PROP_NAME));
            }

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

        //collect the node with the same parentDbId to aimRows
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

    private List<MindDB.OutEdgeIdPair> getNodeOutEdgeIdPairs(Node sourceNode) {
        return (List<MindDB.OutEdgeIdPair>)sourceNode.get(OUT_EDGE_ID_PAIRS_PROP_NAME);
    }

    protected void exposeNodeRelation(Node sourceNode, int pos, EdgeVertex toTarget)
    {
        assert sourceNode != null;
        assert sourceNode.isValid();

        List<MindDB.OutEdgeIdPair> outEdgeIdPairs = getNodeOutEdgeIdPairs(sourceNode);

        //if this node is updated, skip
        if (outEdgeIdPairs.size() > pos && outEdgeIdPairs.get(pos).belongTo(toTarget.m_edge)) {
            return;
        }

        //如果原来没有挂上子节点，仅仅更新outEdgeIdPairs
        if (! isChildrenAttached(sourceNode)) {
            outEdgeIdPairs.add(pos, m_mindDb.getOutEdgeIdPair(toTarget.m_edge));
            return;
        }

        outEdgeIdPairs.add(pos, m_mindDb.getOutEdgeIdPair(toTarget.m_edge));

        Tree tree = (Tree)sourceNode.getGraph();

        Node child = tree.addNode();
        Edge edge = tree.addChildEdge(sourceNode, child, pos);

        loadNodeProperties(toTarget.m_vertex, child);
        loadEdgeProperties(toTarget.m_edge, edge);
    }

    private int getPositionInOutEdgeIdPairs(List<MindDB.OutEdgeIdPair> outEdgeIdPairs, Object edgeDbId) {
        for (int i=0; i<outEdgeIdPairs.size(); i++) {
            if (outEdgeIdPairs.get(i).equals(edgeDbId)) {
                return i;
            }
        }
        return -1;
    }

    protected void hideNodeRelation(Node sourceNode, Object edgeDbId)
    {
        assert (sourceNode != null);
        assert (sourceNode.isValid());

        List<MindDB.OutEdgeIdPair> outEdgeIdPairs = getNodeOutEdgeIdPairs(sourceNode);

        int pos = getPositionInOutEdgeIdPairs(outEdgeIdPairs, edgeDbId);

        //if this node is updated, skip
        if (pos == -1) {
            return;
        }

        //如果原来没有挂上子节点，仅仅更新outEdgeIdPairs
        if (! isChildrenAttached(sourceNode)) {
            outEdgeIdPairs.remove(pos);
            return;
        }

        outEdgeIdPairs.remove(pos);

        Tree tree = (Tree)sourceNode.getGraph();
        Node child = tree.getChild(sourceNode, pos);
        tree.removeChild(child);
    }

    protected void exposeNodeRelations(Node sourceNode, int pos, List<EdgeVertex> toTargets)
    {
        assert sourceNode != null;
        assert sourceNode.isValid();

        for (int i=0; i<toTargets.size(); i++) {
            exposeNodeRelation(sourceNode, i+pos, toTargets.get(i));
        }
    }

    protected void hideNodeRelations(Node sourceNode, List<Object> edgeDbIds)
    {
        assert (sourceNode != null);
        assert (sourceNode.isValid());

        for (Object edgeDbId : edgeDbIds) {
            hideNodeRelation(sourceNode, edgeDbId);
        }
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

    private void hideTreeRelation(final Tree tree, final Object sourceId, final Object edgeDbId)
    {
        visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                hideNodeRelation(sourceNode, edgeDbId);
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

    private void hideTreeRelations(final Tree tree, final Object sourceId, final List<Object> edgeDbIds)
    {
        visitNodeAvatars(tree, sourceId, new Visitor() {
            public void visit(Node sourceNode)
            {
                hideNodeRelations(sourceNode, edgeDbIds);
            }
        });
    }

    //opNode: user operated node
    protected void exposeModelRelation(Node opNode, int pos, EdgeVertex edgeVertex)
    {
        for (Tree tree : m_trees) {
            exposeTreeRelation(tree, getDbId(opNode), pos, edgeVertex);
        }

        verifyNode(opNode, true);
    }

    private void hideModelRelation(Object sourceId, Object edgeDbId)
    {
        for (final Tree tree : m_trees) {
            hideTreeRelation(tree, sourceId, edgeDbId);
        }
    }

    //opNode: user operated node
    protected void exposeModelRelations(Node opNode, int pos, List<EdgeVertex> toTargets)
    {
        for (Tree tree : m_trees) {
            exposeTreeRelations(tree, getDbId(opNode), pos, toTargets);
        }

        verifyNode(opNode, true);
    }

    private void hideModelRelations(Object sourceId, List<Object> edgeDbIds)
    {
        for (final Tree tree : m_trees) {
            hideTreeRelations(tree, sourceId, edgeDbIds);
        }
    }

    //return new child node
	public Node addChild(Node parent, int pos, String text)
    {
        if (! isChildrenAttached(parent)) {
            attachChildren(parent);
        }

        Object parentDbId = getDbId(parent);
		Vertex dbParent = m_mindDb.getVertex(parentDbId);
		EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);

        edgeVertex.m_vertex.setProperty(TEXT_PROP_NAME, text);

        exposeModelRelation(parent, pos, edgeVertex);

        return parent.getChild(pos);
	}

    private Importer getImporter(String path)
    {
        if (path == null) {
            return new TikaPlainTextImporter(m_mindDb);
        }

        File file = new File(path);
        if (file.isDirectory()) {
            return new DirectoryImporter(m_mindDb);

        } else if (file.isFile()) {
            if (path.endsWith(".mm")) {
                return new FreemindImporter(m_mindDb);
            } else {
                return new TikaPlainTextImporter(m_mindDb);
            }
        } else {
            return null;
        }
    }

    //return new child node
    public List importFile(final Node parent, final String path, final Component progressMonitorParent) throws Exception
    {
        if (! isChildrenAttached(parent)) {
            attachChildren(parent);
        }

        final int pos = parent.getChildCount();

        Object parentDbId = getDbId(parent);
        final Importer importer = getImporter(path);

        if (importer == null) {
            return new ArrayList();
        }

        List newChildren;

        if (path == null) {
            String text = Utils.getSystemClipboardText();
            if (text != null && !text.isEmpty()) {
                newChildren = importer.importString(getDbId(parent), pos, text);
            } else {
                newChildren = new ArrayList();
            }

        } else {
            Thread.sleep(5000);
            SwingWorkerDialog swingWorkerDialog = new SwingWorkerDialog(progressMonitorParent, "import :" + path) {
                @Override
                protected Object doInBackground() throws Exception {
                    importer.setProgressListener(new Importer.ProgressListener() {
                        @Override
                        public void notifyProgress(int progress, int maxProgress, String message) {
                            notifyProgressA(progress, maxProgress, message);
                            if (isCancelButtonPressed()) {
                                importer.cancel();
                            }
                        }
                    });
                    return importer.importFile(getDbId(parent), pos, path);
                }
            };

            newChildren = (List)swingWorkerDialog.executeInProgressDialog();
        }

        Vertex dbParent = m_mindDb.getVertex(parentDbId);
        ArrayList<EdgeVertex> newToTargets = new ArrayList<EdgeVertex>();
        for (int i=0; i<newChildren.size(); i++) {
            EdgeVertex edgeVertex = m_mindDb.getChildOrReferent(dbParent, pos+i);
            newToTargets.add(edgeVertex);
        }

        exposeModelRelations(parent, pos, newToTargets);
        return newChildren;
    }

	//return the DBid of node
	public Object trashNode(Object parentDbId, int pos)
	{
        Vertex parent = m_mindDb.getVertex(parentDbId);
        final EdgeVertex edgeChild = m_mindDb.getChildOrReferent(parent, pos);

        Object removedDbId = edgeChild.m_vertex.getId();
        Object removedEdgeDbId = edgeChild.m_edge.getId();

        m_mindDb.trashSubTree(parent, pos);

        hideModelRelation(parentDbId, removedEdgeDbId);

        removeFromFavorite(removedDbId);

        return removedDbId;
	}

    public boolean isVertexTrashed(Object dbId)
    {
        Vertex vertex = m_mindDb.getVertex(dbId);
        return m_mindDb.isVertexTrashed(vertex);
    }

	public void restoreNodeFromTrash(Node parent, final Object dbId)
	{
        if (! isChildrenAttached(parent)) {
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

    public void addReference(Node referrerNode, int pos, Object referentDbId) {
        if (! isChildrenAttached(referrerNode)) {
            attachChildren(referrerNode);
        }

        s_logger.info(String.format("addReference : %s -- %s", getText(referrerNode), referentDbId.toString()));
        Object referrerDbId  = getDbId(referrerNode);
        Vertex referrerVertex = m_mindDb.getVertex(referrerDbId);
        Vertex referentVertex = m_mindDb.getVertex(referentDbId);
        com.tinkerpop.blueprints.Edge refEdge = m_mindDb.addRefEdge(referrerVertex, referentVertex, pos);

        exposeModelRelation(referrerNode, pos, new EdgeVertex(refEdge, referentVertex));
    }


    public void removeReference(Object referrerDbId, int pos) {
        Vertex referrerVertex = m_mindDb.getVertex(referrerDbId);
        com.tinkerpop.blueprints.Edge edge = m_mindDb.getEdge(referrerVertex, pos);
        Object edgeId = edge.getId();

        m_mindDb.removeRefEdge(referrerVertex, pos);
        hideModelRelation(referrerDbId, edgeId);
    }

    public void changeChildPos(final Object parentDbId, final int oldPos, final int newPos)
    {
        s_logger.info("arg: parentDBID:{}", parentDbId);
        s_logger.info("arg: oldPos:{}", oldPos);
        s_logger.info("arg: newPos:{}", newPos);

        Vertex parent = m_mindDb.getVertex(parentDbId);
        m_mindDb.changeChildOrReferentPos(parent, oldPos, newPos);
        com.tinkerpop.blueprints.Edge edge = m_mindDb.getEdge(parent, newPos);
        final MindDB.OutEdgeIdPair outEdgeIdPair = m_mindDb.getOutEdgeIdPair(edge);

        for (final Tree tree : m_trees) {
            visitNodeAvatars(tree, parentDbId,
                new Visitor() {
                    public void visit(Node parent)
                    {
                        List<MindDB.OutEdgeIdPair> outEdgeIdPairs = getNodeOutEdgeIdPairs(parent);

                        s_logger.info("before change: outEdgeIdPairs:{}", outEdgeIdPairs);
                        outEdgeIdPairs.remove(oldPos);
                        outEdgeIdPairs.add(newPos, outEdgeIdPair);

                        s_logger.info("after change: outEdgeIdPairs:{}", outEdgeIdPairs);
                        if (parent.getChildCount() > 0) {
                            Edge edge = parent.getChildEdge(oldPos);
                            edge.set(EDGE_INNER_ID_PROP_NAME, outEdgeIdPair.m_innerId);
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

	static public Object getDbId(Tuple tuple)
	{
		return tuple.get(sm_dbIdColumnName);
	}

    static public String getOutEdgeInnerId(Edge edge)
    {
        return (String)edge.get(EDGE_INNER_ID_PROP_NAME);
    }

    public int getDBChildCount(Node node)
    {
        return getNodeOutEdgeIdPairs(node).size();
    }

    public boolean isChildrenAttached(Node node)
    {
        assert  node.getChildCount() == 0 || node.getChildCount() == getNodeOutEdgeIdPairs(node).size();
        return getNodeOutEdgeIdPairs(node).size() == node.getChildCount();
    }

    public boolean isSelfInDB(Node n1, Node n2)
    {
        return m_mindDb.isVertexIdSelf(getDbId(n1), getDbId(n2));
    }

    public boolean isChildInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdChild(getDbId(thiz), getDbId(that));
    }

    public boolean isParentInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdParent(getDbId(thiz), getDbId(that));
    }

    public boolean isSiblingInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdSibling(getDbId(thiz), getDbId(that));
    }

    public boolean isDescendantInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdDescendant(getDbId(thiz), getDbId(that));
    }

    public boolean isAncestorInDB(Node thiz, Node that) {
        return m_mindDb.isVertexIdAncestor(getDbId(thiz), getDbId(that));
    }

    private String objectToString(Object object)
    {
        return object == null ? "null" : object.toString();
    }
    public String getNodeDebugInfo(Node node) {
        int row = node.getRow();
        ArrayList<Integer> nodePath = getNodePath(node);
        Object rootId = getDbId(((Tree)node.getGraph()).getRoot());


        Object dbId = getDbId(node);
        if (dbId != null) {
            List inheritPath = m_mindDb.getInheritPath(dbId);
            String infoFmt = "row:%d, rootId:%s, nodePath:%s, inheritPath:%s, id:%s, text:%s";
            return String.format(infoFmt,
                    row,
                    objectToString(rootId),
                    objectToString(nodePath),
                    objectToString(inheritPath),
                    objectToString(getDbId(node)),
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
        return node.getString(TEXT_PROP_NAME);
    }

    public String getContextText(Object dbId)
    {
        Vertex vertex = m_mindDb.getVertex(dbId);
        String text = vertex.getProperty(TEXT_PROP_NAME);
        Vertex parent = m_mindDb.getParent(vertex);

        if (parent == null) {
            return text;
        } else {
            String parentText = parent.getProperty(TEXT_PROP_NAME);
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

    public Node getNodeByPath(Tree tree, int[] path)
    {
        Node node = tree.getRoot();

        for (int i=0; i<path.length; i++) {

            if (! isChildrenAttached(node)) {
                attachChildren(node);
            }

            int pos = path[i];
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

    //TODO: add cached
    public VertexBasicInfo getVertexBasicInfo(Object dbId) {
        return new VertexBasicInfo(m_mindDb.getVertex(dbId));
    }

    static final String MIRROR_X = "mirrorX";
    static final String MIRROR_Y = "mirrorY";

    public static void addNodeMirrorXYColumn(Tree tree, VisualTree visualTree)
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

    private NodeAvatarsPairingInfo pairNodeAvatars(Tree tree, Object dbId1, Object dbId2,
                                             int enforceNode1, int enforceNode2)
    {
        s_logger.info("arg: tree:{}", getDbId(tree.getRoot()));
        s_logger.info("arg: dbID1:{}", dbId1);
        s_logger.info("arg: dbID2:{}", dbId2);
        s_logger.info("arg: enforceNode1:{}", enforceNode1);
        s_logger.info("arg: enforceNode2:{}", enforceNode2);

        final ArrayList<Integer> nodeAvatars1 = getNodeAvatars(tree, dbId1);
        final ArrayList<Integer> nodeAvatars2 = getNodeAvatars(tree, dbId2);

        //跳过没有展开子节点的目标父节点
        Iterator<Integer> iterator2 = nodeAvatars2.iterator();
        while (iterator2.hasNext()) {
            int node2 = iterator2.next();
            if (! isChildrenAttached(tree.getNode(node2))) {
                iterator2.remove();
            }
        }

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

            if (! isChildrenAttached(tree.getNode(node1))) {
                continue;
            }

            int nearestNode2 = -1;
            Double minDistanceSquare = Double.MAX_VALUE;

            for (int node2 : nodeAvatars2) {

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
                                  EdgeVertex childEdgeVertex, Object oldEdgeDbId)
    {
        HashMap<Integer, Integer> pairs = oldNewParentPairingInfo.m_nodeAvatarPairs;
        for (int node1 : pairs.keySet())
        {
            int node2 = pairs.get(node1);
            Node oldParent = tree.getNode(node1);
            Node newParent = tree.getNode(node2);
            Node child = oldParent.getChild(oldChildPos);

            List<MindDB.OutEdgeIdPair> outEdgeIdPairs;

            outEdgeIdPairs = getNodeOutEdgeIdPairs(oldParent);
            outEdgeIdPairs.remove(oldChildPos);

            tree.removeEdge(tree.getEdge(oldParent, child));

            outEdgeIdPairs = getNodeOutEdgeIdPairs(newParent);
            outEdgeIdPairs.add(m_mindDb.getOutEdgeIdPair(childEdgeVertex.m_edge));

            Edge newEdge = tree.addChildEdge(newParent, child, newChildPos);
            loadEdgeProperties(childEdgeVertex.m_edge, newEdge);

            verifyNode(oldParent, true);
            verifyNode(newParent, true);
        }

        for (int node1 : oldNewParentPairingInfo.m_nodeAvatars1Alone) {
            if (tree.getNodeTable().isValidRow(node1)) {
                Node oldParent = tree.getNode(node1);
                hideNodeRelation(oldParent, oldEdgeDbId);
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
                              EdgeVertex newEdgeVertex, Object oldEdgeDbId)
    {
        Object oldParentDbId = getDbId(oldParent);
        Object newParentDbId = getDbId(newParent);

        for (Tree tree : m_trees) {
            NodeAvatarsPairingInfo oldNewParentPairingInfo;
            if(tree == oldParent.getGraph())
            {
                oldNewParentPairingInfo =  pairNodeAvatars(tree, oldParentDbId, newParentDbId,
                        oldParent.getRow(), newParent.getRow());
            } else {
                oldNewParentPairingInfo =  pairNodeAvatars(tree, oldParentDbId, newParentDbId,
                        -1, -1);
            }

            rebuildChildEdge(tree, oldNewParentPairingInfo, oldPos, newPos, newEdgeVertex, oldEdgeDbId);
        }
    }

    public void handoverChild(Node oldParent, int oldPos, Node newParent, int newPos)
    {
        s_logger.info("arg: oldParent:{}", oldParent);
        s_logger.info("arg: oldPos:{}", oldPos);
        s_logger.info("arg: newParent:{}", newParent);
        s_logger.info("arg: newPos:{}", newPos);

        assert (oldParent.getGraph() == newParent.getGraph());

        if (! isChildrenAttached(newParent)) {
            attachChildren(newParent);
        }

        Object oldParentDbId = getDbId(oldParent);
        Object newParentDbId = getDbId(newParent);
        assert (! oldParentDbId.equals(newParentDbId));

        Vertex oldParentVertex = m_mindDb.getVertex(oldParentDbId);
        Vertex newParentVertex = m_mindDb.getVertex(newParentDbId);
        Object oldEdgeId = getDbId(oldParent.getChildEdge(oldPos));

        EdgeVertex edgeVertex = m_mindDb.handoverChild(oldParentVertex, oldPos, newParentVertex, newPos);

        handoverNode(oldParent, oldPos, newParent, newPos, edgeVertex, oldEdgeId);

        s_logger.info("ret:");

    }

    public void handoverReferent(Node oldReferrer, int oldPos, Node newReferrer, int newPos)
    {
        s_logger.info("arg: oldReferrer:{}", oldReferrer);
        s_logger.info("arg: oldPos:{}", oldPos);
        s_logger.info("arg: newReferrer:{}", newReferrer);
        s_logger.info("arg: newPos:{}", newPos);

        assert (oldReferrer.getGraph() == newReferrer.getGraph());

        if (! isChildrenAttached(newReferrer)) {
            attachChildren(newReferrer);
        }

        Object oldReferrerDbId = getDbId(oldReferrer);
        Object newReferrerDbId = getDbId(newReferrer);
        assert (! oldReferrerDbId.equals(newReferrerDbId));

        Vertex oldReferrerVertex = m_mindDb.getVertex(oldReferrerDbId);
        Vertex newReferrerVertex = m_mindDb.getVertex(newReferrerDbId);
        Object oldEdgeId = getDbId(oldReferrer.getChildEdge(oldPos));

        EdgeVertex edgeVertex = m_mindDb.handoverReferent(oldReferrerVertex, oldPos, newReferrerVertex, newPos);
        handoverNode(oldReferrer, oldPos, newReferrer, newPos, edgeVertex, oldEdgeId);

        s_logger.info("ret:");

    }

    public boolean m_verifyNodeEnabled = true;

    void verifyNode(Node node, boolean forceChildAttached)
    {
        if (m_verifyNodeEnabled == false) {
            return;
        }

        Vertex vertex = getDBVertex(node);

        m_mindDb.verifyVertex(vertex);

        verifyElementProperties(vertex, node, sm_nodePropNames);

        List<MindDB.OutEdgeIdPair> outEdgeIdPairs = m_mindDb.getOutEdgeIdPairs(getDBVertex(node));
        if (outEdgeIdPairs.size() > 0 && forceChildAttached) {
            assert node.getChildCount() == outEdgeIdPairs.size();
        }

        for (int i=0; i<node.getChildCount(); i++) {
            Node childOrReferenceNode = node.getChild(i);
            Edge outEdge = (node.getGraph()).getEdge(node, childOrReferenceNode);

            assert(getOutEdgeInnerId(outEdge).equals(outEdgeIdPairs.get(i).m_innerId));
            verifyElementProperties(getDBEdge(outEdge), outEdge, sm_edgePropNames);

            Integer outEdgeType = (Integer)outEdge.get(sm_edgeTypePropName);
            if (MindDB.EdgeType.values()[outEdgeType] == MindDB.EdgeType.INCLUDE) {
                assert m_mindDb.isVertexIdParent(getDbId(childOrReferenceNode), getDbId(node));
            } else {
                assert MindDB.EdgeType.values()[outEdgeType] == MindDB.EdgeType.REFERENCE;
            }
        }

        Edge inEdge = node.getParentEdge();
        if (inEdge != null) {
            verifyElementProperties(getDBEdge(inEdge), inEdge, sm_edgePropNames);

            Integer inEdgeType = (Integer)inEdge.get(sm_edgeTypePropName);
            Node parentOrReferrerNode = inEdge.getSourceNode();

            List<MindDB.OutEdgeIdPair> parentOrReferrerOutEdgeIdPairs =
                    m_mindDb.getOutEdgeIdPairs(getDBVertex(parentOrReferrerNode));

            if (node.getIndex() >= parentOrReferrerOutEdgeIdPairs.size()) {
                int debug = 1;
            }

            assert parentOrReferrerOutEdgeIdPairs.get(node.getIndex()).m_innerId.equals(getOutEdgeInnerId(inEdge));

            if (MindDB.EdgeType.values()[inEdgeType] == MindDB.EdgeType.INCLUDE) {
                assert m_mindDb.isVertexIdChild(getDbId(parentOrReferrerNode), getDbId(node));
            } else {
                assert MindDB.EdgeType.values()[inEdgeType] == MindDB.EdgeType.REFERENCE;
            }
        }
    }

    public static Font getNodeFont(Tuple tuple)
    {
        String family = (String)tuple.get(sm_fontFamilyPropName);
        Integer size = (Integer)tuple.get(sm_fontSizePropName);
        Boolean bold = (Boolean)tuple.get(sm_boldPropName);
        Boolean italic = (Boolean)tuple.get(sm_italicPropName);

        if (family == null) {
            family = Style.getFontFamilySurely(tuple.getString(STYLE_PROP_NAME));
        }

        if (size == null) {
            size = Style.getFontSizeSurely(tuple.getString(STYLE_PROP_NAME));
        }

        if (bold == null) {
            bold = Style.getBoldSurely(tuple.getString(STYLE_PROP_NAME));
        }

        if (italic == null) {
            italic = Style.getItalicSurely(tuple.getString(STYLE_PROP_NAME));
        }

        if (family == null || bold == null || italic == null || size ==null) {
            int debug =1;
        }
        return FontLib.getFont(family, bold, italic, size);
    }

    public static int getNodeColor(Tuple tuple)
    {
        Integer color = (Integer)tuple.get(sm_nodeColorPropName);

        if (color != null) {
            return color;
        }

        return Style.getNodeColorSurely(tuple.getString(STYLE_PROP_NAME));
    }

    public static int getNodeTextColor(Tuple tuple)
    {
        Integer color = (Integer)tuple.get(sm_textColorPropName);

        if (color != null) {
            return color;
        }

        return Style.getTextColorSurely(tuple.getString(STYLE_PROP_NAME));
    }

    public static String getNodeIcon(Tuple tuple)
    {
        String icon = (String)tuple.get(sm_iconPropName);

        if (icon != null) {
            return icon;
        }

        return Style.getIconSurely(tuple.getString(STYLE_PROP_NAME));
    }

    public String getSubTreeText(Node subTreeRoot)
    {
        Tree tree = (Tree)subTreeRoot.getGraph();
        final StringBuilder stringBuilder = new StringBuilder();
        final String newline  = System.getProperty("line.separator");

        Tree.TraverseProcessor appendTextProc = new Tree.TraverseProcessor() {
            public boolean run(Node parent, Node node, int level) {
                for (int i=0; i<level; i++) {
                    stringBuilder.append("    ");
                }
                stringBuilder.append(getText(node));
                stringBuilder.append(newline);
                return true;
            }
        };

        tree.deepTraverse(subTreeRoot, appendTextProc);
        return stringBuilder.toString();
    }

    private void pasteNodeRecursively(final Node externalNode, final Node newNode)
    {
        visitNodeAvatars((Tree)newNode.getGraph(), getDbId(newNode), new Visitor() {
            @Override
            public void visit(Node newNodeAvatar)
            {
                Table.copyTuple(externalNode, newNodeAvatar, sm_nodePropNames);
                if (newNodeAvatar == newNode) {
                    storeNodeProperties(getDBVertex(newNodeAvatar), newNodeAvatar);
                }

                Edge externalEdgeToParent = externalNode.getParentEdge();
                if (externalEdgeToParent != null) {
                    Edge newAvatarEdgeToParent = newNodeAvatar.getParentEdge();

                    Table.copyTuple(externalEdgeToParent, newAvatarEdgeToParent, sm_edgePropNames);
                    if (newNodeAvatar == newNode) {
                        storeEdgeProperties(getDBEdge(newAvatarEdgeToParent), newAvatarEdgeToParent);
                    }
                }
            }
        });

        for(int i=0; i<externalNode.getChildCount(); i++) {
            Node newChild = addChild(newNode, i, "pasteNodeRecursively");
            pasteNodeRecursively(externalNode.getChild(i), newChild);
        }
    }

    public Node pasteTree(Node pastePoint, int position, Tree externalTree)
    {
        if (externalTree == null || externalTree.getRoot() == null) {
            return null;
        }

        Node subTreeRoot = addChild(pastePoint, position, "pasteTree");
        pasteNodeRecursively(externalTree.getRoot(), subTreeRoot);
        return subTreeRoot;
    }
}
