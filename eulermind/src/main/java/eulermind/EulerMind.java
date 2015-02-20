package eulermind;

import java.awt.*;

import eulermind.component.*;
import eulermind.view.MindEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

import org.swixml.SwingTagLibrary;

import java.io.File;

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

public class EulerMind {
    static Logger m_logger = LoggerFactory.getLogger(EulerMind.class);

    public static void main(String argv[]) {

        /*
        testJava();
        testXML();
        */
        SwingTagLibrary.getInstance().registerTag("fontFamilyCombobox", FontFamilyCombobox.class);
        SwingTagLibrary.getInstance().registerTag("fontSizeCombobox", FontSizeCombobox.class);
        SwingTagLibrary.getInstance().registerTag("booleanCombobox", BooleanCombobox.class);
        SwingTagLibrary.getInstance().registerTag("colorCombobox", ColorCombobox.class);
        SwingTagLibrary.getInstance().registerTag("iconButton", IconButton.class);
        SwingTagLibrary.getInstance().registerTag("styleList", StyleList.class);
        SwingTagLibrary.getInstance().registerTag("mindEditor", MindEditor.class);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Style.load();
        Style.save();

        String dbPath = System.getProperty("user.home") + "/.eulermind/mind_db";
        //TODO: for debug

        final String dbUrl = "local:" + dbPath.replace(File.separatorChar, '/');
        m_logger.info ("dbUrl = " + dbUrl);

        boolean newDb = false;
        if (newDb) {
            Utils.deleteDir(dbPath);

            MindDB mindDb = new MindDB(dbUrl);
            //Utils.createTree(mindDb, 2);

            Crawler.sm_mindDb = mindDb;
            Crawler crawler = new Crawler();
            crawler.start();
            mindDb = null;
        }


        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {

                JFrame frame = new MainFrame(dbUrl);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();

            }
        });
	}

} // end of class TreeMap
