package mindworld;

import prefuse.util.FontLib;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-3-26
 * Time: 上午6:35
 * To change this template use File | Settings | File Templates.
 */
public class FontCombobox extends JComboBox{

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    static boolean sm_constantLoaded = false;
    static String sm_fontFamilies[];

    static Integer sm_fontSizes[] = {10, 12, 14, 16, 18, 20, 24, 28};
    static Color sm_colors[] = {Color.black, Color.blue, Color.cyan, Color.darkGray};

    enum ListWhich {
        list_family,
        list_size,
        list_color
    }

    ListWhich m_listWhich;
    String m_family;
    int m_size;
    Color m_color;

    static void loadConstant()
    {
        if (sm_constantLoaded)
            return;

        ArrayList<String> fontFamilies = new ArrayList<String>();
        for (String family : GraphicsEnvironment .getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            Font font = new Font(family, Font.PLAIN, 1);
            if (font.canDisplay('a')) {
                fontFamilies.add(family);
            }
        }

        sm_fontFamilies = new String[fontFamilies.size()];
        fontFamilies.toArray(sm_fontFamilies);
        sm_constantLoaded = true;
    }

    FontCombobox(ListWhich listWhich)
    {
        loadConstant();
        this.setRenderer(new FontCellRenderer());

        m_family = "SansSerif";
        m_size = 14;
        m_color = sm_colors[0];

        setListWitch(listWhich);
    }

    private void setListWitch(ListWhich listWhich)
    {
        if (m_listWhich == listWhich) {
            return;
        }

        m_listWhich = listWhich;
        removeAllItems();

        Object items[] = null;

        switch (m_listWhich) {
            case list_family:
                items = sm_fontFamilies;
                break;
            case list_size:
                items = sm_fontSizes;
                break;
            case list_color:
                items = sm_colors;
                break;
        }

        for (Object item : items) {
            addItem(item);
        }
    }

    void setProperties(String family, int size, Color color)
    {
        m_family = family;
        m_size = size;
        m_color = color;

        switch (m_listWhich) {
            case list_family:
                setSelectedItem(family);
                break;
            case list_size:
                setSelectedItem(size);
                break;
            case list_color:
                setSelectedItem(color);
                break;
        }
    }

    class FontCellRenderer implements ListCellRenderer {
        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);

            String family = null;
            Integer size = 0;
            Color color = null;
            String text = null;

            switch (m_listWhich) {
                case list_family:
                    family = (String) value;
                    text = family;
                    size = m_size;
                    color = m_color;
                    break;

                case list_size:
                    size = (Integer)value;
                    text = size.toString();
                    family = m_family;
                    color = m_color;
                    break;

                case list_color:
                    color = (Color)value;
                    text = color.toString();
                    family = m_family;
                    size = m_size;
                    break;
            }

            if (cellHasFocus) {
                color = renderer.getForeground();
            }

            Font font = FontLib.getFont(family, size);

            renderer.setFont(font);
            renderer.setText(text);
            renderer.setForeground(color);

            return renderer;
        }
    }
}
