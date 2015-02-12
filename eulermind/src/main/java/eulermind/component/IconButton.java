package eulermind.component;

import eulermind.Style;
import prefuse.util.ColorLib;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorChooserComponentFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;

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

public class IconButton extends JButton implements PropertyComponent {


    private final int m_iconSize = 27;
    private String m_iconName;

    public IconButton() {
        addActionListener(m_updateMindNodeAction);
    }

    ActionListener m_updateMindNodeAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            MindIconDialog mindIconDialog = new MindIconDialog(null);
            mindIconDialog.setModal(true);
            mindIconDialog.setVisible(true);

            String result = mindIconDialog.getResult();

            if (result != null) {
                m_iconName = (result == MindIconDialog.REMOVE_ICON_NAME) ? null : result;
                m_propertyComponentConnector.updateMindNode(m_iconName);
                updateButtonIcon();
            }
        }
    };

    private void updateButtonIcon()
    {
        if (m_iconName != null) {
            setIcon(Style.getImageIcon(m_iconName));
        } else {
            setIcon(null);
        }
    }

    @Override
    public void setValue(Object value)
    {
        m_iconName = (String)value;
        if (value == null) {
            updateButtonIcon();
        } else {
            updateButtonIcon();
        }
    }

    @Override
    public String getValue()
    {
        return m_iconName;
    }

    @Override
    public void setPropertyComponentConnector(PropertyComponentConnector propertyComponentConnector)
    {
        m_propertyComponentConnector = propertyComponentConnector;
    }

    PropertyComponentConnector m_propertyComponentConnector;
}
