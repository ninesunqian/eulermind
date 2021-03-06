package eulermind.component;

import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prefuse.util.FontLib;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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

public class FontSizeCombobox extends JComboBox implements MindPropertyComponent {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    String m_mindPropertyName;

    public FontSizeCombobox()
    {
        setRenderer(new FontCellRenderer());

        ArrayList<Integer> fontSizes = Style.getFontSizes();

        addItem(null);
        for (Object item : fontSizes) {
            addItem(item);
        }

        addActionListener(m_updateMindNodeAction);
    }

    ActionListener m_updateMindNodeAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            firePropertyChange(m_mindPropertyName, null, getMindPropertyValue());
        }
    };

    class FontCellRenderer implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);

            Integer fontSize = (Integer) value;

            if (fontSize == null) {
                fontSize = 16;
            }

            Font font = FontLib.getFont("SansSerif", fontSize);

            renderer.setFont(font);

            if (value == null) {
                renderer.setText("default");
            } else {
                renderer.setText(fontSize.toString());
            }

            return renderer;
        }
    }


    @Override
    public void setMindPropertyName(String propertyName)
    {
        m_mindPropertyName = propertyName;
    }

    @Override
    public String getMindPropertyName()
    {
        return m_mindPropertyName;
    }

    @Override
    public void setMindPropertyValue(Object value)
    {
        setSelectedItem(value);
    }

    @Override
    public Integer getMindPropertyValue()
    {
        return (Integer)getSelectedItem();
    }
}
