package excitedmind.operators;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import prefuse.visual.NodeItem;

import excitedmind.VisualMindTree;
import excitedmind.MindView;

public class EditAction extends AbstractAction {
	
	MindView m_mindView;
	NodeItem m_nodeItem;
	
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
                    AbstractUndoableEdit undoer = visMindTree.setCursorText(text);

                    m_mindView.removeKeyListener(m_textEditorKeyListener);
                    m_mindView.stopEditing();

					m_mindView.getUndoManager().addEdit(undoer);
                    m_mindView.renderTree();
				}
			}
		};
	
	public EditAction (MindView mindView)
	{
		m_mindView = mindView;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		m_nodeItem = m_mindView.getVisMindTree().getCursor();
		m_mindView.getTextEditor().addKeyListener(m_textEditorKeyListener);
		m_mindView.editText(m_nodeItem, VisualMindTree.sm_textPropName) ;
	}
}

