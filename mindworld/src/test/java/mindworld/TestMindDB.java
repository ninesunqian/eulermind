package mindworld;

import com.tinkerpop.blueprints.Vertex;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Demonstration of a node-link tree viewer
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TestMindDB extends TestCase {
    MindDB m_mindDB;
    String m_dbPath;

    TestMindDB(String method) {
        super(method);
    }

    public void setUp() {
        m_dbPath = System.getProperty("java.io.tmpdir") + File.separator + "mindworld_test";
        m_mindDB = new MindDB(m_dbPath);
    }

    public void tearDown() {
        try {
            FileUtils.deleteDirectory(new File(m_dbPath));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void testInsertChild() {
        Vertex root = m_mindDB.getVertex(m_mindDB.getRootId());
        MindDB.EdgeVertex v0 = m_mindDB.addChild(root);
        MindDB.EdgeVertex v1 = m_mindDB.addChild(root);

        MindDB.EdgeVertex v00 = m_mindDB.addChild(v0.m_vertex);
        MindDB.EdgeVertex v01 = m_mindDB.addChild(v0.m_vertex);

        MindDB.EdgeVertex v10 = m_mindDB.addChild(v1.m_vertex);
        MindDB.EdgeVertex v100 = m_mindDB.addChild(v10.m_vertex);

        assertTrue(m_mindDB.isVertexIdParent(v0.m_vertex.getId(), root.getId()));
        assertTrue(m_mindDB.isVertexIdChild(root.getId(), v0.m_vertex.getId()));

        assertTrue(m_mindDB.isVertexIdAncestor(v0.m_vertex.getId(), root.getId()));
        assertTrue(m_mindDB.isVertexIdAncestor(v100.m_vertex.getId(), root.getId()));
        assertTrue(m_mindDB.isVertexIdDescendant(root.getId(), v100.m_vertex.getId()));

        assertTrue(m_mindDB.isVertexIdSibling(v00.m_vertex.getId(), v01.m_vertex.getId()));
        assertFalse(m_mindDB.isVertexIdSibling(v00.m_vertex.getId(), v10.m_vertex.getId()));

    }
    /*
        MindDB [MindDB]
    finalize [MindDB]
    addEdge [MindDB]
    addVertex [MindDB]
    getEdge [MindDB]
    getEdges [MindDB]
    getEdges [MindDB]
    getFeatures [MindDB]
    getVertex [MindDB]
    getVertices [MindDB]
    getVertices [MindDB]
    removeEdge [MindDB]
    removeVertex [MindDB]
    shutdown [MindDB]
    commit [MindDB]
    getOrCreateIndex [MindDB]
    getRootId [MindDB]
    getEdgeSource [MindDB]
    getEdgeTarget [MindDB]
    getContainerProperty [MindDB]
    getOutEdgeInnerIds [MindDB]
    getOutEdgeInnerId [MindDB]
    getParentSkipCache [MindDB]
    getInheritPath [MindDB]
    getSharedAncestorId [MindDB]
    isVertexIdSelf [MindDB]
    isVertexIdChild [MindDB]
    isVertexIdParent [MindDB]
    isVertexIdSibling [MindDB]
    isVertexIdAncestor [MindDB]
    isVertexIdDescendant [MindDB]
    getEdgeType [MindDB]
    getNumberHoles [MindDB]
    allocateOutEdgeInnerId [MindDB]
    addEdge [MindDB]
    addRefEdge [MindDB]
    removeEdge [MindDB]
    removeRefEdgeImpl [MindDB]
    removeRefEdge [MindDB]
    getEdge [MindDB]
    EdgeVertex [MindDB.EdgeVertex]
    addChild [MindDB]
    getChildOrReferentCount [MindDB]
    getChildOrReferent [MindDB]
    getChildrenAndReferents [MindDB]
    getParentDBId [MindDB]
    getParent [MindDB]
    handoverChild [MindDB]
    handoverReferent [MindDB]
    changeChildOrReferentPos [MindDB]
    getReferrers [MindDB]
    run [MindDB.Processor]
    deepTraverse [MindDB]
    deepTraverse [MindDB]
    RefLinkInfo [MindDB.RefLinkInfo]
    toStream [MindDB.RefLinkInfo]
    fromStream [MindDB.RefLinkInfo]
    trashSubTree [MindDB]
    TrashedTreeContext [MindDB.TrashedTreeContext]
    getTrashedTreeContext [MindDB]
    restoreTrashedSubTree [MindDB]
    removeSubTree [MindDB]
    cleanTrash [MindDB]
    copyProperty [MindDB]
    query [MindDB]
    createFullTextVertexKeyIndex [MindDB]
    getVertices [MindDB]
    getVertices [MindDB]
    verifyCachedInheritPathValid [MindDB]
    isParentChildRelation [MindDB]
    verifyOutEdges [MindDB]
    verifyInEdges [MindDB]
    verifyVertex [MindDB]
    isVertexTrashed [MindDB]
    setVertexTrashed [MindDB]
    verifyTrashedTree [MindDB]


     */








} // end of class TreeMap
