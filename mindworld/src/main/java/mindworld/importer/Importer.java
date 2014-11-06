package mindworld.importer;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-10-7
 * Time: 上午6:28
 * To change this template use File | Settings | File Templates.
 */
import com.tinkerpop.blueprints.Vertex;
import mindworld.MindDB;
import mindworld.MindModel;
import java.util.List;

public abstract class Importer {

    protected MindDB m_mindDb;

    Importer(MindDB mindDB)
    {
        m_mindDb = mindDB;
    }

    protected Object addTextDBChild(Object parentDBId, int pos, String text)
    {
        Vertex dbParent = m_mindDb.getVertex(parentDBId);
        MindDB.EdgeVertex edgeVertex = m_mindDb.addChild(dbParent, pos);
        edgeVertex.m_vertex.setProperty(MindModel.sm_textPropName, text);
        return edgeVertex.m_vertex.getId();
    }

    abstract public List importFile(Object parentDBId, int pos, final String path) throws Exception;

}
