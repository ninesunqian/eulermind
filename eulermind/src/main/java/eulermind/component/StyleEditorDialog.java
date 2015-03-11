package eulermind.component;

import eulermind.Style;
import org.swixml.SwingEngine;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

    FontFamilyCombobox m_fontFamilyCombobox;

    FontSizeCombobox m_fontSizeCombobox;

    BooleanCombobox m_italicCombobox;

    BooleanCombobox m_boldCombobox;

    ColorCombobox m_textColorCombobox;

    ColorCombobox m_nodeColorCombobox;

    IconButton m_iconButton;

    JLabel m_previewLabel;
    JButton m_okButton;
    JButton m_cancelButton;

    Style m_style;

    public StyleEditorDialog(Component parent, Style style)
    {
        super(JOptionPane.getFrameForComponent(parent), true);
        try {
            m_swingEngine = new SwingEngine(this);
            m_swingEngine.render("style_editor_layout.xml");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        m_style = style;

        m_nameTextField.setText(style.m_name);
        if (style.m_name.equals(Style.DEFAULT_STYLE_NAME)) {
            m_nameTextField.setEditable(false);
        }

        m_fontFamilyCombobox.setMindPropertyValue(style.m_fontFamily);
        m_fontSizeCombobox.setMindPropertyValue(style.m_fontSize);

        m_italicCombobox.setMindPropertyValue(style.m_italic);
        m_boldCombobox.setMindPropertyValue(style.m_bold);

        m_textColorCombobox.setMindPropertyValue(style.m_textColor);
        m_textColorCombobox.setForBackground(false);
        m_nodeColorCombobox.setMindPropertyValue(style.m_nodeColor);
        m_nodeColorCombobox.setForBackground(true);

        m_iconButton.setMindPropertyValue(style.m_icon);

        addListenerForUpdatePreview(m_fontFamilyCombobox);
        addListenerForUpdatePreview(m_fontSizeCombobox);

        addListenerForUpdatePreview(m_italicCombobox);
        addListenerForUpdatePreview(m_boldCombobox);

        addListenerForUpdatePreview(m_textColorCombobox);
        addListenerForUpdatePreview(m_nodeColorCombobox);

        addListenerForUpdatePreview(m_iconButton);

        m_okButton.addActionListener(m_okActionListener);
        m_cancelButton.addActionListener(m_cancelActionListener);

        //swixml没有自动计算size， 所以要pack一下
        pack();
        setLocationRelativeTo(parent);
        updatePreviewLabel();
    }


    ActionListener updatePreviewLabelActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            updatePreviewLabel();
        }
    };

    void updatePreviewLabel()
    {
        String fontFamily = m_fontFamilyCombobox.getMindPropertyValue();
        if (fontFamily == null) {
            fontFamily = Style.getFontFamilySurely(null);
        }
        Integer fontSize = m_fontSizeCombobox.getMindPropertyValue();
        if (fontSize == null) {
            fontSize = Style.getFontSizeSurely(null);
        }

        Boolean italic = m_italicCombobox.getMindPropertyValue();
        if (italic == null) {
            italic = Style.getItalicSurely(null);
        }

        Boolean bold = m_boldCombobox.getMindPropertyValue();
        if (bold == null) {
            bold = Style.getBoldSurely(null);
        }

        Integer textColor = m_textColorCombobox.getMindPropertyValue();
        if (textColor  == null) {
            textColor = Style.getTextColorSurely(null);
        }

        Integer nodeColor = m_nodeColorCombobox.getMindPropertyValue();
        if (nodeColor == null) {
            nodeColor = Style.getNodeColorSurely(null);
        }

        String icon = m_iconButton.getMindPropertyValue();
        if (icon == null) {
            icon = Style.getIconSurely(null);
        }

        Font font = FontLib.getFont(fontFamily, bold, italic, fontSize);
        m_previewLabel.setFont(font);
        m_previewLabel.setForeground(ColorLib.getColor(textColor | 0xFF000000));
        m_previewLabel.setBackground(ColorLib.getColor(nodeColor | 0xFF000000));
        m_previewLabel.setOpaque(true);
        m_previewLabel.setIcon(Style.getImageIcon(icon));
    }

    ActionListener m_okActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            String newStyleName = m_nameTextField.getText();
            if (Style.getStyle(newStyleName) != null && Style.getStyle(newStyleName) != m_style) {
                JOptionPane.showMessageDialog(null, "跟其他Style重名了!", null, JOptionPane.ERROR_MESSAGE);
                return;
            }

            m_style.m_name = newStyleName;

            m_style.m_fontFamily = m_fontFamilyCombobox.getMindPropertyValue();
            m_style.m_fontSize = m_fontSizeCombobox.getMindPropertyValue();

            m_style.m_bold = m_boldCombobox.getMindPropertyValue();
            m_style.m_italic = m_italicCombobox.getMindPropertyValue();

            m_style.m_textColor = m_textColorCombobox.getMindPropertyValue();
            m_style.m_nodeColor = m_nodeColorCombobox.getMindPropertyValue();

            m_style.m_icon = m_iconButton.getMindPropertyValue();

            setVisible(false);
        }
    };

    ActionListener m_cancelActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            m_style = null;
            setVisible(false);
        }
    };

    public static void editStyle(Component parent, Style style)
    {
        StyleEditorDialog dialog = new StyleEditorDialog(parent, style);
        dialog.setVisible(true);
    }

    private void addListenerForUpdatePreview(MindPropertyComponent component)
    {
        String fakeMindPropertyName = MindPropertyComponent.MIND_PROPERTY_PREFIX + "fakeProperty";
        component.setMindPropertyName(fakeMindPropertyName);
        component.addPropertyChangeListener(fakeMindPropertyName, m_listenerForUpdatePreview);
    }

    PropertyChangeListener m_listenerForUpdatePreview = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            updatePreviewLabel();
        }
    };
}
