package eulermind.component;

import eulermind.Style;
import eulermind.component.*;
import org.swixml.SwingEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

public class StyleEditorDialog extends JDialog {

    SwingEngine m_swingEngine;

    JTextField m_nameTextField;

    FontFamilyCombobox m_fontFamilyCombbox;

    FontSizeCombobox m_fontSizeCombbox;

    BooleanCombobox m_italicCombobox;

    BooleanCombobox m_boldCombobox;

    ColorButton m_textColorButton;

    ColorButton m_nodeColorButton;

    IconButton m_iconButton;

    JButton m_okButton;
    JButton m_cancelButton;

    public StyleEditorDialog(Component parent, Style style)
    {
        super(JOptionPane.getFrameForComponent(parent), true);
        try {
            m_swingEngine = new SwingEngine(this);
            m_swingEngine.getTaglib().registerTag("fontFamilyCombobox", FontFamilyCombobox.class);
            m_swingEngine.getTaglib().registerTag("fontSizeCombobox", FontSizeCombobox.class);
            m_swingEngine.getTaglib().registerTag("booleanCombobox", BooleanCombobox.class);
            m_swingEngine.getTaglib().registerTag("colorButton", ColorButton.class);
            m_swingEngine.getTaglib().registerTag("iconButton", IconButton.class);

            m_swingEngine.render("style_editor_layout.xml");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        m_nameTextField.setText(style.m_name);

        m_fontFamilyCombbox.setValue(style.m_fontFamily);
        m_fontSizeCombbox.setValue(style.m_fontSize);

        m_italicCombobox.setValue(style.m_italic);
        m_boldCombobox.setValue(style.m_bold);

        m_textColorButton.setValue(style.m_textColor);
        m_nodeColorButton.setValue(style.m_nodeColor);
        m_nodeColorButton.setForBackground(true);

        m_iconButton.setValue(style.m_icon);

        m_okButton.addActionListener(m_okActionListener);
        m_cancelButton.addActionListener(m_cancelActionListener);

        //swixml没有自动计算size， 所以要pack一下
        pack();
        setLocationRelativeTo(parent);
    }

    Style m_retStyle;

    ActionListener m_okActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            m_retStyle = new Style(m_nameTextField.getText());

            m_retStyle.m_fontFamily = m_fontFamilyCombbox.getValue();
            m_retStyle.m_fontSize = m_fontSizeCombbox.getValue();

            m_retStyle.m_italic = m_italicCombobox.getValue();
            m_retStyle.m_italic = m_italicCombobox.getValue();

            m_retStyle.m_textColor = m_textColorButton.getValue();
            m_retStyle.m_nodeColor = m_nodeColorButton.getValue();

            m_retStyle.m_icon = m_iconButton.getValue();

            setVisible(false);
        }
    };

    ActionListener m_cancelActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            m_retStyle = null;
            setVisible(false);
        }
    };

    public static Style showDialog(Component component, Style style)
    {
        StyleEditorDialog dialog = new StyleEditorDialog(component, style);
        dialog.setVisible(true);
        return dialog.m_retStyle;
    }
}
