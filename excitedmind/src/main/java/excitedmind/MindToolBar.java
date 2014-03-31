package excitedmind;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-3-28
 * Time: 上午7:11
 * To change this template use File | Settings | File Templates.
 */
public class MindToolBar extends JToolBar {
    MindModel m_mindModel;
    FontCombobox m_fontFamilyCombobox;
    FontCombobox m_fontSizeCombobox;
    FontCombobox m_colorCombobox;
    MindPrompter m_prompter;
    JTextField m_editor;

    MindToolBar(MindModel mindModel) {
        m_mindModel = mindModel;

        m_fontFamilyCombobox = new FontCombobox(FontCombobox.ListWhich.list_family);
        m_fontSizeCombobox = new FontCombobox(FontCombobox.ListWhich.list_size);
        m_colorCombobox = new FontCombobox(FontCombobox.ListWhich.list_color);

        m_fontFamilyCombobox.setFocusable(false);
        m_fontSizeCombobox.setFocusable(false);
        m_colorCombobox.setFocusable(false);

        add(m_fontFamilyCombobox);
        add(m_fontSizeCombobox);
        add(m_colorCombobox);


        m_editor = new JTextField();
        m_prompter = new MindPrompter(this, m_mindModel.m_mindDb);
        add(m_editor);
        m_editor.setText("   ");
        m_editor.setEditable(true);
        m_editor.setVisible(true);
        m_prompter.show(m_editor);
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
