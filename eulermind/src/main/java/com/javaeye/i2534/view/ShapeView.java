package com.javaeye.i2534.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import com.javaeye.i2534.model.Entity;

/**
 * 小部件视图
 */
public class ShapeView extends JComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1145192280862730141L;

	/**
	 * 部件的图标
	 */
	private Image icon;
	/**
	 * 部件的文字内容
	 */
	private String text;
	/**
	 * 部件的鼠标提示
	 */
	private String tooltip;
	private Entity entity;

	/**
	 * 是否是焦点
	 */
	private boolean focus;

	public ShapeView(Entity attr) {
		super();
		this.entity = attr;

		init();
	}

	private void init() {
		icon = entity.getIcon();
		text = entity.getText();
		tooltip = entity.getTooltip();

		this.setSize(100, 80);
		setPreferredSize(this.getSize());
		if (tooltip != null) {
			setToolTipText(tooltip);
		}
		setOpaque(false);
		setFocusable(true);

		// 鼠标监听
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setCursor(Cursor.getDefaultCursor());
			}

			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
			}
		});

		// 焦点监听
		addFocusListener(new FocusAdapter() {

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

		});

		// 拖拽监听
		DragSource ds = DragSource.getDefaultDragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY,
				new DragGestureListener() {

					private EntityTransfer transferable = new EntityTransfer(
							entity);

					public void dragGestureRecognized(DragGestureEvent dge) {
						dge.startDrag(DragSource.DefaultCopyDrop, transferable);
					}
				});
	}

	@Override
	protected void paintComponent(Graphics g) {
		int h = getHeight();
		int w = getWidth();
		Graphics2D g2d = (Graphics2D) g;
		if (focus) {
			Color old = g2d.getColor();
			g2d.setColor(Color.GREEN);
			g2d.fillRect(0, 0, w, h);
			g2d.setColor(old);
		}
		g2d.drawRect(0, 0, w - 1, h - 1);
		int x = 5;
		if (icon != null) {
			g2d.drawImage(icon, 0, (h - icon.getHeight(this)) / 2, this);
			x += icon.getWidth(this);
		}
		g2d.drawString(text, x, calcFontInMiddle(h, g));
	}

	private static int calcFontInMiddle(int height, Graphics g) {
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int leading = fm.getLeading();
		int fontHeight = ascent + descent + leading;
		int y = height / 2;
		y += (ascent + leading - fontHeight / 2);
		return y;
	}

	public void setIcon(Image icon) {
		this.icon = icon;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

}
