/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package excitedmind;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.distortion.Distortion;
import prefuse.action.distortion.FisheyeDistortion;
import prefuse.action.layout.Layout;
import prefuse.controls.AnchorUpdateControl;
import prefuse.controls.ControlAdapter;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;

public class NodeListView extends Display {

	private static final String sm_group = "history";
    private Table m_items;
    private int m_maxLength;
    
    public void setMaxLength (int maxLength)
    {
    	m_maxLength = maxLength;
    }
    
    public void addNode (Node node)
    {
    	int r = m_items.addRow();
    	//TODO
    	
    }
    /**
     * Create a new, empty FisheyeMenu.
     * @see #addMenuItem(String, javax.swing.Action)
     */
    public NodeListView() {
        super(new Visualization());
        setHighQuality(true);
        
        m_vis.addTable(sm_group, m_items);
        
        // set up the renderer to use
        LabelRenderer renderer = new LabelRenderer(LABEL);
        renderer.setHorizontalPadding(0);
        renderer.setVerticalPadding(1);
        renderer.setHorizontalAlignment(Constants.LEFT);
        m_vis.setRendererFactory(new DefaultRendererFactory(renderer));
        
        /*
        addControlListener(new ControlAdapter() {
            // dispatch an action event to the menu item
            public void itemClicked(VisualItem item, MouseEvent e) {
                ActionListener al = (ActionListener)item.get(ACTION);
                al.actionPerformed(new ActionEvent(item, e.getID(),
                    "click", e.getWhen(), e.getModifiers()));
            }
        });
        */
        
        // text color function
        // items with the mouse over printed in red, otherwise black
        ColorAction colors = new ColorAction(ITEMS, VisualItem.TEXTCOLOR);
        colors.setDefaultColor(ColorLib.gray(0));
        colors.add("hover()", ColorLib.rgb(255,0,0));
        
        // initial layout and coloring
        ActionList init = new ActionList();
        init.add(new HorizontalLineLayout(ITEMS, m_maxHeight));
        init.add(colors);
        init.add(new RepaintAction());
        m_vis.putAction("init", init);
    }
    
    /**
     * Adds a menu item to the fisheye menu.
     * @param name the menu label to use
     * @param action the ActionListener to notify when the item is clicked
     * The prefuse VisualItem corresponding to this menu item will
     * be returned by the ActionEvent's getSource() method.
     */
    public void addMenuItem(String name, ActionListener listener) {
        int row = m_items.addRow();
        m_items.set(row, LABEL, name);
        m_items.set(row, ACTION, listener);
    }
    
    /**
     * Run a demonstration of the FisheyeMenu
     */
    public static final void main(String[] argv) {
        // only log warnings
        Logger.getLogger("prefuse").setLevel(Level.WARNING);

        NodeListView fm = demo();
        
        // create and display application window
        JFrame f = new JFrame("p r e f u s e  |  f i s h e y e");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(fm);
        f.pack();
        f.setVisible(true);
    }
    
    public static NodeListView demo() {
        // create a new fisheye menu and populate it
        NodeListView fm = new NodeListView();
        for ( int i=1; i<=72; ++i ) {
            // add menu items that simply print their label when clicked
            fm.addMenuItem(String.valueOf(i), new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    System.out.println("clicked item: "+
                        ((VisualItem)e.getSource()).get(LABEL));
                    System.out.flush();
                }
            });
        }
        fm.getVisualization().run("init");
        return fm;
    }
    
    /**
     * Lines up all VisualItems vertically. Also scales the size such that
     * all items fit within the maximum layout size, and updates the
     * Display to the final computed size.
     */
    public class HorizontalLineLayout extends Layout {
        private double m_maxHeight = 600;
        
		public HorizontalLineLayout(String group, double maxHeight) {
        	super (group);
            m_maxHeight = maxHeight;
        }
        
        public void run(double frac) {
            // first pass
            double w = 0, h = 0;
            Iterator iter = m_vis.items(m_group);
            while ( iter.hasNext() ) {
                VisualItem item = (VisualItem)iter.next();
                item.setSize(1.0);
                h += item.getBounds().getHeight();
            }
            double scale = h > m_maxHeight ? m_maxHeight/h : 1.0;
            
            Display d = m_vis.getDisplay(0);
            Insets ins = d.getInsets();
            
            // second pass
            h = ins.top;
            double ih, y=0, x=ins.left;
            iter = m_vis.items();
            while ( iter.hasNext() ) {
                VisualItem item = (VisualItem)iter.next();
                item.setSize(scale); item.setEndSize(scale);
                Rectangle2D b = item.getBounds();
                
                w = Math.max(w, b.getWidth());
                ih = b.getHeight();
                y = h+(ih/2);
                setX(item, null, x);
                setY(item, null, y);
                h += ih;
            }
            
        }
    } // end of inner class horizontalLineLayout
    
} // end of class FisheyeMenu
