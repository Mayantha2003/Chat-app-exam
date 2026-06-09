module lk.ijse.chatappexam {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;


    opens lk.ijse.chatappexam to javafx.fxml;
    exports lk.ijse.chatappexam;
}