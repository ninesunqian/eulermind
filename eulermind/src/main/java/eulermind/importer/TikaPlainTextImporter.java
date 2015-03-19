package eulermind.importer;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.util.ULocale;
import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import eulermind.MindDB;
import eulermind.Utils;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import javax.swing.tree.DefaultMutableTreeNode;

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

    static Logger s_logger = LoggerFactory.getLogger(Importer.class);

    //7个节点是人处理信息的上限，我们以20个为限。超出此范围，添加树的层次
    static final int MAX_CHILD_COUNT = 20;

    public TikaPlainTextImporter(MindDB mindDB)
    {
        super(mindDB);

        if (s_languageDetector == null) {
            initLanguageDetector();
        }
    }

    static class LineNode extends DefaultMutableTreeNode{
        int m_indent;
        int m_tail;
        String m_trimLine;
        int m_blankLines;

        LineNode(String line) {
            super();

            for (m_indent=0; m_indent<line.length() && Character.isWhitespace(line.charAt(m_indent)); m_indent++) {
            }

            for (m_tail=line.length(); m_tail>m_indent && Character.isWhitespace(line.charAt(m_tail-1)); m_tail--) {
            }

            m_trimLine = line.substring(m_indent, m_tail);

            m_blankLines = m_trimLine.length() == 0 ? 1 : 0;
        }

        LineNode(int blankLines) {
            assert blankLines > 0;
            m_indent = 0;
            m_tail = 0;
            m_trimLine = "";
            m_blankLines = blankLines;
        }

        public String toString() {
            if (isBlank()) {
                return "blank line: " + ((Integer)m_blankLines).toString();
            } else {
                return m_trimLine;
            }
        }

        public LineNode getChildAt(int i)
        {
            return (LineNode)super.getChildAt(i);
        }

        public LineNode getParent()
        {
            return (LineNode)super.getParent();
        }

        public LineNode getFirstChild()
        {
            return (LineNode)super.getFirstChild();
        }

        boolean isBlank()
        {
            return m_blankLines > 0;
        }
    }

    //把相邻连续空行压缩成一个，并记录下连续空行数。 以后利用空行对文章分章节
    private List<LineNode> splitTextToLines(String text)
    {
        String lines[] = StringUtils.splitPreserveAllTokens(text, '\n');

        ArrayList<LineNode> compressedLines = new ArrayList<LineNode>();

        for (String line : lines) {
            LineNode curNode = new LineNode(line);

            if (curNode.isBlank()) {
                if (compressedLines.size() == 0) {
                    continue;
                }

                LineNode last = compressedLines.get(compressedLines.size() - 1);
                if (last.isBlank()) {
                    last.m_blankLines++;
                } else {
                    compressedLines.add(curNode);
                }

            } else {
                compressedLines.add(curNode);
            }
        }

        return compressedLines;
    }

    private void removeRedundantBlankLines(LineNode root)
    {
        int childCount = root.getChildCount();
        for (int i=0; i<childCount; i++) {
            removeRedundantBlankLines(root.getChildAt(i));
        }

        //所有的子节点都满足这个条件：1 都有唯一的子节点，2这个唯一子节点是一个非空行
        boolean anyChildHasOneLeaf = true;
        for (int i=0; i<childCount; i++) {
            LineNode child = root.getChildAt(i);
            if (!(child.getChildCount() == 1 && child.getChildAt(0).isBlank() == false)) {
                anyChildHasOneLeaf = false;
            }
        }

        //所有叶子节点，都变成自己的子节点
        if (root.getChildCount() > 0 && anyChildHasOneLeaf) {
            ArrayList<LineNode> leaves = new ArrayList<>();
            for (int i=0; i<childCount; i++) {
                leaves.add(root.getChildAt(i).getChildAt(0));
            }

            root.removeAllChildren();

            for (LineNode leaf : leaves) {
                root.add(leaf);
            }
        }
    }


    private LinkedList<LineNode> pollLastSameLineNodes(LinkedList<LineNode> stack)
    {
        int lastBlankLines = stack.peekLast().m_blankLines;
        LinkedList<LineNode> lastSameLineNodes = new LinkedList<LineNode>();

        while (!stack.isEmpty() && stack.peekLast().m_blankLines == lastBlankLines) {
            //pollLast是逆序的， addFirst是把顺序正果来
            lastSameLineNodes.addFirst(stack.pollLast());
        }
        return lastSameLineNodes;
    }


    private LineNode reduceToChapterTreeByBlankLine(List<LineNode> lineNodes)
    {
        LinkedList<LineNode> newlineNodes = new LinkedList<LineNode>();
        Iterator<LineNode> iterator = lineNodes.iterator();
        newlineNodes.add(iterator.next());

        //给lineNode 添加节点，使之变成深度有限的递归向上的搜索路径：newLineNodes
        //以空白行作为文章层次分割i标记。连续空行越多，表示分割层次越高

        //建立一个树的递归向上搜索路径，并保证每一级的空行分割节点都存在。
        {
            int maxBlankLines = 0;
            while (iterator.hasNext()) {
                LineNode lineNode = iterator.next();
                maxBlankLines = Math.max(maxBlankLines, lineNode.m_blankLines);

                //如果空行分割跨级了，补足中间级别的空行分割节点。
                for (int i = newlineNodes.peekLast().m_blankLines + 1; i < lineNode.m_blankLines; i++) {
                    newlineNodes.add(new LineNode(i));
                }
                newlineNodes.add(lineNode);
            }

            //如果空行分割跨级了，补足中间级别的空行分割节点。
            for (int i = newlineNodes.peekLast().m_blankLines + 1; i <= maxBlankLines + 1; i++) {
                newlineNodes.add(new LineNode(i));
            }
        }

        //组织成树
        LinkedList<LineNode> stack = new LinkedList<LineNode>();

        for (LineNode newLineNode : newlineNodes) {
            if (!stack.isEmpty() && stack.peekLast().m_blankLines < newLineNode.m_blankLines) {
                List<LineNode> reducedLineNodes = pollLastSameLineNodes(stack);

                for (LineNode reducedLineNode : reducedLineNodes) {
                    newLineNode.add(reducedLineNode);
                }
            }

            stack.add(newLineNode);
        }

        assert stack.size() == 1;

        return  stack.peekFirst();
    }

    /*
    static void handoverChildren(LineNode from, LineNode to)
    {
        while (from.getChildCount() > 0) {
            //to添加一个，from自动少一个
            to.add(from.getFirstChild());
        }
    }
    */

    static void moveChildrenToList(LineNode from, List list)
    {
        for (int i = 0; i < from.getChildCount(); i++) {
            list.add(from.getChildAt(i));
        }
        from.removeAllChildren();
    }

    private LineNode removeBlankNodeWithSingleChild(LineNode lineNode)
    {
        if (lineNode.getChildCount() == 1 && lineNode.m_blankLines > 0) {
            return removeBlankNodeWithSingleChild(lineNode.getFirstChild());
        }

        if (lineNode.getChildCount() == 0) {
            return lineNode;
        }

        ArrayList<LineNode> oldChildren = new ArrayList<LineNode>();
        moveChildrenToList(lineNode, oldChildren);

        for (LineNode oldChild : oldChildren) {
            lineNode.add(removeBlankNodeWithSingleChild(oldChild));
        }

        return lineNode;
    }

    //整理树，方法是如果某个节点的子节点太多，加入新的节点层次。
    private LineNode tooManySiblingsToSubTree(LineNode root)
    {
        if (root.getChildCount() == 0) {
            return root;
        }

        ArrayList<LineNode> children = new ArrayList<LineNode>();
        moveChildrenToList(root, children);

        ArrayList<LineNode> newParents = new ArrayList<LineNode>();

        //利用两个队列， 广度优先遍历的逆序方法，逐层添加新的父节点

        while (children.size() > MAX_CHILD_COUNT) {
            for (int i = 0; i < children.size(); i++) {
                if (i / MAX_CHILD_COUNT >= newParents.size()) {
                    int childBlankLines = children.get(i).m_blankLines;
                    newParents.add(new LineNode(childBlankLines > 0 ? childBlankLines : 1));
                }
                newParents.get(newParents.size() - 1).add(children.get(i));
            }
            children.clear();

            ArrayList<LineNode> tmp = children;
            children = newParents;
            newParents = tmp;
        }

        for (LineNode child : children) {
            root.add(tooManySiblingsToSubTree(child));
        }

        return root;
    }

    private Object[] getIndentStatistics(LineNode parent) {

        ArrayList<Integer> indents = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            LineNode line = parent.getChildAt(i);
            assert ! line.isBlank();
            indents.add(line.m_indent);
        }

        int minIndent = Utils.getMinimumItem(indents);
        int maxIndent = Utils.getMaximumItem(indents);

        LinkedHashMap<Integer, Integer> indentCounts = Utils.count(indents);

        Object ret [] =  {minIndent, maxIndent, indentCounts};
        return ret;
    }

    private List<String> breakToParagraphs(LineNode root, int firstLineIndent)
    {
        ArrayList<String> paragraph = new ArrayList<String>();

        StringBuilder stringBuilder =  new StringBuilder();
        stringBuilder.append((root.getChildAt(0)).m_trimLine);

        for (int i = 1; i < root.getChildCount(); i++) {
            LineNode oldChild = root.getChildAt(i);

            if (oldChild.m_indent == firstLineIndent) {
                paragraph.add(stringBuilder.toString());
                //清空stringBuilder
                stringBuilder.setLength(0);
            }

            stringBuilder.append(oldChild.m_trimLine);
        }

        paragraph.add(stringBuilder.toString());
        return paragraph;
    };

    private LineNode breakParagraphToLineTree(String paragraph)
    {
        LineNode parent = new LineNode(1);

        BreakIterator boundary = BreakIterator.getSentenceInstance(getStringULocale(paragraph));
        boundary.setText(paragraph);

        int start = boundary.first();
        int end = boundary.next();

        for (; end != BreakIterator.DONE; start = end, end = boundary.next()) {
            LineNode child = new LineNode(paragraph.substring(start, end));
            if (child.m_trimLine.length() == 0) {
                continue;
            }
            parent.add(child);
        }
        return parent;
    }

    private void reduceTextLineSiblingsToSubTree(LineNode root)
    {
        if (root.getChildCount() == 0) {
            return;
        }

        if ((root.getChildAt(0)).isBlank()) {
            for (int i = 0; i < root.getChildCount(); i++) {
                reduceTextLineSiblingsToSubTree(root.getChildAt(i));
            }
            return;
        }

        Object[] indentStatistics = getIndentStatistics(root);
        int minIndent = (Integer)indentStatistics[0];
        int maxIndent = (Integer)indentStatistics[1];
        LinkedHashMap<Integer, Integer> indentCountMap = (LinkedHashMap<Integer, Integer>)indentStatistics[2];
        Map.Entry<Integer, Integer> indentCounts[] = indentCountMap.entrySet().toArray(new Map.Entry[0]);

        //如果概率最高的缩进是最小缩进，那么这个一篇被自动断行的文章
        if (indentCounts[indentCounts.length - 1].getKey() == minIndent) {


            int sentenceBreakCount = 0;
            for (int i=0; i<root.getChildCount(); i++) {
                String line = root.getChildAt(i).m_trimLine;
                if (UCharacter.getIntPropertyValue(line.codePointAt(line.length() - 1), UProperty.SENTENCE_BREAK) == 0) {
                    sentenceBreakCount++;
                }
            }

            //如果中途有非句号的断行。 TODO:暂时不考虑首行缩进
            if (sentenceBreakCount > 0 && sentenceBreakCount < root.getChildCount() - 1) {
                LineNode firstChild = root.getFirstChild();
                for (int i=1; i<root.getChildCount(); i++) {
                    firstChild.m_trimLine.concat(root.getChildAt(i).m_trimLine);
                }
                root.removeAllChildren();
                root.add(firstChild);
            }

            int firstLineIndent;
            if (indentCounts.length >= 2) {
                //第二个最大概率的缩进，就是首行缩进
                firstLineIndent = indentCounts[indentCounts.length - 2].getKey();
            } else {
                firstLineIndent = minIndent + 1;
            }

            List<String> paragraphs = breakToParagraphs(root, firstLineIndent);

            root.removeAllChildren();

            for (String paragraph : paragraphs) {
                assert paragraph.length() > 0;
                LineNode child = breakParagraphToLineTree(paragraph);
                root.add(child);
            }
        } else {

            ArrayList<LineNode> oldChildren = new ArrayList<LineNode>();
            for (int i = 1; i < root.getChildCount(); i++) {
                oldChildren.add(root.getChildAt(i));
            }
            root.removeAllChildren();

            if (oldChildren.get(0).m_indent > minIndent) {
                LineNode fakeFirstLine = new LineNode("fake first node");
                fakeFirstLine.m_indent = minIndent;
                oldChildren.add(0, fakeFirstLine);
            }

            root.add(oldChildren.get(0));

            for (int i=1; i<oldChildren.size(); i++) {

                LineNode oldChild = oldChildren.get(i);

                //向上找到一行，它的缩进大于或等于当前行
                //等于： 它是当前行的兄弟
                //小于：它是当前行的父亲
                for (int j = i - 1; j >= 0; j--) {
                    LineNode addedChild = oldChildren.get(j);
                    if (addedChild.m_indent < oldChild.m_indent) {
                        addedChild.add(oldChild);
                        break;
                    } else if (addedChild.m_indent == oldChild.m_indent) {
                        addedChild.getParent().add(oldChild);
                        break;
                    }
                }

                assert(oldChild.getParent() != null);
            }
        }
    }

    private Object importLineNode(Object parentDBId, int pos, LineNode root)
    {
        BreakIterator boundary = BreakIterator.getSentenceInstance(getStringULocale(root.m_trimLine));
        boundary.setText(root.m_trimLine);

        int start = boundary.first();
        int end = boundary.next();

        Object dbId;

        int childPos = 0;

        if (end == root.m_trimLine.length()) {
            dbId = addTextDBChild(parentDBId, pos, root.m_trimLine);
            s_logger.debug("import line {}", root.m_trimLine);


        } else {
            dbId = addTextDBChild(parentDBId, pos, root.toString());

            for (; end != BreakIterator.DONE; start = end, end = boundary.next()) {
                LineNode lineNode = new LineNode(root.m_trimLine.substring(start, end));

                if (lineNode.m_trimLine.length() == 0) {
                    continue;
                }

                addTextDBChild(dbId, childPos, lineNode.m_trimLine);

                s_logger.debug("import sentence {}", lineNode.m_trimLine);
                childPos++;
            }
        }

        for (int i=0; i<root.getChildCount(); i++) {
            importLineNode(dbId, childPos, root.getChildAt(i));
            childPos++;
        }

        return dbId;
    }

    public List importString(Object parentDBId, int pos, String text)
    {
        List<LineNode> lines = splitTextToLines(text);


        //返回的树，非叶子节点都是空行，叶子节点都是文字
        LineNode root = reduceToChapterTreeByBlankLine(lines);

        //保持相对逻辑层次不变的情况下，去掉变成链的空白行节点
        removeRedundantBlankLines(root);

        //该函数的前置条件：同一个节点的子节点必须都是空行或都是文字节点
        reduceTextLineSiblingsToSubTree(root);

        root = tooManySiblingsToSubTree(root);
        root = removeBlankNodeWithSingleChild(root);

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

    public static String getLanguage(String data)
    {
       CharsetDetector detector = new CharsetDetector();
       detector.setText(data.getBytes());
       CharsetMatch match = detector.detect();
       String encoding = match.getName();
       String language = match.getLanguage();
       return encoding;
    }

    static List<LanguageProfile> s_languageProfiles = null;
    static LanguageDetector s_languageDetector = null;
    static TextObjectFactory s_textObjectFactory = null;

    static void initLanguageDetector() {
        try {
            s_languageProfiles = new LanguageProfileReader().readAll();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //build language detector:
        s_languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(s_languageProfiles)
                .build();

        //create a text object factory
        s_textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
    }

    static ULocale getStringULocale(String str) {
        TextObject textObject = s_textObjectFactory.forText(str);
        //不能用这个函数：s_languageDetector.detect(textObject); 因为这个函数相当严格，有可能找不到匹配的语言
        List<DetectedLanguage> languages = s_languageDetector.getProbabilities(textObject);
        return ULocale.forLanguageTag(languages.get(0).getLanguage());
    }

    public static void main(String argv[]) {
        //String ch = "英文句号.中文句号。英文句号.中文句号。";
        String ch = "太阳当空照，我去上学校。学校有老师，下课有作业。";

        initLanguageDetector();
        ULocale uLocale = getStringULocale(ch);



        BreakIterator boundary = BreakIterator.getSentenceInstance(uLocale);
        boundary.setText(ch);

        int start = boundary.first();
        int end = boundary.next();

        String sub = ch.substring(start, end);
        System.out.println(sub);

    }

}
