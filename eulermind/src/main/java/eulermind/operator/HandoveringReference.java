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

public class HandoveringReference extends MindOperator{

    ArrayList<Integer> m_oldReferrerPath;
    int m_oldPos;
    ArrayList<Integer> m_newReferrerPath;
    int m_newPos;

    ArrayList<Integer> m_oldReferrerPathAfterDoing;
    ArrayList<Integer> m_newReferrerPathAfterDoing;

    public HandoveringReference(MindModel mindModel, Node formerCursor, Node newReferrer, int newPos)
    {
        super(mindModel, formerCursor);

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newReferrer", newReferrer);
        m_logger.info("arg: {}: {}", "newPos", newPos);

        m_oldReferrerPath = getNodePath(formerCursor.getParent());
        m_oldPos = formerCursor.getIndex();

        m_newReferrerPath = getNodePath(newReferrer);
        m_newPos = newPos;

        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");

        Node oldReferrerNode = getNodeByPath(m_oldReferrerPath);
        Node newReferrerNode = getNodeByPath(m_newReferrerPath);

        handoverReferent(m_oldReferrerPath, m_oldPos, m_newReferrerPath, m_newPos);

        m_oldReferrerPathAfterDoing = getNodePath(oldReferrerNode);
        m_newReferrerPathAfterDoing = getNodePath(newReferrerNode);

        m_laterCursorPath = (ArrayList) m_newReferrerPathAfterDoing.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");

        handoverReferent(m_newReferrerPathAfterDoing, m_newPos,
                m_oldReferrerPathAfterDoing, m_oldPos);
        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg: ");
        handoverReferent(m_oldReferrerPath, m_oldPos, m_newReferrerPath, m_newPos);
        m_logger.info("ret: ");
    }

    private void handoverReferent(ArrayList<Integer> oldReferrerPath, int oldPos, ArrayList<Integer> newReferrerPath, int newPos)
    {
        m_logger.info("arg: {}:{}", "oldReferrerPath", oldReferrerPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newReferrerPath", newReferrerPath);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        Tree tree = m_mindModel.findTree(m_rootDbId);
        Node oldReferrerNode = m_mindModel.getNodeByPath(tree, oldReferrerPath);
        Node newReferrerNode = m_mindModel.getNodeByPath(tree, newReferrerPath);

        Node child = oldReferrerNode.getChild(oldPos);

        assert !MindModel.getDbId(oldReferrerNode).equals(MindModel.getDbId(newReferrerNode));
        assert m_mindModel.isRefNode(child);

        m_mindModel.handoverReferent(oldReferrerNode, oldPos, newReferrerNode, newPos);

        m_logger.info("ret:");
    }
}
