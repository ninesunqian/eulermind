package eulermind.component;

import eulermind.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prefuse.util.FontLib;

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

public class BooleanCombobox extends JComboBox implements PropertyComponent {

    final Logger m_logger = LoggerFactory.getLogger(this.getClass());

    public BooleanCombobox()
    {
        addItem("default");
        addItem("Yes");
        addItem("No");

        addActionListener(m_updateMindNodeAction);
    }

    ActionListener m_updateMindNodeAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (m_propertyComponentConnector != null) {
                m_propertyComponentConnector.updateMindNode(getValue());
            }
        }
    };

    @Override
    public void setValue(Object value)
    {
        if (value == null) {
            setSelectedIndex(0);
        }
        else if (value.equals(true)) {
            setSelectedIndex(1);
        } else if (value.equals(false)) {
            setSelectedIndex(2);
        } else {
            setSelectedIndex(0);
        }
    }

    @Override
    public Boolean getValue()
    {
        int index = getSelectedIndex();
        Boolean []values = {null, true, false};
        return values[index];
    }

    @Override
    public void setPropertyComponentConnector(PropertyComponentConnector propertyComponentConnector)
    {
        m_propertyComponentConnector = propertyComponentConnector;
    }

    PropertyComponentConnector m_propertyComponentConnector;
}
