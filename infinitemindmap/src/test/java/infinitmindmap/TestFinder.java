package infinitmindmap;

import junit.framework.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

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

public class TestFinder {
    public static void run(Class which, String[] args) {
        TestSuite suite = null;
        if (args.length > 0) {
            //测试参数指定的方法
            try {
                Constructor constructor = which.getConstructor(new Class[]{String.class});
                suite = new TestSuite();
                for (int i = 0; i < args.length; i++) {
                    suite.addTest((TestCase)constructor.newInstance(new Object[]{args[i]}));
                }
            } catch (Exception e) {
                System.err.println("Unable to instantiate " + which.getName() + ": " + e.getMessage());
                System.exit(1);

            }
        } else {
            try {
                //测试类指定的suite
                Method suite_method = which.getMethod("suite", new Class[0]);
                suite = (TestSuite) suite_method.invoke(null, null);
            } catch (Exception e) {
                //测试所有test方法
                suite = new TestSuite(which);
            }
        }

        junit.textui.TestRunner.run(suite);
    }
}
