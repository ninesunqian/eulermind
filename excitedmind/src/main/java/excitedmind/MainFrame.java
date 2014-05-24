package excitedmind;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

public class MainFrame extends JFrame {
    static Logger m_logger = Logger.getLogger(Mindmap.class.getName());

    MindModel m_mindModel;
    MindController m_mindController;
    public MainFrame(String dbUrl)
    {
        setLocationByPlatform(true);
        addMenu();

        m_mindModel = new MindModel(dbUrl);

        InputMap map;
        map = (InputMap) UIManager.get("TabbedPane.ancestorInputMap");
        KeyStroke keyStrokeCtrlUp = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK);
        map.remove(keyStrokeCtrlUp);

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);

        m_mindController = new MindController(m_mindModel, tabbedPane);

        add(new MindToolBar(m_mindController), BorderLayout.NORTH);

        m_mindController.findOrAddMindView(m_mindModel.m_mindDb.getRootId());
        //tabbedPane.add("aaaa", new TextArea());
        //mindController.findOrAddMindView(Mindmap.m_rootVertex1.getId());

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Component comp = tabbedPane.getSelectedComponent();
                comp.requestFocusInWindow();
            }
        });

        //switch tab0 using ALT_1
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        add(tabbedPane, BorderLayout.CENTER);

        /*
        Component textArea = new TextArea();
        textArea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent)
            {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void keyPressed(KeyEvent keyEvent)
            {
                s_logger.info("text area key " + keyEvent.toString());
            }

            @Override
            public void keyReleased(KeyEvent keyEvent)
            {
                s_logger.info("text area key released " + keyEvent.toString());
            }
        });
        tabbedPane.addTab("text", textArea);
        */

        Component comp = tabbedPane.getSelectedComponent();
        comp.requestFocusInWindow();


        MindIcons mindIcons = new MindIcons(m_mindController);
        add(mindIcons.getToolbar(), BorderLayout.WEST);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                m_mindModel.m_mindDb.shutdown();
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