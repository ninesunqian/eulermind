package excitedmind;

import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Created with IntelliJ IDEA.
 * User: wangxuguang
 * Date: 14-4-3
 * Time: 上午6:38
 * To change this template use File | Settings | File Templates.
 */
public class Config {
    static boolean m_isLoaded;
    static Properties m_properties;
    final static String sm_split_regex = "[ ]";

    private Config() {

    }

    static void load() {
        if (m_isLoaded)
            return;
        //TODO
        m_isLoaded = true;
    }

    static String[] getIcons()
    {
        String icons = m_properties.getProperty("icons");
        return icons.split(sm_split_regex);
    }

    static int[] getFramePosition()
    {
        String position_str = m_properties.getProperty("window.location");
        String strs [] = position_str.split(sm_split_regex);
        int position[] = new int[4];

        for (int i=0; i<4; i++) {
            position[i] = Integer.parseInt(strs[i]);
        }
        return position;
    }

    static void setFramePosition(int x, int y, int w, int h)
    {

    }
}
