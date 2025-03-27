# HTTP Web Server

A simple, multi-threaded HTTP server implemented in Java that supports both HTTP 1.0 and HTTP 1.1 protocols.

## Overview

This project implements a lightweight, customizable HTTP web server from scratch in Java. The server handles client requests in separate threads, supports persistent connections (HTTP 1.1), and serves static content like HTML, images, and text files.

## Features

- **Multi-threaded architecture**: Handles multiple client connections concurrently
- **Support for HTTP/1.0 and HTTP/1.1**: Configurable protocol version
- **Configurable document root**: Serve files from any directory
- **Persistent connections**: Keep-alive support for HTTP/1.1
- **MIME type detection**: Automatically determines content types for different file formats
- **Error handling**: Custom error pages (400, 403, 404)
- **Logging**: Comprehensive logging to files and console
- **Security features**: Path traversal protection, file access control
- **Timeouts**: Configurable server and client connection timeouts
- **Debug mode**: Optional detailed request/response logging

## Supported File Types

The server can serve the following file types:
- HTML (.html)
- Images (.jpg, .jpeg, .png, .gif)
- PDF (.pdf)
- Text (.txt)
- JavaScript (.js)
- CSS (.css)
- JSON (.json)
- Video (.mp4)

## Requirements

- Java Development Kit (JDK) 8 or higher

## Usage

### Compilation

```bash
javac Server.java
```

### Running the Server

```bash
java Server -document_root <path> -port <port> [--protocol_version <1.0|1.1>] [--debug_mode <true|false>]
```

### Command-line Arguments

- `-document_root <path>`: Sets the root directory from which files will be served (required)
- `-port <port>`: Sets the port number for the server to listen on (required, must be between 8000-9999)
- `--protocol_version <1.0|1.1>`: Sets the HTTP protocol version (optional, default: 1.1)
- `--debug_mode <true|false>`: Enables detailed request/response logging (optional, default: false)

### Examples

Start the server with default settings:
```bash
java Server -document_root ./www -port 8080
```

Start the server with HTTP/1.0 and debug mode:
```bash
java Server -document_root ./www -port 8888 --protocol_version 1.0 --debug_mode true
```

## Implementation Details

### Core Components

- **Server**: Main class that initializes the server socket and manages client connections
- **ClientTask**: Handles individual client connections in separate threads
- **Logger**: Provides detailed logging of server activities and errors
- **Error Handling**: Custom error pages for common HTTP error codes

### HTTP Protocol Implementation

The server implements the core features of HTTP/1.0 and HTTP/1.1 including:

- Request parsing
- Response generation with appropriate headers
- Content type detection based on file extensions
- Handling of persistent connections (keep-alive) in HTTP/1.1
- Proper handling of query parameters

### Security Considerations

- Protection against directory traversal attacks (restricting access to files outside the document root)
- Validation of file types to prevent serving potentially dangerous files
- File access permission checking



## Server Architecture

The server uses a multi-threaded design where each client connection is handled by a separate thread:

1. The main thread creates a server socket and listens for incoming connections
2. When a client connects, the server creates a new `ClientTask` instance to handle the connection
3. The `ClientTask` runs in a separate thread, processing the client's requests
4. For HTTP/1.1, the connection remains open to handle multiple requests (persistent connection)
5. For HTTP/1.0, the connection closes after each request unless explicitly kept alive

## Logging

The server logs activities to both the console and a log file (`logs/server.log`). Logs include:

- Server startup and configuration information
- Client connection and disconnection events
- Request details (when debug mode is enabled)
- Error information
- File access attempts

