package eulermind;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import eulermind.component.*;
import eulermind.view.MindEditor;
import eulermind.view.MindView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swixml.SwingEngine;

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

public class MainFrame  extends JFrame {
    static Logger m_logger = LoggerFactory.getLogger(EulerMind.class);

    JMenu m_mindMapMenu;
    JMenu m_importMenu;

    JMenu m_ancestorMenu;
    JMenu m_favoriteMenu;

    MindModel m_mindModel;
    MindController m_mindController;
    StyleList m_styleList;
    JLabel m_tabInfoLabel;
    JTabbedPane m_tabbedPane;

    SwingEngine m_swingEngine;

    FontFamilyCombobox m_fontFamilyCombobox;
    FontSizeCombobox m_fontSizeCombobox;

    BooleanCombobox m_italicCombobox;
    BooleanCombobox m_boldCombobox;

    ColorCombobox m_textColorCombobox;
    ColorCombobox m_nodeColorCombobox;

    IconButton m_iconButton;

    JButton m_styleNewButton;
    JButton m_styleDeletingButton;
    JButton m_styleEditingButton;
    JButton m_styleUpButton;
    JButton m_styleDownButton;

    MindEditor m_searchInputer;

    JToolBar m_propertyToolBar;
    JCheckBoxMenuItem m_propertyToolBarVisibleCheckMenu;
    JCheckBoxMenuItem m_alwaysOnTopCheckMenu;

    String m_currentMapName;

    public MainFrame()
    {
        try {
            m_swingEngine = new SwingEngine(this);
            m_swingEngine.render("main_frame_layout.xml");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        InputMap map;
        map = (InputMap)UIManager.get("TabbedPane.ancestorInputMap");
        KeyStroke keyStrokeCtrlUp = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK);
        map.remove(keyStrokeCtrlUp);

        setLocationByPlatform(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeMindDb();
                MainFrame.this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });

        addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                Component comp = m_tabbedPane.getSelectedComponent();
                if (comp != null) {
                    comp.requestFocusInWindow();
                }
            }
        });

        m_mindMapMenu.addMenuListener(m_mapMenuListener);
        m_favoriteMenu.addMenuListener(m_favoriteMenuListener);
        m_ancestorMenu.addMenuListener(m_ancestorMenuListener);
        m_searchInputer.setMindEditorListener(searchInputerListener);

        m_nodeColorCombobox.setForBackground(true);
        m_textColorCombobox.setForBackground(false);

        m_styleNewButton.addActionListener(m_styleNewAction);
        m_styleDeletingButton.addActionListener(m_styleDeletingAction);
        m_styleEditingButton.addActionListener(m_styleEditingAction);
        m_styleUpButton.addActionListener(m_styleUpAction);
        m_styleDownButton.addActionListener(m_styleDownAction);

        m_propertyToolBarVisibleCheckMenu.addActionListener(m_propertyToolBarVisibleAction);
        m_alwaysOnTopCheckMenu.addActionListener(m_alwaysOnTopAction);

        if (Utils.isDebugging()) {
            openMindDb("debug");

        } else {

            String lastOpenedMap = Utils.getLastOpenedMap();
            if (lastOpenedMap != null) {
                openMindDb(lastOpenedMap);
            } else {
                openMindDb("example");
            }
        }
    }

    MindEditor.MindEditorListener searchInputerListener = new MindEditor.MindEditorListener() {
        public void promptListOk(Object dbId, String text, Object parentDBId, String parentText)
        {
            if (m_mindController.isChanging()) {
                return;
            }

            m_mindController.findOrAddMindView(dbId);
        }
    };

    public void openMindDb(String name)
    {
        if (m_currentMapName != null) {
            m_logger.error("must close current map, before open another");
            return;
        }

        String url = Utils.mindMapNameToUrl(name);
        m_mindModel = new MindModel(url);
        m_mindController = new MindController(m_mindModel, m_tabbedPane, m_tabInfoLabel);

        bindComponents();
        setComponentEnabled(true);

        m_currentMapName = name;

        //调试模式下，不记录最后一次打开脑图
        if (! Utils.isDebugging()) {
            Utils.recordLastOpenedMap(name);
        }
    }

    public void closeMindDb()
    {
        m_searchInputer.clearSearchResults();
        m_searchInputer.setText("");

        if (m_currentMapName == null) {
            return;
        }

        setComponentEnabled(false);
        unbindComponents();

        m_mindModel.close();
        m_mindModel = null;
        m_mindController = null;
        m_currentMapName = null;
    }

    private void bindComponents()
    {
        m_mindController.addMindPropertyComponent(MindModel.sm_fontFamilyPropName, m_fontFamilyCombobox);
        m_mindController.addMindPropertyComponent(MindModel.sm_fontSizePropName, m_fontSizeCombobox);
        m_mindController.addMindPropertyComponent(MindModel.sm_italicPropName, m_italicCombobox);
        m_mindController.addMindPropertyComponent(MindModel.sm_boldPropName, m_boldCombobox);
        m_mindController.addMindPropertyComponent(MindModel.sm_textColorPropName, m_textColorCombobox);
        m_mindController.addMindPropertyComponent(MindModel.sm_nodeColorPropName, m_nodeColorCombobox);
        m_mindController.addMindPropertyComponent(MindModel.sm_iconPropName, m_iconButton);

        m_mindController.addMindPropertyComponent(MindModel.STYLE_PROP_NAME, m_styleList);

        m_searchInputer.setMindDb(m_mindModel.m_mindDb);
        m_searchInputer.setHasPromptList(true);
    }

    private void unbindComponents()
    {
        m_tabbedPane.removeAll();

        m_mindController.removeMindPropertyComponent(MindModel.sm_fontFamilyPropName, m_fontFamilyCombobox);
        m_mindController.removeMindPropertyComponent(MindModel.sm_fontSizePropName, m_fontSizeCombobox);

        m_mindController.removeMindPropertyComponent(MindModel.sm_italicPropName, m_italicCombobox);
        m_mindController.removeMindPropertyComponent(MindModel.sm_boldPropName, m_boldCombobox);

        m_mindController.removeMindPropertyComponent(MindModel.sm_textColorPropName, m_textColorCombobox);
        m_mindController.removeMindPropertyComponent(MindModel.sm_nodeColorPropName, m_nodeColorCombobox);

        m_mindController.removeMindPropertyComponent(MindModel.sm_iconPropName, m_iconButton);
        m_mindController.removeMindPropertyComponent(MindModel.STYLE_PROP_NAME, m_styleList);

        m_mindController.removeMindPropertyComponent(MindModel.STYLE_PROP_NAME, m_styleList);
    }

    private void setComponentEnabled(boolean enabled)
    {
        m_ancestorMenu.setEnabled(enabled);
        m_favoriteMenu.setEnabled(enabled);
        m_importMenu.setEnabled(enabled);

        m_fontFamilyCombobox.setEnabled(enabled);
        m_fontSizeCombobox.setEnabled(enabled);

        m_italicCombobox.setEnabled(enabled);
        m_boldCombobox.setEnabled(enabled);

        m_textColorCombobox.setEnabled(enabled);
        m_nodeColorCombobox.setEnabled(enabled);

        m_iconButton.setEnabled(enabled);

        m_styleList.setEnabled(enabled);
        m_tabbedPane.setEnabled(enabled);

        m_styleNewButton.setEnabled(enabled);
        m_styleDeletingButton.setEnabled(enabled);
        m_styleEditingButton.setEnabled(enabled);
        m_styleUpButton.setEnabled(enabled);
        m_styleDownButton.setEnabled(enabled);

        m_searchInputer.setEnabled(enabled);
    }

    public Action m_importAction = new AbstractAction() {

        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            if (m_mindController.isChanging()) {
                return;
            }

            m_mindController.getCurrentView().importFileOrDirectory();
        }
    };

    private void addMenuSeparator(JMenu menu) {
        JSeparator sep = new JSeparator();
        sep.setPreferredSize(new Dimension(0, 3));
        sep.setBorder(BorderFactory.createLineBorder(Color.blue, 3));

        menu.add(sep);
    }

    MenuListener m_mapMenuListener = new MenuListener() {
        @Override
        public void menuSelected(MenuEvent e)
        {
            if (m_mindController.isChanging()) {
                return;
            }

            JMenuItem addingMenuItem = new JMenuItem("new map ...");
            addingMenuItem.setIcon(Utils.getAppIcon("new.png"));
            addingMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    String name = null;
                    while(true) {
                        name = JOptionPane.showInputDialog("input a map name, must alphabet, number or '_'", null);
                        if (name == null || name.matches("[a-zA-Z0-9_]+")) {
                            break;
                        } else {
                            JOptionPane.showMessageDialog(null, "name format error", null, JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    if (name != null && name != m_currentMapName) {
                        closeMindDb();
                        openMindDb(name);
                    }
                }
            });

            m_mindMapMenu.add(addingMenuItem);

            JMenuItem removingMenuItem = new JMenuItem("remove current map");
            removingMenuItem.setIcon(Utils.getAppIcon("delete.png"));
            removingMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    String inputedName = JOptionPane.showInputDialog("to confirm removing, please input the map name", null);
                    if (!inputedName.equals(m_currentMapName)) {
                        JOptionPane.showMessageDialog(null, "map name error, cancel removing", null,
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        int ret = JOptionPane.showConfirmDialog(null, "Do you confirm removing the map?  can't come back !!!", null,
                                JOptionPane.YES_NO_OPTION);
                        if (ret == JOptionPane.YES_OPTION) {
                            String map = m_currentMapName;
                            closeMindDb();
                            Utils.removeMap(map);
                        }
                    }
                }
            });
            m_mindMapMenu.add(removingMenuItem);

            addMenuSeparator(m_mindMapMenu);

            for (final String mapName : Utils.getAllMapNames()) {
                JMenuItem menuItem = new JMenuItem(mapName);
                if (mapName.equals(m_currentMapName)) {
                    menuItem.setIcon(Utils.getAppIcon("current.png"));
                }
                else {
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent)
                        {
                            closeMindDb();
                            openMindDb(mapName);
                        }
                    });
                }

                m_mindMapMenu.add(menuItem);
            }
        }

        @Override
        public void menuDeselected(MenuEvent e)
        {
            m_mindMapMenu.removeAll();
        }

        @Override
        public void menuCanceled(MenuEvent e)
        {
            m_mindMapMenu.removeAll();
        }
    };

    MenuListener m_favoriteMenuListener = new MenuListener() {
        @Override
        public void menuSelected(MenuEvent menuEvent)
        {
            if (m_mindController.isChanging()) {
                return;
            }

            final Object currentVertexId = m_mindController.getCurrentVertexId();
            JMenuItem addingMenuItem = new JMenuItem("add to favorite");
            addingMenuItem.setEnabled(currentVertexId != null && ! m_mindModel.isInFavorite(currentVertexId));
            addingMenuItem.setIcon(Utils.getAppIcon("new.png"));
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
            removingMenuItem.setIcon(Utils.getAppIcon("delete.png"));
            removingMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    m_mindModel.removeFromFavorite(currentVertexId);
                }
            });
            m_favoriteMenu.add(removingMenuItem);

            addMenuSeparator(m_favoriteMenu);

            for (final MindModel.VertexBasicInfo vertexBasicInfo : m_mindModel.m_favoriteInfoes) {
                if (! m_mindModel.isVertexTrashed(vertexBasicInfo.m_dbId)) {
                    addVertexSubMenu(m_favoriteMenu, vertexBasicInfo, true);
                }
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
    };

    private void addVertexSubMenu(JMenu menu, final MindModel.VertexBasicInfo vertexBasicInfo, boolean hasParentInfo)
    {
        String text;
        if (hasParentInfo)
            text = (vertexBasicInfo.m_contextText);
        else
            text = (vertexBasicInfo.m_text);

        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                m_mindController.findOrAddMindView(vertexBasicInfo.m_dbId);
            }
        });

        menu.add(menuItem);
    }

    MenuListener m_ancestorMenuListener = new MenuListener() {
        @Override
        public void menuSelected(MenuEvent menuEvent)
        {
            MindView currentView = m_mindController.getCurrentView();
            currentView.addSubMenuForOpenAncestors(m_ancestorMenu, false);
        }

        @Override
        public void menuDeselected(MenuEvent menuEvent)
        {
            m_ancestorMenu.removeAll();
        }

        @Override
        public void menuCanceled(MenuEvent menuEvent)
        {
            m_ancestorMenu.removeAll();
        }
    };

    public Action m_styleNewAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            m_styleList.setUpdatingMindNodeEnabled(false);
            m_styleList.newStyle();
            m_styleList.setUpdatingMindNodeEnabled(true);
        }
    };

    public Action m_styleDeletingAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            m_styleList.setUpdatingMindNodeEnabled(false);
            m_styleList.removeSelectedStyle();
            m_styleList.setUpdatingMindNodeEnabled(true);
        }
    };
    public Action m_styleEditingAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            m_styleList.setUpdatingMindNodeEnabled(false);
            m_styleList.editSelectedStyle();
            m_mindController.updateAllMindViews();
            m_styleList.setUpdatingMindNodeEnabled(true);
        }
    };
    public Action m_styleUpAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            m_styleList.setUpdatingMindNodeEnabled(false);
            m_styleList.upSelectedStyle();
            m_styleList.setUpdatingMindNodeEnabled(true);
        }
    };
    public Action m_styleDownAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            m_styleList.setUpdatingMindNodeEnabled(false);
            m_styleList.downSelectedStyle();
            m_styleList.setUpdatingMindNodeEnabled(true);
        }
    };

    public Action m_propertyToolBarVisibleAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean visible = m_propertyToolBarVisibleCheckMenu.isSelected();
            m_propertyToolBar.setVisible(visible);
        }
    };

    public Action m_alwaysOnTopAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            boolean alwaysOnTop = m_alwaysOnTopCheckMenu.isSelected();
            setAlwaysOnTop(alwaysOnTop);
        }
    };
}