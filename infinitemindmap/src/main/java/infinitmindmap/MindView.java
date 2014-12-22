package infinitmindmap;

import java.awt.event.*;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import infinitmindmap.operator.*;
import prefuse.Display;
import prefuse.Visualization;

import prefuse.controls.*;
import prefuse.data.*;
import prefuse.util.PrefuseLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
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

    TreeCursor m_cursor;
    Node m_savedCursor = null;

    TreeFolder m_folder;

    Tree m_tree;
	MindTreeRenderEngine m_renderEngine;

    MindEditor m_mindEditor;

    boolean m_isChanging = false;

    //提出这个函数是为了单元测试
    void endInserting(String text)
    {
        MindOperator operator = new AddingChild(m_mindModel, getCursorSourceNode().getParent(), getCursorSourceNode().getIndex(), text);
        m_logger.info("MindView fire OK, insert at {}", getCursorSourceNode().getIndex());
        removePlaceholderCursor();

        //该函数会重绘所有的MindView
        m_mindController.does(operator);
        hideEditor();

    }

    void endInserting(Object dbId, String text, Object parentDBId, String parentText)
    {
        MindOperator operator = new AddingReference(m_mindModel, getCursorSourceNode().getParent(),
                dbId, getCursorSourceNode().getIndex());
        removePlaceholderCursor();

        //该函数会重绘所有的MindView
        m_mindController.does(operator);
        hideEditor();

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

        public void promptListOk(Object dbId, String text, Object parentDBId, String parentText) {
            endInserting(dbId, text, parentDBId, parentText);
        }

        public void cancel() {
            cancelInserting();
        }
    };

    void endEditing(String text)
    {
        MindOperator operator;

        operator = new SettingProperty(m_mindModel, getCursorSourceNode(), MindModel.sm_textPropName, text);

        m_mindController.does(operator);
        hideEditor();

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

    protected FocusListener m_mindEditorFocusListener = new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                /*
                if (isEditing()) {
                    String text = getTextEditor().getText();
                    m_mindEditorListener.editorOk(text);
                }
                */
            }
    };

    VisualTree m_visualTree;

	public MindView(MindModel mindModel, MindController undoManager, Object rootId) {
		super(new Visualization());

        //s_logger.setLevel(Level.OFF);
		setSize(700, 600);
		setHighQuality(true);

        m_mindModel = mindModel;
        m_mindController = undoManager;

        m_tree = mindModel.findOrPutTree(rootId, 1);
        m_visualTree = (VisualTree)m_vis.add(m_treeGroupName, m_tree);
        MindModel.addNodeMirrorXYColumn(m_tree, m_visualTree);

        setItemSorter(new TreeDepthItemSorter());
        m_renderEngine = new MindTreeRenderEngine(this, m_treeGroupName);

        m_cursor = new TreeCursor(this);
        m_folder = new TreeFolder(this);

		setMouseControlListener();

        m_mindEditor = new MindEditor(m_mindModel.m_mindDb);
        m_mindEditor.setHasPromptList(true);

        m_mindEditor.setBorder(null);
        m_mindEditor.setVisible(false);

        setTextEditor(m_mindEditor);
        this.requestFocusInWindow();

        m_mindEditor.addFocusListener(m_mindEditorFocusListener);
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
        editText(toVisual(getCursorSourceNode()), MindModel.sm_textPropName) ;
    }

    private void hideEditor()
    {
        super.stopEditing2(false);
    }

    public void setCursorNodeByPath(ArrayList<Integer> path)
    {
        m_cursor.setCursorNodeItem(toVisual(m_mindModel.getNodeByPath(m_tree, path)));
    }

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
    }

    void beginAdding(boolean asChild)
    {
        if (! beginChanging()) {
            return;
        }

        if (getCursorSourceNode() == m_tree.getRoot()) {
            alert("you must open the root parent, before add a sibling of the root");
            endChanging();
            return;
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
                return;
            }
        }

        addPlaceholder(asChild);

        renderTree(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        m_logger.warn("++++++++++++++ show Editor");
                        //TODO
                        //listener 内部，调用endInstering (重绘，设置重绘后的hander是 恢复状态)
                        m_mindEditor.setMindEditorListener(m_editorListenerForInserting);
                        m_mindEditor.setHasPromptList(true);
                        showEditor();
                        m_logger.warn("--------------- after show Editor");
                    }
                });
            }
        });
    }

    void importFile()
    {
        beginChanging();
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        String path;

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            path = chooser.getSelectedFile().getPath();
            MindOperator operator = new ImportingFile(m_mindModel, getCursorSourceNode(), path);
            m_mindController.does(operator);
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

        //NOTE: newNode.setString(MindModel.sm_textPropName, "") error

        newNode.set(MindModel.sm_textPropName, "");

        m_cursor.setCursorNodeItem(toVisual(newNode));
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
        m_cursor.setCursorNodeItem(toVisual(m_savedCursor));
    }

    //include node and edge, the edge is used rendering
    public boolean isPlaceholder(Tuple tuple)
    {
        return (m_mindModel.getDBId(tuple) == null);
    }

    public NodeItem getCursorNodeItem()
    {
        return m_cursor.getCursorNodeItem();
    }

    public Node getCursorSourceNode()
    {
        if (m_cursor.getCursorNodeItem() == null) {
            int debug = 1;
        }
        return toSource(m_cursor.getCursorNodeItem());
    }

    public void setCursorProperty(String key, Object value)
    {
        //called by toolbar controls' action listener,
        beginChanging();

        Node cursorNode = getCursorSourceNode();
        MindOperator operator = new SettingProperty(m_mindModel, cursorNode, key, value);

        m_mindController.does(operator);
    }


    void undo() {
        beginChanging();
        if (m_mindController.canUndo()) {
            m_mindController.undo();
        } else {
            endChanging();
        }
    }

    void redo() {
        beginChanging();
        if (m_mindController.canRedo()) {
            m_mindController.redo();
        } else {
            endChanging();
        }
    }

    void save() {
        m_mindModel.m_mindDb.commit();
    }

    public void remove()
    {
        beginChanging();
        if (Removing.canDo(m_mindModel, m_tree, getCursorSourceNode())) {
            Node cursorNode = getCursorSourceNode();
            MindOperator operator = new Removing(m_mindModel, cursorNode);
            m_mindController.does(operator);
        } else {
            endChanging();
        }
    }

    public void addChild() {
        beginAdding(true);
    }

    public void addSibling() {
        beginAdding(false);
    }


    public void edit() {
        beginChanging();
        m_mindEditor.setMindEditorListener(m_editorListenerForEditing);
        m_mindEditor.setHasPromptList(false);
        showEditor();
    }

    public void cursorMoveUp() {
        m_cursor.moveUp();
    }

    public void cursorMoveDown() {
        m_cursor.moveDown();
    }

    public void cursorMoveLeft() {
        m_cursor.moveLeft();
    }
    public void cursorMoveRight() {
        m_cursor.moveRight();
    }

    public void toggleFoldNode() {
        m_folder.toggleFoldNode(m_cursor.getCursorNodeItem());
    }
} // end of class TreeMap
