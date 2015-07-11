package com.javaeye.i2534.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.javaeye.i2534.model.Entity;

/**
 * 主绘制面板
 */
public class FlowPanel extends JPanel {

	/**
	 * 拖拽目标处理
	 */
	private final class MyDropTarget extends DropTarget {
		/**
		 * 
		 */
		private static final long serialVersionUID = 5014352026377552380L;

		/**
		 * 绘制面板
		 */
		private FlowPanel parent = FlowPanel.this;

		/**
		 * 拖拽进来的组件
		 */
		private JComponent c = null;

		@Override
		public synchronized void drop(DropTargetDropEvent e) {
			e.acceptDrop(DnDConstants.ACTION_COPY);// 只接受copy事件
			e.dropComplete(true);// 拖拽完成
		}

		@Override
		public synchronized void dragOver(DropTargetDragEvent e) {
			Point p = e.getLocation();
			int x = p.x;
			int y = p.y;
			y -= c.getHeight() / 2;
			x -= c.getWidth() / 2;
			c.setLocation(x, y);// 设置拖拽中组件的坐标
		}

		@Override
		public synchronized void dragEnter(DropTargetDragEvent e) {
			Transferable transfer = e.getTransferable();
			try {
				Object o = transfer.getTransferData(EntityTransfer.FLAVOR);
				if (o instanceof Entity) {// 此处可以依据传过来的模型判断而创建不同的视图
					c = new EntityView((Entity) o);
				}
				if (c == null) {
					throw new NullPointerException();
				}
				parent.add(c, 0);// 使其总处于最上面
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		@Override
		public synchronized void dragExit(DropTargetEvent dte) {
			if (c != null) {
				parent.remove(c);
				parent.repaint();
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3973309499628595815L;

	public FlowPanel() {
		super();
		setLayout(null);// 绝对排版
		setDropTarget(new MyDropTarget());// 成为拖拽目标
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		// 填充白色背景
		Graphics2D g2d = (Graphics2D) g;
		Color old = g2d.getColor();
		g2d.setColor(Color.WHITE);
		g2d.fill(new Rectangle(0, 0, getWidth(), getHeight()));
		g2d.setColor(old);
	}

}
