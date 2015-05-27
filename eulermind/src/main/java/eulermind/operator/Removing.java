package eulermind.operator;

import eulermind.MindModel;
import eulermind.MindOperator;
import prefuse.data.Node;
import prefuse.data.Tree;

import java.util.ArrayList;

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
    public Object m_parentDBId;
    public int m_siblingPos;
    public Object m_removedDBId;

    public boolean m_isRefRelation;

    public Removing(MindModel mindModel, Node formerCursor)
    {
        super(mindModel, formerCursor);
        Node parent = formerCursor.getParent();
        m_parentDBId = m_mindModel.getDbId(parent);

        m_siblingPos = formerCursor.getIndex();
        m_removedDBId = mindModel.getDbId(formerCursor);

        computeLaterCursor(formerCursor);

        m_isRefRelation = m_mindModel.isRefNode(formerCursor);
    }

    public void does()
    {
        if (m_isRefRelation) {
            m_mindModel.removeReference(m_parentDBId, m_siblingPos);
        } else {
            m_mindModel.trashNode(m_parentDBId, m_siblingPos);
        }
    }

    public void undo()
    {
        ArrayList parentPath = (ArrayList)m_formerCursorPath.clone();
        parentPath.remove(parentPath.size() - 1);

        Node parentNode = getNodeByPath(parentPath);
        if (m_isRefRelation) {
            m_mindModel.addReference(parentNode, m_siblingPos, m_removedDBId);
        } else {
            m_mindModel.restoreNodeFromTrash(parentNode, m_removedDBId);
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
        formerCursor = parent.getChild(m_siblingPos);

        m_formerCursorPath = getNodePath(formerCursor);

        if (m_isRefRelation) {
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
