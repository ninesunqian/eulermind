package excitedmind.operators;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.AbstractUndoableEdit;

import prefuse.data.Node;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableNodeItem;

import excitedmind.MindTree;
import excitedmind.MindView;

public class EditAction extends AbstractAction {
	
	MindView m_mindView;
	
	public EditAction (MindView mindView)
	{
		m_mindView = mindView;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		TableNodeItem node = m_mindView.getFocusNode ();
		String oldText = node.getString (MindTree.sm_textPropName);
		//m_mindView.editText(node, MindTree.sm_textPropName) ;
		String newText = "newText";
		MindTree mindTree = m_mindView.getMindTree();
		mindTree.setNodeProperty(
				mindTree.getDBItemId(m_mindView.getVisualization().getSourceTuple(node)),
				MindTree.sm_textPropName, 
				newText);
		// TODO Auto-generated method stub
		m_mindView.getVisualization().run(MindView.sm_layoutAction);
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

