package infinitmindmap.operator;

import infinitmindmap.MindModel;
import infinitmindmap.MindOperator;
import prefuse.data.Node;

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

public class ImportingFile extends MindOperator{
    String m_importedFilePath;
    List m_newChildren;

    ArrayList<Integer> m_parentPath;
    ArrayList<Integer> m_parentPathAfterDoing;

    public ImportingFile(MindModel mindModel, Node formerCursor,  String path) {
        super(mindModel, formerCursor);
        m_importedFilePath = path;
        m_parentPath = getNodePath(formerCursor);
    }

    public void does() throws Exception
    {
        Node parent = getNodeByPath(m_parentPath);
        m_newChildren = m_mindModel.importFile(parent, m_importedFilePath);

        //importFIle之后，parent的路径可能会改变，所以要重新取一次路径
        m_parentPathAfterDoing = getNodePath(parent);

        m_laterCursorPath = (ArrayList)m_parentPathAfterDoing.clone();
        m_laterCursorPath.add(parent.getChildCount() - 1);
    }

    public void undo() {
        Node parent = getNodeByPath(m_parentPathAfterDoing);
        for (int i=0; i<m_newChildren.size(); i++) {
            m_mindModel.trashNode(MindModel.getDBId(parent), parent.getChildCount() + i);
        }
    }

    public void redo() {
        for (int i=0; i<m_newChildren.size(); i++) {
            m_mindModel.restoreNodeFromTrash(getNodeByPath(m_formerCursorPath), m_newChildren.get(i));
        }
    }
}
