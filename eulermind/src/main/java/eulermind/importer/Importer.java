package eulermind.importer;

import com.tinkerpop.blueprints.Vertex;
import eulermind.MindDB;
import eulermind.MindModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public abstract class Importer {

    private static Logger m_logger = LoggerFactory.getLogger(Importer.class);

    protected MindDB m_mindDb;

    protected Importer(MindDB mindDB)
    {
        m_mindDb = mindDB;
    }

    protected Object addTextDBChild(Object parentDBId, int pos, String text)
    {
        m_logger.info("import addTextDBChild : {}", text);

        Vertex dbParent = m_mindDb.getVertex(parentDBId);
        MindDB.EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);
        edgeVertex.m_target.setProperty(MindModel.TEXT_PROP_NAME, text);
        return edgeVertex.m_target.getId();
    }

    abstract public List importFile(Object parentDBId, int pos, final String path) throws Exception;

    abstract public List importString(Object parentDBId, int pos, final String path) throws Exception;

    int m_maxProgress = 0;
    int m_progress = 0;

    protected void resetProgress(int maxProgress) {
        m_maxProgress = maxProgress;
        m_progress = 0;
    }

    protected void progressStep(String info) {
        m_progress++;
        if (m_progressListener != null) {
            m_progressListener.notifyProgress(m_progress, m_maxProgress, info);
        }
    }

    protected ProgressListener m_progressListener;

    public void setProgressListener(ProgressListener progressListener) {
        m_progressListener = progressListener;
    }

    public static interface ProgressListener {
        abstract void notifyProgress(int progress, int maxPrograss, String message);
    }

    volatile boolean m_canceled;

    public void cancel() {
        m_canceled = true;
    }
}
