package eulermind.component;

import eulermind.Style;

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

public class IconButton extends JButton implements MindPropertyComponent {

    private final int m_iconSize = 27;
    private String m_iconName;
    String m_mindPropertyName;

    public IconButton() {
        addActionListener(m_updateMindNodeAction);
        setIcon(new EmptyIcon(16, 16));
    }

    ActionListener m_updateMindNodeAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            MindIconDialog mindIconDialog = new MindIconDialog(IconButton.this);
            mindIconDialog.setModal(true);
            mindIconDialog.setVisible(true);

            String result = mindIconDialog.getResult();

            if (result != null) {
                m_iconName = (result == MindIconDialog.REMOVE_ICON_NAME) ? null : result;

                firePropertyChange(m_mindPropertyName, null, m_iconName);
                updateButtonIcon();
            }
        }
    };

    private void updateButtonIcon()
    {
        if (m_iconName != null) {
            setIcon(Style.getImageIcon(m_iconName));
        } else {
            setIcon(new EmptyIcon(16, 16));
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
        m_iconName = (String)value;
        if (value == null) {
            updateButtonIcon();
        } else {
            updateButtonIcon();
        }
    }

    @Override
    public String getMindPropertyValue()
    {
        return m_iconName;
    }

    public final class EmptyIcon implements Icon {

        private int width;
        private int height;

        public EmptyIcon() {
            this(0, 0);
        }

        public EmptyIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.white);
            g.drawRoundRect(x, y, width, height, 2, 2);
        }

    }
}
