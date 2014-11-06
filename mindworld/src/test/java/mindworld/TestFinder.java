package mindworld;

import junit.framework.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;


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
