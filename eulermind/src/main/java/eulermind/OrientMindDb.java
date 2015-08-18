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
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

import java.util.Set;

public class OrientMindDb extends MindDB{

    OrientMindDb(String path) {
        super(path);
    }

    void initBackGraph(String path) {
        m_graph = new OrientGraph (path, true);
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
