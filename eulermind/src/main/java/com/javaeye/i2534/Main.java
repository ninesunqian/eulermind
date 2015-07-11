package com.javaeye.i2534;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;

import com.javaeye.i2534.view.FlowPanel;
import com.javaeye.i2534.view.ShapePanel;

public class Main extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4015352198071511920L;
	private JPanel contentPane;
	private JSplitPane splitPane;
	private ShapePanel shapePanel;
	private FlowPanel flowPanel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main frame = new Main();
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Main() {
		initComponents();
	}

	private void initComponents() {
		setTitle("DND示例");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		contentPane.add(getSplitPane(), BorderLayout.CENTER);
	}

	private JSplitPane getSplitPane() {
		if (splitPane == null) {
			splitPane = new JSplitPane();
			splitPane.setLeftComponent(getShapePanel());
			splitPane.setRightComponent(getFlowPanel());
		}
		return splitPane;
	}

	private ShapePanel getShapePanel() {
		if (shapePanel == null) {
			shapePanel = new ShapePanel();
		}
		return shapePanel;
	}

	private FlowPanel getFlowPanel() {
		if (flowPanel == null) {
			flowPanel = new FlowPanel();
		}
		return flowPanel;
	}
}
