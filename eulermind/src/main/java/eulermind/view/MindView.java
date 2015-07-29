package eulermind.view;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.tinkerpop.blueprints.Vertex;
import eulermind.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import eulermind.operator.*;
import prefuse.Display;
import prefuse.Visualization;

import prefuse.controls.*;
import prefuse.data.*;
import prefuse.data.event.TableListener;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.util.ui.UILib;
import prefuse.visual.*;
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
    final static String TEXT_AREA_COLUMN_NAME = "textBounds";

    final public MindModel m_mindModel;
    MindController m_mindController;

    public Tree m_tree;
    VisualTree m_visualTree;
    MindTreeRenderEngine m_renderEngine;

    public TreeCursor m_cursor;
    Node m_savedCursor = null;

    TreeFolder m_folder;

    boolean m_isChanging = false;

    MindEditor m_mindEditor;
    MindEditor.MindEditorListener m_editorListenerForEditing;
    MindEditor.MindEditorListener m_editorListenerForInserting;

    NodeDraggingControl m_dragControl;
    ControlAdapter m_stopEditControl;
    ControlAdapter m_wheelZoomControl;
    ControlAdapter m_panControl;
    ControlAdapter m_wheelPanControl;


    public MindView(MindModel mindModel, MindController undoManager, Tree tree) {
        super(new Visualization());

        //s_logger.setLevel(Level.OFF);
        setSize(700, 600);
        setHighQuality(true);

        m_mindModel = mindModel;
        m_mindController = undoManager;

        m_tree = tree;
        m_visualTree = (VisualTree)m_vis.add(m_treeGroupName, m_tree);
        MindModel.addNodeMirrorXYColumn(m_tree, m_visualTree);

        VisualTable nodeItemTable = (VisualTable)m_visualTree.getNodeTable();
        nodeItemTable.addColumn(TEXT_AREA_COLUMN_NAME, Rectangle2D.Double.class, null);

        setItemSorter(new TreeDepthItemSorter());
        m_renderEngine = new MindTreeRenderEngine(this, m_treeGroupName);

        initEditor();

        initMouseControlListener();

        this.requestFocusInWindow();
        this.setFocusCycleRoot(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panToExposeItem(m_cursor.getLastSelectedNodeItem());
                repaint();
            }
        });

        setDragAndDrop();

        m_popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                setAllControlEnabled(false);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                setAllControlEnabled(true);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                setAllControlEnabled(true);
            }
        });

        renderTree();
    }

    private void initEditor() {

        m_mindEditor = new MindEditor();
        m_mindEditor.setMindDb(m_mindModel.m_mindDb);
        m_mindEditor.setHasPromptList(true);

        m_mindEditor.setVisible(false);

        setTextEditor(m_mindEditor);

        m_editorListenerForEditing = new MindEditor.MindEditorListener() {
            public void editorOk(String text) {
                endEditing(text);
            }

            public void cancel() {
                cancelEditing();
            }
        };

        m_editorListenerForInserting = new MindEditor.MindEditorListener() {
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

    }

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

    public NodeItem getDragHitNode()
    {
        return m_dragControl.m_hitNode;
    }

	private void initMouseControlListener()
    {
        m_wheelZoomControl = new WheelZoomControl(true, false) {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown() || e.isMetaDown()) {
                    super.mouseWheelMoved(e);
                }
            }
        };

        m_panControl = new PanControl();

        m_wheelPanControl = new ControlAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown() || e.isMetaDown()) {
                    return;
                }

                double dx = 0;
                double dy = 0;

                if (e.isShiftDown()) {
                    dx = -getWidth() * 0.1 * e.getPreciseWheelRotation();
                } else {
                    dy = -getHeight() * 0.1 * e.getPreciseWheelRotation();
                }

                panOnItems(dx, dy);
                repaint();
            }
        };

        m_stopEditControl = new ControlAdapter() {
            public void mousePressed(MouseEvent e) {
                if (m_mindEditor.isVisible()) {
                    m_mindEditor.confirm();
                }
            }
        };

        //TODO:修改这三个control, 内部不要保存MindView的成员变量。需要的时候实时获取。
        m_cursor = new TreeCursor(this);
        m_folder = new TreeFolder(this);
        m_dragControl = new NodeDraggingControl(this);


        addControlListener(m_controlArbiter);
        addControlListener(m_wheelZoomControl);
        addControlListener(m_panControl);
        addControlListener(m_wheelPanControl);

        addControlListener(m_cursor);
        addControlListener(m_dragControl);
        addControlListener(m_folder);

        addControlListener(m_stopEditControl);

        addControlListener(new NodeControl(this) {

            @Override
            public void mousePressed(MouseEvent e) {
                m_logger.info("press: x:{}, y{}", e.getX(), e.getY());
            }

            @Override
            public void nodeItemPressed(NodeItem item, MouseEvent e) {
                m_logger.info("nodeItemPressed: x:{}, y{},   item x:{},y:{}", e.getX(), e.getY(),
                        item.getX(), item.getY());

            }

        });
	}

    @Override
    protected Rectangle2D getTextBounds(VisualItem item)
    {
        if (item.get(TEXT_AREA_COLUMN_NAME) != null) {
            return (Rectangle2D.Double)item.get(TEXT_AREA_COLUMN_NAME);
        }else {
            return item.getBounds();
        }
    }

    void setTransformEnabled(boolean enabled)
    {
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

    /*
    public void setCursorNodeByPath(ArrayList<Integer> path)
    {
        m_cursor.moveToNode(toVisual(m_mindModel.getNodeByPath(m_tree, path)));
    }
    */

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

        NodeItem cursorItem = m_cursor.getLastSelectedNodeItem();
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

    public void copySubTree()
    {
        //这里需要用一个变量保存下剪切板内容，以判断paste操作时来自内部，还是外部
        m_mindController.m_clipboardTextFormHere = getSelectedSubTreesText();
        m_mindController.m_toBeLinkedDbIds = getDBIdsOfSelectedNodes();
        m_mindController.m_copiedSubTrees = getSelectedSubTrees();

        Utils.copyStringToSystemClipboard(m_mindController.m_clipboardTextFormHere);
    }

    public String getSelectedSubTreesText()
    {
        String text = "";

        final String newline  = System.getProperty("line.separator");

        List<Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = breadthFirstSort(selectedNodes);
        selectedNodes = removeNodesInSameDisplaySubTree(selectedNodes);

        for (Node node : selectedNodes) {
            String subTreeText = m_mindModel.getSubTreeText(node);
            if (!subTreeText.endsWith(newline)) {
                subTreeText += newline;
            }

            text += subTreeText;
        }

        return text;
    }

    public ArrayList<Tree> getSelectedSubTrees()
    {
        ArrayList<Tree> subTrees = new ArrayList<>();

        List<Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = breadthFirstSort(selectedNodes);
        selectedNodes = removeNodesInSameDisplaySubTree(selectedNodes);

        for (Node node : selectedNodes) {
            //只复制可见的部分，不可见的部分可能在数据库内数目巨大
            subTrees.add(m_visualTree.copySubTree(toVisual(node), VisiblePredicate.TRUE,
                    MindModel.sm_nodePropNames, MindModel.sm_edgePropNames));
        }

        return subTrees;
    }

    public ArrayList<Object> getInEdgeDBIdsOfSelectedNodes()
    {
        List<Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = breadthFirstSort(selectedNodes);
        selectedNodes = removeNodesWithSameInEdgeDbId(selectedNodes);
        ArrayList<Object> inEdgeDbIds = new ArrayList<>();

        for (Node node : selectedNodes) {
            Edge inEdge = node.getParentEdge();
            inEdgeDbIds.add(m_mindModel.getDbId(inEdge));
        }

        return inEdgeDbIds;
    }

    public ArrayList<Object> getDBIdsOfSelectedNodes()  {
        List<Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = breadthFirstSort(selectedNodes);
        ArrayList<Object> dbIds = new ArrayList<>();
        for (Node node : selectedNodes) {
            dbIds.add(m_mindModel.getDbId(node));
        }

        return dbIds;
    }



    public void pasteAsSubTree()
    {
        if (! beginChanging()) {
            return;
        }

        if (m_mindController.m_copiedSubTrees.size() != 0 &&
                m_mindController.m_clipboardTextFormHere.equals(Utils.getSystemClipboardText())) {

            ArrayList<MindOperator> operators = new ArrayList<>();

            for (Tree copiedTree : m_mindController.m_copiedSubTrees) {
                MindOperator operator = new PastingExternalTree(m_mindModel, getCursorSourceNode(), copiedTree);
                operators.add(operator);
            }

            m_mindController.does(operators);

        } else {
            String text = Utils.getSystemClipboardText();
            if (text != null) {
                MindOperator operator = new ImportingFile(m_mindModel, getCursorSourceNode(), text);
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
            newNode = m_tree.addChild(cursorNode, cursorNode.getChildCount());
            m_folder.unfoldNode(toVisual(cursorNode));

        } else {
            newNode = m_tree.addChild(cursorNode.getParent(), cursorNode.getIndex() + 1);
            m_logger.info("add sibling at {}", cursorNode.getIndex() + 1);
        }

        //NOTE: newNode.setString(MindModel.TEXT_PROP_NAME, "") error

        newNode.set(MindModel.TEXT_PROP_NAME, "");

        m_cursor.selectOnlyOneNodeItem(toVisual(newNode));
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

        m_cursor.selectOnlyOneNodeItem(toVisual(m_savedCursor));
    }

    //include node and edge, the edge is used rendering
    public boolean isPlaceholder(Tuple tuple)
    {
        return (m_mindModel.getDbId(tuple) == null);
    }

    public NodeItem getCursorNodeItem()
    {
        return m_cursor.getLastSelectedNodeItem();
    }

    public List<NodeItem> getSelectedNodeItems()
    {
        return m_cursor.getSelectedNodeItems();
    }

    //FIXME：不能加判断, 接口中要确保光标有效
    public Node getCursorSourceNode()
    {
        NodeItem cursorItem = m_cursor.getLastSelectedNodeItem();
        assert cursorItem != null && cursorItem.isValid();
        return toSource(cursorItem);
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

        if (m_mindController.m_toBeLinkedDbIds.size() > 0) {

            ArrayList<MindOperator> operators = new ArrayList<>();

            for (Object markedDbId : m_mindController.m_toBeLinkedDbIds) {

                if (m_mindModel.isVertexTrashed(markedDbId)) {
                    continue;
                }

                MindOperator operator = new AddingReference(m_mindModel, getCursorSourceNode(),
                        markedDbId, getCursorSourceNode().getChildCount());

                operators.add(operator);
            }

            m_mindController.does(operators);
        }

        endChanging();
    }

    public void toggleFoldNode() {
        if (m_isChanging) {
            return;
        }

        m_folder.toggleFoldNode(m_cursor.getLastSelectedNodeItem());
        //FIXME:  renderTreeToEndChanging() 挪到这里？
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

    void setAllControlEnabled(boolean enabled) {

        m_wheelZoomControl.setEnabled(enabled);
        m_panControl.setEnabled(enabled);

        m_cursor.setEnabled(enabled);
        m_dragControl.setEnabled(enabled);
        m_folder.setEnabled(enabled);

    }

    NodeControl m_controlArbiter = new NodeControl(this) {
        @Override
        public void nodeItemEntered(NodeItem item, MouseEvent e) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }

        @Override
        public void nodeItemExited(NodeItem item, MouseEvent e) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }

            setAllControlEnabled(true);

            if (getCursorNodeItem() == item) {
                if (m_cursor.getSelectedNodeItems().size() > 1) {
                    m_cursor.setEnabled(true);
                    m_folder.setEnabled(false);
                } else {
                    m_cursor.setEnabled(false);
                    m_folder.setEnabled(true);
                }

            } else {
                m_cursor.setEnabled(true);
                m_folder.setEnabled(false);
            }
        }

        @Override
        public void nodeItemPressed(NodeItem item, MouseEvent e) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }

        @Override
        public void nodeItemClicked(NodeItem item, MouseEvent e) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }

        @Override
        public void dragHit(NodeItem draggedNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }

        @Override
        public void dragMiss(NodeItem draggedNode, NodeItem hitNode, DragAction dragAction) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }

        @Override
        public void dragStart(NodeItem draggedNode, DragAction dragAction) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }

        @Override
        public void dragActionChanged(NodeItem draggedNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }

        @Override
        public void dragEnd(NodeItem draggedNode, NodeItem hitNode, HitPosition hitPosition, DragAction dragAction) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
        }


        @Override
        public void nodeItemReleased(NodeItem item, MouseEvent e) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);

            if ( UILib.isButtonPressed(e, RIGHT_MOUSE_BUTTON) ) {
                setAllControlEnabled(false);
                openContextMenu(e.getX(), e.getY());
            } else {
                setAllControlEnabled(true);
            }
        }


        @Override
        public void nodeItemKeyPressed(NodeItem item, KeyEvent e) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
            processKey(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (m_popupMenu.isVisible()) {
                setAllControlEnabled(false);
                return;
            }
            setAllControlEnabled(true);
            processKey(e);
        }
    };


    List<MindOperator> getDragOperators(Node droppedNode,
                                        NodeControl.HitPosition hitPosition,
                                        boolean toAddReference)
    {
        MindModel mindModel = m_mindModel;

        List<MindOperator> operators = new ArrayList<>();

        List<Node> selectedNodes = getSelectedSourceNodes();

        selectedNodes = breadthFirstSort(selectedNodes);

        if (toAddReference) {

            int edgePosition;
            Node referrerNode;

            if (hitPosition == NodeControl.HitPosition.TOP || hitPosition == NodeControl.HitPosition.BOTTOM) {
                referrerNode = droppedNode.getParent();
                if (referrerNode == null) {
                    return null;
                }

                edgePosition = (hitPosition == NodeControl.HitPosition.TOP) ? droppedNode.getIndex() : droppedNode.getIndex() + 1;

            } else {
                referrerNode = droppedNode;
                edgePosition = droppedNode.getChildCount();
            }

            //由于添加引用操作，是新建Node。所以多选的时候，选集中的后续节点不能作为前驱节点的兄弟
            for (Node selectedNode : selectedNodes) {
                operators.add(new AddingReference(mindModel, selectedNode, referrerNode, edgePosition));
                edgePosition++;
            }

        } else {

            selectedNodes = removeNodesWithSameInEdgeDbId(selectedNodes);

            operators.add(new DraggingNode(mindModel, selectedNodes.get(0), droppedNode, hitPosition));

            for(int i=1; i<selectedNodes.size(); i++) {
                operators.add(new DraggingNode(mindModel, selectedNodes.get(i), selectedNodes.get(i-1), NodeControl.HitPosition.BOTTOM));
            }
        }

        return operators;
    }

    List<MindOperator> getExternalDragOperators(Node droppedNode,
                                        NodeControl.HitPosition hitPosition,
                                        boolean toAddReference,
                                        DndData dndData)
    {
        MindModel mindModel = m_mindModel;

        List<MindOperator> operators = new ArrayList<>();

        if (toAddReference) {

            int edgePosition;
            Node referrerNode;

            if (hitPosition == NodeControl.HitPosition.TOP || hitPosition == NodeControl.HitPosition.BOTTOM) {
                referrerNode = droppedNode.getParent();
                if (referrerNode == null) {
                    return null;
                }

                edgePosition = (hitPosition == NodeControl.HitPosition.TOP) ? droppedNode.getIndex() : droppedNode.getIndex() + 1;

            } else {
                referrerNode = droppedNode;
                edgePosition = droppedNode.getChildCount();
            }

            //由于添加引用操作，是新建Node。所以多选的时候，选集中的后续节点不能作为前驱节点的兄弟
            for (Object referentDbId : dndData.m_vertexIdsForLinking) {
                operators.add(new AddingReference(mindModel, referrerNode, referentDbId, edgePosition));
                edgePosition++;
            }

        } else {

            operators.add(new DraggingNode(mindModel, droppedNode, dndData.m_edgeIdsForDragging.get(0), hitPosition, null));

            for(int i=1; i<dndData.m_edgeIdsForDragging.size(); i++) {
                com.tinkerpop.blueprints.Edge previousEdge = m_mindModel.m_mindDb.getEdge(dndData.m_edgeIdsForDragging.get(i-1));
                Vertex previousEdgeTarget = m_mindModel.m_mindDb.getEdgeTarget(previousEdge);

                operators.add(new DraggingNode(mindModel, droppedNode, dndData.m_edgeIdsForDragging.get(i), hitPosition,
                        previousEdgeTarget.getId()));
            }
        }

        return operators;
    }

    void moveUp() {
        List <Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = removeNodesWithSameInEdgeDbId(selectedNodes);
        selectedNodes = removeNodesInSameDisplaySubTree(selectedNodes);
        BoundaryNodes boundary = getBoundaryNodes(selectedNodes);

        if (boundary.top.getPreviousSibling() == null) {
            return;
        }

        //选集的上移：放在最上端的哥哥上面 (肯定不在选集内)


        List<MindOperator> operators = getDragOperators(boundary.top.getPreviousSibling(), NodeControl.HitPosition.TOP, false);
        if (operators != null && operators.size() > 0) {
            m_mindController.does(operators);
        }
    }

    void moveDown() {
        List <Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = removeNodesWithSameInEdgeDbId(selectedNodes);
        selectedNodes = removeNodesInSameDisplaySubTree(selectedNodes);
        BoundaryNodes boundary = getBoundaryNodes(selectedNodes);

        if (boundary.bottom.getNextSibling() == null) {
            return;
        }

        //选集的下移：放在最下端的弟弟的下面 (肯定不在选集内)

        List<MindOperator> operators = getDragOperators(boundary.bottom.getNextSibling(), NodeControl.HitPosition.BOTTOM, false);
        if (operators != null && operators.size() > 0) {
            m_mindController.does(operators);
        }
    }
    void moveLeft() {
        List <Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = removeNodesWithSameInEdgeDbId(selectedNodes);
        selectedNodes = removeNodesInSameDisplaySubTree(selectedNodes);
        BoundaryNodes boundary = getBoundaryNodes(selectedNodes);

        //选集的左移：放入最左端的父亲的弟弟位置 (肯定不在选集内)
        if (boundary.left.getParent() != null) {
            List<MindOperator> operators = getDragOperators(boundary.left.getParent(), NodeControl.HitPosition.BOTTOM, false);
            if (operators != null && operators.size() > 0) {
                m_mindController.does(operators);
            }
        }
    }
    void moveRight() {

        List <Node> selectedNodes = getSelectedSourceNodes();
        selectedNodes = removeNodesWithSameInEdgeDbId(selectedNodes);
        selectedNodes = removeNodesInSameDisplaySubTree(selectedNodes);
        BoundaryNodes boundary = getBoundaryNodes(selectedNodes);

        Node leftSibling;

        for (leftSibling = boundary.left.getPreviousSibling();
             leftSibling != null;
             leftSibling = leftSibling.getPreviousSibling()) {
            if (! selectedNodes.contains(leftSibling)) {
                break;
            }
        }

        if (leftSibling == null) {
            for (leftSibling = boundary.left.getNextSibling();
                 leftSibling != null;
                 leftSibling = leftSibling.getNextSibling()) {
                if (! selectedNodes.contains(leftSibling)) {
                    break;
                }
            }
        }

        if (leftSibling != null) {
            List<MindOperator> operators = getDragOperators(leftSibling, NodeControl.HitPosition.RIGHT, false);
            if (operators != null && operators.size() > 0) {
                m_mindController.does(operators);
            }
        }
    }

    void processKey(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                toggleFoldNode();
                break;

            case KeyEvent.VK_F2:
                edit();
                break;

            case KeyEvent.VK_DELETE:
                remove();
                break;

            case KeyEvent.VK_INSERT:
                if (e.isControlDown()) {
                    addChildWithPrompt();
                } else  {
                    addChild();
                }
                break;

            case KeyEvent.VK_ENTER:
                if (! e.isShiftDown()) {
                    if (e.isControlDown()) {
                        addSiblingWithPrompt();
                    } else {
                        addSibling();
                    }
                }
                break;

            case KeyEvent.VK_Z:
                undo();
                break;
            case KeyEvent.VK_Y:
                redo();
                break;

            case KeyEvent.VK_S:
                save();
                break;

            case KeyEvent.VK_C:
                copySubTree();
                break;
            case KeyEvent.VK_V:
                pasteAsSubTree();
                break;
            case KeyEvent.VK_L:
                linkMarkedVertexToCursor();
                break;

            case KeyEvent.VK_KP_UP:
            case KeyEvent.VK_UP:
                if (e.isControlDown()) {
                    moveUp();
                }
                break;
            case KeyEvent.VK_KP_DOWN:
            case KeyEvent.VK_DOWN:
                if (e.isControlDown()) {
                    moveDown();
                }
                break;
            case KeyEvent.VK_KP_LEFT:
            case KeyEvent.VK_LEFT:
                if (e.isControlDown()) {
                    moveLeft();
                }
                break;
            case KeyEvent.VK_KP_RIGHT:
            case KeyEvent.VK_RIGHT:
                if (e.isControlDown()) {
                    moveRight();
                }
                break;
        }
    }

    static public BoundaryNodes getBoundaryNodes(final List<Node> nodes)
    {
        if (nodes.size() == 0) {
            return null;
        }

        Tree tree = (Tree)nodes.get(0).getGraph();

        Node root = tree.getRoot();

        final BoundaryNodes boundaryNodes = new BoundaryNodes();

        Tree.DepthFirstReverseTraverseProcessor leftRightTopFinder = new Tree.DepthFirstReverseTraverseProcessor() {
            int minLevel = Integer.MAX_VALUE;
            int maxLevel = Integer.MIN_VALUE;

            @Override
            public void run(Node parent, Node node, int level) {
                if (nodes.contains(node)) {
                    if (level < minLevel) {
                        minLevel = level;
                        boundaryNodes.left = node;
                    } else if (maxLevel < level) {
                        maxLevel = level;
                        boundaryNodes.right  = node;
                    }

                    if (boundaryNodes.top == null) {
                        boundaryNodes.top = node;
                    }
                }
            }
        };
        tree.depthFirstReverseTraverse(root, leftRightTopFinder, Tree.ChildTraverseOrder.OLDER_FIRST);

        Tree.DepthFirstReverseTraverseProcessor bottomFinder = new Tree.DepthFirstReverseTraverseProcessor() {
            @Override
            public void run(Node parent, Node node, int level) {
                if (nodes.contains(node)) {
                    if (boundaryNodes.bottom == null) {
                        boundaryNodes.bottom = node;
                    }
                }
            }
        };
        tree.depthFirstReverseTraverse(root, bottomFinder, Tree.ChildTraverseOrder.YOUNGER_FIRST);

        return boundaryNodes;
    }


    public static class BoundaryNodes {
        public Node top;
        public Node bottom;
        public Node left;
        public Node right;
    }

    private JMenuItem createMenuItemForOpeningInNewView(Object vertexDbId, boolean hasParentInfo, String displayText)
    {
        final MindModel.VertexBasicInfo vertexBasicInfo = m_mindModel.getVertexBasicInfo(vertexDbId);
        if (displayText == null) {
            if (hasParentInfo)
                displayText = (vertexBasicInfo.m_contextText);
            else
                displayText = (vertexBasicInfo.m_text);
        }

        JMenuItem menuItem = new JMenuItem(displayText);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                m_mindController.findOrAddMindView(vertexBasicInfo.m_dbId);
            }
        });

        return menuItem;
    }

    public JMenuItem createMenuItemForOpeningInNewView(Object vertexDbId, boolean hasParentInfo)
    {
        return createMenuItemForOpeningInNewView(vertexDbId, hasParentInfo, null);
    }

    public void addSubMenuForOpenAncestors(JMenu menu, boolean includeCursor)
    {
        if (isChanging()) {
            return;
        }

        Node node = getCursorSourceNode();
        Object currentVertexId = m_mindModel.getDbId(node);

        List ancestorAndSelf = m_mindModel.m_mindDb.getInheritPath(currentVertexId);
        if (includeCursor) {
            ancestorAndSelf.add(currentVertexId);
        }

        Collections.reverse(ancestorAndSelf);

        for (Object ancestor : ancestorAndSelf) {
            menu.add(createMenuItemForOpeningInNewView(ancestor, false));
        }
    }

    public JMenuItem createStyleMenuItem(final String styleName) {

        String family = Style.getFontFamilySurely(styleName);
        Integer size = Style.getFontSizeSurely(styleName);

        boolean bold = Style.getBoldSurely(styleName);
        boolean italic = Style.getItalicSurely(styleName);

        Integer textColorValue = Style.getTextColorSurely(styleName);
        Integer nodeColorValue = Style.getNodeColorSurely(styleName);

        String icon = Style.getIconSurely(styleName);

        Font font = FontLib.getFont(family, bold, italic, size);

        JMenuItem menuItem = new JMenuItem(styleName);

        menuItem.setFont(font);
        menuItem.setForeground(ColorLib.getColor(textColorValue));
        menuItem.setBackground(ColorLib.getColor(nodeColorValue));
        menuItem.setOpaque(true);

        if (icon != null) {
            menuItem.setIcon(Style.getImageIcon(icon));
        }

        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setCursorProperty(MindModel.STYLE_PROP_NAME, styleName);
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        return menuItem;
    }

    JPopupMenu m_popupMenu = new JPopupMenu();

    public void openContextMenu(int x, int y) {
        if (isChanging()) {
            return;
        }

        m_popupMenu.removeAll();

        Node node = getCursorSourceNode();
        Object currentVertexId = m_mindModel.getDbId(node);
        m_popupMenu.add(createMenuItemForOpeningInNewView(currentVertexId, false, "Open in new view"));

        JMenu ancestorSubMenu = new JMenu("ancestors");
        addSubMenuForOpenAncestors(ancestorSubMenu, false);
        m_popupMenu.add(ancestorSubMenu);

        JMenu styleSubMenu = new JMenu("styles");
        for (String styleName : Style.getStyleNames()) {
            styleSubMenu.add(createStyleMenuItem(styleName));
        }

        m_popupMenu.add(styleSubMenu);
        m_popupMenu.show(this, x, y);
    }

    public Object getRootDbId() {
        return MindModel.getDbId(m_tree.getRoot());
    }

    private void setDragAndDrop() {

        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK,
            new DragGestureListener() {
                public void dragGestureRecognized(DragGestureEvent dragGestureEvent) {

                    Point dragOrigin = dragGestureEvent.getDragOrigin();
                    VisualItem item = findItem(dragOrigin);
                    //不同拖动一个子树的根，到另一棵子树上 （因为子树所在位置显示上未知）
                    if (item != null && item instanceof NodeItem && !isEditing() && ((NodeItem) item).getParent() != null) {
                        m_mindController.m_dndSourceMindView = MindView.this;
                        dragGestureEvent.startDrag(null, new MindTransferable(MindView.this));
                    }
                }
            }
        );

        setDropTarget(new MindDropTarget());
    }

    static class DndData {
        final public String m_textForOuterCopying;
        final public ArrayList<Tree> m_treesForInnerCopying;
        final public ArrayList<Object> m_vertexIdsForLinking;
        final public ArrayList<Object> m_edgeIdsForDragging;

        DndData(MindView mindView) {
            m_textForOuterCopying = mindView.getSelectedSubTreesText();
            m_treesForInnerCopying = mindView.getSelectedSubTrees();
            m_vertexIdsForLinking = mindView.getDBIdsOfSelectedNodes();
            m_edgeIdsForDragging = mindView.getInEdgeDBIdsOfSelectedNodes();
        }
    }

    NodeItem m_dndDargOverNode;
    NodeControl.HitPosition m_dndHitPosition;

    static class MindTransferable implements Transferable {

        public static final DataFlavor MIND_DATA_FLAVOR = new DataFlavor(DndData.class, DataFlavor.javaJVMLocalObjectMimeType);

        DndData m_data;

        MindTransferable(MindView mindView) {
            m_data = new DndData(mindView);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.stringFlavor, MIND_DATA_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor == DataFlavor.stringFlavor || flavor == MIND_DATA_FLAVOR;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (flavor == DataFlavor.stringFlavor) {
                return m_data.m_textForOuterCopying;
            } else if (flavor == MIND_DATA_FLAVOR) {
                return m_data;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }

    void dndHitTest(Point point)
    {
        NodeItem dragOverNode = null;
        NodeControl.HitPosition hitPosition = null;

        if (point != null)  {
            Object hitInfo[] = NodeControl.hitTest(MindView.this, point);
            dragOverNode = (NodeItem)hitInfo[0];
            hitPosition = (NodeControl.HitPosition)hitInfo[1];
        }

        m_dndDargOverNode = dragOverNode;
        m_dndHitPosition = hitPosition;
    }

    void dndHitTestAndUpdateDisplay(Point point)
    {
        NodeItem dragOverNode = null;
        NodeControl.HitPosition hitPosition = null;

        if (point != null)  {
            Object hitInfo[] = NodeControl.hitTest(MindView.this, point);
            dragOverNode = (NodeItem)hitInfo[0];
            hitPosition = (NodeControl.HitPosition)hitInfo[1];
        }

        if (dragOverNode != m_dndDargOverNode || hitPosition != m_dndHitPosition) {
            renderTree();
            m_dndDargOverNode = dragOverNode;
            m_dndHitPosition = hitPosition;
        }
    }

    private final class MindDropTarget extends DropTarget {

        boolean canAccept(DropTargetDragEvent e) {
            Transferable transfer = e.getTransferable();
            return transfer.isDataFlavorSupported(MindTransferable.MIND_DATA_FLAVOR) ||
                    transfer.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public void dragEnter(DropTargetDragEvent e) {
            if (! canAccept(e)) {
                e.rejectDrag();
                return;
            }

            dndHitTestAndUpdateDisplay(e.getLocation());
        }

        @Override
        public void dragOver(DropTargetDragEvent e) {
            if (! canAccept(e)) {
                e.rejectDrag();
                return;
            }

            dndHitTestAndUpdateDisplay(e.getLocation());
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
            //do nothing
            dndHitTestAndUpdateDisplay(null);
        }

        @Override
        public void drop(DropTargetDropEvent e) {

            dndHitTest(e.getLocation());

            if (m_dndDargOverNode == null || m_dndHitPosition == NodeControl.HitPosition.OUTSIDE) {
                dndHitTestAndUpdateDisplay(null);
                return;
            }

            Transferable transfer = e.getTransferable();
            DndData dndData = null;
            String stringData = null;

            try {
                dndData = (DndData)transfer.getTransferData(MindTransferable.MIND_DATA_FLAVOR);
            } catch (UnsupportedFlavorException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
                dndHitTestAndUpdateDisplay(null);
                return;
            }

            try {
                stringData = (String)transfer.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
                dndHitTestAndUpdateDisplay(null);
                return;
            }

            Node droppedNode = toSource(m_dndDargOverNode);

            if (dndData != null) {

                List<MindOperator> operators = null;
                if (e.getDropAction() == DnDConstants.ACTION_LINK || e.getDropAction() == DnDConstants.ACTION_MOVE) {
                    if (m_mindController.m_dndSourceMindView == MindView.this) {
                        operators = getDragOperators(droppedNode,
                                m_dndHitPosition, e.getDropAction() == DnDConstants.ACTION_LINK);
                    } else {
                        m_cursor.selectOnlyOneNodeItem(m_dndDargOverNode);
                        operators = getExternalDragOperators(droppedNode,
                                m_dndHitPosition, e.getDropAction() == DnDConstants.ACTION_LINK, dndData);

                    }

                } else {
                    operators = new ArrayList<>();

                    for (Tree copiedTree : dndData.m_treesForInnerCopying) {
                        MindOperator operator = new PastingExternalTree(m_mindModel, droppedNode, copiedTree);
                        operators.add(operator);
                    }

                }

                m_dndDargOverNode = null;
                m_dndHitPosition = null;

                if (operators != null && operators.size() > 0) {
                    m_mindController.does(operators);
                } else {
                    renderTreeToEndChanging();
                }

                e.dropComplete(true);

            } else if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                MindOperator operator = new ImportingFile(m_mindModel, droppedNode, stringData);
                m_mindController.does(operator);
                e.dropComplete(true);

            } else {
                dndHitTestAndUpdateDisplay(null);
                e.dropComplete(false);
            }
        }

    }

    public void renderTreeAfterCursorChanging(final NodeItem newCursorNode)
    {
        //activity是被一个定时器调度的，不是立即执行的。renderTree仅仅是加入到调度器中，所以panToExposeItem不能直接调用。
        renderTree(new Runnable() {
            @Override
            public void run() {
                panToExposeItem(newCursorNode);
            }
        });

        m_mindController.updateAllMindViews();
        m_mindController.updateMindPropertyComponents(newCursorNode);
        if (! isPlaceholder(newCursorNode)) {
            m_mindController.setNodeInfoLabelText(m_mindModel.getVertexDbIdInheritInfo(MindModel.getDbId(newCursorNode)));
        }
    }

} // end of class TreeMap
