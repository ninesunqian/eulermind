package eulermind.importer;

import eulermind.MindDB;
import org.apache.tika.exception.TikaException;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.*;
import java.util.*;

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

public class TikaPlainTextImporter extends Importer{

    public TikaPlainTextImporter(MindDB mindDB)
    {
        super(mindDB);
    }

    private Object importLineNode(Object parentDBId, int pos, LineNode root)
    {
        Object dbId = addTextDBChild(parentDBId, pos, root.toString());

        for (int i=0; i<root.getChildCount(); i++) {
            importLineNode(dbId, i, root.getChildAt(i));
        }
        return dbId;
    }

    @Override
    public List importString(Object parentDBId, int pos, String text)
    {
        LineNode root = LineNode.textToLineTree(text);
        Object dbId = importLineNode(parentDBId, pos, root);
        List list = new ArrayList();
        list.add(dbId);
        return list;
    }

    public String getPlainTextByTika(File file) throws IOException, TikaException, SAXException
    {
        Detector detector = new DefaultDetector();
        Parser parser = new AutoDetectParser(detector);
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, file.getName());

        TikaInputStream input = TikaInputStream.get(file, metadata);
        String text = null;
        try {
            Writer output = new CharArrayWriter();
            parser.parse(input, new BodyContentHandler(output), metadata, context);
            text = output.toString();
        } finally {
            input.close();
        }

        return text;
    }

    public List importFile(Object parentDBId, int pos, final String path) throws Exception
    {
        File file = new File(path);

        String plainText = getPlainTextByTika(file);
        if (plainText != null && !plainText.isEmpty()) {
            return importString(parentDBId, pos, plainText);
        }
        return new ArrayList();
    }

}
