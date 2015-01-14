package eulermind;

import prefuse.util.FontLib;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class FontCombobox extends JComboBox{

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    static boolean sm_constantLoaded = false;
    static String sm_fontFamilies[];

    static Integer sm_fontSizes[] = {10, 12, 14, 16, 18, 20, 24, 28};
    static Color sm_colors[] = {Color.black, Color.blue, Color.cyan, Color.darkGray};

    enum ListWhich {
        list_family,
        list_size,
        list_color
    }

    ListWhich m_listWhich;
    String m_family;
    int m_size;
    Color m_color;

    static void loadConstant()
    {
        if (sm_constantLoaded)
            return;

        ArrayList<String> fontFamilies = new ArrayList<String>();
        for (String family : GraphicsEnvironment .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            Font font = new Font(family, Font.PLAIN, 1);
            if (font.canDisplay('a')) {
                fontFamilies.add(family);
            }
        }

        sm_fontFamilies = new String[fontFamilies.size()];
        fontFamilies.toArray(sm_fontFamilies);
        sm_constantLoaded = true;
    }

    public FontCombobox()
    {
        loadConstant();
        this.setRenderer(new FontCellRenderer());

        m_family = "SansSerif";
        m_size = 14;
        m_color = sm_colors[0];
    }

    public void setListWitch(ListWhich listWhich)
    {
        if (m_listWhich == listWhich) {
            return;
        }

        m_listWhich = listWhich;
        removeAllItems();

        Object items[] = null;

        switch (m_listWhich) {
            case list_family:
                items = sm_fontFamilies;
                break;
            case list_size:
                items = sm_fontSizes;
                break;
            case list_color:
                items = sm_colors;
                break;
        }

        for (Object item : items) {
            addItem(item);
        }
    }

    void setProperties(String family, int size, Color color)
    {
        m_family = family;
        m_size = size;
        m_color = color;

        switch (m_listWhich) {
            case list_family:
                setSelectedItem(family);
                break;
            case list_size:
                setSelectedItem(size);
                break;
            case list_color:
                setSelectedItem(color);
                break;
        }
    }

    class FontCellRenderer implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);

            String family = null;
            Integer size = 0;
            Color color = null;
            String text = null;

            switch (m_listWhich) {
                case list_family:
                    family = (String) value;
                    text = family;
                    size = m_size;
                    color = m_color;
                    break;

                case list_size:
                    size = (Integer)value;
                    text = size.toString();
                    family = m_family;
                    color = m_color;
                    break;

                case list_color:
                    color = (Color)value;
                    text = color.toString();
                    family = m_family;
                    size = m_size;
                    break;
            }

            if (cellHasFocus) {
                color = renderer.getForeground();
            }

            Font font = FontLib.getFont(family, size);

            renderer.setFont(font);
            renderer.setText(text);
            renderer.setForeground(color);

            return renderer;
        }
    }
}
