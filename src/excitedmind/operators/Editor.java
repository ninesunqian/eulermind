package excitedmind.operators;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.undo.AbstractUndoableEdit;

import excitedmind.MindView;

public class Editor extends AbstractAction {

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	static class Executor extends AbstractUndoableEdit 
	{
		
	}
}

