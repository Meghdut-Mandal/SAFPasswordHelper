import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

public class MainApp
{

    private static final String CREDENTIALS_FILE_NAME = "pass.txt";
    private PopupMenu trayPopupMenu;

    // base64 decode a string
    public static String decode(String s) {
        byte[] data = Base64.getDecoder().decode(s);
        return new String(data);
    }

    // encode a string to base64
    public static String encode(String s) {
        byte[] data = s.getBytes();
        return Base64.getEncoder().encodeToString(data);
    }

    public static void main(String[] args) throws IOException {
        MainApp mainApp = new MainApp();
        mainApp.launch();
    }


    public void showCredentialsInputDialog() {
        // show the dialog
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbUsername = new JLabel("Username: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);

        JTextField tfUsername = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(tfUsername, cs);

        JLabel lbPassword = new JLabel("Password: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);

        JTextField pfPassword = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(pfPassword, cs);
        panel.setBorder(new LineBorder(Color.GRAY));

        JButton btnAdd = new JButton("Add");
        JButton btnCancel = new JButton("Cancel");
        JPanel bp = new JPanel();
        bp.add(btnAdd);
        bp.add(btnCancel);
        JFrame frame = new JFrame("Add User Credentials");
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.getContentPane().add(bp, BorderLayout.PAGE_END);
        frame.pack();
        btnCancel.addActionListener(e -> frame.dispose());
        btnAdd.addActionListener(e -> {
            // get the username and password
            String username = tfUsername.getText();
            String password = pfPassword.getText();
            if (username.isEmpty() || password.isEmpty()) {
                return;
            }
            // create the credentials
            Credentials credentials = new Credentials(username, password);
            addCredentialMenuItem(credentials);
            String encodedCredentials = "\n" + encode(username) + ":" + encode(password);
            File credentialFile = new File(CREDENTIALS_FILE_NAME);
            try{
                Files.write(credentialFile.toPath(), encodedCredentials.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            frame.dispose();
        });
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setAlwaysOnTop(true);
    }

    // read a file by each line
    public List<Credentials> readFile(String fileName) {
        List<Credentials> credentials = new ArrayList<>();
        try{
            File f = new File(fileName);
            if (!f.exists()) {
                System.out.println("File not found");
                return credentials;
            }
            Files.lines(f.toPath()).forEach(line -> {
                // add the credentials to the list
                credentials.add(new Credentials(line));
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return credentials;
    }

    public void addCredentials() {
        // read the file
        List<Credentials> credentials = readFile(CREDENTIALS_FILE_NAME);
        // add the credentials to popup menu
        for (Credentials credential : credentials) {
            addCredentialMenuItem(credential);
        }
    }

    private void addCredentialMenuItem(Credentials credential) {
        MenuItem menuItem = new MenuItem(credential.getUser());
        menuItem.addActionListener(e -> {
            // copy password to clipboard
            StringSelection stringSelection = new StringSelection(credential.getPassword());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        });
        trayPopupMenu.add(menuItem);
    }

    public void launch() throws IOException {
        //checking for support
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported !!! ");
            return;
        }
        //get the systemTray of the system
        SystemTray systemTray = SystemTray.getSystemTray();

         // get class loader
        Image image = ImageIO.read(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("password.png")));

        //popupmenu
        trayPopupMenu = new PopupMenu();


        // New item
        MenuItem newItem = new MenuItem("Add New Credentials ...");
        newItem.addActionListener(e -> showCredentialsInputDialog());
        trayPopupMenu.add(newItem);

        //close menuitem of popupmenu
        MenuItem close = new MenuItem("Exit");
        close.addActionListener(e -> System.exit(0));
        trayPopupMenu.add(close);

        addCredentials();
        //setting tray icon
        TrayIcon trayIcon = new TrayIcon(image, "Newton Password Manager", trayPopupMenu);
        //adjust to default size as per system recommendation
        trayIcon.setImageAutoSize(true);

        try{
            systemTray.add(trayIcon);
        } catch (AWTException awtException) {
            awtException.printStackTrace();
        }
    }

    static class Credentials
    {
        String user;
        String password;

        public Credentials(String user, String password) {
            this.user = user;
            this.password = password;
        }

        public Credentials(String line) {
            // split the line into user and password by ":"
            String[] parts = line.split(":");
            user = decode(parts[0]);
            password = decode(parts[1]);
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

    }
}
