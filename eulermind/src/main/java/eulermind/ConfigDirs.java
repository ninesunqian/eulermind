package eulermind;

import com.helger.commons.io.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Properties;

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

public class ConfigDirs {
    static public String TOP_DIR;
    static public String MAPS_DIR;
    static public String STYLE_FILE;
    static public String LAST_OPENED_MAP_RECORD_FILE;

    static void init () {

        //windows的快捷方式只能以.lnk后缀名保存。 手动重命名时开头不能是"."

        String try_dirs [] = {".eulermind", ".eulermind.lnk", "dot_eulermind", "dot_eulermind.lnk"};
        for (String dir : try_dirs) {
            File file = new File(System.getProperty("user.home") + File.separator + dir);

            //Files.isSymbolicLink不支持windows的快捷方式
            if (dir.endsWith(".lnk")) {
                WindowsShortcut shortcut;
                try {
                    shortcut = new WindowsShortcut(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                } catch (ParseException e) {
                    e.printStackTrace();
                    continue;
                }

                TOP_DIR = shortcut.getRealFilename();
                break;
            }

            Path path = file.toPath();
            if (Files.isSymbolicLink(path)) {
                try {
                    TOP_DIR = Files.readSymbolicLink(path).toString();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            } else if (file.isDirectory()) {
                TOP_DIR = file.getPath();
                break;
            }
        }

        if (TOP_DIR == null) {
            TOP_DIR = System.getProperty("user.home") + File.separator + ".eulermind";
        }

        MAPS_DIR = TOP_DIR + File.separator + "maps";
        STYLE_FILE = TOP_DIR + File.separator + "styles.xml";
        LAST_OPENED_MAP_RECORD_FILE = TOP_DIR + File.separator + "last_opened_map";
    }
}
