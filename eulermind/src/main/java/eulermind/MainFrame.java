package eulermind;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import eulermind.component.BooleanCombobox;
import eulermind.component.ColorButton;
import eulermind.component.FontCombobox;
import eulermind.component.MindIconToolBar;
import eulermind.view.MindEditor;
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

    MindModel m_mindModel;
    MindController m_mindController;
    MindIconToolBar m_mindIconToolBar;
    JTabbedPane m_tabbedPane;

    SwingEngine m_swingEngine;

    FontCombobox m_fontFamilyCombobox;
    BooleanCombobox m_italicCombobox;
    BooleanCombobox m_boldCombobox;
    //FontCombobox m_fontSizeCombobox;

    ColorButton m_textColorButton;
    ColorButton m_nodeColorButton;

    MindEditor m_searchInputer;

    JMenu m_ancestorMenu;
    JMenu m_favoriteMenu;

    public MainFrame(String dbUrl)
    {
        try {
            m_swingEngine = new SwingEngine(this);
            m_swingEngine.getTaglib().registerTag("mindIconToolBar", MindIconToolBar.class);
            m_swingEngine.getTaglib().registerTag("fontCombobox", FontCombobox.class);
            m_swingEngine.getTaglib().registerTag("booleanCombobox", BooleanCombobox.class);
            m_swingEngine.getTaglib().registerTag("colorButton", ColorButton.class);

            m_swingEngine.getTaglib().registerTag("mindEditor", MindEditor.class);
            m_swingEngine.render("main_frame_layout.xml");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        m_mindModel = new MindModel(dbUrl);
        m_mindController = new MindController(m_mindModel, m_tabbedPane);

        m_mindIconToolBar.setMindController(m_mindController);

        m_favoriteMenu.addMenuListener(m_favoriteMenuListener);
        m_ancestorMenu.addMenuListener(m_ancestorMenuListener);

        m_textColorButton.setForBackground(false);
        m_nodeColorButton.setForBackground(true);

        m_mindController.connectPropertyComponent(Style.sm_fontFamilyPropName, m_fontFamilyCombobox);
        m_mindController.connectPropertyComponent(Style.sm_italicPropName, m_italicCombobox);
        m_mindController.connectPropertyComponent(Style.sm_boldPropName, m_boldCombobox);
        m_mindController.connectPropertyComponent(Style.sm_textColorPropName, m_textColorButton);
        m_mindController.connectPropertyComponent(Style.sm_nodeColorPropName, m_nodeColorButton);

        m_searchInputer.init(m_mindController.m_mindModel.m_mindDb);
        m_searchInputer.setHasPromptList(true);
        m_searchInputer.setMindEditorListener(new MindEditor.MindEditorListener() {
            public void promptListOk(Object dbId, String text, Object parentDBId, String parentText)
            {
                m_mindController.findOrAddMindView(dbId);
            }
        });

        InputMap map;
        map = (InputMap) UIManager.get("TabbedPane.ancestorInputMap");
        KeyStroke keyStrokeCtrlUp = KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK);
        map.remove(keyStrokeCtrlUp);

        setLocationByPlatform(true);

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

    public Action m_importAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            m_mindController.getCurrentView().importFile();
        }

    };

    MenuListener m_favoriteMenuListener = new MenuListener() {
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
    };

    MenuListener m_ancestorMenuListener = new MenuListener() {
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

                m_ancestorMenu.add(menuItem);
            }
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
}