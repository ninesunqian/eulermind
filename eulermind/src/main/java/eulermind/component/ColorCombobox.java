package eulermind.component;

import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class ColorCombobox extends JComboBox implements PropertyComponent {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    boolean m_forBackground = false;
    Color m_anotherColor;

    public ColorCombobox()
    {
        setRenderer(new ColorCellRenderer());

        Integer[] defaultColorValues = Style.getColors();

        addItem(null);

        for (Integer item : defaultColorValues) {
            addItem(item | 0xFF000000);
        }

        addItem(0);

        addActionListener(m_updateMindNodeAction);
    }

    ActionListener m_updateMindNodeAction = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (getSelectedIndex() == getItemCount() - 1) {
                Color newColor = JColorChooser.showDialog(ColorCombobox.this, "select color", null);
                if (newColor != null) {
                    insertItemAt(newColor.getRGB(), getItemCount() - 1);
                    setSelectedIndex(getItemCount() - 2);
                }

            } else {
                if (m_propertyComponentConnector != null) {
                    m_propertyComponentConnector.updateMindNode(getValue());
                }
            }
        }
    };

    class ColorCellRenderer implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel colorCellComponent = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);

            Integer colorValue = (Integer) value;
            String text;

            if (index == getItemCount() - 1) {
                colorCellComponent.setText("add color");
                return colorCellComponent;
            }

            if (colorValue == null) {
                colorCellComponent.setText("default");
                return colorCellComponent;
            }

            Color color = new Color(colorValue);

            if (!m_forBackground) {
                colorCellComponent.setForeground(color);
                if (m_anotherColor != null) {
                    colorCellComponent.setBackground(color);
                    colorCellComponent.setOpaque(true);
                }
            } else {
                colorCellComponent.setBackground(color);
                colorCellComponent.setOpaque(true);

                if (m_anotherColor != null) {
                    colorCellComponent.setForeground(m_anotherColor);
                }
            }

            text = String.format("#%06x", colorValue & 0xFFFFFF);
            colorCellComponent.setText(text);
            return colorCellComponent;
        }
    }

    public void setForBackground(Boolean forBackground)
    {
        m_forBackground = forBackground;
    }

    @Override
    public void setValue(Object value)
    {
        setSelectedItem(value);
    }

    @Override
    public Integer getValue()
    {
        return (Integer)getSelectedItem() ;
    }

    @Override
    public void setPropertyComponentConnector(PropertyComponentConnector propertyComponentConnector)
    {
        m_propertyComponentConnector = propertyComponentConnector;
    }

    PropertyComponentConnector m_propertyComponentConnector;
}
