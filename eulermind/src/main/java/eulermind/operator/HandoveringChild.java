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

public class HandoveringChild extends MindOperator{

    ArrayList<Integer> m_oldParentPath;
    int m_oldPos;
    ArrayList<Integer> m_newParentPath;
    int m_newPos;

    ArrayList<Integer> m_oldParentPathAfterDoing;
    ArrayList<Integer> m_newParentPathAfterDoing;
    public HandoveringChild(MindModel mindModel, Node formerCursor, Node newParent, int newPos)
    {
        super(mindModel, formerCursor);

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newParent", newParent);
        m_logger.info("arg: {}: {}", "newPos", newPos);


        m_oldParentPath = getNodePath(formerCursor.getParent());
        m_oldPos = formerCursor.getIndex();

        m_newParentPath = getNodePath(newParent);
        m_newPos = newPos;

        m_laterCursorPath = (ArrayList<Integer>)m_newParentPath.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");

        Node oldParentNode = getNodeByPath(m_oldParentPath);
        Node newParentNode = getNodeByPath(m_newParentPath);

        handoverChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);

        m_oldParentPathAfterDoing = getNodePath(oldParentNode);
        m_newParentPathAfterDoing = getNodePath(newParentNode);

        m_laterCursorPath = (ArrayList) m_newParentPathAfterDoing.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");
        handoverChild(m_newParentPathAfterDoing, m_newPos,
                m_oldParentPathAfterDoing, m_oldPos);
        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg: ");
        handoverChild(m_oldParentPath, m_oldPos, m_newParentPath, m_newPos);
        m_logger.info("ret: ");
    }

    private void handoverChild(ArrayList<Integer> oldParentPath, int oldPos, ArrayList<Integer> newParentPath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldParentPath", oldParentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newParentPath", newParentPath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDBId);
        Node oldParentNode = m_mindModel.getNodeByPath(tree, oldParentPath);
        Node newParentNode = m_mindModel.getNodeByPath(tree, newParentPath);

        Node child = oldParentNode.getChild(oldPos);

        assert ! m_mindModel.isSelfInDB(child, newParentNode) && ! m_mindModel.isDescendantInDB(child, newParentNode);

        assert !MindModel.getDBId(oldParentNode).equals(MindModel.getDBId(newParentNode));
        assert !m_mindModel.isRefNode(child);

        m_mindModel.handoverChild(oldParentNode, oldPos, newParentNode, newPos);

        m_logger.info("ret:");
    }
}
