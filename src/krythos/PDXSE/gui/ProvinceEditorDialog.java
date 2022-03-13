package krythos.PDXSE.gui;

import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import krythos.PDXSE.database.DataNode;
import krythos.util.logger.Log;
import krythos.util.swing.SwingMisc;

public class ProvinceEditorDialog extends JDialog {
	private static final long serialVersionUID = 6501163777060294147L;
	private DataNode province;
	JComboBox<String> cbxClaims, cbxProvRank, cbxTradeGood;
	JTextField txtName, txtOwner, txtCivValue, txtTradeGood_o;
	JLabel lblName, lblOwner, lblClaims, lblCivValue, lblTradeGood, lblProvRank;


	public ProvinceEditorDialog(DataNode province) {
		this.province = province;

		this.setModalityType(ModalityType.APPLICATION_MODAL);
		this.setTitle("Province Editor");

		Container cp = this.getContentPane();
		SpringLayout sp = new SpringLayout();
		cp.setLayout(sp);

		lblName = new JLabel("Province Name: ");
		cp.add(lblName);
		txtName = new JTextField("Default");
		txtName.setEditable(true);
		cp.add(txtName);

		lblOwner = new JLabel("Province Owner: ");
		cp.add(lblOwner);
		txtOwner = new JTextField("Default");
		txtOwner.setEditable(true);
		cp.add(txtOwner);

		lblClaims = new JLabel("Claims: ");
		cp.add(lblClaims);
		cbxClaims = new JComboBox<String>();
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
		cp.add(cbxClaims);

		lblCivValue = new JLabel("Civilization Value:");
		cp.add(lblCivValue);
		txtCivValue = new JTextField("Default");
		txtCivValue.setEditable(true);
		cp.add(txtCivValue);

		lblTradeGood = new JLabel("Trade Goods:");
		cp.add(lblTradeGood);
		cbxTradeGood = new JComboBox<String>(new String[] { "\"iron\"", "\"horses\"", "\"wood\"", "\"elephants\"",
				"\"steppe_horses\"", "\"camel\"", "\"grain\"", "\"salt\"", "\"fish\"", "\"cattle\"", "\"honey\"",
				"\"vegetables\"", "\"papyrus\"", "\"cloth\"", "\"dye\"", "\"marble\"", "\"incense\"", "\"silk\"",
				"\"amber\"", "\"spices\"", "\"precious_metals\"", "\"earthware\"", "\"gems\"", "\"glass\"", "\"wine\"",
				"\"leather\"", "\"base_metals\"", "\"hemp\"", "\"dates\"", "\"stone\"", "\"wild_game\"", "\"fur\"",
				"\"olive\"", "\"woad\"" });
		cbxTradeGood.setEditable(false);
		cp.add(cbxTradeGood);
		// txtTradeGood = new JTextField("Default");
		// txtTradeGood.setEditable(true);
		// cp.add(txtTradeGood);

		lblProvRank = new JLabel("Default");
		cp.add(lblProvRank);
		cbxProvRank = new JComboBox<String>(new String[] { "\"settlement\"", "\"city\"", "\"city_metropolis\"" });
		cbxProvRank.setEditable(false);
		cp.add(cbxProvRank);

		JButton btnConfirm = new JButton("Confirm");
		btnConfirm.addActionListener(e -> confirm());
		cp.add(btnConfirm);
		JButton btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(e -> close());
		cp.add(btnCancel);

		// Name
		sp.putConstraint(SpringLayout.NORTH, lblName, 5, SpringLayout.NORTH, cp);
		sp.putConstraint(SpringLayout.WEST, lblName, 5, SpringLayout.WEST, cp);
		sp.putConstraint(SpringLayout.EAST, lblName, 110, SpringLayout.WEST, lblName);
		sp.putConstraint(SpringLayout.WEST, txtName, 5, SpringLayout.EAST, lblName);
		sp.putConstraint(SpringLayout.NORTH, txtName, 5, SpringLayout.NORTH, cp);
		sp.putConstraint(SpringLayout.WEST, txtName, 5, SpringLayout.EAST, lblName);
		sp.putConstraint(SpringLayout.EAST, txtName, 100, SpringLayout.WEST, txtName);

		// Owner
		sp.putConstraint(SpringLayout.NORTH, lblOwner, 5, SpringLayout.SOUTH, lblName);
		sp.putConstraint(SpringLayout.WEST, lblOwner, 5, SpringLayout.WEST, cp);
		sp.putConstraint(SpringLayout.EAST, lblOwner, 0, SpringLayout.EAST, lblName);

		sp.putConstraint(SpringLayout.NORTH, txtOwner, 5, SpringLayout.SOUTH, lblName);
		sp.putConstraint(SpringLayout.WEST, txtOwner, 5, SpringLayout.EAST, lblOwner);
		sp.putConstraint(SpringLayout.EAST, txtOwner, 0, SpringLayout.EAST, txtName);

		// Claims
		sp.putConstraint(SpringLayout.NORTH, lblClaims, 5, SpringLayout.SOUTH, lblOwner);
		sp.putConstraint(SpringLayout.WEST, lblClaims, 5, SpringLayout.WEST, cp);
		sp.putConstraint(SpringLayout.EAST, lblClaims, 0, SpringLayout.EAST, lblName);

		sp.putConstraint(SpringLayout.NORTH, cbxClaims, 5, SpringLayout.SOUTH, lblOwner);
		sp.putConstraint(SpringLayout.WEST, cbxClaims, 5, SpringLayout.EAST, lblClaims);
		sp.putConstraint(SpringLayout.EAST, cbxClaims, 0, SpringLayout.EAST, txtName);

		// Civilization Value
		sp.putConstraint(SpringLayout.NORTH, lblCivValue, 5, SpringLayout.SOUTH, cbxClaims);
		sp.putConstraint(SpringLayout.WEST, lblCivValue, 5, SpringLayout.WEST, cp);
		sp.putConstraint(SpringLayout.EAST, lblCivValue, 0, SpringLayout.EAST, lblName);

		sp.putConstraint(SpringLayout.NORTH, txtCivValue, 0, SpringLayout.NORTH, lblCivValue);
		sp.putConstraint(SpringLayout.WEST, txtCivValue, 5, SpringLayout.EAST, lblCivValue);
		sp.putConstraint(SpringLayout.EAST, txtCivValue, 0, SpringLayout.EAST, txtName);

		// Trade Good
		sp.putConstraint(SpringLayout.NORTH, lblTradeGood, 5, SpringLayout.SOUTH, lblCivValue);
		sp.putConstraint(SpringLayout.WEST, lblTradeGood, 5, SpringLayout.WEST, cp);
		sp.putConstraint(SpringLayout.EAST, lblTradeGood, 0, SpringLayout.EAST, lblName);

		sp.putConstraint(SpringLayout.NORTH, cbxTradeGood, 5, SpringLayout.SOUTH, lblCivValue);
		sp.putConstraint(SpringLayout.WEST, cbxTradeGood, 5, SpringLayout.EAST, lblTradeGood);
		sp.putConstraint(SpringLayout.EAST, cbxTradeGood, 0, SpringLayout.EAST, txtName);

		// Province Rank
		sp.putConstraint(SpringLayout.NORTH, lblProvRank, 5, SpringLayout.SOUTH, cbxTradeGood);
		sp.putConstraint(SpringLayout.WEST, lblProvRank, 5, SpringLayout.WEST, cp);
		sp.putConstraint(SpringLayout.EAST, lblProvRank, 0, SpringLayout.EAST, lblName);

		sp.putConstraint(SpringLayout.NORTH, cbxProvRank, 5, SpringLayout.NORTH, lblProvRank);
		sp.putConstraint(SpringLayout.WEST, cbxProvRank, 5, SpringLayout.EAST, lblProvRank);
		sp.putConstraint(SpringLayout.EAST, cbxProvRank, 0, SpringLayout.EAST, txtName);

		// Buttons
		sp.putConstraint(SpringLayout.NORTH, btnConfirm, 5, SpringLayout.SOUTH, cbxProvRank);
		sp.putConstraint(SpringLayout.WEST, btnConfirm, 5, SpringLayout.WEST, cp);
		sp.putConstraint(SpringLayout.EAST, btnConfirm, 0, SpringLayout.EAST, lblName);

		sp.putConstraint(SpringLayout.NORTH, btnCancel, 5, SpringLayout.SOUTH, cbxProvRank);
		sp.putConstraint(SpringLayout.WEST, btnCancel, 5, SpringLayout.EAST, btnConfirm);
		sp.putConstraint(SpringLayout.EAST, btnCancel, 0, SpringLayout.EAST, txtName);

		// Frame
		sp.putConstraint(SpringLayout.EAST, cp, 5, SpringLayout.EAST, txtName);
		sp.putConstraint(SpringLayout.SOUTH, cp, 5, SpringLayout.SOUTH, btnConfirm);

		initValues();

		this.setResizable(false);
		pack();
		SwingMisc.centerWindow(this);
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


	/**
	 * Close the dialog.
	 */
	private void close() {
		this.setVisible(false);
		this.dispose();
	}


	/**
	 * Change the province's information and close the dialog.
	 */
	private void confirm() {
		if (province != null) {
			province.find(Arrays.asList("province_name", "name")).setKeyValue(txtName.getText());
			province.find("owner").setKeyValue(txtOwner.getText());
			province.find("civilization_value").setKeyValue(txtCivValue.getText());
			province.find("trade_goods").setKeyValue(cbxTradeGood.getSelectedItem().toString());
			province.find("province_rank").setKeyValue(cbxProvRank.getSelectedItem().toString());

			// Remove existing claims, add the claims in the editor.
			province.removeNode("claim", true);
			for (int i = 0; i < cbxClaims.getItemCount(); i++)
				province.addNode(new DataNode("claim", new DataNode(cbxClaims.getItemAt(i).toString()), false));

		}
		close();
	}


	private void initValues() {
		if (province != null) {
			txtName.setText(province.find(Arrays.asList("province_name", "name")).getKeyValue());
			txtOwner.setText(province.find("owner").getKeyValue());
			txtCivValue.setText(province.find("civilization_value").getKeyValue());
			// txtTradeGood.setText(province.find("trade_goods").getKeyValue());
			cbxTradeGood.setSelectedItem(province.find("trade_goods").getKeyValue());
			cbxProvRank.setSelectedItem(province.find("province_rank").getKeyValue());

			for (DataNode c : province.findAll("claim"))
				cbxClaims.addItem(c.getKeyValue());
		}

	}
}
