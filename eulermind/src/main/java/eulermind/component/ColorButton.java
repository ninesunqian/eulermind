package eulermind.component;

import eulermind.Style;
import prefuse.util.ColorLib;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorChooserComponentFactory;
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

public class ColorButton extends JButton implements PropertyComponent {

    private JColorChooser m_colorChooser = new JColorChooser();
    private Color m_color;

    private boolean m_choosed;

    boolean m_forBackground = false;
    JDialog m_dialog;


    public ColorButton() {
        addActionListener(m_updateMindNodeAction);

        AbstractColorChooserPanel panels[] = ColorChooserComponentFactory.getDefaultChooserPanels();
        m_colorChooser.setChooserPanels(panels);

        m_dialog = JColorChooser.createDialog(ColorButton.this.getTopLevelAncestor(),
                "", true, m_colorChooser,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        m_color = m_colorChooser.getColor();
                        m_choosed = true;
                    }
                },
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        m_choosed = false;
                    }
                });


        JButton button = new JButton();
        button.setText("style default color");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                m_color = null;
                m_choosed = true;
                m_dialog.setVisible(false);
            }
        });
        Container contentPane = m_dialog.getContentPane();
        contentPane.add(button, BorderLayout.NORTH);

    }

    public void setForBackground(boolean forBackground) {
        m_forBackground = forBackground;
    }

    ActionListener m_updateMindNodeAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {

            m_dialog.setVisible(true);
            //如何区分去掉颜色，和取消选择
            if (m_choosed) {
                m_propertyComponentConnector.updateMindNode(m_color == null ? null : (Integer)m_color.getRGB());
                updateButtonColor();
            }
        }
    };

    private void updateButtonColor()
    {
        Color color = m_color;

        if (! m_forBackground) {
            if (color == null) {
                color = ColorLib.getColor(Style.sm_defaultTextColor);
            }
            setForeground(color);
        } else {
            if (color == null) {
                color = ColorLib.getColor(Style.sm_defaultNodeColor);
            }
            setBackground(color);
            setOpaque(true);
        }
    }

    @Override
    public void setPropertyValue(Object value)
    {
        if (value == null) {
            m_color = null;
        } else {
            m_color = ColorLib.getColor((Integer)value);
        }
        updateButtonColor();
    }

    @Override
    public void setPropertyComponentConnector(PropertyComponentConnector propertyComponentConnector)
    {
        m_propertyComponentConnector = propertyComponentConnector;
    }

    PropertyComponentConnector m_propertyComponentConnector;
}
