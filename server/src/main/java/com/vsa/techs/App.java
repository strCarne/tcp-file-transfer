package com.vsa.techs;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {

  public static void main(String[] args) {
    Options opts = new Options();

    Option addrOpt = new Option("a", "address", true, "IPv4 server address");
    addrOpt.setRequired(true);
    opts.addOption(addrOpt);

    Option portOpt = new Option("p", "port", true, "port to bind server");
    portOpt.setRequired(true);
    opts.addOption(portOpt);

    Option rootDirOpt = new Option("rd", "root-dir", true, "path to broadcast dir");
    rootDirOpt.setRequired(true);
    opts.addOption(rootDirOpt);

    Option tokenOpt = new Option("t", "token", true, "check token for incomming connections");
    tokenOpt.setRequired(true);
    opts.addOption(tokenOpt);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    CommandLine cmd;
    try {
      cmd = parser.parse(opts, args);
    } catch (ParseException e) {
      formatter.printHelp("tcp-fs-server", opts);
      return;
    }

    String address = cmd.getOptionValue(addrOpt.getLongOpt());
    int port = Integer.parseInt(cmd.getOptionValue(portOpt.getLongOpt()));
    String rootDir = cmd.getOptionValue(rootDirOpt.getLongOpt());
    byte[] token = takeHashUtf8(cmd.getOptionValue(tokenOpt.getLongOpt()));

    System.out.println("Host: " + address + ":" + port);
    System.out.println("Broadcast directory: " + new File(rootDir).toPath().toAbsolutePath().toString());
    System.out.println("Token hash: " + Arrays.toString(token));
    System.out.write('\n');

    try (ServerSocket server = new ServerSocket(port, 16, InetAddress.getByName(address))) {
      System.out.println("Server started.");
      System.out.println("Waiting for incoming connections.");
      System.out.write('\n');

      while (true) {
        Session session = new Session(server.accept(), token, rootDir);
        System.out.println("User " + session.client.getRemoteSocketAddress().toString() + " accepted.");

        Thread sessionThread = new Thread(session);
        sessionThread.start();
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  public static byte[] takeHashUtf8(String text) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(toUtf8(text));
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  public static byte[] toUtf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }
}
