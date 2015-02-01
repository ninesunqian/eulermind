package eulermind;

import eulermind.component.PropertyComponent;
import eulermind.component.PropertyComponentConnector;
import eulermind.operator.Removing;
import eulermind.view.MindKeyView;
import eulermind.view.MindView;
import eulermind.view.NodeControl;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.util.collections.IntIterator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Hashtable;

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

public class MindController extends UndoManager {
    Logger m_logger = LoggerFactory.getLogger(this.getClass());

    Hashtable<Object, MindView> m_mindViews = new Hashtable<Object, MindView>();

    MindModel m_mindModel;
    JTabbedPane m_tabbedPane;

    ArrayList<NodeControl> m_externalMouseContollers =
            new ArrayList<NodeControl>();

    MindController(MindModel mindModel, JTabbedPane tabbedPane) {
        super();
        m_mindModel = mindModel;
        m_tabbedPane = tabbedPane;

        m_tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Component comp = m_tabbedPane.getSelectedComponent();
                comp.requestFocusInWindow();
            }
        });

        //findOrAddMindView(m_mindModel.sm_mindDb.getRootId());
       Crawler.sm_mindDb = m_mindModel.m_mindDb;

        Crawler crawler = new Crawler();
        crawler.start();
        int rootDepth = crawler.m_depthParentIdMap.size() - 5;
        if (rootDepth < 0)
            rootDepth = 0;

        findOrAddMindView(crawler.m_depthParentIdMap.get(rootDepth));


        //防止切换tab时，焦点被切换到工具栏
        m_tabbedPane.setFocusCycleRoot(true);
    }

    void addExternalMouseController(NodeControl robustNodeItemController)
    {
        if (m_externalMouseContollers.contains(robustNodeItemController)) {
            return;
        }

        for (Object rootDBId : m_mindViews.keySet()) {
            m_mindViews.get(rootDBId).addControlListener(robustNodeItemController);
        }
    }

    public MindView findOrAddMindView(Object rootDBId) {
        MindView mindView = m_mindViews.get(rootDBId);
        if (mindView != null) {
            return mindView;
        }

        mindView = new MindKeyView(m_mindModel, this, rootDBId);

        m_mindViews.put(rootDBId, mindView);
        Node root = mindView.m_tree.getRoot();

        m_tabbedPane.addTab(m_mindModel.getText(root), mindView);

        for(NodeControl controller : m_externalMouseContollers) {
            mindView.addControlListener(controller);
        }

        int lastMindViewIndex = m_tabbedPane.getTabCount() - 1;
        if (lastMindViewIndex < 9) {
            m_tabbedPane.setMnemonicAt(lastMindViewIndex, KeyEvent.VK_1 + lastMindViewIndex);
        } else if (lastMindViewIndex == 9) {
            m_tabbedPane.setMnemonicAt(lastMindViewIndex, KeyEvent.VK_0);
        } else {
            //not setMnemonicAt
        }

        return  mindView;
    }

    public void removeMindView(Object rootDBId) {
        MindView mindView = m_mindViews.get(rootDBId);
        m_tabbedPane.remove(mindView);
        m_mindViews.remove(rootDBId);
    }

    public MindView exposeMindView(Object rootDBId) {
        MindView mindView = m_mindViews.get(rootDBId);
        if (mindView == null) {
            findOrAddMindView(rootDBId);
        }

        if (m_tabbedPane.getSelectedComponent() != mindView) {
            m_tabbedPane.setSelectedComponent(mindView);
        }
        return mindView;
    }

    public MindView getCurrentView() {
        return (MindView)m_tabbedPane.getSelectedComponent();
    }

    public Object getCurrentVertexId() {
        MindView currentView = getCurrentView();
        if (currentView == null) {
            return null;
        }

        Node node = currentView.getCursorSourceNode();
        return m_mindModel.getDbId(node);
    }

    String getTreeNodeRows(Tree tree)
    {
        Table nodeTable = tree.getNodeTable();
        IntIterator allRows = nodeTable.rows();

        String str = "[";

        while (allRows.hasNext()) {
             str += ((Integer)allRows.nextInt()).toString() + ", ";
        }

        str += "],  ";

        Table edgeTable = tree.getEdgeTable();
        allRows = edgeTable.rows();

        while (allRows.hasNext()) {
            str += ((Integer)allRows.nextInt()).toString() + ", ";
        }
        str += "]";

        return str;
    }

    private void updateMindViews(MindOperator operator, boolean isUndo)
    {
        //remove no needed mindview
        if (operator instanceof Removing) {
            Removing removing = (Removing)operator;
            Object trashedDBId = removing.m_removedDBId;

            for (Object rootDBId : m_mindViews.keySet()) {
                if (m_mindModel.m_mindDb.isVertexIdDescendant(trashedDBId, rootDBId)) {
                    removeMindView(rootDBId);
                }
            }
        }

        if (operator == null) {
            int i = 0;
        }

        MindView operatorBornView = exposeMindView(operator.m_rootDbId);

        //repaint remain mindviews
        for (Object rootDBId : m_mindViews.keySet()) {

            MindView mindView = m_mindViews.get(rootDBId);
            if (mindView == operatorBornView) {
                mindView.setCursorNodeByPath(isUndo ? operator.m_formerCursorPath : operator.m_laterCursorPath);
            } else {
                //not using mindView.getCursorSourceNode(), because if nodeItem is not valid,
                //the source node is can't be got
                if (! mindView.m_cursor.m_currentCursor.isValid()) {
                    ArrayList<Integer> rootPath = new ArrayList<Integer>();
                    mindView.setCursorNodeByPath(rootPath);
                }
            }

            mindView.renderTreeToEndChanging();
        }
    }

    public boolean does(UndoableEdit edit) {
        MindOperator operator = (MindOperator)edit;
        try {
            operator.does();
            updateMindViews(operator, false);

            m_logger.info("m_formerCursorPath: " + operator.m_formerCursorPath.toString());
            m_logger.info("m_laterCursorPath: " + operator.m_laterCursorPath.toString());

            return super.addEdit(edit);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage(), e.getMessage(), JOptionPane.ERROR_MESSAGE);
            m_logger.warn("operator exception" + e.getMessage());
        }

        return true;
    }


    public void redo()
    {
        if (! canRedo())
            return;

        MindOperator operator = (MindOperator)editToBeRedone();
        super.redo();

        updateMindViews(operator, false);
    }

    public void undo()
    {
        if (! canUndo())
            return;

        MindOperator operator = (MindOperator)editToBeUndone();
        super.undo();

        updateMindViews(operator, true);
    }

    ArrayList<PropertyComponentConnector> m_propertyComponentConnectors = new ArrayList<PropertyComponentConnector>();

    public void connectPropertyComponent(String propertyName, PropertyComponent propertyComponent) {
        if (propertyComponent == null) {
            int debug = 1;
        }
        PropertyComponentConnector connector = new PropertyComponentConnector(this, propertyComponent, propertyName);
        m_propertyComponentConnectors.add(connector);
        propertyComponent.setPropertyComponentConnector(connector);
    }

    public void disconnectPropertyComponent(PropertyComponent propertyComponent) {
        for (PropertyComponentConnector connector : m_propertyComponentConnectors) {
            if (connector.m_component == propertyComponent) {
                m_propertyComponentConnectors.remove(connector);
                connector.m_component.setPropertyComponentConnector(null);
            }
        }
    }

    private void updatePropertyComponent(String propertyName, Object propertyValue)
    {
        for (PropertyComponentConnector connector : m_propertyComponentConnectors) {
            if (connector.m_propertyName == propertyName) {
                connector.updateComponent(propertyValue);
            }
        }
    }

    public void updateNodePropertyComponent(Node node)
    {
        for (String property : MindModel.sm_nodePropNames) {
            updatePropertyComponent(property, node.get(property));
        }
    }
}
