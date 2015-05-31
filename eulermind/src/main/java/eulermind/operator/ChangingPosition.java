package eulermind.operator;

import eulermind.MindModel;
import eulermind.MindOperator;
import prefuse.data.Edge;
import prefuse.data.Node;

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

public class ChangingPosition extends MindOperator{

    int m_oldPos;
    int m_newPos;
    ArrayList<Integer> m_parentPath;

    ArrayList<Integer> m_parentPathAfterDoing;

    public ChangingPosition(MindModel mindModel, Node formerCursor, int newPos)
    {
        super(mindModel, formerCursor);

        m_logger.info("arg: {}: {}", "mindModel", mindModel);
        m_logger.info("arg: {}: {}", "formerCursor", formerCursor);
        m_logger.info("arg: {}: {}", "newPos", newPos);

        m_oldPos = formerCursor.getIndex();
        m_newPos = newPos;

        m_parentPath = getNodePath(formerCursor.getParent());

        m_logger.info("ret:");
    }

    public void does()
    {
        m_logger.info("arg:");

        Node parent = getNodeByPath(m_parentPath);
        changePosition(m_parentPath, m_oldPos, m_newPos);

        m_parentPathAfterDoing = getNodePath(parent);

        //在引用父节点的情况下， 在显示树中改变一个节点的位置，有可能改变父节点，以及父节点的父节点的位置..
        //所以要重新获取一下路径
        m_laterCursorPath = (ArrayList)m_parentPathAfterDoing.clone();
        m_laterCursorPath.add(m_newPos);
        m_logger.info("ret:");
    }

    public void undo()
    {
        m_logger.info("arg:");

        changePosition(m_parentPathAfterDoing, m_newPos, m_oldPos);

        m_logger.info("ret:");
    }

    public void redo()
    {
        m_logger.info("arg: ");
        changePosition(m_parentPath, m_oldPos, m_newPos);
        m_logger.info("ret: ");
    }

    private void changePosition(ArrayList<Integer> parentPath, int oldPos, int newPos)
    {
        m_logger.info("arg: {}:{}", "parentPath", parentPath);
        m_logger.info("arg: {}:{}", "oldPos", oldPos);
        m_logger.info("arg: {}:{}", "newPos", newPos);

        if (oldPos == newPos) {
            return;
        }

        Node parentNode = getNodeByPath(parentPath);
        Edge childEdge = parentNode.getChildEdge(oldPos);
        m_mindModel.changeChildPos(m_mindModel.getDbId(childEdge), newPos);

        m_logger.info("ret:");
    }
}
