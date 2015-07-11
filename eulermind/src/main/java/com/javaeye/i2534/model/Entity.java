package com.javaeye.i2534.model;

import java.awt.Image;

public class Entity {
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

	public Image getIcon() {
		return icon;
	}

	public void setIcon(Image icon) {
		this.icon = icon;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

}
