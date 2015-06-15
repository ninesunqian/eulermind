package eulermind.view;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import eulermind.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import eulermind.operator.*;
import prefuse.Display;
import prefuse.Visualization;

import prefuse.controls.*;
import prefuse.data.*;
import prefuse.util.PrefuseLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
import prefuse.visual.expression.VisiblePredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

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

public class MindView extends Display {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());
    final String m_treeGroupName = "tree";

    final public MindModel m_mindModel;
    MindController m_mindController;

    public TreeCursor m_cursor;
    Node m_savedCursor = null;

    TreeFolder m_folder;

    public Tree m_tree;
	MindTreeRenderEngine m_renderEngine;

    MindEditor m_mindEditor;

    boolean m_isChanging = false;

    //提出这个函数是为了单元测试
    void endInserting(final String text)
    {
        hideEditor();

        //由于事件处理机制，此时输入框不会立即消失。用invokeLater，在下面代码设置断点后，输入焦点才能切换到IDE中
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                m_logger.warn("++++++++++++++ hide Editor");
                //TODO
                MindOperator operator = new AddingChild(m_mindModel, getCursorSourceNode().getParent(),
                        getCursorSourceNode().getIndex(),  text);
                m_logger.info("MindView fire OK, insert at {}", getCursorSourceNode().getIndex());
                removePlaceholderCursor();

                //该函数会重绘所有的MindView
                m_mindController.does(operator);
                m_logger.warn("--------------- after hide Editor");
            }
        });
    }

    void endInserting(final Object dbId, final String text, final Object parentDbId, final String parentText)
    {
        hideEditor();

        //由于事件处理机制，此时输入框不会立即消失。用invokeLater，在下面代码设置断点后，输入焦点才能切换到IDE中

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                m_logger.warn("++++++++++++++ hide Editor");
                //TODO
                MindOperator operator = new AddingReference(m_mindModel, getCursorSourceNode().getParent(),
                        dbId, getCursorSourceNode().getIndex());
                removePlaceholderCursor();

                //该函数会重绘所有的MindView
                m_mindController.does(operator);
                m_logger.warn("--------------- after hide Editor");
            }
        });
    }

    void cancelInserting()
    {
        removePlaceholderCursor();

        //重绘，并回到正常状态
        renderTreeToEndChanging();
        hideEditor();
    }

    MindEditor.MindEditorListener m_editorListenerForInserting = new MindEditor.MindEditorListener() {
        public void editorOk(String text) {
            endInserting(text);
        }

        public void promptListOk(Object dbId, String text, Object parentDbId, String parentText) {
            endInserting(dbId, text, parentDbId, parentText);
        }

        public void cancel() {
            cancelInserting();
        }
    };

    void endEditing(final String text)
    {
        hideEditor();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                MindOperator operator;
                operator = new SettingProperty(m_mindModel,
                        getCursorSourceNode(), MindModel.TEXT_PROP_NAME, text);
                m_mindController.does(operator);
            }
        });
    }

    void cancelEditing()
    {
        hideEditor();
    }

    MindEditor.MindEditorListener m_editorListenerForEditing = new MindEditor.MindEditorListener() {
        public void editorOk(String text) {
            endEditing(text);
        }

        public void cancel() {
            cancelEditing();
        }
    };

    VisualTree m_visualTree;

	public MindView(MindModel mindModel, MindController undoManager, Tree tree) {
		super(new Visualization());
        JPanel pp = new JPanel();

        //s_logger.setLevel(Level.OFF);
		setSize(700, 600);
		setHighQuality(true);

        m_mindModel = mindModel;
        m_mindController = undoManager;

        m_tree = tree;
        m_visualTree = (VisualTree)m_vis.add(m_treeGroupName, m_tree);
        MindModel.addNodeMirrorXYColumn(m_tree, m_visualTree);

        setItemSorter(new TreeDepthItemSorter());
        m_renderEngine = new MindTreeRenderEngine(this, m_treeGroupName);

        m_cursor = new TreeCursor(this);
        m_folder = new TreeFolder(this);

		setMouseControlListener();

        m_mindEditor = new MindEditor();
        m_mindEditor.setMindDb(m_mindModel.m_mindDb);
        m_mindEditor.setHasPromptList(true);

        m_mindEditor.setBorder(null);
        m_mindEditor.setVisible(false);

        addControlListener(m_stopEditControl);

        setTextEditor(m_mindEditor);
        this.requestFocusInWindow();

        this.setFocusCycleRoot(true);

	}

    public NodeItem toVisual (Node node)
    {
        if (node instanceof NodeItem) {
            return  (NodeItem) node;
        } else {
            String treeNodesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.NODES);
            return (NodeItem) m_vis.getVisualItem(treeNodesGroupName, node);
        }
    }

    public EdgeItem toVisual (Edge edge)
    {
        if (edge instanceof EdgeItem) {
            return (EdgeItem) edge;
        } else {
            String treeEdgesGroupName = PrefuseLib.getGroupName(m_treeGroupName, Graph.EDGES);
            return (EdgeItem) m_vis.getVisualItem(treeEdgesGroupName, edge);
        }
    }

    public Node toSource (NodeItem nodeItem)
    {
        return (Node) m_vis.getSourceTuple (nodeItem);
    }

    public Edge toSource (EdgeItem edgeItem)
    {
        return (Edge) m_vis.getSourceTuple (edgeItem);
    }

	public void renderTree() {
        if (m_renderEngine != null) {
            m_renderEngine.run(null);
        }
	}

    public void renderTree(Runnable runAfterRePaint) {
        m_renderEngine.run(runAfterRePaint);
    }

    public void renderTreeToEndChanging() {
        m_renderEngine.run(new Runnable() {
            public void run() {
                endChanging();
            }
        });
    }

    ControlAdapter m_zoomToFitControl;
    ControlAdapter m_zoomControl;
    ControlAdapter m_wheelZoomControl;
    ControlAdapter m_panControl;

    ControlAdapter m_stopEditControl = new ControlAdapter() {
        public void mousePressed(MouseEvent e) {
            if (m_mindEditor.isVisible()) {
                m_mindEditor.confirm();
            }
        }
    };

    NodeDraggingControl m_dragControl;

    public NodeItem getDragHitNode()
    {
        return m_dragControl.m_hitNode;
    }

	private void setMouseControlListener()
    {
		m_zoomToFitControl = new ZoomToFitControl(Control.MIDDLE_MOUSE_BUTTON);
		m_zoomControl = new ZoomControl();
		m_wheelZoomControl = new WheelZoomControl();
		m_panControl = new PanControl();

		m_dragControl = new NodeDraggingControl(this);

        addControlListener(m_zoomToFitControl);
        addControlListener(m_zoomControl);
        addControlListener(m_wheelZoomControl);
        addControlListener(m_panControl);

        addControlListener(m_cursor);
        addControlListener(m_dragControl);
        addControlListener(m_folder);
	}

    void setTransformEnabled(boolean enabled)
    {
        m_zoomToFitControl.setEnabled(enabled);
        m_zoomControl.setEnabled(enabled);
        m_wheelZoomControl.setEnabled(enabled);
        m_panControl.setEnabled(enabled);
    }

    private void showEditor()
    {
        editText(toVisual(getCursorSourceNode()), MindModel.TEXT_PROP_NAME) ;
    }

    private void hideEditor()
    {
        super.stopEditing2(false);
    }

    public void clearSelectAndSetCursorNodeByPath(ArrayList<Integer> path)
    {
        m_cursor.clearSelectAndSetCursorNodeItem(toVisual(m_mindModel.getNodeByPath(m_tree, path)));
    }

    public boolean isChanging() {
        return m_isChanging;
    }

    //防止用户重复操作
    boolean beginChanging() {
        if (m_isChanging) {
            return false;
        }

        m_logger.info("beginChanging++++++++++++++++++++++");
        m_cursor.hold();
        setTransformEnabled(false);
        m_isChanging = true;
        return true;
    }

    void endChanging() {
        if (! m_isChanging) {
            return;
        }
        m_cursor.free();
        setTransformEnabled(true);

        m_logger.info("endChanging----------------------");
        m_isChanging = false;
        requestFocus();
    }

    void beginAdding(final boolean asChild, final boolean hasPrompt)
    {
        if (! beginChanging()) {
            return;
        }

        if (asChild == false) {
            if (getCursorSourceNode() == m_tree.getRoot()) {
                alert("you must open the root parent, before add a sibling of the root");
                endChanging();
                return;
            }
        }

        m_logger.info("beginAdding---------------");

        NodeItem cursorItem = m_cursor.getCursorNodeItem();
        if (asChild) {
            if (m_folder.isFolded(cursorItem)) {
                m_folder.unfoldNode(cursorItem);
            }
        } else {
            //TODO 防止 NodeIem与Node比较
            if (toSource(cursorItem) == m_tree.getRoot()) {
                assert(false);
                endChanging();
                return;
            }
        }

        addPlaceholder(asChild);

        renderTree(new Runnable() {
            @Override
            public void run()
            {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run()
                    {
                        m_logger.warn("++++++++++++++ show Editor");
                        //TODO
                        //listener 内部，调用endInstering (重绘，设置重绘后的hander是 恢复状态)
                        m_mindEditor.setMindEditorListener(m_editorListenerForInserting);
                        m_mindEditor.setHasPromptList(hasPrompt);
                        showEditor();
                        m_logger.warn("--------------- after show Editor");
                    }
                });
            }
        });
    }

    public void importFileOrDirectory()
    {
        if (! beginChanging()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        String path;

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            path = chooser.getSelectedFile().getPath();
            MindOperator operator = new ImportingFile(m_mindModel, getCursorSourceNode(), path, this);
            m_mindController.does(operator);
        }
        endChanging();
    }

    public void markToBeLinkedDbId()
    {
        m_mindController.m_toBeLinkedDbId = m_mindModel.getDbId(getCursorSourceNode());
    }

    public void copySubTree()
    {
        m_mindController.m_copiedSubTree = m_visualTree.copySubTree(getCursorNodeItem(), VisiblePredicate.TRUE,
                MindModel.sm_nodePropNames, MindModel.sm_edgePropNames);

        String text = m_mindModel.getSubTreeText(getCursorSourceNode());
        m_mindController.m_clipboardTextFormHere = text;
        Utils.copyStringToSystemClipboard(text);
    }

    public void pasteAsSubTree()
    {
        if (! beginChanging()) {
            return;
        }

        if (m_mindController.m_copiedSubTree != null &&
                m_mindController.m_clipboardTextFormHere == Utils.getSystemClipboardText()) {
            MindOperator operator = new PastingExternalTree(m_mindModel, getCursorSourceNode(), m_mindController.m_copiedSubTree);
            m_mindController.does(operator);

        } else {
            String text = Utils.getSystemClipboardText();
            if (text != null) {
                MindOperator operator = new ImportingFile(m_mindModel, getCursorSourceNode(), null, null);
                m_mindController.does(operator);
            }
        }

        endChanging();
    }

    private void alert(String msg)
    {
        JOptionPane.showMessageDialog(null, msg);
    }

    private void addPlaceholder(boolean asChild)
    {
        Node cursorNode = getCursorSourceNode();
        m_savedCursor = cursorNode;

        Node newNode;

        if (asChild) {
            m_folder.unfoldNode(toVisual(cursorNode));

            newNode = m_tree.addChild(cursorNode, cursorNode.getChildCount());

        } else {
            newNode = m_tree.addChild(cursorNode.getParent(), cursorNode.getIndex() + 1);
            m_logger.info("add sibling at {}", cursorNode.getIndex() + 1);
        }

        //NOTE: newNode.setString(MindModel.TEXT_PROP_NAME, "") error

        newNode.set(MindModel.TEXT_PROP_NAME, "");

        m_cursor.clearSelectAndSetCursorNodeItem(toVisual(newNode));
    }

    private void removePlaceholderCursor()
    {
        Node placeholderNode = getCursorSourceNode();
        if (isPlaceholder(placeholderNode) == false) {
            int i=0;
        }
        assert(isPlaceholder(placeholderNode));
        assert(placeholderNode != m_tree.getRoot());

        m_tree.removeChild(placeholderNode);
        m_cursor.clearSelectAndSetCursorNodeItem(toVisual(m_savedCursor));
    }

    //include node and edge, the edge is used rendering
    public boolean isPlaceholder(Tuple tuple)
    {
        return (m_mindModel.getDbId(tuple) == null);
    }

    public NodeItem getCursorNodeItem()
    {
        return m_cursor.getCursorNodeItem();
    }

    public List<NodeItem> getSelectedNodeItems()
    {
        return m_cursor.getSelectedNodeItems();
    }

    //FIXME：不能加判断, 接口中要确保光标有效
    public Node getCursorSourceNode()
    {
        NodeItem cursorItem = m_cursor.getCursorNodeItem();

        if (cursorItem != null && cursorItem.isValid()) {
            return toSource(cursorItem);
        } else {
            return null;
        }
    }

    public void verifyCursor()
    {

    }

    public List<Node> getSelectedSourceNodes()
    {
        List<NodeItem> nodeItems = m_cursor.getSelectedNodeItems();
        ArrayList<Node> nodes = new ArrayList<>();

        for (NodeItem nodeItem : nodeItems) {
            nodes.add(toSource(nodeItem));
        }

        return nodes;
    }

    public void setCursorProperty(String key, Object value)
    {
        //called by toolbar controls' action listener,
        if (! beginChanging()) {
            return;
        }

        m_logger.info("setCursorProperty: {} - {}", key, value);

        List<Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = removeNodesWithSameDbId(selectedNodes);

        ArrayList<MindOperator> operators = new ArrayList<>();
        for (Node node : selectedNodes) {
            operators.add(new SettingProperty(m_mindModel, node, key, value));
        }

        m_mindController.does(operators);
        endChanging();
    }


    void undo() {
        if (! beginChanging()) {
            return;
        }

        if (m_mindController.canUndo()) {
            m_mindController.undo();
        }

        endChanging();
    }

    void redo() {
        if (! beginChanging()) {
            return;
        }

        if (m_mindController.canRedo()) {
            m_mindController.redo();
        }

        endChanging();
    }

    void save() {
        m_mindModel.m_mindDb.commit();
    }

    public void remove()
    {
        if (! beginChanging()) {
            return;
        }

        List<Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = breadthFirstSort(selectedNodes);
        selectedNodes = removeNodesWithSameInEdgeDbId(selectedNodes);

        ArrayList<MindOperator> operators = new ArrayList<>();
        for (Node node : selectedNodes) {
            operators.add(new Removing(m_mindModel, node));
        }

        m_mindController.does(operators);

        endChanging();
    }

    public void addChild() {
        beginAdding(true, false);
    }

    public void addSibling() {
        beginAdding(false, false);
    }

    public void addChildWithPrompt() {
        beginAdding(true, true);
    }

    public void addSiblingWithPrompt() {
        beginAdding(false, true);
    }

    public void edit() {
        if (! beginChanging()) {
            return;
        }

        m_mindEditor.setMindEditorListener(m_editorListenerForEditing);
        m_mindEditor.setHasPromptList(false);
        showEditor();
    }

    public void linkMarkedVertexToCursor()
    {
        if (! beginChanging()) {
            return;
        }

        if (m_mindController.m_toBeLinkedDbId != null
                && ! m_mindModel.isVertexTrashed(m_mindController.m_toBeLinkedDbId)) {

            MindOperator operator = new AddingReference(m_mindModel, getCursorSourceNode(),
                    m_mindController.m_toBeLinkedDbId, getCursorSourceNode().getChildCount());
            m_mindController.does(operator);

        }

        endChanging();
    }

    public void cursorMoveUp() {

        if (m_isChanging) {
            return;
        }

        m_cursor.moveUp();
    }

    public void cursorMoveDown() {
        if (m_isChanging) {
            return;
        }
        m_cursor.moveDown();
    }

    public void cursorMoveLeft() {
        if (m_isChanging) {
            return;
        }
        m_cursor.moveLeft();
    }

    public void cursorMoveRight() {
        if (m_isChanging) {
            return;
        }
        m_cursor.moveRight();
    }

    public void toggleFoldNode() {
        if (m_isChanging) {
            return;
        }
        m_folder.toggleFoldNode(m_cursor.getCursorNodeItem());
    }

    public List<Node> breadthFirstSort(final List<Node> nodes)
    {
        final ArrayList<Node> sortedNodes = new ArrayList<>(nodes.size());

        if (nodes.size() == 0) {
            return sortedNodes;
        }

        Tree tree = nodes.get(0).getGraph().getSpanningTree();

        tree.breadthFirstTraverse(tree.getRoot(), new Tree.BreadthFristTraverseProcessor() {
            @Override
            public void run(Node node) {
                if (nodes.contains(node)) {
                    sortedNodes.add(node);
                }
            }
        });

        return sortedNodes;
    }


    public List<Node> removeNodesWithSameDbId(List<Node> nodes)
    {
        ArrayList<Node> leftNodes = new ArrayList<>();

        if (nodes.size() == 0) {
            return leftNodes;
        }

        HashSet<Object> leftDbIds = new HashSet<>();

        for (Node node : nodes) {
            if (! leftDbIds.contains(m_mindModel.getDbId(node))) {
                leftNodes.add(node);
                leftDbIds.add(m_mindModel.getDbId(node));
            }
        }

        return leftNodes;
    }

    //这个函数会去掉根节点。删除多选节点时用, 拖动多选节点时也用
    public List<Node> removeNodesWithSameInEdgeDbId(List<Node> nodes)
    {
        ArrayList<Node> leftNodes = new ArrayList<>();

        if (nodes.size() == 0) {
            return leftNodes;
        }

        HashSet<Object> leftInEdgeDbIds = new HashSet<>();

        for (Node node : nodes) {
            Edge inEdge = node.getParentEdge();
            if (inEdge == null) {
                continue;
            }

            if (! leftInEdgeDbIds.contains(m_mindModel.getDbId(inEdge))) {
                leftNodes.add(node);
                leftInEdgeDbIds.add(m_mindModel.getDbId(inEdge));
            }
        }

        return leftNodes;
    }

    public List<Node> removeNodesInSameDisplaySubTree(List<Node> nodes)
    {
        ArrayList<Node> subTreeRoots = new ArrayList<>();

        if (nodes.size() == 0) {
            return subTreeRoots;
        }

        breadthFirstSort(nodes);

        Tree tree = nodes.get(0).getGraph().getSpanningTree();

        for (Node node : nodes) {

            boolean inOneSubTree = false;

            for (Node subTreeRoot: subTreeRoots) {
                if (tree.subTreeContains(subTreeRoot, node)) {
                    inOneSubTree = true;
                    break;
                }
            }

            if (! inOneSubTree) {
                subTreeRoots.add(node);
            }
        }

        return subTreeRoots;
    }

} // end of class TreeMap
