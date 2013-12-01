package excitedmind.operators;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;

import prefuse.visual.NodeItem;

import excitedmind.VisualMindTree;
import excitedmind.MindView;

public class EditAction extends AbstractAction {
	
	MindView m_mindView;
	NodeItem m_nodeItem;
    VisualMindTree m_visMindTree;
    boolean m_isPlaceholder;

	
	KeyListener m_textEditorKeyListener = new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
                    VisualMindTree visMindTree = m_mindView.getVisMindTree();

                    String text = m_mindView.getTextEditor().getText();

                    //stopEditing will set text of cursor
                    m_mindView.stopEditing();

                    AbstractUndoableEdit undoer;

                    if (m_isPlaceholder) {
                        visMindTree.setPlaceholderCursorText(text);
                        undoer = visMindTree.placeNewNodeUndoable();
                    } else {
                        undoer = visMindTree.setCursorText(text);
                    }

                    m_mindView.getUndoManager().addEdit(undoer);

                    m_mindView.removeKeyListener(m_textEditorKeyListener);

                    m_mindView.renderTree();
				}
			}
		};

    MouseListener m_promptListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent mouseEvent) {

            JList promptList = m_mindView.getPromptJList();
            promptList.removeMouseListener(m_promptListener);

            int idx = promptList.getSelectedIndex();
            MindView.QueriedNode selected = m_mindView.m_queriedNodes.get(idx);

            m_mindView.stopEditing();

            AbstractUndoableEdit undoer = m_mindView.getVisMindTree().placeRefereeUndoable(selected.m_dbId);
            m_mindView.getUndoManager().addEdit(undoer);
            m_mindView.renderTree();
        }
    };


	public EditAction (MindView mindView, boolean isPlaceholder)
	{
		m_mindView = mindView;
        m_visMindTree = m_mindView.getVisMindTree();
        m_isPlaceholder = isPlaceholder;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		m_nodeItem = m_mindView.getVisMindTree().getCursor();

		m_mindView.getTextEditor().addKeyListener(m_textEditorKeyListener);
        m_mindView.getPromptJList().addMouseListener(m_promptListener);

		m_mindView.editText(m_nodeItem, VisualMindTree.sm_textPropName) ;

	}
}

