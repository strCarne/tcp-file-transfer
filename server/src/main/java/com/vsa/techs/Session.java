package com.vsa.techs;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.vsa.techs.fs.View;

public class Session implements Runnable {
  private final static Charset CHARSET = StandardCharsets.UTF_8;
  private final static String PERMISSION_DENIED = "Permision denied!";
  private final static int BUF_CAP = 4096;

  Socket client;
  private String rootDir;
  private final byte[] token;

  public Session(Socket client, byte[] serverToken, String rootDir) {
    this.client = client;
    token = serverToken;
    this.rootDir = rootDir;
  }

  @Override
  public void run() {
    try {
      byte[] clientToken = new byte[32];

      client.getInputStream().read(clientToken);

      if (!Arrays.equals(clientToken, token)) {
        client.getOutputStream().write(App.toUtf8(PERMISSION_DENIED));
        client.close();
        System.out.println("User " + client.getRemoteSocketAddress().toString() + 
          " BAD TOKEN");
        return;
      }
      System.out.println("User " + client.getRemoteSocketAddress().toString() + 
        " TOKEN VERIFIED");

      byte[] buffer = new byte[BUF_CAP];
      View view = new View(rootDir, createRootMask());

      byte[] response = App.toUtf8(view.render("info"));
        client.getOutputStream().write(response);

      ////
      ////
      ////
      while (!client.isClosed()) {
        int len = client.getInputStream().read(buffer);
        if (len == -1) {
          break;
        }

        String input = new String(buffer, 0, len, CHARSET).trim();
        String[] params = input.split("\\s+");
        
        String cmd = params[0];
        if (params.length > 1) {
          params = Arrays.copyOfRange(params, 1, params.length);
        } else {
          params = null;
        }

        System.out.println("User " + client.getRemoteSocketAddress().toString() + 
          " " + cmd + " " + Arrays.toString(params));

        switch (cmd) {
        case CMD_DOWNLOAD -> {
          if (params.length != 1) {
            deny(App.toUtf8(view.render("info")));
          } 

          Path currDir = view.curr;
          String fileName = params[0];
          Path fullPath = Paths.get(currDir.toString(), fileName);

          if (Files.exists(fullPath) && !Files.isDirectory(fullPath)) {
            client.getOutputStream().write(FLAG_DOWNLOAD);
            client.getInputStream().read();
            sendData(fullPath);
          } else {
            deny(App.toUtf8(view.render("info")));
          }
        }
        case CMD_EXIT -> {
          client.getOutputStream().write(FLAG_EXIT);
          client.close();
        }
        default -> {
          client.getOutputStream().write(FLAG_GENERAL);
          client.getInputStream().read();
          response = App.toUtf8(view.render(cmd, params));
          client.getOutputStream().write(response);
        }
        }

      }
      ////
      ////
      ////

      if (!client.isClosed()) {
        client.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void deny(byte[] response) throws IOException {
    client.getOutputStream().write(FLAG_DENY);
    client.getInputStream().read();
    client.getOutputStream().write(response);
  }

  final static String CMD_DOWNLOAD = "download";
  final static String CMD_EXIT = "exit";

  final static int FLAG_DOWNLOAD = 1;
  final static int FLAG_EXIT = 2;
  final static int FLAG_GENERAL = 3;
  final static int FLAG_OK = 4;
  final static int FLAG_DENY = 5;

  private void sendData(Path p) throws IOException {
    client.getOutputStream().write(
        p.toFile().getName().getBytes(CHARSET));

    client.getInputStream().read();

    long size = Files.size(p);
    client.getOutputStream().write(String.valueOf(size).getBytes(CHARSET));

    client.getInputStream().read();

    FileInputStream fileInputStream = new FileInputStream(p.toAbsolutePath().toString());
    byte[] buffer = new byte[BUF_CAP];
    int bytes;
    while ((bytes = fileInputStream.read(buffer)) != -1) {
      client.getOutputStream().write(buffer, 0, bytes);

    }
    fileInputStream.close();

    client.getInputStream().read();

    client.getOutputStream().write(createRootMask().getBytes(CHARSET));
  }

  private String createRootMask() {
    return "\nroot" + client.getRemoteSocketAddress().toString();
  }
}
