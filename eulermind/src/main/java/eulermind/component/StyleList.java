package eulermind.component;

import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

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

        for (String styleName : Style.getStyleNames()) {
            m_listMode.addElement(styleName);
        }
        this.setModel(m_listMode);
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

            if (icon != null) {
                listCellRendererComponent.setIcon(Style.getImageIcon(icon));
            }

            return listCellRendererComponent;
        }
    }

    @Override
    public void setPropertyValue(Object value)
    {
    }

    @Override
    public void setPropertyComponentConnector(PropertyComponentConnector propertyComponentConnector)
    {
        m_propertyComponentConnector = propertyComponentConnector;
    }

    PropertyComponentConnector m_propertyComponentConnector;
}
