package excitedmind.operators;

import excitedmind.MindView;
import excitedmind.VisualMindTree;

import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import java.awt.event.ActionEvent;

public abstract class SimpleMindTreeAction extends AbstractAction {

	MindView m_mindView;
    VisualMindTree m_visMindTree;

    public abstract AbstractUndoableEdit operateMindTree(ActionEvent e);

	public SimpleMindTreeAction(MindView mindView)
	{
		m_mindView = mindView;
        m_visMindTree = mindView.getVisMindTree();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
        AbstractUndoableEdit undoer = operateMindTree(e);
        if (undoer != null) {
            m_mindView.getUndoManager().addEdit(undoer);
        }
        m_mindView.renderTree ();
	}
}

