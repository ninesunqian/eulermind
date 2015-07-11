package com.javaeye.i2534.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.border.LineBorder;

import com.javaeye.i2534.model.Entity;

/**
 * 实体视图
 */
public class EntityView extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8461246679806038702L;

	/**
	 * 视图默认的背景色
	 */
	private static final Color BG = new Color(232, 238, 247);

	/**
	 * 实体模型
	 */
	private Entity entity;

	/**
	 * 是否是选定状态
	 */
	private boolean focus;

	public EntityView(Entity entity) {
		this.entity = entity;
		this.setBounds(0, 0, 100, 80);

		init();
	}

	private void init() {
		// 单线边框
		setBorder(LineBorder.createBlackLineBorder());

		MouseEvents me = new MouseEvents();
		addMouseListener(me);// 鼠标点击动作

		// 焦点动作
		addFocusListener(new FocusEvents());

	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		int w = getWidth();
		int h = getHeight();
		Color old = g2d.getColor();
		g2d.setColor(BG);
		g2d.fillRect(0, 0, w, h);
		if (focus) {
			g2d.setColor(Color.GREEN);
		} else {
			g2d.setColor(Color.BLACK);
		}
		// 是焦点时,先绘制绿边框,不是,绘制黑边框
		g2d.drawRect(0, 0, w - 1, h - 1);
		if (focus) {// 是焦点,绘制黑色虚线
			g2d.setColor(Color.BLACK);
			BasicStroke bs = new BasicStroke(1, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_BEVEL, 1, new float[] { 3, 3 }, 0);
			g2d.setStroke(bs);
			g2d.drawRect(0, 0, w - 1, h - 1);
		}
		// 此处绘制没用到Entity,其实可以扩展Entity,然后进行更复杂的绘制工作
		// 如绘制entity.getText()文字,等等
		g2d.setColor(old);
	}

	/**
	 * 焦点动作
	 */
	private final class FocusEvents extends FocusAdapter {
		@Override
		public void focusGained(FocusEvent e) {
			focus = true;
			repaint(100);
		}

		@Override
		public void focusLost(FocusEvent e) {
			focus = false;
			repaint(100);
		}
	}

	/**
	 * 鼠标动作
	 */
	private final class MouseEvents extends MouseAdapter {

		@Override
		public void mouseEntered(MouseEvent e) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}

		@Override
		public void mouseExited(MouseEvent e) {
			setCursor(Cursor.getDefaultCursor());
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			requestFocusInWindow();
		}
	}
}
