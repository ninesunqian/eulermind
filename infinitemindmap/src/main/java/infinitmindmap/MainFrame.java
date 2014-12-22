package infinitmindmap;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainFrame extends JFrame {
    static Logger m_logger = LoggerFactory.getLogger(Mindmap.class);

    MindModel m_mindModel;
    MindController m_mindController;
    MindIcons m_mindIcons;
    MindToolBar m_mindToolBar;
    JTabbedPane m_tabbedPane = new JTabbedPane();

    public MainFrame(String dbUrl)
    {
        setLocationByPlatform(true);
        addMenu();

        m_mindModel = new MindModel(dbUrl);

        InputMap map;
        map = (InputMap) UIManager.get("TabbedPane.ancestorInputMap");
        KeyStroke keyStrokeCtrlUp = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK);
        map.remove(keyStrokeCtrlUp);

        //m_tabbedPane.setFocusable(false);

        m_tabbedPane = new JTabbedPane();

        m_mindController = new MindController(m_mindModel, m_tabbedPane);
        add(m_tabbedPane, BorderLayout.CENTER);

        m_mindIcons = new MindIcons(m_mindController);
        add(m_mindIcons.getToolbar(), BorderLayout.WEST);

        /*
        Component comp = m_tabbedPane.getSelectedComponent();
        comp.requestFocusInWindow();
        */

        m_mindToolBar = new MindToolBar(m_mindController);
        add(m_mindToolBar, BorderLayout.NORTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                m_mindModel.m_mindDb.shutdown();
                MainFrame.this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });

        addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                Component comp = m_tabbedPane.getSelectedComponent();
                comp.requestFocusInWindow();
            }
        });
    }

    void addMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("file");
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);

        JMenuItem importMenuItem = new JMenuItem("import", KeyEvent.VK_O);
        fileMenu.add(importMenuItem);
        importMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                m_mindController.getCurrentView().importFile();
            }

        });

        addFavoriteMenu(menuBar);
        addAncestorsMenu(menuBar);

        setJMenuBar(menuBar);
    }

    JMenu m_favoriteMenu;

    void addFavoriteMenu(JMenuBar menuBar)
    {
        m_favoriteMenu = new JMenu("Favorite");
        m_favoriteMenu.setMnemonic('f');

        m_favoriteMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent menuEvent)
            {
                final Object currentVertexId = m_mindController.getCurrentVertexId();
                JMenuItem addingMenuItem = new JMenuItem("add to favorite");
                addingMenuItem.setEnabled(currentVertexId != null && ! m_mindModel.isInFavorite(currentVertexId));
                addingMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        m_mindModel.addToFavorite(currentVertexId);
                    }
                });
                m_favoriteMenu.add(addingMenuItem);

                JMenuItem removingMenuItem = new JMenuItem("remove from favorite");
                removingMenuItem.setEnabled(currentVertexId != null && m_mindModel.isInFavorite(currentVertexId));
                removingMenuItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        m_mindModel.removeFromFavorite(currentVertexId);
                    }
                });
                m_favoriteMenu.add(removingMenuItem);

                m_favoriteMenu.addSeparator();

                for (final MindModel.VertexBasicInfo vertexBasicInfo : m_mindModel.m_favoriteInfoes) {
                    JMenuItem menuItem = new JMenuItem(vertexBasicInfo.m_contextText);
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent)
                        {
                            m_mindController.findOrAddMindView(vertexBasicInfo.m_dbId);
                        }
                    });

                    m_favoriteMenu.add(menuItem);
                }
            }

            @Override
            public void menuDeselected(MenuEvent menuEvent)
            {
                m_favoriteMenu.removeAll();
            }

            @Override
            public void menuCanceled(MenuEvent menuEvent)
            {
                m_favoriteMenu.removeAll();
            }
        }

        );

        menuBar.add(m_favoriteMenu);
    }

    void addAncestorsMenu(JMenuBar menuBar)
    {
        m_favoriteMenu = new JMenu("Ancestors");
        m_favoriteMenu.setMnemonic('a');

        m_favoriteMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent menuEvent)
            {
                final Object currentVertexId = m_mindController.getCurrentVertexId();
                MindModel.VertexBasicInfo vertexBasicInfo =  m_mindModel.getVertexBasicInfo(currentVertexId);

                for (Object ancestor : vertexBasicInfo.m_inheritPath) {
                    final MindModel.VertexBasicInfo ancestorBasicInfo = m_mindModel.getVertexBasicInfo(ancestor);
                    JMenuItem menuItem = new JMenuItem(ancestorBasicInfo.m_contextText);
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent)
                        {
                            m_mindController.findOrAddMindView(ancestorBasicInfo.m_dbId);
                        }
                    });

                    m_favoriteMenu.add(menuItem);
                }
            }

            @Override
            public void menuDeselected(MenuEvent menuEvent)
            {
                m_favoriteMenu.removeAll();
            }

            @Override
            public void menuCanceled(MenuEvent menuEvent)
            {
                m_favoriteMenu.removeAll();
            }
        }

        );

        menuBar.add(m_favoriteMenu);
        //弹出菜单时再显示菜单
    }
}