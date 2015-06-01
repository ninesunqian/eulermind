package eulermind.operator;

import eulermind.MindDB;
import eulermind.MindModel;
import eulermind.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

import java.util.ArrayList;
import java.util.List;

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

public class Removing extends MindOperator {

    public Removing(MindModel mindModel, Node formerCursor)
    {
        super(mindModel, formerCursor);
        computeLaterCursor(formerCursor);
    }

    public void does()
    {
        if (m_isRefNode) {
            MindDB mindDb = m_mindModel.m_mindDb;
            List<MindDB.EdgeVertexId> outEdgeVetexIds = mindDb.getOutEdgeVertexIds(mindDb.getVertex(m_formerCursorParentId));
            m_mindModel.removeReference(outEdgeVetexIds.get(m_formerCursorPos).m_edgeId);
        } else {
            m_mindModel.trashNode(m_formerCursorId);
        }
    }

    public void undo()
    {
        ArrayList parentPath = (ArrayList)m_formerCursorPath.clone();
        parentPath.remove(parentPath.size() - 1);

        Node parentNode = getNodeByPath(parentPath);
        if (m_isRefNode) {
            m_mindModel.addReference(parentNode, m_formerCursorPos, m_formerCursorId);
        } else {
            m_mindModel.restoreNodeFromTrash(m_formerCursorId);
        }
    }

    public void redo()
    {
        does();
    }

    public static boolean canDo(MindModel mindModel, Tree tree, Node node)
    {
        Node root = tree.getRoot();

        if (node == root) {
            return false;
        }

        if (mindModel.isRefNode(node)) {
            return true;
        } else {
            if (mindModel.subTreeContainsInDB(node, root)) {
                return false;
            } else {
                return true;
            }
        }
    }

    private Node getNearestKeptSibling(Node node)
    {
        int start = node.getIndex();
        Node parent = node.getParent();

        //firstly to right
        for (int i=start+1; i<parent.getChildCount(); i++) {
            Node tmp = parent.getChild(i);
            if (!m_mindModel.subTreeContainsInDB(node, tmp)) {
                return tmp;
            }
        }

        //then to left
        for (int i=start-1; i>=0; i--) {
            Node tmp = parent.getChild(i);
            if (!m_mindModel.subTreeContainsInDB(node, tmp)) {
                return tmp;
            }
        }

        return null;
    }

    private Node topSameDBNode(Tree tree, Node node)
    {
        Node topNode = node;

        Node tmpNode = node;
        Node root = tree.getRoot();

        while (tmpNode != null)
        {
            if (m_mindModel.isSelfInDB(tmpNode, node)) {
                topNode = tmpNode;
            }
            tmpNode = tmpNode.getParent();
        }

        return topNode;
    }

    private void computeLaterCursor(Node formerCursor)
    {
        Tree tree = (Tree)formerCursor.getGraph();
        Node parent = formerCursor.getParent();

        //change formerCursor to its avatar nearest to root
        parent = topSameDBNode(tree, parent);
        formerCursor = parent.getChild(m_formerCursorPos);

        m_formerCursorPath = getNodePath(formerCursor);

        if (m_isRefNode) {
            if (parent.getChildCount()  == 1) {
                m_laterCursorPath = getNodePath(parent);

            } else {
                if (formerCursor.getIndex() == parent.getChildCount() - 1) {
                    m_laterCursorPath = getNodePath(formerCursor);
                    m_laterCursorPath.set(0, formerCursor.getIndex() - 1);
                } else {
                    //formerCursor's older sibling move into this position
                    m_laterCursorPath = getNodePath(formerCursor);
                }
            }

        } else {

            Node nearestKeptSibling =  getNearestKeptSibling(formerCursor);

            if (nearestKeptSibling == null) {
                m_laterCursorPath = getNodePath(parent);

            } else {
                ArrayList<Node> keptSiblings = new ArrayList<Node>();

                for (int i=0; i<parent.getChildCount(); i++) {
                    Node sibling = parent.getChild(i);
                    if (! m_mindModel.subTreeContainsInDB(formerCursor, sibling)) {
                        keptSiblings.add(parent.getChild(i));
                    }
                }

                m_laterCursorPath = getNodePath(formerCursor);
                m_laterCursorPath.set(m_laterCursorPath.size() - 1, keptSiblings.indexOf(nearestKeptSibling));
            }
        }
    }
}
