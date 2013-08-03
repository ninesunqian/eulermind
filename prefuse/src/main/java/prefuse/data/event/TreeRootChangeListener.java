package prefuse.data.event;

import java.util.EventListener;

import prefuse.data.Tree;

public interface TreeRootChangeListener extends EventListener {
	public void rootChanged(Tree t, int newRoot, int oldRoot);
}
