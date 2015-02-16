package eulermind.component;

import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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

public class StyleList extends JList implements PropertyComponent {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    DefaultListModel<String> m_listMode = new DefaultListModel<String>();

    public StyleList()
    {
        this.setCellRenderer(new StyleCellRenderer());

        m_listMode.addElement(null);
        for (String styleName : Style.getStyleNames()) {
            if (styleName != "default") {
                m_listMode.addElement(styleName);
            }
        }
        this.setModel(m_listMode);
    }

    MouseListener mouseListenerForUpdatingMindNode = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {
            int index = locationToIndex(mouseEvent.getPoint());
            if (index >= 0) {
                Object value = m_listMode.getElementAt(index);
                if (m_propertyComponentConnector != null) {
                    m_propertyComponentConnector.updateMindNode(value);
                }
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

            String styleName = (value == null) ? "default" : (String)value;

            String family = Style.getFontFamilySurely(styleName);
            Integer size = Style.getFontSizeSurely(styleName);

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
    public String getValue()
    {
        return (String)getSelectedValue();
    }

    public void removeSelectedStyle() {
        String styleName = getValue();

        if (styleName == Style.DEFAULT_STYLE_NAME) {
            return;
        }

        Style.removeStyle(styleName);
        m_listMode.removeElement(styleName);
    }

    static void copyStyle(Style from, Style to)
    {
        to.m_fontFamily = from.m_fontFamily;
        to.m_fontSize = from.m_fontSize;
        to.m_bold = from.m_bold;
        to.m_italic = from.m_italic;
        to.m_nodeColor = from.m_nodeColor;
        to.m_textColor = from.m_textColor;
        to.m_icon = from.m_icon;
        to.m_name = from.m_name;
    }

    public void editSelectedStyle() {
        String styleName = getValue();
        Style style = Style.getStyle(styleName);
        int index = Style.getStyleIndex(style);

        Style retStyle = StyleEditorDialog.showDialog(this, style);

        if (retStyle == null) {
            return;
        }

        if (!retStyle.m_name.equals(style.m_name) && Style.hasStyle(retStyle.m_name)) {
            JOptionPane.showMessageDialog(this, "style name is same as other style");
            return;
        }

        copyStyle(retStyle, style);
        m_listMode.set(index, style.m_name);
    }

    public void upSelectedStyle() {
        int index = getSelectedIndex();
        if (index == 0) {
            return;
        }

        String styleName = getValue();
        Style.moveStyle(styleName, -1);

        String upperStyleName = m_listMode.get(index - 1);
        m_listMode.set(index - 1, styleName);
        m_listMode.set(index, upperStyleName);
    }

    public void downSelectedStyle() {
        int index = getSelectedIndex();
        if (index == m_listMode.size() - 1) {
            return;
        }

        String styleName = getValue();
        Style.moveStyle(styleName, 1);

        String downerStyleName = m_listMode.get(index + 1);
        m_listMode.set(index + 1, styleName);
        m_listMode.set(index, downerStyleName);
    }

    public void newStyle() {
        Integer name_postfix = 0;
        String name = "new Style";
        while (Style.hasStyle(name)) {
            name = "new style" + name_postfix.toString();
            name_postfix++;
        }

        Style style = new Style(name);
        Style retStyle = StyleEditorDialog.showDialog(this, style);

        if (retStyle == null) {
            return;
        }

        if (Style.hasStyle(retStyle.m_name)) {
            JOptionPane.showMessageDialog(this, "style name is same as other style");
        } else {
            Style.addStyle(retStyle);
            m_listMode.addElement(retStyle.m_name);
        }
    }

    @Override
    public void setValue(Object value)
    {
        int index = m_listMode.indexOf(value);
        if (index >= 0) {
            setSelectedIndex(index);
        }
    }

    @Override
    public void setPropertyComponentConnector(PropertyComponentConnector propertyComponentConnector)
    {
        m_propertyComponentConnector = propertyComponentConnector;
    }

    PropertyComponentConnector m_propertyComponentConnector;

}
