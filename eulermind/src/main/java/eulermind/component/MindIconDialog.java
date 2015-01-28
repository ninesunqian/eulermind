package eulermind.component;

import eulermind.MindController;
import eulermind.Style;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

public class MindIconDialog extends JDialog {
    private MindController m_mindController;

    private final int m_iconSize = 27;

    private String m_result;

    public final static String REMOVE_ICON_NAME = "remove";

    public MindIconDialog(Frame owner)
    {
        super(owner, "select icon", true);

		getContentPane().setLayout(new BorderLayout());

        JPanel iconPanel = new JPanel();
        getContentPane().add(iconPanel, BorderLayout.CENTER);

		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
                m_result = null;
                dispose();
			}
		});

        addIcons(iconPanel);
		getContentPane().add(iconPanel, BorderLayout.CENTER);

		getContentPane().add(createButton(REMOVE_ICON_NAME), BorderLayout.SOUTH);
		pack();
    }

    private void addIcons(JPanel iconPanel){

        String icons[] = Style.sm_iconsList.split(";");

        int iconColumn = new Double(Math.ceil(Math.sqrt(icons.length))).intValue();
        int iconRow = iconColumn * iconColumn == icons.length ? iconColumn : iconColumn + 1;

        Dimension dimension = new Dimension(iconColumn * m_iconSize, iconRow * m_iconSize);
        iconPanel.setPreferredSize(dimension);
        iconPanel.setMinimumSize(dimension);
        iconPanel.setMaximumSize(dimension);
        iconPanel.setSize(dimension);

        GridLayout gridlayout = new GridLayout(0, iconColumn);
        gridlayout.setHgap(3);
        gridlayout.setVgap(3);

        iconPanel.setLayout(gridlayout);

        for (int i = 0; i < icons.length; ++i) {
            iconPanel.add(createButton(icons[i]));
        };
    }

    private JButton createButton(final String iconName)
    {
        JButton button = new JButton();

        Icon icon = Style.getImageIcon(iconName);
        AbstractAction action = new AbstractAction(iconName, icon) {
            public void actionPerformed(ActionEvent event) {
                m_result = iconName;
                dispose();
            }
        };

        button.setAction(action);
        return button;
    }

    public String getResult() {
        return m_result;
    }
}
