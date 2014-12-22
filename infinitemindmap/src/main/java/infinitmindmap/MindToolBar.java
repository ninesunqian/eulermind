package infinitmindmap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

public class MindToolBar extends JToolBar {
    MindController m_mindController;
    FontCombobox m_fontFamilyCombobox;
    FontCombobox m_fontSizeCombobox;
    FontCombobox m_colorCombobox;
    MindEditor m_searchInputer;

    MindToolBar(MindController mindController) {
        m_mindController = mindController;

        m_fontFamilyCombobox = new FontCombobox(FontCombobox.ListWhich.list_family);
        m_fontSizeCombobox = new FontCombobox(FontCombobox.ListWhich.list_size);
        m_colorCombobox = new FontCombobox(FontCombobox.ListWhich.list_color);

        m_fontFamilyCombobox.setFocusable(false);
        m_fontSizeCombobox.setFocusable(false);
        m_colorCombobox.setFocusable(false);

        add(m_fontFamilyCombobox);
        add(m_fontSizeCombobox);
        add(m_colorCombobox);

        m_searchInputer = new MindEditor(10, m_mindController.m_mindModel.m_mindDb);
        m_searchInputer.setHasPromptList(true);
        m_searchInputer.setMinimumSize(new Dimension(50, 14));
        m_searchInputer.setMindEditorListener(new MindEditor.MindEditorListener() {
            public void promptListOk(Object dbId, String text, Object parentDBId, String parentText)
            {
                m_mindController.findOrAddMindView(dbId);
            }
        });
        add(m_searchInputer);

        m_fontFamilyCombobox.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        String family = (String) m_fontFamilyCombobox.getSelectedItem();
                        m_mindController.getCurrentView().setCursorProperty(
                                MindModel.sm_fontFamilyPropName, family);
                    }
                }
        );

        m_fontSizeCombobox.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        Integer size = (Integer) m_fontSizeCombobox.getSelectedItem();
                        m_mindController.getCurrentView().setCursorProperty(
                                MindModel.sm_fontSizePropName, size);
                    }
                }
        );

        m_colorCombobox.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent actionEvent) {
                        Color color = (Color) m_colorCombobox.getSelectedItem();
                        m_mindController.getCurrentView().setCursorProperty(
                                MindModel.sm_textColorPropName, color.getRGB());
                    }
                }
        );
    }

    /*
    * TODO: combox.addItemListenrer
    * 		fonts.setMaximumRowCount(9);
    *
    	// Daniel Polansky: both the following methods trigger item listeners above.
	// Those listeners obtain two events: first DESELECTED and then
	// SELECTED. Both events are to be ignored - we don't want to update
	// a node with its own font. The item listeners should react only
	// to a user change, not to our change.

	public void selectFontSize(String fontSize) // (DiPo)
	{
		fontSize_IgnoreChangeEvent = true;
		size.setSelectedItem(fontSize);
		fontSize_IgnoreChangeEvent = false;
	}

	Component getLeftToolBar() {
		return iconToolBarScrollPane;
	}

	public void selectFontName(String fontName) // (DiPo)
	{
		if (fontFamily_IgnoreChangeEvent) {
			return;
		}
		fontFamily_IgnoreChangeEvent = true;
		fonts.setEditable(true);
		fonts.setSelectedItem(fontName);
		fonts.setEditable(false);
		fontFamily_IgnoreChangeEvent = false;
	}

	void setAllActions(boolean enabled) {
		fonts.hold(enabled);
		size.hold(enabled);
	}
*/


}
