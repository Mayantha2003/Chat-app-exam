package lk.ijse.chatappexam;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerController {

    @FXML private Label lblItemName;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentHighest;
    @FXML private Label lblHighestBidder;
    @FXML private TextArea txtLog;
    @FXML private ListView<String> listClients;
    @FXML private TextField txtCommand;

    private final String ITEM_NAME = "Vintage Watch";
    private final double STARTING_PRICE = 5000.0;
    private double currentHighestBid = STARTING_PRICE;
    private String highestBidder = "None";

    private final int PORT = 6000;
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private volatile boolean auctionActive = true;

    @FXML
    public void initialize() {
        lblItemName.setText(ITEM_NAME);
        lblStartingPrice.setText("Rs. " + (int) STARTING_PRICE);
        updateHighestDisplay();

        new Thread(this::startServer).start();
        new Thread(this::consoleInput).start();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            log("[Auction server Started - port " + PORT + "]");
            log("[Item:" + ITEM_NAME + " | Starting price " + (int) STARTING_PRICE + "]");

            while (auctionActive) {
                Socket socket = serverSocket.accept();
                if (!auctionActive) break;
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            if (auctionActive) log("Server error: " + e.getMessage());
        }
    }

    private void consoleInput() {
        Scanner scanner = new Scanner(System.in);
        while (auctionActive) {
            String cmd = scanner.nextLine();
            if (cmd.equalsIgnoreCase("End")) {
                endAuction();
                break;
            }
        }
    }

    @FXML
    void sendCommand(ActionEvent event) {
        String cmd = txtCommand.getText().trim();
        txtCommand.clear();
        if (cmd.equalsIgnoreCase("End")) {
            endAuction();
        }
    }

    private void endAuction() {
        if (!auctionActive) return;
        auctionActive = false;

        String winnerMsg = "[Auction closed] Winner: " + highestBidder + " : " + (int) currentHighestBid;
        log(winnerMsg);

        broadcast("AUCTION_ENDED|" + highestBidder + "|" + currentHighestBid);

        for (ClientHandler c : clients) {
            c.closeConnection();
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String message) {
        for (ClientHandler c : clients) {
            c.send(message);
        }
    }

    private void log(String msg) {
        System.out.println(msg);
        Platform.runLater(() -> txtLog.appendText(msg + "\n"));
    }

    private void updateHighestDisplay() {
        Platform.runLater(() -> {
            lblCurrentHighest.setText("Rs. " + (int) currentHighestBid);
            lblHighestBidder.setText(highestBidder);
        });
    }
    private class ClientHandler extends Thread {
        Socket socket;
        DataInputStream dis;
        DataOutputStream dos;
        String clientName;

        ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                clientName = dis.readUTF();
                log("Client connected : " + clientName);

                Platform.runLater(() -> listClients.getItems().add(clientName));

                dos.writeUTF("INIT|" + ITEM_NAME + "|" + STARTING_PRICE + "|" + currentHighestBid + "|" + highestBidder);
                dos.flush();

                broadcast("CLIENT_JOINED|" + clientName);

                while (auctionActive) {
                    String msg = dis.readUTF();
                    if (msg.startsWith("BID|")) {
                        double amount = Double.parseDouble(msg.split("\\|")[1]);
                        handleBid(amount);
                    } else if (msg.equals("DISCONNECT")) {
                        break;
                    }
                }
            } catch (IOException e) {
            } finally {
                disconnect();
            }
        }

        void handleBid(double amount) {
            if (amount > currentHighestBid) {
                currentHighestBid = amount;
                highestBidder = clientName;
                updateHighestDisplay();

                String broadcastMsg = "BID_ACCEPTED|" + clientName + "|" + amount + "|" + currentHighestBid;
                broadcast(broadcastMsg);
                log("Bid accepted - " + clientName + " : " + (int) amount + " (new Highest)");
            } else {
                String rejectMsg = "BID_REJECTED|" + amount + "|" + currentHighestBid;
                send(rejectMsg);
                log("Bid rejected - " + clientName + " : " + (int) amount + " (too low)");
            }
        }

        void send(String msg) {
            try {
                dos.writeUTF(msg);
                dos.flush();
            } catch (IOException e) {
            }
        }

        void closeConnection() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {}
        }

        void disconnect() {
            clients.remove(this);
            Platform.runLater(() -> listClients.getItems().remove(clientName));
            if (clientName != null) {
                log("Client disconnected : " + clientName);
            }
            closeConnection();
        }
    }
}