package eulermind;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prefuse.data.Node;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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

@XStreamAlias("style")
public class Style {

    public final static String sm_fontFamilyPropName = "fontFamily";
    public final static String sm_fontSizePropName = "fontSize";
    public final static String sm_boldPropName = "bold";
    public final static String sm_italicPropName = "italic";
    public final static String sm_underlinedPropName = "underlined";
    public final static String sm_nodeColorPropName = "nodeColor";
    public final static String sm_textColorPropName = "textColor";
    public final static String sm_stylePropName = "style";

    public final static int m_cursorBackColor = ColorLib.rgb(210, 210, 210);
    public final static int m_shadowBackColor = ColorLib.rgb(240, 240, 240);
    public final static int m_normalBackColor = ColorLib.rgb(255, 255, 255);

    @XStreamAlias("fontFamily")
    String m_fontFamily;

    @XStreamAlias("fontSize")
    Integer m_fontSize;

    @XStreamAlias("bold")
    Boolean m_bold;

    @XStreamAlias("italic")
    Boolean m_italic;

    @XStreamAlias("underlined")
    Boolean m_underlined;

    @XStreamAlias("nodeColor")
    Integer m_nodeColor;

    @XStreamAlias("textColor")
    Integer m_textColor;

    @XStreamAlias("icon")
    String m_icon;

    @XStreamAlias("shortcutKey")
    String m_shortcutKey;

    Style() {

    }

    static HashMap<String, Style> sm_styles = new HashMap<String, Style>();

    static public Style getStyle(String name) {
        return sm_styles.get(name);
    }

    static public Style getDefaultStyle() {
        return sm_styles.get("default");
    }

    static public void addStyle(String name, Style style) {
        sm_styles.put(name, style);
    }

    static public void removeStyle(String name) {
        sm_styles.remove(name);
    }

    static public boolean hasStyle(String name) {
        return sm_styles.containsKey(name);
    }

    private static XStream createXStream() {
        XStream xstream = new XStream(new DomDriver());
        NamedMapConverter converter = new NamedMapConverter(xstream.getMapper(), "entry",
                "name", String.class, "style", Style.class,
                true, false,
                xstream.getConverterLookup());
        xstream.registerConverter(converter);
        xstream.processAnnotations(Style.class);
        return xstream;
    }

    static void load() {
        String userStylesPath = System.getProperty("user.home") + "/.eulermind/styles.xml";
        File userStylesFile = new File(userStylesPath);

        if (!userStylesFile.exists()) {

            String defaultStylesPath = Style.class.getClassLoader().getResource("styles.xml").getPath();
            File defaultStylesFile = new File(defaultStylesPath);

            try {
                FileUtils.copyFile(defaultStylesFile, userStylesFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        XStream xstream = createXStream();
        sm_styles = (HashMap)xstream.fromXML(userStylesFile);
    }

    static void save() {

        String userStylesPath = System.getProperty("user.home") + "/.eulermind/styles.xml";
        File userStylesFile = new File(userStylesPath);

        XStream xstream = createXStream();
        String xml = xstream.toXML(sm_styles);

        s_logger.info("xml string is {}", xml);
        try {
            FileUtils.write(userStylesFile, xml);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    static Logger s_logger = LoggerFactory.getLogger(Style.class);

    public static void main(String argv[]) {
        Style normalStyle = new Style();

        XStream xstream = new XStream(new DomDriver());
        xstream.processAnnotations(Style.class);
        String normalXml = xstream.toXML(normalStyle);
        s_logger.info("normalStyle = {}", normalXml);

        normalStyle.m_bold = true;
        Style bigStyle = new Style();
        normalStyle.m_fontSize = 100;

        addStyle("normal", normalStyle);
        addStyle("big", bigStyle);

        save();
        load();

        normalStyle = getStyle("normal");
        s_logger.info("fontSize = {}", normalStyle.m_fontSize);
    }

    static Style sm_defaultStyle;

    static Style getNodeStyle(Node node)
    {
        String styleName = (String) node.get(Style.sm_stylePropName);

        if (styleName != null && styleName.length() > 0) {
            Style specialStyle = getStyle(styleName);
            if (specialStyle != null) {
                return specialStyle;
            }
        }

        return sm_defaultStyle;
    }

    public static Font getNodeFont(Node node)
    {
        String family = (String)node.get(Style.sm_fontFamilyPropName);
        Integer size = (Integer)node.get(Style.sm_fontSizePropName);
        Boolean bold = (Boolean)node.get(Style.sm_boldPropName);
        Boolean italic = (Boolean)node.get(Style.sm_italicPropName);

        Style style = getNodeStyle(node);

        if (family == null) {
            family = (style != null && style.m_fontFamily != null)  ?  style.m_fontFamily : "SansSerif";
        }

        if (size == null) {
            size = (style != null && style.m_fontSize != null) ?  style.m_fontSize : 16;
        }

        if (bold == null) {
            bold = (style != null && style.m_bold != null) ? style.m_bold : false;
        }

        if (italic == null) {
            italic = (style != null && style.m_italic != null) ? style.m_italic : false;
        }

        int fontStyle = Font.PLAIN;
        if (bold) {
            fontStyle |= Font.BOLD;
        }
        if (italic) {
            fontStyle |= Font.ITALIC;
        }

        //String family = (family)item.get(MindModel.sm_fontFamilyPropName);
        return FontLib.getFont(family, fontStyle, size);
    }

    public static int getNodeColor(Node node)
    {
        Integer color = (Integer)node.get(Style.sm_nodeColorPropName);

        Style style = getNodeStyle(node);

        if (color == null) {
            color = style != null && style.m_nodeColor != null ? style.m_nodeColor : ColorLib.rgb(255, 255, 255);
        }

        return color;
    }

    public static int getTextColor(Node node)
    {
        Integer color = (Integer)node.get(Style.sm_textColorPropName);

        Style style = getNodeStyle(node);

        if (color == null) {
            color = style != null && style.m_textColor != null ? style.m_textColor : ColorLib.rgb(0, 0, 0);
        }

        return color;
    }
}
