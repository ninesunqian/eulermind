package excitedmind;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class MindIcons {
    private MindController m_mindController;
    private JToolBar m_iconToolBar;
	private JScrollPane m_iconScrollPane;

    private static String sm_iconDir = MindIcons.class.getClassLoader().getResource("icons/").getPath();

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
		m_iconToolBar.setRollover(true);
		m_iconScrollPane.getVerticalScrollBar().setUnitIncrement(100);

        File iconDir = new File(sm_iconDir);
        String icon_files[] = iconDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s)
            {
                return s.endsWith(".png") || s.endsWith(".PNG");
            }
        });

        for (String icon_file : icon_files){
            addIcon(icon_file);

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
