package eulermind.component;

import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import prefuse.util.ColorLib;
import prefuse.util.FontLib;

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

public class StyleList extends JList implements MindPropertyComponent {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    DefaultListModel<String> m_listMode = new DefaultListModel<String>();
    String m_mindPropertyName;

    public StyleList()
    {
        this.setCellRenderer(new StyleCellRenderer());

        for (String styleName : Style.getStyleNames()) {
            m_listMode.addElement(styleName);
        }
        this.setModel(m_listMode);
        setUpdatingMindNodeEnabled(true);
    }

    MouseListener mouseListenerForUpdatingMindNode = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {
            int index = locationToIndex(mouseEvent.getPoint());
            if (index >= 0) {
                Object value = m_listMode.getElementAt(index);
                if (value == "default") {
                    value = null;
                }

                firePropertyChange(m_mindPropertyName, null, value);
            }
        }
    };

    public void setUpdatingMindNodeEnabled(boolean enabled)
    {
        if (enabled) {
            this.addMouseListener(mouseListenerForUpdatingMindNode);
        } else {
            this.removeMouseListener(mouseListenerForUpdatingMindNode);
        }
    }

    class StyleCellRenderer implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel listCellRendererComponent = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);

            String styleName = (String)value;

            String family = Style.getFontFamilySurely(styleName);
            Integer size = Style.getFontSizeSurely(styleName);

            if (styleName == null) {
                int debug =1;
            }
            boolean bold = Style.getBoldSurely(styleName);
            boolean italic = Style.getItalicSurely(styleName);

            Integer textColorValue = Style.getTextColorSurely(styleName);
            Integer nodeColorValue = Style.getNodeColorSurely(styleName);

            String icon = Style.getIconSurely(styleName);

            Font font = FontLib.getFont(family, bold, italic, size);

            listCellRendererComponent.setFont(font);
            listCellRendererComponent.setText(styleName);
            listCellRendererComponent.setForeground(ColorLib.getColor(textColorValue));
            listCellRendererComponent.setBackground(ColorLib.getColor(nodeColorValue));
            listCellRendererComponent.setOpaque(true);

            int borderThickness = 3;

            if (isSelected) {
                listCellRendererComponent.setBorder(BorderFactory.createLineBorder(Color.blue, borderThickness));
            } else {
                listCellRendererComponent.setBorder(BorderFactory.createEmptyBorder(
                        borderThickness, borderThickness, borderThickness, borderThickness));
            }

            if (icon != null) {
                listCellRendererComponent.setIcon(Style.getImageIcon(icon));
            }

            return listCellRendererComponent;
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
    public String getMindPropertyValue()
    {
        String valueInList = getValueInList();
        return (valueInList == "default") ? null : valueInList;
    }

    public String getValueInList()
    {
        return (String)getSelectedValue();
    }

    private void updateList(String selectedName)
    {
        int prevSelectedIndex = getSelectedIndex();
        m_listMode.clear();
        for (String styleName : Style.getStyleNames()) {
            m_listMode.addElement(styleName);
        }

        if (selectedName == null) {
            if (prevSelectedIndex >= m_listMode.size() - 1) {
                setSelectedIndex(m_listMode.size() - 1);
            } else {
                setSelectedIndex(prevSelectedIndex);
            }
        } else {
            setSelectedValue(selectedName, true);
        }
    }

    public void removeSelectedStyle() {
        String styleName = getValueInList();

        if (styleName == Style.DEFAULT_STYLE_NAME) {
            return;
        }

        Style.removeStyle(styleName);
        updateList(null);
        Style.save();
    }

    public void editSelectedStyle() {
        String styleName = getValueInList();
        Style style = Style.getStyle(styleName);

        StyleEditorDialog.editStyle(this, style);
        updateList(style.m_name);
        Style.save();
    }

    public void upSelectedStyle() {
        int index = getSelectedIndex();
        if (index == 0) {
            return;
        }

        String styleName = getValueInList();
        Style.moveStyle(styleName, -1);
        updateList(styleName);
        Style.save();
    }

    public void downSelectedStyle() {
        int index = getSelectedIndex();
        if (index == m_listMode.size() - 1) {
            return;
        }

        String styleName = getValueInList();
        Style.moveStyle(styleName, 1);
        updateList(styleName);
        Style.save();
    }

    public void newStyle() {
        Style style = Style.newStyle();
        StyleEditorDialog.editStyle(this, style);
        updateList(style.m_name);
        Style.save();
    }

    @Override
    public void setMindPropertyValue(Object value)
    {
        if (value == null) {
            value = "default";
        }

        int index = m_listMode.indexOf(value);
        if (index >= 0) {
            setSelectedIndex(index);
        }
    }
}
