package excitedmind.operators;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractAction;
import javax.swing.undo.AbstractUndoableEdit;

import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.visual.tuple.TableNodeItem;

import excitedmind.VisualMindTree;
import excitedmind.MindView;

public class RemoveAction extends AbstractAction {
	
	MindView m_mindView;
	TableNodeItem m_nodeItem;
	
	public RemoveAction (MindView mindView)
	{
		m_mindView = mindView;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		m_nodeItem = m_mindView.getFocusNode ();
		VisualMindTree mindTree = m_mindView.getMindTree();
		Tuple tuple = m_mindView.getVisualization().getSourceTuple(m_nodeItem);
		Node node = mindTree.m_tree.getNode(tuple.getRow());
		m_mindView.getMindTree().moveNodeToTrash(node);
		m_mindView.renderTree ();
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
			m_mindView.renderTree ();
		}
		
		public void undo ()
		{
			m_mindView.getMindTree().setNodeProperty(m_bpId, MindTree.sm_textPropName, m_oldText);
			m_mindView.renderTree ();
		}
	}
}

