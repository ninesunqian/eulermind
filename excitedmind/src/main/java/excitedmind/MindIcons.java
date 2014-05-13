package excitedmind;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class MindIcons {
    private MindController m_mindController;
    private JToolBar m_iconToolBar;
	private JScrollPane m_iconScrollPane;

    private static String sm_iconDir = MindIcons.class.getClassLoader().getResource("icons/").getPath();

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

    public MindIcons(MindController mindController)
    {
        m_mindController = mindController;
        m_iconToolBar = new JToolBar();
		m_iconScrollPane = new JScrollPane(m_iconToolBar);
		m_iconToolBar.setOrientation(JToolBar.VERTICAL);
		m_iconScrollPane.getVerticalScrollBar().setUnitIncrement(100);
        m_iconScrollPane.setPreferredSize(new Dimension(50, 0));



        String icon_files[] = sm_iconsList.split(";");
        for (String icon_file : icon_files){
            addIcon(icon_file + ".png");
        }
    }

    private void addIcon(final String name)
    {
        ImageIcon imageIcon = new ImageIcon(getIconPath(name));
        AbstractAction action = new AbstractAction(name, imageIcon) {
            public void actionPerformed(ActionEvent event) {
                m_mindController.getCurrentView().setCursorProperty(
                        MindModel.sm_iconPropName, name == "remove" ? null : name);
            }
        };

		m_iconToolBar.add(action);
    }

    public JComponent getToolbar()
    {
        return m_iconScrollPane;
    }
}
