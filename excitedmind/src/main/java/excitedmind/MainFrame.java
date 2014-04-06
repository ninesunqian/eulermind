package excitedmind;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;

public class MainFrame extends JFrame {
        public MainFrame(String dbUrl, Object rootId)
        {

            setLocationByPlatform(true);
            addMenu();

            Color BACKGROUND = Color.WHITE;
            Color FOREGROUND = Color.BLACK;

            //JPanel panel = new JPanel(new BorderLayout());
            //panel.setBackground(BACKGROUND);
            //panel.setForeground(FOREGROUND);


            final MindModel mindModel = new MindModel(dbUrl);
            final JTabbedPane tabbedPane = new JTabbedPane();
            final MindController mindController = new MindController(mindModel, tabbedPane);

            add(new MindToolBar(mindController), BorderLayout.NORTH);

            mindController.findOrAddMindView(rootId);
            tabbedPane.add("aaaa", new TextArea());
            //mindController.findOrAddMindView(Mindmap.m_rootVertex1.getId());

            tabbedPane.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    Component comp = tabbedPane.getSelectedComponent();
                    comp.requestFocus();
                }
            });

            tabbedPane.getComponentAt(0).requestFocus();
            //switch tab0 using ALT_1
            tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

            add(tabbedPane, BorderLayout.CENTER);

            //panel.add(box, BorderLayout.SOUTH);

            //MindIcons mindIcons = new MindIcons(mindController);
            //add(mindIcons.getToolbar(), BorderLayout.WEST);
            //add(panel);
        }

    void addMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("文件");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);

        JMenuItem openMenuItem = new JMenuItem("open", KeyEvent.VK_O);
        fileMenu.add(openMenuItem);

        setJMenuBar(menuBar);

    }
}