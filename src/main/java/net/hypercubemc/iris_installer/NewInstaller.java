/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package net.hypercubemc.iris_installer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;

import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Utils;
import net.hypercubemc.iris_installer.layouts.Settings;
import org.json.JSONException;

/**
 *
 * @author ims --- and Emin (less so) and also SpacEagle17 (also less so)
 */
@SuppressWarnings("serial")
public class NewInstaller extends JFrame {

    private static boolean dark = false;
    private boolean installAsMod;
    private boolean styleIsUnbound = true;
    private String outdatedPlaceholder = "Warning: Iris shader loader has ended support for <version>.";
    private String snapshotPlaceholder = "Warning: <version> is a snapshot build and may";
    private String BASE_URL = "https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Files/master/";
    private boolean finishedSuccessfulInstall;
    private InstallerMeta.Version selectedVersion;
    private final List<InstallerMeta.Version> GAME_VERSIONS;
    private final InstallerMeta INSTALLER_META;
    private Path customInstallDir;

    Settings settings = new Settings();

    /**
     * Creates new form Installer
     */
    public NewInstaller() {
        super("Euphoria Patches Installer");
        Main.LOADER_META = new MetaHandler(("v2/versions/loader"));

        try {
            Main.LOADER_META.load();
        } catch (IOException e) {
            System.out.println("Failed to fetch fabric version info from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch fabric version info from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }

        INSTALLER_META = new InstallerMeta(BASE_URL + "meta-new.json");

        try {
            INSTALLER_META.load();
        } catch (IOException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch metadata from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        } catch (JSONException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact the Iris support team via Discord! \nError: " + e, "Metadata Parsing Failed!", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }

        GAME_VERSIONS = INSTALLER_META.getVersions();
        Collections.reverse(GAME_VERSIONS);
        selectedVersion = GAME_VERSIONS.get(0);

        initComponents();

        // Change outdated version text color based on dark mode
        if (!dark) {
            Color newTextColor = new Color(154, 136, 63, 255);

            outdatedText1.setForeground(newTextColor);
            outdatedText2.setForeground(newTextColor);
        }

        gameVersionList.removeAllItems();

        for (InstallerMeta.Version version : GAME_VERSIONS) {
            gameVersionList.addItem(version.name);
        }

        // Set default dir (.minecraft)
        directoryName.setText(getDefaultInstallDir().toFile().getName());

        // Hide outdated version text
        outdatedText1.setVisible(false);
        outdatedText2.setVisible(false);
    }

    public Path getStorageDirectory() {
        return getAppDataDirectory().resolve(getStorageDirectoryName());
    }

    public Path getInstallDir() {
        return customInstallDir != null ? customInstallDir : getDefaultInstallDir();
    }

    public Path getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA")).toPath();
        } else if (os.contains("mac")) {
            return new File(System.getProperty("user.home") + "/Library/Application Support").toPath();
        } else if (os.contains("nux")) {
            return new File(System.getProperty("user.home")).toPath();
        } else {
            return new File(System.getProperty("user.dir")).toPath();
        }
    }

    public String getStorageDirectoryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return "iris-installer";
        } else {
            return ".iris-installer";
        }
    }

    private Path getDefaultInstallDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            return getAppDataDirectory().resolve("minecraft");
        } else {
            return getAppDataDirectory().resolve(".minecraft");
        }
    }

    public Path getVanillaGameDir() {
        String os = System.getProperty("os.name").toLowerCase();

        return os.contains("mac") ? getAppDataDirectory().resolve("minecraft") : getAppDataDirectory().resolve(".minecraft");
    }

    public boolean installFromZip(File zip) {
        try {
            int BUFFER_SIZE = 2048; // Buffer Size
            try ( ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zip.toPath()))) {
                ZipEntry entry = zipIn.getNextEntry();
                // iterates over entries in the zip file
                if (!installAsMod) {
                    getInstallDir().resolve("iris-reserved/").toFile().mkdir();
                }

                while (entry != null) {
                    String entryName = entry.getName();

                    if (!installAsMod && entryName.startsWith("mods/")) {
                        entryName = entryName.replace("mods/", "iris-reserved/" + selectedVersion + "/");
                    }

                    File filePath = getInstallDir().resolve(entryName).toFile();
                    if (!entry.isDirectory()) {
                        try ( // if the entry is a file, extracts it
                                 BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(filePath.toPath()))) {
                            byte[] bytesIn = new byte[BUFFER_SIZE];
                            int read = 0;
                            while ((read = zipIn.read(bytesIn)) != -1) {
                                bos.write(bytesIn, 0, read);
                            }
                        }
                    } else {
                        // if the entry is a directory, make the directory
                        filePath.mkdir();
                    }
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void decryptEuphoriaPatches(File file) throws Exception {
        SecretKeySpec key = new SecretKeySpec(new byte[]{-93, 70, -5, -49, -51, -113, 103, 109, 69, 18, -13, 63, -106, -18, 115, 6}, "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[]{-91, -62, 93, 55, 58, 21, -60, -82, 82, -54, 87, -96, -88, 112, 45, -105});

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        fileBytes = cipher.doFinal(fileBytes);

        Files.write(file.toPath(), fileBytes);
    }

    private boolean isInternetAvailable() {
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000); // 3 second timeout
            connection.setReadTimeout(3000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (responseCode == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean showNetworkErrorDialog(String operationName) {
        String message = "Internet connection lost while " + operationName + ".\n" +
                        "Please check your connection and try again.";
        
        int response = JOptionPane.showConfirmDialog(
            this,
            message,
            "Network Connection Error",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE
        );
        
        // Reset UI elements to enable retry
        if (response == JOptionPane.YES_OPTION) {
            installButton.setEnabled(true);
            installButton.setText("Install");
            return true; // User wants to retry
        } else {
            installButton.setEnabled(true);
            installButton.setText("Install");
            progressBar.setValue(0);
            return false; // User canceled
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay(0);
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
        java.awt.GridBagConstraints gridBagConstraints;

        installType = new javax.swing.ButtonGroup();
        irisInstallerLabel = new javax.swing.JLabel();
        linkLabel = new javax.swing.JLabel();
        gameVersionLabel = new javax.swing.JLabel();
        outdatedText1 = new javax.swing.JLabel();
        outdatedText2 = new javax.swing.JLabel();
        advancedSettingsButton = new javax.swing.JButton();
        installationType = new javax.swing.JLabel();
        installationDirectory = new javax.swing.JLabel();
        installationTypesContainer = new javax.swing.JPanel();
        standaloneType = new javax.swing.JRadioButton();
        fabricType = new javax.swing.JRadioButton();
        unboundType = new javax.swing.JRadioButton();
        reimaginedType = new javax.swing.JRadioButton();
        styleType = new javax.swing.ButtonGroup();
        visualStyleContainer = new javax.swing.JPanel();
        gameVersionList = new javax.swing.JComboBox<>();
        directoryName = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        installButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("euphoriaPatchesIcon.png"))).getImage());
        setMaximumSize(new java.awt.Dimension(480, 600));
        setMinimumSize(new java.awt.Dimension(480, 600));
        setPreferredSize(new java.awt.Dimension(480, 600));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        irisInstallerLabel.setFont(irisInstallerLabel.getFont().deriveFont((float)36));
        irisInstallerLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        irisInstallerLabel.setIcon(new javax.swing.ImageIcon(Objects.requireNonNull(getClass().getResource("/euphoriaPatchesDetailed.png")))); // NOI18N
        irisInstallerLabel.setText(" Euphoria Patches");
        irisInstallerLabel.setMaximumSize(new java.awt.Dimension(350, 64));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        getContentPane().add(irisInstallerLabel, gridBagConstraints);

        //Shader Style Selector//
        visualStyleContainer.setLayout(new java.awt.BorderLayout(10, 0));
            styleType.add(unboundType);
            unboundType.setFont(unboundType.getFont().deriveFont((float)16));
            unboundType.setSelected(true);
            unboundType.setText("Unbound Style");
            unboundType.setToolTipText("");
            unboundType.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    unboundStyleMouseClicked(evt);
                }
            });
            visualStyleContainer.add(unboundType, java.awt.BorderLayout.LINE_START);

            styleType.add(reimaginedType);
            reimaginedType.setFont(reimaginedType.getFont().deriveFont((float)16));
            reimaginedType.setText("Reimagined Style");
            reimaginedType.setToolTipText("");
            reimaginedType.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    reimaginedStyleMouseClicked(evt);
                }
            });
            visualStyleContainer.add(reimaginedType, java.awt.BorderLayout.LINE_END);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        getContentPane().add(visualStyleContainer, gridBagConstraints);

        String linkText = dark ? "<html><a href='https://www.complementary.dev/shaders/#style-section' style='color:#E94BC1FF'>What's the difference?</a></html>" :
                                 "<html><a href='https://www.complementary.dev/shaders/#style-section' style='color:#BD2E92FF'>What's the difference?</a></html>";
        linkLabel.setText(linkText);
        linkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                openURL(URI.create("https://www.complementary.dev/shaders/#style-section"));
            }
        });
        linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
        getContentPane().add(linkLabel, gridBagConstraints);
        //Shader Style Selector//

        gameVersionLabel.setFont(gameVersionLabel.getFont().deriveFont(gameVersionLabel.getFont().getStyle() | java.awt.Font.BOLD, 16));
        gameVersionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gameVersionLabel.setText("Select Minecraft version:");
        gameVersionLabel.setToolTipText("");
        gameVersionLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        gameVersionLabel.setMaximumSize(new java.awt.Dimension(300, 24));
        gameVersionLabel.setMinimumSize(new java.awt.Dimension(168, 24));
        gameVersionLabel.setPreferredSize(new java.awt.Dimension(168, 24));
        gameVersionLabel.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        getContentPane().add(gameVersionLabel, gridBagConstraints);
        gameVersionList.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        gameVersionList.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1.19", "1.18.2", "1.17.1", "1.16.5" }));
        gameVersionList.setMaximumSize(new java.awt.Dimension(168, 35));
        gameVersionList.setMinimumSize(new java.awt.Dimension(168, 35));
        gameVersionList.setPreferredSize(new java.awt.Dimension(168, 35));
        gameVersionList.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                gameVersionListItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        getContentPane().add(gameVersionList, gridBagConstraints);

        outdatedText1.setFont(outdatedText1.getFont().deriveFont((float)16));
        outdatedText1.setForeground(new java.awt.Color(255, 204, 0));
        outdatedText1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        outdatedText1.setText("Warning: Iris shader loader has ended support for <version>.");
        outdatedText1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        outdatedText1.setMaximumSize(new java.awt.Dimension(400, 21));
        outdatedText1.setMinimumSize(new java.awt.Dimension(310, 21));
        outdatedText1.setPreferredSize(new java.awt.Dimension(310, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        getContentPane().add(outdatedText1, gridBagConstraints);

        outdatedText2.setFont(outdatedText2.getFont().deriveFont((float)16));
        outdatedText2.setForeground(new java.awt.Color(255, 204, 0));
        outdatedText2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        outdatedText2.setText("The Iris version you get will most likely be outdated.");
        outdatedText2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        outdatedText2.setMaximumSize(new java.awt.Dimension(450, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(outdatedText2, gridBagConstraints);

        advancedSettingsButton.setFont(advancedSettingsButton.getFont().deriveFont((float)16));
        advancedSettingsButton.setText("Advanced Settings");
        advancedSettingsButton.setToolTipText("");
        advancedSettingsButton.setMargin(new java.awt.Insets(10, 30, 10, 30));
        advancedSettingsButton.setMaximumSize(new java.awt.Dimension(320, 45));
        advancedSettingsButton.setMinimumSize(new java.awt.Dimension(173, 45));
        advancedSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                settings.setVisible(true);
            }
        });
        advancedSettingsButton.putClientProperty( "JButton.buttonType", "roundRect" );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(25, 0, 0, 0);
        getContentPane().add(advancedSettingsButton, gridBagConstraints);

        progressBar.setFont(progressBar.getFont().deriveFont((float)16));
        progressBar.setAlignmentX(0.0F);
        progressBar.setAlignmentY(0.0F);
        progressBar.setMaximumSize(new java.awt.Dimension(380, 25));
        progressBar.setMinimumSize(new java.awt.Dimension(380, 25));
        progressBar.setPreferredSize(new java.awt.Dimension(380, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        getContentPane().add(progressBar, gridBagConstraints);

        installButton.setFont(installButton.getFont().deriveFont((float)20).deriveFont(1));
        installButton.setText("Install");
        installButton.setToolTipText("");
        installButton.setMargin(new java.awt.Insets(10, 70, 10, 70));
        installButton.setMaximumSize(new java.awt.Dimension(320, 45));
        installButton.setMinimumSize(new java.awt.Dimension(173, 45));
        installButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                installButtonMouseClicked(evt);
            }
        });
        installButton.putClientProperty( "JButton.buttonType", "roundRect" );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 30, 0);
        getContentPane().add(installButton, gridBagConstraints);

        installationDirectory.setFont(installationDirectory.getFont().deriveFont(installationDirectory.getFont().getStyle() | java.awt.Font.BOLD, 16));
        installationDirectory.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        installationDirectory.setText("Installation directory:");
        installationDirectory.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        installationDirectory.setMaximumSize(new java.awt.Dimension(300, 24));
        installationDirectory.setMinimumSize(new java.awt.Dimension(165, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        settings.add(installationDirectory, gridBagConstraints);

        directoryName.setFont(directoryName.getFont().deriveFont((float)16));
        directoryName.setLabel("Directory Name");
        directoryName.setMaximumSize(new java.awt.Dimension(300, 36));
        directoryName.setMinimumSize(new java.awt.Dimension(300, 36));
        directoryName.setPreferredSize(new java.awt.Dimension(300, 36));
        directoryName.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                directoryNameMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        settings.add(directoryName, gridBagConstraints);

        installationType.setFont(installationType.getFont().deriveFont(installationType.getFont().getStyle() | java.awt.Font.BOLD, 16));
        installationType.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        installationType.setText(" Installation type:");
        installationType.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        installationType.setMaximumSize(new java.awt.Dimension(300, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        settings.add(installationType, gridBagConstraints);

        installationTypesContainer.setLayout(new java.awt.BorderLayout(10, 0));
            installType.add(standaloneType);
            standaloneType.setFont(standaloneType.getFont().deriveFont((float)16));
            standaloneType.setSelected(true);
            standaloneType.setText("Iris Only");
            standaloneType.setToolTipText("Installs Iris + Sodium by itself, without any mods. And adds Complementary + Euphoria Patches.");
            standaloneType.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    standaloneTypeMouseClicked(evt);
                }
            });
            installationTypesContainer.add(standaloneType, java.awt.BorderLayout.LINE_START);

            installType.add(fabricType);
            fabricType.setFont(fabricType.getFont().deriveFont((float)16));
            fabricType.setText("Iris + Fabric");
            fabricType.setToolTipText("This installs Iris + Sodium alongside an installation of Fabric to be able to use other mods. Then adds Complementary + Euphoria Patches.");
            fabricType.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    fabricTypeMouseClicked(evt);
                }
            });
            installationTypesContainer.add(fabricType, java.awt.BorderLayout.LINE_END);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 4;
            gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        settings.add(installationTypesContainer, gridBagConstraints);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void directoryNameMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_directoryNameMouseClicked
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setFileHidingEnabled(false);

        int option = fileChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            customInstallDir = file.toPath();
            directoryName.setText(file.getName());
        }
    }//GEN-LAST:event_directoryNameMouseClicked

    private void gameVersionListItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_gameVersionListItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            selectedVersion = GAME_VERSIONS.stream().filter(v -> v.name.equals(evt.getItem())).findFirst().orElse(GAME_VERSIONS.get(0));

            if (selectedVersion.outdated) {
                outdatedText1.setText(outdatedPlaceholder.replace("<version>", selectedVersion.name));
                outdatedText1.setVisible(true);
                outdatedText2.setText("The Iris version you get will most likely be outdated.");
                outdatedText2.setVisible(true);
            } else if (selectedVersion.snapshot) {
                outdatedText1.setText(snapshotPlaceholder.replace("<version>", selectedVersion.name));
                outdatedText1.setVisible(true);
                outdatedText2.setText("lose support at any time.");
                outdatedText2.setVisible(true);
            } else {
                outdatedText1.setVisible(false);
                outdatedText2.setVisible(false);
            }
        }
    }//GEN-LAST:event_gameVersionListItemStateChanged

    private void standaloneTypeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_standaloneTypeMouseClicked
        installAsMod = false;
    }//GEN-LAST:event_standaloneTypeMouseClicked
    private void fabricTypeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fabricTypeMouseClicked
        installAsMod = true;
    }//GEN-LAST:event_fabricTypeMouseClicked

    private void unboundStyleMouseClicked(java.awt.event.MouseEvent evt) {
        styleIsUnbound = true;
    }
    private void reimaginedStyleMouseClicked(java.awt.event.MouseEvent evt) {
        styleIsUnbound = false;
    }

    private void installButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_installButtonMouseClicked
        if (!isInternetAvailable()) {
            JOptionPane.showMessageDialog(
                this,
                "No internet connection detected. Please check your connectivity and try again.",
                "Network Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        installButton.setText("Downloading...");
        installButton.setEnabled(false);
        progressBar.setForeground(new Color(199, 21, 133));
        progressBar.setValue(0);

        String loaderName = installAsMod ? "fabric-loader" : "iris-fabric-loader";

        try {
            URL loaderVersionUrl = new URL("https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Maven/master/latest-loader");
            String profileName = installAsMod ? "Fabric Loader " : "Iris & Sodium for ";
            VanillaLauncherIntegration.Icon profileIcon = installAsMod ? VanillaLauncherIntegration.Icon.FABRIC : VanillaLauncherIntegration.Icon.IRIS;
            Path modsFolder0 = installAsMod ? getInstallDir().resolve("mods") : getInstallDir().resolve("iris-reserved").resolve(selectedVersion.name);
            String loaderVersion = Main.LOADER_META.getLatestVersion(false).getVersion();
            boolean success = VanillaLauncherIntegration.installToLauncher(this, getVanillaGameDir(), getInstallDir(), modsFolder0, profileName + selectedVersion.name, selectedVersion.name, loaderName, loaderVersion, profileIcon);
            if (!success) {
                System.out.println("Failed to install to launcher, canceling!");
                return;
            }
        } catch (IOException e) {
            System.out.println("Failed to install version and profile to vanilla launcher!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to install to vanilla launcher, please contact the Iris support team via Discord! \nError: " + e, "Failed to install to launcher", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File storageDir = getStorageDirectory().toFile();
        if (!storageDir.exists() || !storageDir.isDirectory()) {
            storageDir.mkdir();
        }

        String zipName = "Iris-Sodium-" + selectedVersion.name + ".zip";
        String irisDownURL = "https://github.com/IrisShaders/Iris-Installer-Files/releases/latest/download/" + zipName;
        File saveLocation = getStorageDirectory().resolve(zipName).toFile();

        final Downloader downloaderI = new Downloader(irisDownURL, saveLocation);
        downloaderI.addPropertyChangeListener(eventI -> {
            if ("progress".equals(eventI.getPropertyName())) {
                progressBar.setValue(((Integer) eventI.getNewValue() ) / 2);
            } else if (eventI.getNewValue() == SwingWorker.StateValue.DONE) {
                try {
                    downloaderI.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Failed to download Iris!");
                    e.getCause().printStackTrace();

                    String msg = String.format("An error occurred while attempting to download Iris and Sodium, please check your internet connection and try again! \nError: %s",
                            e.getCause().toString());
                    installButton.setEnabled(true);
                    installButton.setText("Download Failed!");
                    progressBar.setForeground(new Color(204, 0, 0));
                    progressBar.setValue(100);
                    JOptionPane.showMessageDialog(this,
                            msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }

                boolean cancelled = false;

                File installDir = getInstallDir().toFile();
                if (!installDir.exists() || !installDir.isDirectory()) {
                    installDir.mkdir();
                }

                File modsFolder = installAsMod ? getInstallDir().resolve("mods").toFile() : getInstallDir().resolve("iris-reserved").resolve(selectedVersion.name).toFile();
                File[] modsFolderContents = modsFolder.listFiles();

                if (modsFolderContents != null) {
                    boolean isEmpty = modsFolderContents.length == 0;

                    if (installAsMod && modsFolder.exists() && modsFolder.isDirectory() && !isEmpty) {
                        int result = JOptionPane.showConfirmDialog(this, "An existing mods folder was found in the selected game directory. Do you want to update/install iris?", "Mods Folder Detected",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (result != JOptionPane.YES_OPTION) {
                            cancelled = true;
                        }
                    }

                    if (!cancelled) {
                        boolean shownOptifineDialog = false;
                        boolean failedToRemoveOptifine = false;

                        for (File mod : modsFolderContents) {
                            if (mod.getName().toLowerCase().contains("optifine") || mod.getName().toLowerCase().contains("optifabric")) {
                                if (!shownOptifineDialog) {
                                    int result = JOptionPane.showOptionDialog(this, "Optifine was found in your mods folder, but Optifine is incompatible with Iris. Do you want to remove it, or cancel the installation?", "Optifine Detected",
                                            JOptionPane.DEFAULT_OPTION,
                                            JOptionPane.WARNING_MESSAGE, null, new String[]{"Yes", "Cancel"}, "Yes");

                                    shownOptifineDialog = true;
                                    if (result != JOptionPane.YES_OPTION) {
                                        cancelled = true;
                                        break;
                                    }
                                }

                                if (!mod.delete()) {
                                    failedToRemoveOptifine = true;
                                }
                            }
                        }

                        if (failedToRemoveOptifine) {
                            System.out.println("Failed to delete optifine from mods folder");
                            JOptionPane.showMessageDialog(this, "Failed to remove optifine from your mods folder, please make sure your game is closed and try again!", "Failed to remove optifine", JOptionPane.ERROR_MESSAGE);
                            cancelled = true;
                        }
                    }

                    if (!cancelled) {
                        boolean failedToRemoveIrisOrSodium = false;

                        for (File mod : modsFolderContents) {
                            if (mod.getName().toLowerCase().contains("iris") || mod.getName().toLowerCase().contains("sodium-fabric")) {
                                if (!mod.delete()) {
                                    failedToRemoveIrisOrSodium = true;
                                }
                            }
                        }

                        if (failedToRemoveIrisOrSodium) {
                            System.out.println("Failed to remove Iris or Sodium from mods folder to update them!");
                            JOptionPane.showMessageDialog(this, "Failed to remove iris and sodium from your mods folder to update them, please make sure your game is closed and try again!", "Failed to prepare mods for update", JOptionPane.ERROR_MESSAGE);
                            cancelled = true;
                        }
                    }
                }

                if (cancelled) {
                    installButton.setEnabled(true);
                    return;
                }

                if (!modsFolder.exists() || !modsFolder.isDirectory()) {
                    modsFolder.mkdir();
                }

                boolean installISuccess = installFromZip(saveLocation);

                if (installISuccess) {
                    final String finalShaderName;
                    try {
                        String url;
                        url = "https://raw.githubusercontent.com/EuphoriaPatches/Complementary-Installer-Files/main/epLatest.txt";
                        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));

                        String shaderName = in.readLine();
                        if (styleIsUnbound) {
                            shaderName = in.readLine();
                        }
                        finalShaderName = shaderName;

                        in.close();
                    } catch (IOException e) {
                        System.out.println("Failed to download Comp!");
                        e.getCause().printStackTrace();

                        String msg = String.format("An error occurred while attempting to download Complementary files, please check your internet connection and try again! \nError: (Code C1) %s",
                                e.getCause().toString());
                        installButton.setEnabled(true);
                        installButton.setText("Download Failed!");
                        progressBar.setForeground(new Color(204, 0, 0));
                        progressBar.setValue(100);
                        JOptionPane.showMessageDialog(this,
                                msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                        return;
                    }

                    String compDownURL;
                    String base64ep = Base64.getEncoder().withoutPadding().encodeToString(finalShaderName.getBytes());
                    compDownURL = "https://github.com/EuphoriaPatches/Complementary-Installer-Files/releases/download/release/" + base64ep;

                    final Downloader downloaderC = getDownloader(installDir, finalShaderName, compDownURL);

                    downloaderC.execute();
                } else {
                    installButton.setText("Failed!");
                    progressBar.setForeground(new Color(204, 0, 0));
                    System.out.println("Failed to install to mods folder!");
                    JOptionPane.showMessageDialog(this, "Failed to install to mods folder, please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        downloaderI.execute();
    }//GEN-LAST:event_installButtonMouseClicked

    private Downloader getDownloader(File installDir, String finalShaderName, String compDownURL) {
        // Add timeout settings for all connections
        System.setProperty("sun.net.client.defaultConnectTimeout", "5000");
        System.setProperty("sun.net.client.defaultReadTimeout", "5000");
        
        File shaderDir = new File(installDir, "shaderpacks");
        if (!shaderDir.exists() || !shaderDir.isDirectory()) {
            shaderDir.mkdir();
        }
        File shaderLoc = new File(shaderDir, finalShaderName);
        
        try {
            if (!isInternetAvailable()) {
                throw new IOException("Internet connection unavailable");
            }
            
            String confirmationURL = compDownURL.substring(0, compDownURL.lastIndexOf("/") + 1) + "aaaDownloadConfirmation.txt";
            File confirmationFile = new File(shaderDir, "aaaDownloadConfirmation.txt");
            
            // Download the confirmation file
            System.out.println("Downloading confirmation file from: " + confirmationURL);
            URL url = new URL(confirmationURL);
            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(confirmationFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            System.out.println("Could not download confirmation file: " + e.getMessage());
        }

        final Downloader downloaderC = new Downloader(compDownURL, shaderLoc);
        downloaderC.addPropertyChangeListener(eventC -> {
            if ("progress".equals(eventC.getPropertyName())) {
                progressBar.setValue(50 + ((Integer) eventC.getNewValue()) / 2);
            } else if (eventC.getNewValue() == SwingWorker.StateValue.DONE) {
                try {
                    downloaderC.get();
                    
                    if (!isInternetAvailable()) {
                        throw new IOException("Internet connection lost");
                    }
                    
                    decryptEuphoriaPatches(shaderLoc);
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Failed to download Comp!");
                    e.getCause().printStackTrace();

                    String msg;
                    if (e.getCause() instanceof java.net.UnknownHostException || 
                        e.getCause() instanceof java.net.SocketTimeoutException) {
                        msg = "Internet connection lost while downloading shader pack. Please check your connection and try again.";
                    } else {
                        msg = String.format("An error occurred while attempting to download Complementary files, please check your internet connection and try again! \nError: (Code C2) %s",
                                e.getCause().toString());
                    }
                    installButton.setEnabled(true);
                    installButton.setText("Download Failed!");
                    progressBar.setForeground(new Color(204, 0, 0));
                    progressBar.setValue(100);
                    JOptionPane.showMessageDialog(this,
                            msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                    return;
                } catch (Exception e) {
                    System.out.println("Failed to download Comp! (error kind 2)");
                    e.printStackTrace();
                    
                    // Check if it's a network error
                    if (e instanceof java.net.UnknownHostException || 
                        e instanceof java.net.SocketTimeoutException ||
                        !isInternetAvailable()) {
                        
                        if (showNetworkErrorDialog("processing shader pack")) {
                            // User wants to retry - restart download
                            installButtonMouseClicked(null);
                        }
                        return;
                    }
                }

                File configDir = new File(installDir, "config");
                if (!configDir.exists() || !configDir.isDirectory()) {
                    configDir.mkdir();
                }
                File ipDir = new File(configDir, "iris.properties");
                Properties irisProp = new Properties();
                if (ipDir.exists()) {
                    try (InputStream is = Files.newInputStream(ipDir.toPath())) {
                        irisProp.load(is);
                    } catch (IOException e) {
                        System.out.println("Failed to read iris.properties");
                    }
                }
                irisProp.setProperty("shaderPack", finalShaderName);
                irisProp.setProperty("enableShaders", "true");
                try (OutputStream os = Files.newOutputStream(ipDir.toPath())) {
                    irisProp.store(os, "File written by Comp Installer");
                } catch (IOException e) {
                    System.out.println("Failed to write iris.properties");
                }

                installButton.setText("Completed!");
                progressBar.setForeground(new Color(39, 195, 75));
                installButton.setEnabled(false);
                finishedSuccessfulInstall = true;
                System.out.println("Finished Successful Install");
                
                // Delete confirmation file if installation was successful
                File confirmationFile = new File(shaderDir, "aaaDownloadConfirmation.txt");
                if (confirmationFile.exists()) {
                    boolean deleted = confirmationFile.delete();
                    if (deleted) {
                        System.out.println("Confirmation file deleted successfully");
                    } else {
                        System.out.println("Failed to delete confirmation file");
                    }
                }

                String loaderSt = installAsMod ? "fabric-loader" : "iris-fabric-loader";
                String msg = "Successfully installed Iris, Sodium, and "
                             + finalShaderName +"."+
                             "\nYou can run the game by selecting "+loaderSt+" in your Minecraft launcher.";
                JOptionPane.showMessageDialog(this,
                        msg, "Installation Complete!", JOptionPane.PLAIN_MESSAGE, new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("green_tick.png"))));
                System.exit(0);
            }
        });
        
        try {
            java.lang.reflect.Field field = downloaderC.getClass().getDeclaredField("connection");
            field.setAccessible(true);
            HttpURLConnection connection = (HttpURLConnection) field.get(downloaderC);
            if (connection != null) {
                connection.setConnectTimeout(10000); // 10 seconds
                connection.setReadTimeout(10000);
            }
        } catch (Exception ignored) {
        }
        
        return downloaderC;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        dark = DarkModeDetector.isDarkMode();

        System.setProperty("apple.awt.application.appearance", "system");

        Color accentColor = dark ? new Color(199, 21, 133) : new Color(230, 24, 150);
        
        if (dark) {
            // Make background darker (adjust the RGB values as needed)
            UIManager.put("Panel.background", new Color(24, 24, 30));
            UIManager.put("ComboBox.background", new Color(45, 45, 45));
            UIManager.put("TextField.background", new Color(45, 45, 45));
            UIManager.put("Button.background", new Color(60, 60, 60));
            
            // Set darker background for the entire window
            UIManager.put("@background", new Color(30, 30, 30));
        }

        UIManager.put("hyperlink.foreground", new Color(199, 21, 133));

        UIManager.put("Button.outlineColor", accentColor);
        UIManager.put("Button.focusedBorderColor", accentColor.darker());
        UIManager.put("Button.hoverBorderColor", accentColor);
        UIManager.put("Button.default.focusColor", new Color(199, 21, 133, 50));


        UIManager.put("OptionPane.buttonBackground", new Color(60, 60, 60));
        UIManager.put("OptionPane.background", dark ? new Color(30, 30, 30) : null);
        UIManager.put("OptionPane.messageForeground", dark ? Color.WHITE : null);
        UIManager.put("Button.default.background", accentColor);
        UIManager.put("Button.default.outlineColor", accentColor.darker());
        UIManager.put("Button.default.foreground", Color.WHITE);

        UIManager.put("Component.focusColor", accentColor);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.focusColor", accentColor);

        UIManager.put("OptionPane.buttonType", "roundRect");
        UIManager.put("Button.innerFocusWidth", 0);
        UIManager.put("Button.innerFocusColor", new Color(0, 0, 0, 0)); // Transparent
        UIManager.put("Button.hoverBorderColor", accentColor);
        UIManager.put("Button.focusedBorderColor", accentColor);
        UIManager.put("Button.default.borderColor", accentColor);
        UIManager.put("Button.default.focusColor", accentColor);
        UIManager.put("Button.default.focusedBorderColor", accentColor);

        UIManager.put("CheckBox.icon.checkmarkColor", accentColor);
        UIManager.put("CheckBox.icon.selectedBackground", dark ? new Color(60, 60, 60) : Color.WHITE);
        UIManager.put("CheckBox.icon.selectedBorderColor", accentColor);
        UIManager.put("CheckBox.icon.focusedBorderColor", accentColor);
        UIManager.put("CheckBox.icon.selectedFocusedBorderColor", accentColor);
        UIManager.put("CheckBox.icon.hoverBorderColor", accentColor);

        UIManager.put("ComboBox.selectionBackground", accentColor);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("ComboBox.popupBackground", dark ? new Color(45, 45, 45) : Color.WHITE);
        UIManager.put("ComboBox.buttonArrowColor", accentColor);
        UIManager.put("ComboBox.buttonHoverArrowColor", accentColor);
        UIManager.put("ComboBox.buttonPressedArrowColor", accentColor);
        
        // Set up FlatLaf after all UI properties are configured
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        System.out.println("Launching installer...");

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new NewInstaller().setVisible(true));
    }

    public static void openURL(URI uri) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        Desktop d = Desktop.getDesktop();
        if (!d.isSupported(Desktop.Action.BROWSE)) {
            return;
        }

        try {
            d.browse(uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton directoryName;
    private javax.swing.JLabel gameVersionLabel;
    private javax.swing.JComboBox<String> gameVersionList;
    private javax.swing.JLabel installationDirectory;
    private javax.swing.JRadioButton fabricType;
    private javax.swing.JRadioButton standaloneType;
    private javax.swing.JButton installButton;
    private javax.swing.ButtonGroup installType;
    private javax.swing.JLabel installationType;
    private javax.swing.JPanel installationTypesContainer;
    private javax.swing.JRadioButton unboundType;
    private javax.swing.JRadioButton reimaginedType;
    private javax.swing.ButtonGroup styleType;
    private javax.swing.JPanel visualStyleContainer;
    private javax.swing.JLabel irisInstallerLabel;
    private javax.swing.JLabel linkLabel;
    private javax.swing.JLabel outdatedText1;
    private javax.swing.JLabel outdatedText2;
    private javax.swing.JButton advancedSettingsButton;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables
}
