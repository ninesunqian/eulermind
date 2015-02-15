package eulermind.component;

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

import eulermind.MindController;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PropertyComponentConnector {

    Object m_cachedPropertyValue;

    public PropertyComponentConnector(MindController mindController,
                               PropertyComponent component,
                               String propertyName)
    {
        m_mindController = mindController;
        m_component = component;
        m_propertyName = propertyName;

        /*有mouseEnter 来刷新，容易导致多个控件不一致
        m_component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                invalidateComponent();
            }
        });
        */
    }

    boolean m_updateMindNodeEnabled = true;
    boolean m_updateComponentEnabled = true;

    public void updateComponent(final Object value)
    {
        m_cachedPropertyValue = value;
        invalidateComponent();
    }

    private void invalidateComponent() {
        if (! m_updateComponentEnabled) {
            return;
        }
        m_updateMindNodeEnabled = false;
        m_component.setValue(m_cachedPropertyValue);
        m_updateMindNodeEnabled = true;

    }

    public void updateMindNode(Object value)
    {
        if (! m_updateMindNodeEnabled) {
            return;
        }

        m_updateComponentEnabled = false;
        m_mindController.getCurrentView().setCursorProperty(m_propertyName, value);
        m_updateComponentEnabled = true;
    }

    public MindController m_mindController;

    public PropertyComponent m_component;
    public String m_propertyName;
}
