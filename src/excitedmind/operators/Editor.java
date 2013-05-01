package excitedmind.operators;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.AbstractUndoableEdit;

import prefuse.data.Node;
import prefuse.visual.tuple.TableNodeItem;

import excitedmind.MindTree;
import excitedmind.MindView;

public class Editor extends AbstractAction {
	
	MindView m_mindView;
	
	Editor (MindView mindView)
	{
		m_mindView = mindView;
		
		//TODO get editor
		//set text
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Node node = m_mindView.getFocusNode ();
		String oldText = node.getString (MindTree.sm_textPropName);
		// TODO Auto-generated method stub
	}
	
	static class Executor extends AbstractUndoableEdit 
	{
		private static final long serialVersionUID = 1L;
		
		String m_newText;
		String m_oldText;
		Object m_bpId;
		MindTree m_mindTree;

		Executor (MindTree mindTree, Object bpId, String oldText, String newText)
		{
			m_mindTree = mindTree;
			m_bpId = bpId;
			m_oldText = oldText;
			m_newText = newText;
		}

		public void redo ()
		{
			m_mindTree.setNodeProperty(m_bpId, MindTree.sm_textPropName, m_newText);
		}
		
		public void undo ()
		{
			m_mindTree.setNodeProperty(m_bpId, MindTree.sm_textPropName, m_oldText);
		}
	}
}

