package lk.ijse.chatappexam;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class ClientController {

    @FXML private Label lblUsername;
    @FXML private Label lblItemName;
    @FXML private Label lblHighestBid;
    @FXML private Button btnDisconnect;
    @FXML private TextArea txtBidHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;
    private volatile boolean connected = false;
    private double currentHighest = 0;

    @FXML
    void quickBid100(ActionEvent event)  { sendQuickBid(100); }
    @FXML
    void quickBid500(ActionEvent event)  { sendQuickBid(500); }
    @FXML
    void quickBid1000(ActionEvent event) { sendQuickBid(1000); }
    @FXML
    void quickBid5000(ActionEvent event) { sendQuickBid(5000); }

    @FXML
    public void initialize() {
        TextInputDialog dialog = new TextInputDialog("User");
        dialog.setTitle("Auction Client");
        dialog.setHeaderText("Enter your name to join auction:");
        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            username = result.get().trim();
            lblUsername.setText(username);
            connectToServer();
        } else {
            txtBidHistory.setText("No username entered. Close and reopen.\n");
            disableInputs();
        }
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 6000);
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                dos.writeUTF(username);
                dos.flush();

                connected = true;

                while (connected) {
                    String msg = dis.readUTF();

                    if (msg.startsWith("INIT|")) {
                        String[] parts = msg.split("\\|");
                        String item = parts[1];
                        double start = Double.parseDouble(parts[2]);
                        currentHighest = Double.parseDouble(parts[3]);
                        String bidder = parts[4];

                        Platform.runLater(() -> {
                            lblItemName.setText(item);
                            lblHighestBid.setText("Rs. " + (int) currentHighest);
                            txtBidHistory.appendText(" Welcome to Auction!\n");
                            txtBidHistory.appendText(" Item: " + item + "\n");
                            txtBidHistory.appendText(" Starting Price: Rs. " + (int) start + "\n");
                            txtBidHistory.appendText("==========================\n");
                        });

                    } else if (msg.startsWith("BID_ACCEPTED|")) {
                        String[] parts = msg.split("\\|");
                        String bidder = parts[1];
                        double amount = Double.parseDouble(parts[2]);
                        currentHighest = Double.parseDouble(parts[3]);

                        Platform.runLater(() -> {
                            lblHighestBid.setText("Rs. " + (int) currentHighest);
                            txtBidHistory.appendText("[" + bidder + "] placed Rs. " + (int) amount + " (New Highest!)\n");
                            autoScroll();
                        });

                    } else if (msg.startsWith("BID_REJECTED|")) {
                        String[] parts = msg.split("\\|");
                        double amount = Double.parseDouble(parts[1]);
                        double highest = Double.parseDouble(parts[2]);

                        Platform.runLater(() -> {
                            txtBidHistory.appendText(" Your bid Rs. " + (int) amount + " REJECTED! (Current highest: Rs. " + (int) highest + ")\n");
                            autoScroll();
                        });

                    } else if (msg.startsWith("CLIENT_JOINED|")) {
                        String name = msg.split("\\|")[1];
                        Platform.runLater(() -> {
                            txtBidHistory.appendText( name + " joined the auction.\n");
                            autoScroll();
                        });

                    } else if (msg.startsWith("AUCTION_ENDED|")) {
                        String[] parts = msg.split("\\|");
                        String winner = parts[1];
                        double amount = Double.parseDouble(parts[2]);

                        Platform.runLater(() -> {
                            txtBidHistory.appendText("\n AUCTION ENDED \n");
                            txtBidHistory.appendText("Winner: " + winner + "\n");
                            txtBidHistory.appendText("Winning Bid: Rs. " + (int) amount + "\n");
                            txtBidHistory.appendText("──────────────────────────\n");
                            autoScroll();
                            disableInputs();

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Auction Ended");
                            alert.setHeaderText(null);
                            alert.setContentText(" Winner: " + winner + "\n Winning Bid: Rs. " + (int) amount);
                            alert.showAndWait();
                        });
                        connected = false;
                        break;
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    txtBidHistory.appendText("\n️ Disconnected from server.\n");
                    disableInputs();
                });
            }
        }).start();
    }

    @FXML
    void placeBidOnAction(ActionEvent event) {
        if (!connected || dos == null) return;

        try {
            double amount = Double.parseDouble(txtBidAmount.getText().trim());
            if (amount <= 0) {
                txtBidHistory.appendText(" Invalid amount!\n");
                return;
            }

            dos.writeUTF("BID|" + amount);
            dos.flush();
            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            txtBidHistory.appendText(" Please enter a valid number!\n");
        } catch (IOException e) {
            txtBidHistory.appendText(" Failed to send bid!\n");
        }
    }

    private void sendQuickBid(int increment) {
        if (!connected || dos == null) return;
        double newBid = currentHighest + increment;
        try {
            dos.writeUTF("BID|" + newBid);
            dos.flush();
            txtBidAmount.setText(String.valueOf((int) newBid));
        } catch (IOException e) {
            txtBidHistory.appendText("Failed to send bid!\n");
        }
    }

    @FXML
    void disconnectOnAction(ActionEvent event) {
        if (dos != null) {
            try {
                dos.writeUTF("DISCONNECT");
                dos.flush();
            } catch (IOException ignored) {}
        }
        connected = false;
        closeConnection();
        disableInputs();
        txtBidHistory.appendText(" You left the auction.\n");
    }

    private void disableInputs() {
        Platform.runLater(() -> {
            txtBidAmount.setDisable(true);
            btnPlaceBid.setDisable(true);
            btnDisconnect.setDisable(true);
        });
    }

    private void closeConnection() {
        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void autoScroll() {
        txtBidHistory.setScrollTop(Double.MAX_VALUE);
    }
}
