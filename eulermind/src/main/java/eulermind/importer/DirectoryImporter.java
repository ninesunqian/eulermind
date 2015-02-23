package eulermind.importer;

import com.tinkerpop.blueprints.Vertex;
import eulermind.MindDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

public class DirectoryImporter extends Importer{

    static Logger s_logger = LoggerFactory.getLogger(Importer.class);

    public DirectoryImporter(MindDB mindDB)
    {
        super(mindDB);
    }

    private Object importFile(Object parentDBId, int pos, File file)
    {

        Object dbId = addTextDBChild(parentDBId, pos, file.getName());

        if (file.isFile()) {
            Importer importer;
            if (file.getName().endsWith(".mm")) {
                importer = new FreemindImporter(m_mindDb);
            } else {
                importer = new TikaPlainTextImporter(m_mindDb);
            }

            try {
                importer.importFile(dbId, 0, file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else if (file.isDirectory()) {
            File[] innerFiles = file.listFiles();
            for (int i=0; i<innerFiles.length; i++) {
                importFile(dbId, i, innerFiles[i]);
            }
        }

        return dbId;
    }

    public List importFile(Object parentDBId, int pos, String path)
            throws IOException, SAXException, ParserConfigurationException
    {
        File file = new File(path);

        ArrayList newChildren = new ArrayList();
        Object newChild = importFile(parentDBId, pos, file);
        newChildren.add(newChild);
        return newChildren;
    }
}
