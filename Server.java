import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

public class Server
 {
    private static final String DEFAULT_FILE = "www.scu.edu/index.html";
    private static final String ERROR_400_PAGE = "errors/400.html";
    private static final String ERROR_403_PAGE = "errors/403.html";
    private static final String ERROR_404_PAGE = "errors/404.html";

    private static final Set<String> activeClientIPs = ConcurrentHashMap.newKeySet();

    private static Logger logger;

    private final File baseDirectory;
    private final String httpVersion;
    private final boolean debug;
    private final int clientTimeoutSec;

    public static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("Server-Logs");
            try {
                
                File logDir = new File("logs");
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
    
                FileHandler fileHandler = new FileHandler("logs/server.log", true);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);
                logger.setUseParentHandlers(false);
            } catch (IOException e) {
                System.err.println("failed to setup logger: " + e.getMessage());
            }
        }
        return logger;
    }

    private Server(File baseDirectory, String httpVersion, boolean debug, int clientTimeoutSec) {
        this.baseDirectory = baseDirectory;
        this.httpVersion = httpVersion;
        this.debug = debug;
        this.clientTimeoutSec = clientTimeoutSec;
    }
     
    public void start(int port, int serverTimeoutSec) {
        Logger logger = Server.getLogger();
        try (ServerSocket serverSock = new ServerSocket(port)) {
            serverSock.setSoTimeout(serverTimeoutSec * 1000); 
            System.out.println("Server is running on port " + port + "...");
            int sessionCount = 0;

            while (true) {
                Socket clientSock = serverSock.accept();
                sessionCount++;
                ClientTask task = new ClientTask(clientSock);
                Thread t = new Thread(task, "Handler-" + sessionCount);
                t.start();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Server socket timed out: " + e.getMessage());
            logger.warning("Server socket timed out: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Server encountered an error: " + e.getMessage());
            logger.severe("Server encountered an error: " + e.getMessage());
        }
    }

    private class ClientTask implements Runnable {
        private final Socket clientSock;

        public ClientTask(Socket clientSock) {
            Logger logger = Server.getLogger();
            this.clientSock = clientSock;
            String clientIP = clientSock.getInetAddress().getHostAddress();
            if (!activeClientIPs.contains(clientIP)) {
                activeClientIPs.add(clientIP);
                System.out.println("New connection from client " + clientIP);
                logger.info("New connection from " + clientIP);
            }
        }

        @Override
   
public void run() {
    try (
        BufferedReader inReader = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
        OutputStream outStream = clientSock.getOutputStream()
    ) {
     
        clientSock.setSoTimeout(clientTimeoutSec * 1000);
        
        
        boolean keepAlive = httpVersion.equals("1.1");
        
      
        while (true) {
           
            String requestLine = inReader.readLine();
            if (requestLine == null) { 
                break;
            }
            
            if (requestLine.trim().isEmpty()) {
                continue;
            }
            
       
            if (debug) {
                System.out.println("[" + Thread.currentThread().getName() + "] Received: " + requestLine);
                Server.getLogger().info("[" + Thread.currentThread().getName() + "] Received: " + requestLine);
            }
            
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            String headerLine;
            while ((headerLine = inReader.readLine()) != null && !headerLine.trim().isEmpty()) {

                String[] parts = headerLine.split(":", 2);
                if (parts.length == 2) {
                    headers.put(parts[0].trim().toLowerCase(), parts[1].trim());
                }
            }
            
          
            if (headers.containsKey("connection") && headers.get("connection").equalsIgnoreCase("close")) {
                keepAlive = false;
            }
            
   
            String[] tokens = requestLine.split(" ");
            if (tokens.length < 2) {
                sendErrorResponse(outStream, ERROR_400_PAGE, "400 Bad Request", requestLine);
            
                if (!keepAlive) break;
                else continue;
            }
            
            String method = tokens[0];
            String rawPath = tokens[1];
            String decodedPath = URLDecoder.decode(rawPath, "UTF-8");
            
            if (!method.equalsIgnoreCase("GET") || !isAllowedResource(decodedPath)) {
                sendErrorResponse(outStream, ERROR_400_PAGE, "400 Bad Request", method);
                if (!keepAlive) break;
                else continue;
            }
            
            File requestedFile = resolveFile(decodedPath);
            if (requestedFile == null || !requestedFile.exists()) {
                sendErrorResponse(outStream, ERROR_404_PAGE, "404 Not Found", decodedPath);
                if (!keepAlive) break;
                else continue;
            }
            if (!requestedFile.canRead()) {
                sendErrorResponse(outStream, ERROR_403_PAGE, "403 Forbidden", decodedPath);
                if (!keepAlive) break;
                else continue;
            }
            
            String protocolLine = "HTTP/" + httpVersion;
            if (debug) {
                System.out.println("[" + Thread.currentThread().getName() + "] Serving: " +
                        requestedFile.getAbsolutePath());
                Server.getLogger().info("[" + Thread.currentThread().getName() + "] Serving: " +
                        requestedFile.getAbsolutePath());
            }
            
  
            sendFileResponse(outStream, requestedFile, protocolLine, "200 OK");
            
        
            if (!keepAlive || protocolLine.equals("HTTP/1.0")) {
                break;
            }
        }
    } catch (SocketTimeoutException e) {
     
        if (debug) {
            System.out.println("[" + Thread.currentThread().getName() + "] Persistent connection timed out.");
            Server.getLogger().info("[" + Thread.currentThread().getName() + "] Persistent connection timed out.");
        }
    } catch (IOException ex) {
        System.out.println("Error processing client request: " + ex.getMessage());
        Server.getLogger().severe("[" + Thread.currentThread().getName() + "] Error processing client request: " + ex.getMessage());
    } finally {
        closeClient();
    }
}


      
        private File resolveFile(String urlPath) {
          
            int queryIndex = urlPath.indexOf("?");
            if (queryIndex != -1) {
                urlPath = urlPath.substring(0, queryIndex);
            }
       
            if (!urlPath.equals("/") && !urlPath.contains(".")) {
                urlPath += ".html";
            }
            
         
            if(urlPath.equals("/400.html") || urlPath.equals("/403.html") || urlPath.equals("/404.html") ) {
                urlPath = "/errors" + urlPath;
            }
            
            String relativePath = urlPath.equals("/") ? DEFAULT_FILE : urlPath.substring(1);
            Logger logger = Server.getLogger();
            logger.info("[" + Thread.currentThread().getName() + "] resolveFile: relativePath = " + relativePath);
            
            File targetFile = new File(baseDirectory, relativePath);
            try {
                String canonicalBase = baseDirectory.getCanonicalPath();
                String canonicalTarget = targetFile.getCanonicalPath();
                logger.info("[" + Thread.currentThread().getName() + "] resolveFile: canonicalTarget = " + canonicalTarget);
                if (!canonicalTarget.startsWith(canonicalBase)) {
                    logger.warning("[" + Thread.currentThread().getName() + "] resolveFile: " + canonicalTarget + " is outside the document root");
                    return null;
                }
            } catch (IOException e) {
                logger.severe("[" + Thread.currentThread().getName() + "] resolveFile: IOException " + e.getMessage());
                return null;
            }
            return targetFile;
        }
        
        

        private boolean isAllowedResource(String path) {
          
            int queryIndex = path.indexOf("?");
            if (queryIndex != -1) {
                path = path.substring(0, queryIndex);
            }
            int hashIndex = path.indexOf("#");
            if (hashIndex != -1) {
                path = path.substring(0, hashIndex);
            }
          
            if (!path.equals("/") && !path.contains(".")) {
                path += ".html";
            }
            Logger logger = Server.getLogger();
            logger.info("[" + Thread.currentThread().getName() + "] isAllowedResource checking path: " + path);
            
            String lowerPath = path.toLowerCase();
            return lowerPath.endsWith(".pdf") || lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".jpg") ||
                   lowerPath.endsWith(".png") || lowerPath.endsWith(".txt") || lowerPath.endsWith(".gif") ||
                   lowerPath.endsWith(".html") || lowerPath.endsWith(".mp4") || lowerPath.endsWith(".json") ||
                   lowerPath.endsWith(".js") || lowerPath.endsWith(".css") || path.equals("/");
        }
        


        private void sendFileResponse(OutputStream outStream, File file, String protocol, String status) {
            Logger logger = Server.getLogger();
            try {
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                String header = protocol + " " + status + "\r\n" +
                        "Server: Java Custom Server\r\n" +
                        "Date: " + getCurrentGMT() + "\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + file.length() + "\r\n" +
                        "\r\n";
                outStream.write(header.getBytes("UTF-8"));
                outStream.flush();

               
                try (InputStream fileStream = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }
                    outStream.flush();
                }
            } catch (IOException e) {
                System.out.println("Error sending file: " + e.getMessage());
                logger.severe("Error sending file: " + e.getMessage());
            }
        }
        private void sendErrorResponse(OutputStream outStream, String errorPage, String status, String details) {
            Logger logger = Server.getLogger();
            System.out.println(status + ": " + details);
            File errorFile = new File(baseDirectory, errorPage);
            logger.info("Looking for error file at: " + errorFile.getAbsolutePath());
            if (!errorFile.exists() || !errorFile.canRead()) {
                String response = "HTTP/" + httpVersion + " " + status + "\r\n" +
                        "Content-Type: text/plain\r\n\r\n" +
                        status;
                try {
                    outStream.write(response.getBytes("UTF-8"));
                } catch (IOException e) {
                    System.out.println("Error writing error response: " + e.getMessage());
                    logger.severe("Error writing error response: " + e.getMessage());
                }
            } else {
                sendFileResponse(outStream, errorFile, "HTTP/" + httpVersion, status);
            }
        }
        private String getCurrentGMT() {
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            return formatter.format(new Date());
        }

     
        private void closeClient() {
            Logger logger = Server.getLogger();
            try {
                String clientIP = clientSock.getInetAddress().getHostAddress();
                clientSock.close();
                activeClientIPs.remove(clientIP);
                System.out.println("Connection closed: " + clientIP);;
                logger.info("[" + Thread.currentThread().getName() + "] Connection closed: " + clientIP);
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
                logger.severe("[" + Thread.currentThread().getName() + "] Error closing client socket: " + e.getMessage());
            }
        }
    }
    public static void main(String[] args) {
        Logger logger = Server.getLogger();
        String docRootPath = null;
        int port = -1;
        String protocol = "1.1";
        boolean debugFlag = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-document_root":
                    if (i + 1 < args.length) {
                        docRootPath = args[++i];
                    } else {
                        System.out.println("Missing value for -document_root");
                        logger.warning("Missing value for -document_root");
                        return;
                    }
                    break;
                case "-port":
                    if (i + 1 < args.length) {
                        try {
                            port = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException ex) {
                            System.out.println("Port number must be a number.");
                            logger.warning("Port number must be a number.");
                            return;
                        }
                    } else {
                        System.out.println("Missing value for -port");
                        logger.warning("Missing value for -port");
                        return;
                    }
                    break;
                case "--protocol_version":
                    if (i + 1 < args.length) {
                        protocol = args[++i];
                    } else {
                        System.out.println("Missing value for --protocol_version");
                        logger.warning("Missing value for --protocol_version");
                        return;
                    }
                    break;
                case "--debug_mode":
                    if (i + 1 < args.length) {
                        debugFlag = Boolean.parseBoolean(args[++i]);
                    } else {
                        System.out.println("Missing value for --debug_mode");
                        logger.warning("Missing value for --debug_mode");
                        return;
                    }
                    break;
                default:
                    System.out.println("Unknown parameter: " + args[i]);
                    logger.warning("Unknown parameter: " + args[i]);
                    break;
            }
        }

        if (docRootPath == null) {
            System.out.println("java Server -document_root <path> -port <port> [--protocol_version <1.0|1.1>] [--debug_mode <true|false>]");
            logger.warning("Document root path is required.");
            return;
        }
        if (port < 8000 || port > 9999) {
            System.out.println("Port number must be between 8000 and 9999.");
            logger.warning("Port number must be between 8000 and 9999.");
            return;
        }
        if (!protocol.equals("1.0") && !protocol.equals("1.1")) {
            System.out.println("Supported protocols are https 1.0 or https 1.1.");
            logger.warning("Supported protocols are https 1.0 or https 1.1.");
            return;
        }

        File docRoot = new File(docRootPath);
        if (!docRoot.exists() || !docRoot.isDirectory()) {
            System.out.println("Document root not found or is not a directory: " + docRootPath);
            logger.warning("Document root not found or is not a directory: " + docRootPath);
            return;
        }


        Server serverInstance = new Server(docRoot, protocol, debugFlag, 10);
        serverInstance.start(port, 500);
    }


}
