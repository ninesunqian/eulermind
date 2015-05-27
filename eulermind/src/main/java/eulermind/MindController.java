package eulermind;

import eulermind.component.ButtonTabComponent;
import eulermind.component.MindPropertyComponent;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
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

    public Object m_toBeLinkedDbId;

    public Tree m_copiedSubTree;

    //用于记录eulermind生成的剪切板数据。
    //如果当前剪切板数据与m_clipboardTextFromHere不一致时，说明用户从其他地方复制了信息，粘贴时不能用m_copiedSubTree
    public String m_clipboardTextFormHere;

    MindController(MindModel mindModel, JTabbedPane tabbedPane) {
        super();
        m_mindModel = mindModel;
        m_tabbedPane = tabbedPane;

        m_tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (m_tabbedPane.getSelectedComponent() != null) {
                    Component comp = m_tabbedPane.getSelectedComponent();
                    comp.requestFocusInWindow();
                }
            }
        });

        ArrayList<Object> lastOpenedRootId = m_mindModel.getLastOpenedRootId();

        if (lastOpenedRootId.size() > 0) {
            for (Object rootId : lastOpenedRootId) {
                if (! m_mindModel.isVertexTrashed(rootId)) {
                    findOrAddMindView(rootId);
                }
            }
        } else {
            findOrAddMindView(m_mindModel.m_mindDb.getRootId());
        }

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
            m_tabbedPane.setSelectedComponent(mindView);
            return mindView;
        }

        mindView = new MindKeyView(m_mindModel, this, rootDBId);

        m_mindViews.put(rootDBId, mindView);
        Node root = mindView.m_tree.getRoot();

        m_tabbedPane.addTab(m_mindModel.getText(root), mindView);

        ButtonTabComponent buttonTabComponent = new ButtonTabComponent(m_tabbedPane);
        buttonTabComponent.getButton().addActionListener(m_tabCloseButtonListener);
        m_tabbedPane.setTabComponentAt(m_tabbedPane.getTabCount() - 1, buttonTabComponent);

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

        m_tabbedPane.setSelectedComponent(mindView);

        return  mindView;
    }

    ActionListener m_tabCloseButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ButtonTabComponent buttonTabComponent = ((ButtonTabComponent.TabButton)e.getSource()).getButtonTabComponent();
            int pos = m_tabbedPane.indexOfTabComponent(buttonTabComponent);
            MindView removedMindView = (MindView)m_tabbedPane.getComponentAt(pos);

            if (pos != -1) {
                for (Object key: m_mindViews.keySet()) {
                    if (m_mindViews.get(key) == removedMindView) {
                        removeMindView(key);
                    }
                }
            }
        }
    };

    public void removeMindView(Object rootDBId) {
        MindView mindView = m_mindViews.get(rootDBId);
        m_tabbedPane.remove(mindView);
        m_mindViews.remove(rootDBId);
        m_mindModel.removeTree(rootDBId);
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

    public void updateAllMindViews() {
        ArrayList<MindView> mindViews = new ArrayList<MindView>();
        for(int i=0; i<m_tabbedPane.getTabCount(); i++) {
            ((MindView)m_tabbedPane.getComponentAt(i)).renderTree();
        }
    }

    public MindView getCurrentView() {
        return (MindView)m_tabbedPane.getSelectedComponent();
    }

    public boolean isChanging() {
        MindView currentView = getCurrentView();
        if (currentView == null) {
            return false;
        }
        return currentView.isChanging();


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

            HashSet<Object> toBeRemovedMindViewIds = new HashSet<>();

            for (Object rootDBId : m_mindViews.keySet()) {
                if (m_mindModel.m_mindDb.isVertexIdDescendant(trashedDBId, rootDBId)) {
                    toBeRemovedMindViewIds.add(rootDBId);
                }
            }

            for (Object toBeRemovedMindViewId : toBeRemovedMindViewIds) {
                removeMindView(toBeRemovedMindViewId);
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

    HashSet<MindPropertyComponent> m_mindPropertyComponents = new HashSet<>();

    boolean m_settingNodePropertyEnabled = true;
    PropertyChangeListener m_listenerForSettingNodeProperty = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            String eventPropertyName = evt.getPropertyName();
            if (! eventPropertyName.startsWith(MindPropertyComponent.MIND_PROPERTY_PREFIX)) {
                return;
            }

            if (! m_settingNodePropertyEnabled) {
                return;
            }

            String propertyName = eventPropertyName.substring(MindPropertyComponent.MIND_PROPERTY_PREFIX.length());

            getCurrentView().setCursorProperty(propertyName, evt.getNewValue());
        }
    };

    public void addMindPropertyComponent(String propertyName, MindPropertyComponent mindPropertyComponent) {
        String propertyNameInComponent = MindPropertyComponent.MIND_PROPERTY_PREFIX + propertyName;
        mindPropertyComponent.setMindPropertyName(propertyNameInComponent);
        mindPropertyComponent.addPropertyChangeListener(propertyNameInComponent, m_listenerForSettingNodeProperty);
        m_mindPropertyComponents.add(mindPropertyComponent);
    }

    public void removeMindPropertyComponent(String propertyName, MindPropertyComponent mindPropertyComponent) {
        String propertyNameInComponent = MindPropertyComponent.MIND_PROPERTY_PREFIX + propertyName;
        mindPropertyComponent.setMindPropertyName(null);
        mindPropertyComponent.removePropertyChangeListener(propertyNameInComponent, m_listenerForSettingNodeProperty);
        m_mindPropertyComponents.remove(mindPropertyComponent);
    }

    public void updateMindPropertyComponents(Node node)
    {
        m_settingNodePropertyEnabled = false;
        for (String propertyName : MindModel.sm_nodePropNames) {
            Object propertyValue = node.get(propertyName);

            String propertyNameInComponent = MindPropertyComponent.MIND_PROPERTY_PREFIX + propertyName;

            for (MindPropertyComponent component : m_mindPropertyComponents) {
                if (component.getMindPropertyName().equals(propertyNameInComponent)) {
                    component.setMindPropertyValue(propertyValue);
                }
            }
        }
        m_settingNodePropertyEnabled = true;
    }
}
