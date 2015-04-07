package eulermind.view;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;

import eulermind.MindModel;
import prefuse.Visualization;
import prefuse.data.*;

import prefuse.util.PrefuseLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualTree;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

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
public class TreeFolder extends NodeControl {

    private LinkedHashSet<Integer> m_foldedNodes = new LinkedHashSet<Integer>();
    VisualTree m_tree;
    MindView m_mindView;

    TreeFolder(MindView mindView)
    {
        super(mindView);
        m_mindView = mindView;
        m_tree = mindView.m_visualTree;
    }

    void unfoldNode(NodeItem node)
    {
        if (! m_mindView.m_mindModel.childrenAttached(m_mindView.toSource(node))) {
            m_mindView.m_mindModel.attachChildren(m_mindView.toSource(node));
        }

        if (node.getChildCount() == 0 || node.isExpanded()) {
            return;
        }

        assert (m_foldedNodes.contains(node.getRow()));

        m_foldedNodes.remove(node.getRow());

        final Visualization vis = node.getVisualization();
        final Node unfoldTreeRoot = node;
        final String group = node.getGroup();

        //unfold descendants deeply, to the folded descendants
        m_tree.deepTraverse(node, new Tree.TraverseProcessor() {
            @Override
            public boolean run(Node parent, Node node, int level) {

                if (node == unfoldTreeRoot) {
                    return true;
                }

                TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                //s_logger.info ( "visiableNode " + m_mindTree.getText(node));
                PrefuseLib.updateVisible(visualNode, true);
                PrefuseLib.updateVisible(visualEdge, true);

                if (m_foldedNodes.contains(node.getRow())) {
                    return false;
                } else {
                    return true;
                }
            }
        });

        node.setExpanded(true);
    }

    public void foldNode(NodeItem node)
    {
        final Visualization vis = node.getVisualization();
        final String group = node.getGroup();

        if (!node.isExpanded())
        {
            return;
        }

        m_foldedNodes.add(node.getRow());

        final Node foldTreeRoot = node;

        //set descendants unvisible deeply, to the folded descendants
        m_tree.deepTraverse(node, new Tree.TraverseProcessor() {
            public boolean run(Node parent, Node node, int level) {
                if (node == foldTreeRoot)
                {
                    return true;
                }

                TableNodeItem visualNode = (TableNodeItem)vis.getVisualItem(group, node);
                TableEdgeItem visualEdge = (TableEdgeItem)visualNode.getParentEdge();

                PrefuseLib.updateVisible(visualNode, false);
                PrefuseLib.updateVisible(visualEdge, false);

                //s_logger.info ( "invisiableNode " + text);
                if (m_foldedNodes.contains(node.getRow())) {
                    return false;
                } else {
                    return true;
                }
            }
        });


        node.setExpanded(false);
    }

    public boolean isFolded(NodeItem node)
    {
        if (node.getChildCount() > 0) {
            return ! node.isExpanded();
        } else {
            return m_mindView.m_mindModel.getDBChildCount(node) > 0;
        }
    }

    public void toggleFoldNode(NodeItem node)
    {
        if (m_mindView.getCursorNodeItem() != node) {
            return;
        }

        if (! m_mindView.beginChanging()) {
            return;
        }

        if (isFolded(node)) {
            unfoldNode(node);
        }
        else {
            foldNode(node);
        }

        m_mindView.renderTreeToEndChanging();
    }

    @Override
    public void nodeItemClicked(NodeItem item, MouseEvent e) {
        toggleFoldNode(item);
    }

    @Override
    public void nodeItemKeyPressed(NodeItem item, KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            toggleFoldNode(item);
        }
    }
}
