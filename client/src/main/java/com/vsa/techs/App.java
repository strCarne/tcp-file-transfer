package com.vsa.techs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {
  private static final int BUF_CAP = 4096;
  private final static Charset CHARSET = StandardCharsets.UTF_8;

  public static void main(String[] args) {
   Options opts = new Options();

   Option addrOpt = new Option("a", "address", true, "IPv4 server address");
   addrOpt.setRequired(true);
   opts.addOption(addrOpt);

   Option portOpt = new Option("p", "port", true, "server port");
   portOpt.setRequired(true);
   opts.addOption(portOpt);

   Option tokenOpt = new Option("t", "token", true, "token to establish connection");
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
   byte[] token = takeHashUtf8(cmd.getOptionValue(tokenOpt.getLongOpt()));

    try (Socket s = new Socket(address, port)) {

      s.getOutputStream().write(token);
      byte[] buffer = new byte[BUF_CAP];
      int len = s.getInputStream().read(buffer);
      System.out.write(buffer, 0, len);

      ////
      ////
      ////
      while (!s.isClosed()) {
        len = System.in.read(buffer);
        s.getOutputStream().write(buffer, 0, len);

        int flag = s.getInputStream().read();
        switch (flag) {
        case FLAG_DOWNLOAD -> {
          s.getOutputStream().write(FLAG_OK);
          acceptData(s);
        }
        case FLAG_EXIT -> {
          System.out.println("Bye-bye!");
          s.close();
        }
        case FLAG_GENERAL -> {
          s.getOutputStream().write(FLAG_OK);
          len = s.getInputStream().read(buffer);
          System.out.write(buffer, 0, len);
        }
        case FLAG_DENY -> {
          s.getOutputStream().write(FLAG_OK);
          len = s.getInputStream().read(buffer);
          System.out.write(buffer, 0, len);
        }
        }
      }
      ////
      ////
      ////

      if (!s.isClosed()) {
        s.close();
      }

    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }

  final static int FLAG_DOWNLOAD = 1;
  final static int FLAG_EXIT = 2;
  final static int FLAG_GENERAL = 3;
  final static int FLAG_OK = 4;
  final static int FLAG_DENY = 5;

  private static void acceptData(Socket s) throws IOException {
    byte[] buffer = new byte[BUF_CAP];
    int len = s.getInputStream().read(buffer);
    String fileName = new String(buffer, 0, len, CHARSET);
    System.out.println("File name: " + fileName);

    s.getOutputStream().write(FLAG_OK);

    len = s.getInputStream().read(buffer);
    long size = Long.parseLong(new String(buffer, 0, len, CHARSET));
    System.out.println("Size: " + size + " bytes");

    s.getOutputStream().write(FLAG_OK);

    new File("./downloads").mkdirs();

    String fullPath = String.join("/", "./downloads", fileName);
    FileOutputStream writer = new FileOutputStream(fullPath);

      while (size > 0 && (len = s.getInputStream().read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
        writer.write(buffer,0,len);
        size -= len;      // read upto file size
    }
    writer.close();

    s.getOutputStream().write(FLAG_OK);

    System.out.println("Downloaded the file.");

    len = s.getInputStream().read(buffer);
    System.out.write(buffer, 0, len);
  }

  private static byte[] takeHashUtf8(String text) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(toUtf8(text));
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  private static byte[] toUtf8(String text) {
    return text.getBytes(CHARSET);
  }
}
