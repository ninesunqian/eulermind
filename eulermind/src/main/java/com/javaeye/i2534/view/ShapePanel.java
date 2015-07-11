package com.javaeye.i2534.view;

import java.awt.FlowLayout;

import javax.swing.JPanel;

import com.javaeye.i2534.model.Entity;

/**
 * 部件面板
 */
public class ShapePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7789766262754746077L;

	public ShapePanel() {
		super();
		setLayout(new ModifiedFlowLayout(FlowLayout.LEFT, 10, 10));

		for (int i = 0; i < 5; i++) {
			Entity en = new Entity();
			en.setText("测试" + i);
			en.setTooltip("这是一个测试组件");
			this.add(new ShapeView(en));
		}
	}

}
