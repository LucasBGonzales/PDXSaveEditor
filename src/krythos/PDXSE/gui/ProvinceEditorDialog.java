package krythos.PDXSE.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import krythos.PDXSE.database.DataNode;
import krythos.util.logger.Log;
import krythos.util.swing.SwingMisc;

public class ProvinceEditorDialog extends JDialog {
	private static final long serialVersionUID = 6501163777060294147L;
	private DataNode province;
	JComboBox<Object> cbxClaims;
	JTextField txtName, txtOwner, txtCivValue, txtTradeGood, txtProvRank;
	JLabel lblName, lblOwner, lblClaims, lblCivValue, lblTradeGood, lblProvRank;


	public ProvinceEditorDialog(DataNode province) {
		this.province = province;

		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setTitle("Province Editor");

		Container cp = this.getContentPane();
		((JComponent) cp).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JPanel pnlLabels = new JPanel();
		pnlLabels.setLayout(new BoxLayout(pnlLabels, BoxLayout.PAGE_AXIS));

		JPanel pnlInputs = new JPanel();
		pnlInputs.setLayout(new BoxLayout(pnlInputs, BoxLayout.PAGE_AXIS));

		lblName = new JLabel("Province Name: ");
		pnlLabels.add(lblName);
		txtName = new JTextField("Default");
		txtName.setEditable(true);
		pnlInputs.add(txtName);

		lblOwner = new JLabel("Province Owner: ");
		pnlLabels.add(lblOwner);
		txtOwner = new JTextField("Default");
		txtOwner.setEditable(true);
		pnlInputs.add(txtOwner);

		lblClaims = new JLabel("Claims: ");
		pnlLabels.add(lblClaims);
		cbxClaims = new JComboBox<Object>();
		cbxClaims.setEditable(true);
		cbxClaims.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					// Get Selected Item
					String entered = (String) cbxClaims.getSelectedItem();

					// Determine if it already exists in the JComboBox.
					boolean f_dup = false;
					for (int i = 0; i < cbxClaims.getItemCount(); i++) {
						if (cbxClaims.getItemAt(i).toString().equals(entered)) {
							f_dup = true;
							break;
						}
					}

					// If it doesn't exist, add it to the JComboBox
					if (!f_dup)
						cbxClaims.addItem(entered);
				} else if (e.getKeyCode() == KeyEvent.VK_DELETE)
					cbxClaims.removeItem(cbxClaims.getSelectedItem());

				else if (e.getKeyCode() == KeyEvent.VK_P) {
					Log.info("Packing");
					pack();
				}

			}
		});
		pnlInputs.add(cbxClaims);

		lblCivValue = new JLabel("Civilization Value:");
		pnlLabels.add(lblCivValue);
		txtCivValue = new JTextField("Default");
		txtCivValue.setEditable(true);
		pnlInputs.add(txtCivValue);

		lblTradeGood = new JLabel("Trade Goods:");
		pnlLabels.add(lblTradeGood);
		txtTradeGood = new JTextField("Default");
		txtTradeGood.setEditable(true);
		pnlInputs.add(txtTradeGood);

		lblProvRank = new JLabel("Province Rank:");
		pnlLabels.add(lblProvRank);
		txtProvRank = new JTextField("Default");
		txtProvRank.setEditable(true);
		pnlInputs.add(txtProvRank);

		initValues();

		cp.add(pnlLabels,BorderLayout.LINE_START);
		cp.add(pnlInputs,BorderLayout.LINE_END);
		
		// Confirm Button
		JButton btnConfirm = new JButton("Confirm");
		btnConfirm.addActionListener(e -> {
			confirm();
		});
		cp.add(btnConfirm,BorderLayout.PAGE_END);

		pack();
		SwingMisc.centerWindow(this);
	}
	
	private void confirm() {
		this.setVisible(false);
		this.dispose();
	}


	private void initValues() {
		if (province != null) {
			txtName.setText(province.find(Arrays.asList("province_name", "name")).getKeyValue());
			txtOwner.setText(province.find("owner").getKeyValue());
			txtCivValue.setText(province.find("civilization_value").getKeyValue());
			txtTradeGood.setText(province.find("trade_goods").getKeyValue());
			txtProvRank.setText(province.find("province_rank").getKeyValue());

			for (DataNode c : province.findAll("claim"))
				cbxClaims.addItem(c.getKeyValue());
		}

	}


	/**
	 * Use this function to run the dialog and get the result.
	 * 
	 * @return Edited province.
	 */
	public DataNode runDialog() {
		this.setVisible(true);
		return province;
	}
}
