package eulermind;

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

import com.orientechnologies.common.util.OCallable;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import org.apache.ivy.util.FileUtil;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.Set;

public class OrientMindDb extends MindDB{

    String m_orientDbUrl;

    OrientMindDb(String path) {
        super(path);
        m_orientDbUrl = "plocal:" +  path.replace(File.separatorChar, '/');
    }

    void initBackGraph() {
        m_orientDbUrl = "plocal:" +  m_path.replace(File.separatorChar, '/');
        m_graph = new OrientGraph (m_orientDbUrl, true);
    }

    void backup(String backupPath) {

        OCommandOutputListener listener = new OCommandOutputListener() {
            @Override
            public void onMessage(String iText) {
                System.out.print(iText);
            }
        };

        m_graph.shutdown();

        /*
        {
            String luceneDir = m_path + File.separator + "luceneIndexes" + File.separator + "V.x";
            Directory directory = null;
            try {
                directory = FSDirectory.open(new File(luceneDir));
                if (directory.fileExists(IndexWriter.WRITE_LOCK_NAME)) {
                    directory.clearLock(IndexWriter.WRITE_LOCK_NAME);
                    m_logger.warn("Existing write.lock at {} has been found and removed. This is a likely result of non-gracefully terminated server. Check for index discrepancies!"
                                ,luceneDir);
                }
                directory.close();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        */

        ODatabaseDocumentTx db = new ODatabaseDocumentTx(m_orientDbUrl);
        db.open("admin", "admin");
        db.freeze();

        try {
            Utils.mkdir(backupPath);
            OutputStream out = new FileOutputStream(backupPath + File.separator + "mydb.zip");
            db.backup(out, null, null, listener, 9, 2048);

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            db.release();
            db.close();
            m_graph = new OrientGraph (m_orientDbUrl, true);

        }
    }

    void restore(String backupPath) {
        OCommandOutputListener listener = new OCommandOutputListener() {
            @Override
            public void onMessage(String iText) {
                System.out.print(iText);
            }
        };

        m_graph.shutdown();
        ODatabaseDocumentTx db = new ODatabaseDocumentTx(m_orientDbUrl);
        db.open("admin", "admin");

        try {
            InputStream in = new FileInputStream(backupPath + File.separator + "mydb.zip");
            db.restore(in, null, null, listener);

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            db.close();
            m_graph = new OrientGraph (m_orientDbUrl, true);

        }
    }

    public Index<Vertex> getOrCreateIndex(final String indexName)
    {
        OrientGraph graph = (OrientGraph) m_graph;
        Index<Vertex> index = graph.getIndex(indexName, Vertex.class);
        if (index == null) {
            graph.executeOutsideTx(new OCallable<Object, OrientBaseGraph>() {
                @Override
                public Object call(OrientBaseGraph iArgument) {
                    ((OrientGraph)m_graph).createIndex(indexName, Vertex.class);
                    return null;
                }
            });
        }
        index = graph.getIndex(indexName, Vertex.class);

        return index;
    }

    public void createFullTextVertexKeyIndex(final String key)
    {
        OrientGraph graph = (OrientGraph) m_graph;
        Set<String> indexedKeys = graph.getIndexedKeys(Vertex.class);

        for (String indexedKey : indexedKeys) {
            if (indexedKey.equals(key)) {
                return;
            }
        }

        graph.executeOutsideTx(new OCallable<Object, OrientBaseGraph>() {
            @Override
            public Object call(OrientBaseGraph iArgument) {
                OClass type = ((OrientGraph)m_graph).getVertexBaseType();
                if (!type.existsProperty(key)) {
                    type.createProperty(key, OType.STRING);
                }

                ODocument metadata = new ODocument();
                metadata.fromJSON("{\"analyzer\":\"org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer\"}");

                type.createIndex("V."+key, "FULLTEXT", null, metadata, "LUCENE", new String[]{key});
                return null;
            }
        });
    }

    public void dropIndex(String indexName)
    {
        //FIXME: dropIndex后马上 createIndex, 会有bug，提示该index已经存在
        ((OrientGraph)m_graph).dropIndex(indexName);
    }
}
