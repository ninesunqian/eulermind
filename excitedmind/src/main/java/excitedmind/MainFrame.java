package excitedmind;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    MindModel m_mindModel;
    MindController m_mindController;
    public MainFrame(String dbUrl)
    {

        setLocationByPlatform(true);
        addMenu();

        Color BACKGROUND = Color.WHITE;
        Color FOREGROUND = Color.BLACK;

        //JPanel panel = new JPanel(new BorderLayout());
        //panel.setBackground(BACKGROUND);
        //panel.setForeground(FOREGROUND);


        m_mindModel = new MindModel(dbUrl);
        final JTabbedPane tabbedPane = new JTabbedPane();
        m_mindController = new MindController(m_mindModel, tabbedPane);

        add(new MindToolBar(m_mindController), BorderLayout.NORTH);

        m_mindController.findOrAddMindView(m_mindModel.m_mindDb.getRootId());
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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                m_mindModel.m_mindDb.shutdown();
                //mindModel.m_mindDb.
                MainFrame.this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });
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

    void addPinMenu(JMenuBar menuBar)
    {
        JMenu pinMenu = new JMenu("Pin");
        //弹出菜单时再显示菜单
    }
}