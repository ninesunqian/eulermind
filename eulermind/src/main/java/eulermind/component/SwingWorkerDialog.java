package eulermind.component;

/*
 * The MIT License (MIT)
 * Copyright (c) 2012-2014 wangxuguang ninesunqian@163.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutionException;

public abstract class SwingWorkerDialog extends SwingWorker {
    static Logger m_logger = LoggerFactory.getLogger(SwingWorkerDialog.class);

    final static String PROGRESS_INFO = "progress_info";

    static class ProgressInfo {
        int m_progress;
        int m_maxProgress;
        String m_message;
        ProgressInfo(int progress, int maxProgress, String message) {
            m_progress = progress;
            m_maxProgress = maxProgress;
            m_message = message;
        }
        public String toString() {
            return String.format("[%d, %d, %s]", m_progress, m_maxProgress, m_message);
        }
    }

    String m_title;

    Component m_parent;

    JDialog m_dialog;
    JProgressBar m_progressBar;
    JLabel m_messageLabel;
    JOptionPane m_optionPane;


    public SwingWorkerDialog(Component parent, String title) {
        m_parent = parent;
        m_title = title;
        addPropertyChangeListener(m_progressInfoListener);
    }

    PropertyChangeListener m_progressInfoListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (m_dialog == null) {
                return;
            }

            m_logger.info("PropertyChangeListener, {}, {}", evt.getPropertyName(), evt.getNewValue());

            if (evt.getPropertyName().equals("state")) {
                if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
                    m_dialog.setVisible(false);
                    m_dialog.dispose();
                }

            } else if (PROGRESS_INFO.equals(evt.getPropertyName())) {
                ProgressInfo progressInfo = (ProgressInfo)evt.getNewValue();

                if (m_progressBar.getMaximum() != progressInfo.m_maxProgress) {
                    m_progressBar.setMaximum(progressInfo.m_maxProgress);
                }
                m_progressBar.setValue(progressInfo.m_progress);
                m_messageLabel.setText(progressInfo.m_message);
            }
        }
    };

    public void notifyProgressA(int progress, int maxProgress, String message) {
        ProgressInfo progressInfo = new ProgressInfo(progress, maxProgress, message);

 	    firePropertyChange(PROGRESS_INFO, null, progressInfo);
        m_logger.info("firePropertyChange: {}, {}, {}", progress, maxProgress, message);
    }

    boolean executed = false;


    public Object executeInProgressDialog() throws ExecutionException, InterruptedException {
        assert ! executed;
        execute();
        executed = true;

        //先等待0.5秒
        for (int i=0; i<50; i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            if (isDone()) {
                break;
            }
        }

        if (! isDone()) {
            showProgressDialog();
        }

        return get();
    }

    Object m_cancelOption[];

    void showProgressDialog() {
        m_progressBar = new JProgressBar();
        m_progressBar.setMinimum(0);
        m_progressBar.setMaximum(100);
        m_progressBar.setValue(0);

        m_messageLabel = new JLabel("                              ");

        m_cancelOption = new Object[1];
        m_cancelOption[0] = UIManager.getString("OptionPane.cancelButtonText");

        m_optionPane = new JOptionPane(new Object[] {m_messageLabel, m_progressBar},
                JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                m_cancelOption,
                null);

        m_dialog = m_optionPane.createDialog(m_parent, m_title);
        m_dialog.setVisible(true);
    }


    public boolean isCancelButtonPressed() {
        if (m_dialog == null) {
            return false;
        }
        Object v = m_optionPane.getValue();
        return (v != null) && (v.equals(m_cancelOption[0]));
    }
}
