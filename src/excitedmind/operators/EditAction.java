package excitedmind.operators;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import prefuse.visual.tuple.TableNodeItem;

import excitedmind.MindTree;
import excitedmind.MindView;

public class EditAction extends AbstractAction {
	
	MindView m_mindView;
	TableNodeItem m_nodeItem;
	
	KeyListener keyListener = new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					String oldText = m_nodeItem.getString (MindTree.sm_textPropName);

					m_mindView.removeKeyListener(keyListener);

					String text = m_mindView.getTextEditor().getText();
					m_mindView.stopEditing();
					
					MindTree mindTree = m_mindView.getMindTree();
					Object bpId = mindTree.getDBItemId(m_mindView.getVisualization().getSourceTuple(m_nodeItem));
					
					Executor executor = new Executor (m_mindView, bpId, oldText, text);
					executor.redo();
					
					m_mindView.getUndoManager().addEdit(executor);
				}
				
			}
		};
	
	public EditAction (MindView mindView)
	{
		m_mindView = mindView;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		m_nodeItem = m_mindView.getFocusNode ();
		m_mindView.getTextEditor().addKeyListener(keyListener);
		m_mindView.editText(m_nodeItem, MindTree.sm_textPropName) ;
	}
	
	static class Executor extends AbstractUndoableEdit 
	{
		private static final long serialVersionUID = 1L;
		
		MindView m_mindView;
		Object m_bpId;
		
		String m_newText;
		String m_oldText;

		Executor (MindView mindView, Object bpId, String oldText, String newText)
		{
			m_mindView = mindView;
			m_bpId = bpId;
			m_oldText = oldText;
			m_newText = newText;
		}

		public void redo ()
		{
			m_mindView.getMindTree().setNodeProperty(m_bpId, MindTree.sm_textPropName, m_newText);
			m_mindView.getVisualization().run(MindView.sm_layoutAction);
		}
		
		public void undo ()
		{
			m_mindView.getMindTree().setNodeProperty(m_bpId, MindTree.sm_textPropName, m_oldText);
			m_mindView.getVisualization().run(MindView.sm_layoutAction);
		}
	}
}

