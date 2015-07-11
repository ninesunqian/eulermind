package com.javaeye.i2534.view;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import com.javaeye.i2534.model.Entity;

/**
 * 对象的传输类
 */
public class EntityTransfer implements Transferable {

	public static final DataFlavor FLAVOR = new DataFlavor(Entity.class,
			DataFlavor.javaJVMLocalObjectMimeType);// 此处可以添加多个类型而形成数组

	private Entity entity;

	public EntityTransfer(Entity entity) {
		this.entity = entity;
	}

	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (isDataFlavorSupported(flavor)) {
			return entity;
		}
		throw new UnsupportedFlavorException(flavor);
	}

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { FLAVOR };
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return FLAVOR.equals(flavor);
	}

}
