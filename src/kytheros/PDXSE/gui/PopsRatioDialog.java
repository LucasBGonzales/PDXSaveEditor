package kytheros.PDXSE.gui;

import java.awt.Container;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import kytheros.util.swing.SwingMisc;

/**
 * Specialized {@link JDialog} to get and provide an {@link Integer}
 * array
 * representing a ratio of pop types.
 * 
 * Use {@link PopsRatioDialog#runDialog() showDialog} to use this
 * dialog.
 * 
 * @author kytheros
 */
public class PopsRatioDialog extends JDialog {
	private static final long serialVersionUID = 7529802802667079039L;
	public int[] ratio;


	/**
	 * Create GUI
	 */
	public PopsRatioDialog() {
		this.setModalityType(ModalityType.APPLICATION_MODAL);

		// GUI //
		Container contentPane = getContentPane();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		((JComponent) contentPane).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// JLabel
		JTextArea lblDisplay = new JTextArea("Enter ratio of pops:\nN:C:F:T:S");
		lblDisplay.setEditable(false);
		lblDisplay.setOpaque(false);
		lblDisplay.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		contentPane.add(lblDisplay);

		// Ratio Boxes
		JPanel pnlRatios = new JPanel();
		pnlRatios.setLayout(new BoxLayout(pnlRatios, BoxLayout.X_AXIS));

		JTextField txtNobles = new JTextField("1");
		pnlRatios.add(txtNobles);
		pnlRatios.add(new JLabel(":"));

		JTextField txtCitizens = new JTextField("1");
		pnlRatios.add(txtCitizens);
		pnlRatios.add(new JLabel(":"));

		JTextField txtFreemen = new JTextField("1");
		pnlRatios.add(txtFreemen);
		pnlRatios.add(new JLabel(":"));

		JTextField txtTribesmen = new JTextField("0");
		pnlRatios.add(txtTribesmen);
		pnlRatios.add(new JLabel(":"));

		JTextField txtSlaves = new JTextField("1");
		pnlRatios.add(txtSlaves);

		contentPane.add(pnlRatios);

		// Confirm Button
		JButton btnConfirm = new JButton("Confirm");
		btnConfirm.addActionListener(e -> {
			String br = ":";
			String[] parts = (txtNobles.getText() + br + txtCitizens.getText() + br + txtFreemen.getText() + br
					+ txtTribesmen.getText() + br + txtSlaves.getText()).split(br);
			ratio = new int[parts.length];
			for (int i = 0; i < parts.length; i++)
				ratio[i] = Integer.valueOf(parts[i]);
			this.setVisible(false);
		});
		contentPane.add(btnConfirm);

		this.pack();
		SwingMisc.centerWindow(this);
	}


	/**
	 * Use this function to run the dialog and get the result.
	 * 
	 * @return
	 */
	public int[] runDialog() {
		this.setVisible(true);
		return ratio;
	}
}
