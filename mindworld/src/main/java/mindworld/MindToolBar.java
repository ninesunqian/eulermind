package mindworld;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-3-28
 * Time: 上午7:11
 * To change this template use File | Settings | File Templates.
 */
public class MindToolBar extends JToolBar {
    MindController m_mindController;
    FontCombobox m_fontFamilyCombobox;
    FontCombobox m_fontSizeCombobox;
    FontCombobox m_colorCombobox;
    MindCombobox m_comboxbox;

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

        m_comboxbox = new MindCombobox(m_mindController.m_mindModel.m_mindDb);
        m_comboxbox.setEditable(true);
        add(m_comboxbox);

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
		fonts.setEnabled(enabled);
		size.setEnabled(enabled);
	}
*/


}
