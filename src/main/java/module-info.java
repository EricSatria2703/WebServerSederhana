module id.eric.webserversederhana {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.httpserver;


    opens id.eric.webserversederhana to javafx.fxml;
    exports id.eric.webserversederhana;
}