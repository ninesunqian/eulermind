package eulermind.component;

import eulermind.MindController;
import eulermind.MindModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

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

public class MindIconToolBar extends JToolBar {
    private MindController m_mindController;

    private static String sm_iconDir = MindIconToolBar.class.getClassLoader().getResource("icons/").getPath();

    private static String sm_iconsList = "remove;idea;help;yes;messagebox_warning;stop-sign;closed;info;button_ok;button_cancel;"
            + "full-1;full-2;full-3;full-4;full-5;full-6;full-7;full-8;full-9;full-0;"
            + "stop;prepare;go;back;forward;up;down;attach;ksmiletris;"
            + "smiley-neutral;smiley-oh;smiley-angry;smily_bad;clanbomber;desktop_new;gohome;"
            + "folder;korn;Mail;kmail;list;edit;kaddressbook;knotify;password;pencil;wizard;xmag;bell;bookmark;"
            + "penguin;licq;freemind_butterfly;broken-line;calendar;clock;hourglass;launch;"
            + "flag-black;flag-blue;flag-green;flag-orange;flag-pink;flag;flag-yellow;family;"
            + "female1;female2;male1;male2;fema;group";

    public static String getIconPath(String name)
    {
        return name == null ? null : sm_iconDir + "/" + name;
    }

    public MindIconToolBar()
    {
        super();

		setOrientation(JToolBar.VERTICAL);

        String icon_files[] = sm_iconsList.split(";");
        for (String icon_file : icon_files){
            addIcon(icon_file + ".png");
        }
    }

    public void setMindController(MindController mindController)
    {
        m_mindController = mindController;
    }

    private void addIcon(final String name)
    {
        ImageIcon imageIcon = new ImageIcon(getIconPath(name));
        AbstractAction action = new AbstractAction(name, imageIcon) {
            public void actionPerformed(ActionEvent event) {
                m_mindController.getCurrentView().setCursorProperty(
                        MindModel.sm_iconPropName, name.equals("remove.png") ? null : name);
            }
        };

		add(action);
    }
}
