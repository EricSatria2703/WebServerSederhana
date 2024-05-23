package id.eric.webserversederhana;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

public class MainApp extends Application {
    private HttpServer server;
    private int port;
    private String webDirectory;
    private String logDirectory;


    private TextArea logTextArea;

    private TextField portTextField;
    private TextField webDirTextField;
    private TextField logDirTextField;

    private static final String CONFIG_FILE = "config.properties";
    private Properties properties;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Web Server Sederhana");

        // Initialize default values
        String defaultPort = "8080";
        String defaultWebDir = "C:/WebServer/";
        String defaultLogDir = "C:/WebServer/";

        // Load configuration
        properties = new Properties();
        loadConfiguration(defaultPort, defaultWebDir, defaultLogDir);

        // Create UI components
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(15); // 25% width
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(60); // 50% width (second column wider)
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(25); // 25% width

        grid.getColumnConstraints().addAll(col1, col2, col3);

        Label portLabel = new Label("Port:");
        portTextField = new TextField(properties.getProperty("port", defaultPort));
        grid.add(portLabel, 0, 0);
        grid.add(portTextField, 1, 0);

        Label webDirLabel = new Label("Web Directory:");
        webDirTextField = new TextField(properties.getProperty("webDir", defaultWebDir));
        grid.add(webDirLabel, 0, 1);
        grid.add(webDirTextField, 1, 1);
        Button chooseWebDirButton = new Button("Choose");
        grid.add(chooseWebDirButton, 2, 1);
        chooseWebDirButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Web Directory");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                webDirTextField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        Label logDirLabel = new Label("Log Directory:");
        logDirTextField = new TextField(properties.getProperty("logDir", defaultLogDir));
        grid.add(logDirLabel, 0, 2);
        grid.add(logDirTextField, 1, 2);
        Button chooseLogDirButton = new Button("Choose");
        grid.add(chooseLogDirButton, 2, 2);
        chooseLogDirButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Log Directory");
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                logDirTextField.setText(selectedDirectory.getAbsolutePath());
            }
        });

        Button startButton = new Button("Start Server");
        Button stopButton = new Button("Stop Server");
        stopButton.setDisable(true); // Server not running initially
        grid.add(startButton, 0, 3);
        grid.add(stopButton, 1, 3);

        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setPrefHeight(300);
        grid.add(logTextArea, 0, 4, 3, 1);

        // Set action for start button
        startButton.setOnAction(e -> {
            port = Integer.parseInt(portTextField.getText());
            webDirectory = webDirTextField.getText();
            logDirectory = logDirTextField.getText();

            // Save configuration
            properties.setProperty("port", Integer.toString(port));
            properties.setProperty("webDir", webDirectory);
            properties.setProperty("logDir", logDirectory);
            saveConfiguration();

            try {
                startServer();
                startButton.setDisable(true);
                stopButton.setDisable(false);
                log("Server started on port " + port);
            } catch (Exception ex) {
                log("Error starting server: " + ex.getMessage());
            }
        });

        // Set action for stop button
        stopButton.setOnAction(e -> {
            stopServer();
            startButton.setDisable(false);
            stopButton.setDisable(true);
            log("Server stopped");
        });

        // Create scene and show stage
        Scene scene = new Scene(grid, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void log(String message) {
        String formattedMessage = getLogEntry(message);
        logTextArea.appendText(formattedMessage + "\n");
        writeToLogFile(formattedMessage);
    }

    private String getLogEntry(String message) {
        // Mendapatkan waktu sekarang
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // Mendapatkan alamat IP yang terhubung melalui WiFi
        InetAddress sourceIP = getConnectedWiFiIPAddress();
        // Membuat entri log dengan format yang sesuai
        return timestamp + " | " + message + " | " + sourceIP;
    }

    private void writeToLogFile(String logEntry) {
        try {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String logFileName = logDirTextField.getText() + "/access_log_" + dateStr + ".log";
            FileWriter writer = new FileWriter(logFileName, true); // Append mode
            writer.write(logEntry + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfiguration(String defaultPort, String defaultWebDir, String defaultLogDir) {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            // Use default values if config file not found
            properties.setProperty("port", defaultPort);
            properties.setProperty("webDir", defaultWebDir);
            properties.setProperty("logDir", defaultLogDir);
        }
    }

    private void saveConfiguration() {
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InetAddress getConnectedWiFiIPAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback() && !networkInterface.isVirtual() && networkInterface.getName().startsWith("w")) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address) {
                            return address;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error retrieving network interfaces: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String requestMethod = exchange.getRequestMethod();
                String requestedPath = exchange.getRequestURI().getPath();
                if (requestedPath.contains("..")) {
                    sendError(exchange, 400, "Bad Request");
                    return;
                }
                String filePath = webDirectory + (requestedPath.startsWith("/") ? "" : "/") + requestedPath;

                File file = new File(filePath);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        // Handle directory listing
                        handleDirectoryListing(exchange, file);
                    } else {
                        // Serve file content
                        serveFile(exchange, file);
                    }
                } else {
                    // File not found
                    sendError(exchange, 404, "File Not Found");
                }
            }
        });

        server.setExecutor(null); // creates a default executor
        server.start();
//        System.out.println("Server started on port " + port);
    }

    private void handleDirectoryListing(HttpExchange exchange, File dir) throws IOException {
        String dirPath = dir.getPath().replace(webDirectory, ""); // Get the relative directory path
        StringBuilder response = new StringBuilder("<html><body>");
        response.append("<h2>Directory Listing</h2>");
        response.append("<h3>Current Directory: ").append(dirPath).append("</h3>");
        response.append("<table border=\"1\">");
        response.append("<tr><th>Name</th></tr>");

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                response.append("<tr><td><a href=\"").append(fileName);
                if (file.isDirectory()) {
                    response.append("/");
                }
                response.append("\">").append(fileName).append("</a></td></tr>");
            }
        }

        response.append("</table></body></html>");
        sendResponse(exchange, 200, response.toString(), "text/html");
    }



    private void serveFile(HttpExchange exchange, File file) throws IOException {
        String contentType = getContentType(file);
        if (contentType != null) {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            sendResponse(exchange, 200, fileBytes, contentType);
        } else {
            sendError(exchange, 415, "Unsupported Media Type");
        }
    }

    private String getContentType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".log")) {
            return "text/plain";
        } else if (fileName.endsWith(".mp4")) {
            return "video/mp4";
        }else {
            return null; // Unsupported file type
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, byte[] response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.length);
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
        logAccess(exchange, statusCode);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
        logAccess(exchange, statusCode);
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendResponse(exchange, statusCode, message, "text/plain");
    }

    private void logAccess(HttpExchange exchange, int statusCode) {
        String requestUrl = exchange.getRequestURI().toString();
        String requestMethod = exchange.getRequestMethod();

        // Mendapatkan alamat IP yang terhubung melalui WiFi
        InetAddress sourceIP = getConnectedWiFiIPAddress();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = timestamp + " | " + requestMethod + " | " + requestUrl + " | " + " | " + statusCode;
        String logGUI = requestMethod + " | " + requestUrl + " | " + " | " + statusCode;
        
        log(logGUI);

        // Log to file
        try {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String logFileName = logDirectory + "/access_log_" + dateStr + ".log";
            FileWriter writer = new FileWriter(logFileName, true); // Append mode
            writer.write(logEntry + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("Server stopped");
        }
    }
}
