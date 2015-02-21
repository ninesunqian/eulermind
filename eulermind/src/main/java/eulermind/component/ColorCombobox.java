package eulermind.component;

import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
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

public class ColorCombobox extends JComboBox implements MindPropertyComponent {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    boolean m_forBackground = false;
    Color m_anotherColor;
    String m_mindPropertyName;

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

        /*FIXME: how to change background of editor
        JTextField editor = (JTextField)getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                int debug = 1;
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                int debug = 1;
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                int debug = 1;
                //To change body of implemented methods use File | Settings | File Templates.
            }
        }); */
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
                firePropertyChange(m_mindPropertyName, null, getMindPropertyValue());
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
        return (Integer)getSelectedItem() ;
    }
}
