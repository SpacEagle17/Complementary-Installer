package net.hypercubemc.iris_installer.layouts;

import net.fabricmc.installer.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class Settings extends JFrame {
    public Settings() {
        super("Complementary Installer Settings");

        setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("comp_icon.png"))).getImage());
        setMaximumSize(new java.awt.Dimension(400, 500));
        setMinimumSize(new java.awt.Dimension(400, 500));
        setPreferredSize(new java.awt.Dimension(400, 500));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        java.awt.GridBagConstraints gridBagConstraints;

        JButton doneButton = new javax.swing.JButton();
        doneButton.setFont(doneButton.getFont().deriveFont((float)16));
        doneButton.setText("Done");
        doneButton.setToolTipText("");
        doneButton.setMargin(new java.awt.Insets(10, 30, 10, 30));
        doneButton.setMaximumSize(new java.awt.Dimension(320, 45));
        doneButton.setMinimumSize(new java.awt.Dimension(173, 45));
        doneButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                JFrame SettingsFrame = (JFrame) SwingUtilities.getWindowAncestor((JButton) evt.getSource());
                SettingsFrame.setVisible(false);
            }
        });
        doneButton.putClientProperty( "JButton.buttonType", "roundRect" );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        getContentPane().add(doneButton, gridBagConstraints);

        int x = (Toolkit.getDefaultToolkit().getScreenSize().width - getWidth()) / 2;
        int y = (Toolkit.getDefaultToolkit().getScreenSize().height - getHeight()) / 2;
        setLocation(x, y);
    }
}