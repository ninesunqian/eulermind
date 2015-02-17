package eulermind.component;

import eulermind.MindModel;
import eulermind.Style;
import prefuse.util.ColorLib;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.colorchooser.ColorChooserComponentFactory;
import javax.swing.text.SimpleAttributeSet;
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


        JButton defaultColorButton = new JButton();
        defaultColorButton.setText("style default color");
        defaultColorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                m_color = null;
                m_choosed = true;
                m_dialog.setVisible(false);
            }
        });
        Container contentPane = m_dialog.getContentPane();
        contentPane.add(defaultColorButton, BorderLayout.NORTH);
    }

    protected void paintComponent(Graphics g) {
        Color forceGround = ColorLib.getColor(Style.getTextColorSurely(Style.DEFAULT_STYLE_NAME));
        Color backGround = ColorLib.getColor(Style.getNodeColorSurely(Style.DEFAULT_STYLE_NAME));

        if (! m_forBackground) {
            if (m_color != null) {
                forceGround = m_color;
            }
        } else {
            if (m_color != null) {
                backGround = m_color;
            }
        }

        if (getModel().isPressed()) {
            g.setColor(backGround.darker());
        } else if (getModel().isRollover()) {
            g.setColor(backGround.brighter());
        } else {
            g.setColor(backGround);
        }
        g.fillRect(0, 0, getWidth(), getHeight());

        Dimension d = getSize();
        FontMetrics fm = g.getFontMetrics();

        String text = getText();
        int x = (d.width - fm.stringWidth(text)) / 2;
        int y = (d.height + fm.getAscent()) / 2;

        g.setColor(forceGround);
        g.drawString(text, x, y);
    }

   @Override
    public void setContentAreaFilled(boolean b) {
    }

    public void setForBackground(boolean forBackground) {
        m_forBackground = forBackground;
    }

    ActionListener m_updateMindNodeAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            m_dialog.setVisible(true);
            if (m_choosed) {
                if (m_propertyComponentConnector != null) {
                    m_propertyComponentConnector.updateMindNode(getValue());
                }
            }
        }
    };

    @Override
    public void setValue(Object value)
    {
        if (value == null) {
            m_color = null;
        } else {
            m_color = ColorLib.getColor((Integer)value);
        }
        repaint();
    }

    @Override
    public Integer getValue()
    {
        return m_color == null ? null : m_color.getRGB();
    }

    @Override
    public void setPropertyComponentConnector(PropertyComponentConnector propertyComponentConnector)
    {
        m_propertyComponentConnector = propertyComponentConnector;
    }

    PropertyComponentConnector m_propertyComponentConnector;
}
