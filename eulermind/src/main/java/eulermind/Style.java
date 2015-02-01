package eulermind;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import eulermind.component.MindIconDialog;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prefuse.data.Node;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
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

@XStreamAlias("style")
public class Style {

    public final static int sm_cursorBackColor = ColorLib.rgb(210, 210, 210);
    public final static int sm_shadowBackColor = ColorLib.rgb(240, 240, 240);
    public final static int sm_defaultNodeColor = ColorLib.rgb(255, 255, 255);
    public final static int sm_defaultTextColor = ColorLib.rgb(0, 0, 0);

    private final static String DEFAULT_STYLE_NAME = "default";
    public static String sm_iconsList = "idea;help;yes;messagebox_warning;stop-sign;closed;info;button_ok;button_cancel;"
            + "full-1;full-2;full-3;full-4;full-5;full-6;full-7;full-8;full-9;full-0;"
            + "stop;prepare;go;back;forward;up;down;attach;ksmiletris;"
            + "smiley-neutral;smiley-oh;smiley-angry;smily_bad;clanbomber;desktop_new;gohome;"
            + "folder;korn;Mail;kmail;list;edit;kaddressbook;knotify;password;pencil;wizard;xmag;bell;bookmark;"
            + "penguin;licq;freemind_butterfly;broken-line;calendar;clock;hourglass;launch;"
            + "flag-black;flag-blue;flag-green;flag-orange;flag-pink;flag;flag-yellow;family;"
            + "female1;female2;male1;male2;fema;group";
    private static String sm_iconDir = MindIconDialog.class.getClassLoader().getResource("icons/").getPath();

    @XStreamAlias("fontFamily")
    String m_fontFamily;

    @XStreamAlias("fontSize")
    Integer m_fontSize;

    @XStreamAlias("bold")
    Boolean m_bold;

    @XStreamAlias("italic")
    Boolean m_italic;

    @XStreamAlias("nodeColor")
    Integer m_nodeColor;

    @XStreamAlias("textColor")
    Integer m_textColor;

    @XStreamAlias("icon")
    String m_icon;

    Style() {

    }

    static HashMap<String, Style> sm_styles = new HashMap<String, Style>();
    static Style sm_defaultStyle;

    private static void initDefaultStyle()
    {
        if (! hasStyle(DEFAULT_STYLE_NAME)) {
            sm_defaultStyle = new Style();
            sm_styles.put(DEFAULT_STYLE_NAME, sm_defaultStyle);
        } else {
            sm_defaultStyle = sm_styles.get(DEFAULT_STYLE_NAME);
        }

        if (sm_defaultStyle.m_fontFamily == null ||
                !getFontFamilies().contains(sm_defaultStyle.m_fontFamily)) {
            sm_defaultStyle.m_fontFamily = Font.SANS_SERIF;
        }

        if (sm_defaultStyle.m_fontSize == null ||
                !getFontSizes().contains(sm_defaultStyle.m_fontSize)) {
            sm_defaultStyle.m_fontSize = 16;
        }

        if (sm_defaultStyle.m_bold == null) {
            sm_defaultStyle.m_bold = false;
        }

        if (sm_defaultStyle.m_italic == null) {
            sm_defaultStyle.m_italic = false;
        }

        if (sm_defaultStyle.m_textColor == null) {
            sm_defaultStyle.m_textColor = ColorLib.rgb(255, 0, 0);
        }

        if (sm_defaultStyle.m_nodeColor == null) {
            sm_defaultStyle.m_nodeColor = ColorLib.rgb(255, 255, 255);
        }
    }

    public static Set<String> getStyleNames()
    {
        return sm_styles.keySet();
    }

    public static Style getStyle(String name) {
        return sm_styles.get(name);
    }

    public static Style getDefaultStyle() {
        return sm_defaultStyle;
    }

    public static void addStyle(String name, Style style) {
        sm_styles.put(name, style);
    }

    public static void removeStyle(String name) {
        sm_styles.remove(name);
    }

    public static boolean hasStyle(String name) {
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

    public static void load() {
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

        /*
        XStream xstream = createXStream();
        sm_styles = (HashMap)xstream.fromXML(userStylesFile);
        */
        sm_styles = new HashMap<String, Style>();

        initDefaultStyle();
    }

    public static void save() {

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


    ArrayList<String> fontFamilies = null;
    public static ArrayList<String> getFontFamilies() {
        ArrayList<String> fontFamilies = new ArrayList<String>();
        for (String family : GraphicsEnvironment .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            Font font = new Font(family, Font.PLAIN, 1);
            if (font.canDisplay('a')) {
                fontFamilies.add(family);
            }
        }

        return fontFamilies;
    }

    public static ArrayList<Integer> getFontSizes() {
        return new ArrayList<Integer>(Arrays.asList(8, 10, 12, 14, 16, 18, 20, 24, 28));
    }

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

    public static String getIconPath(String name)
    {
        return name == null ? null : sm_iconDir + "/" + name + ".png";
    }

    public static Icon getImageIcon(String name) {
        return new ImageIcon(Style.getIconPath(name));
    }

    static private Style getStyleSurely(String name)
    {
        if (name == null) {
            return sm_defaultStyle;
        } else {
            Style namedStyle = sm_styles.get(name);
            if (namedStyle == null) {
                return namedStyle;
            }
            return sm_defaultStyle;
        }
    }

    public static String getFontFamilySurely(String name)
    {
        Style style = getStyleSurely(name);
        return style.m_fontFamily != null ? style.m_fontFamily : sm_defaultStyle.m_fontFamily;
    }

    public static Integer getFontSizeSurely(String name)
    {
        Style style = getStyleSurely(name);
        return style.m_fontSize != null ? style.m_fontSize : sm_defaultStyle.m_fontSize;
    }

    public static Boolean getBoldSurely(String name)
    {
        Style style = getStyleSurely(name);
        return style.m_bold != null ? style.m_bold : sm_defaultStyle.m_bold;
    }

    public static Boolean getItalicSurely(String name)
    {
        Style style = getStyleSurely(name);
        return style.m_italic != null ? style.m_italic : sm_defaultStyle.m_italic;
    }

    public static Integer getTextColorSurely(String name)
    {
        Style style = getStyleSurely(name);
        return style.m_textColor != null ? style.m_textColor : sm_defaultStyle.m_textColor;
    }

    public static Integer getNodeColorSurely(String name)
    {
        Style style = getStyleSurely(name);
        return style.m_nodeColor != null ? style.m_nodeColor : sm_defaultStyle.m_nodeColor;
    }

    public static String getIconSurely(String name)
    {
        Style style = getStyleSurely(name);
        return style.m_icon != null ? style.m_icon : sm_defaultStyle.m_icon;
    }
}
