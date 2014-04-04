package excitedmind;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;

public class MainFrame extends JFrame {
        public MainFrame(String dbUrl, Object rootId)
        {
            java.util.PropertyResourceBundle


            setLocationByPlatform(true);
            addMenu();

            Color BACKGROUND = Color.WHITE;
            Color FOREGROUND = Color.BLACK;

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(BACKGROUND);
            panel.setForeground(FOREGROUND);


            final MindModel mindModel = new MindModel(dbUrl);
            final JTabbedPane tabbedPane = new JTabbedPane();
            final MindController mindController = new MindController(mindModel, tabbedPane);
            mindController.findOrAddMindView(rootId);
            //mindController.findOrAddMindView(Mindmap.m_rootVertex1.getId());
            tabbedPane.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e)
                {
                    Component comp = tabbedPane.getSelectedComponent();
                    comp.requestFocusInWindow();
                }
            });
            panel.add(tabbedPane, BorderLayout.CENTER);

            Box box = new Box(BoxLayout.X_AXIS);
            box.add(Box.createHorizontalStrut(10));
            box.add(Box.createHorizontalGlue());
            box.add(Box.createHorizontalStrut(3));
            box.setBackground(BACKGROUND);
            //panel.add(box, BorderLayout.SOUTH);


            MindIcons mindIcons = new MindIcons(mindController);
            // panel.add(mindIcons.getToolbar(), BorderLayout.WEST);
            panel.add(new MindToolBar(mindModel), BorderLayout.NORTH);
            add(panel);
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