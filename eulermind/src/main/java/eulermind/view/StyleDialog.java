package eulermind.view;

import eulermind.component.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swixml.SwingEngine;

import javax.swing.*;

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


public class StyleDialog extends JDialog {

    static Logger m_logger = LoggerFactory.getLogger(StyleDialog.class);

    StyleList m_styleList;
    JTabbedPane m_tabbedPane;

    SwingEngine m_swingEngine;

    FontFamilyCombobox m_fontFamilyCombobox;
    BooleanCombobox m_italicCombobox;
    BooleanCombobox m_boldCombobox;

    ColorButton m_textColorButton;
    ColorButton m_nodeColorButton;

    IconButton m_iconButton;


    StyleDialog ()
    {
        try {
            m_swingEngine = new SwingEngine(this);
            m_swingEngine.getTaglib().registerTag("fontCombobox", FontFamilyCombobox.class);
            m_swingEngine.getTaglib().registerTag("booleanCombobox", BooleanCombobox.class);
            m_swingEngine.getTaglib().registerTag("colorButton", ColorButton.class);
            m_swingEngine.getTaglib().registerTag("iconButton", IconButton.class);
            m_swingEngine.getTaglib().registerTag("styleList", StyleList.class);

            m_swingEngine.getTaglib().registerTag("mindEditor", MindEditor.class);
            m_swingEngine.render("main_frame_layout.xml");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
