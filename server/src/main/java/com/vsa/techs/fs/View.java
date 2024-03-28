package com.vsa.techs.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class View {
  private final Path root;
  private final String rootMask;

  public Path curr;

  public View(String root, String rootMask) throws IOException {
    this.root = Paths.get(root);
    this.rootMask = rootMask;

    curr = this.root;
  }

  private String mask() {
    String pathToRoot = root.toString();
    String pathToCurr = curr.toString();
    return pathToCurr.replace(pathToRoot, rootMask) + " > ";
  }

  public String render(String cmd, String... params) {

    String response = switch (cmd) {
      case "info" -> info(params);
      case "ls" -> ls(params);
      case "download" -> download(params);
      case "cd" -> cd(params);
      case "exit" -> exit(params);
      default -> unknownCmd();
    };

    return response;
  }

  private String info(String... params) {
    if (params != null && params.length > 0) {
      return unknownCmd();
    }
    return "info - to get information about commands\n" +
      "ls - to show content in dir\n" +
      "download <file> - to download selected file\n" +
      "cd <dir> - to change current dir\n" + 
      "exit - to leave current session\n" +
      "HINT:\n" + 
      ". - current dir\n" +
      ".. - parent dir\n" + 
      mask();
  }

  private String ls(String... params) {
    if (params != null && params.length > 0) {
      return unknownCmd();
    }

    StringBuilder sb = new StringBuilder(SB_BUF_CAP);

    DirectoryStream<Path> entries;
    try {
      entries = Files.newDirectoryStream(curr);;
    } catch (IOException e) {
      return "Couldn't execute ls.\n" + mask();
    }

    for (Path entry : entries) {
      sb.append(entry.getFileName());
      if (Files.isDirectory(entry)) {
        sb.append(" (dir)\n");
      } else {
        sb.append(" (file) ");
        long size = 0;
        try {
          size = Files.size(entry);
        } catch (IOException e) {}

        sb.append(String.valueOf(size))
          .append("b\n");
      }
    }

    try {
      entries.close();
    } catch (IOException e) {
      // do nothing
    }
    
    return sb.append(mask()).toString();
  }

  private String download(String... params) {
    if (params == null || params.length != 1) {
      return unknownCmd();
    }
    Path pathToGivenFile = Paths.get(curr.toString(), params[0]);
    if (!Files.isDirectory(pathToGivenFile) && Files.exists(pathToGivenFile)) {
      return params[0] + " was downloaded.\n" + mask();
    }
    return unknownCmd();
  }

  private String cd(String... params) {
    if (params == null || params.length != 1) {
      return unknownCmd();
    }
    if (params[0].equals("..") && parentIsPermitted(curr)) {
      curr = curr.getParent();
      return mask();
    } 

    DirectoryStream<Path> entries;
    try {
      entries = Files.newDirectoryStream(curr);;
    } catch (IOException e) {
      return "Couldn't execute ls.\n" + mask();
    }

    for (Path entry : entries) {
      if (entry.toFile().getName().equals(params[0]) && Files.isDirectory(entry)) {
        curr = entry;
        break;
      }
    }

    try {
      entries.close();
    } catch (IOException e) {
      // do nothing
    }
    return mask();
  }

  private String exit(String... params) {
    if (params != null && params.length > 0) {
      return unknownCmd();
    }
    return "Bye bye!\n";
  }

  private String unknownCmd() {
    return "Unknown command.\n Please check supported commands:\n" + info();
  }

  private boolean parentIsPermitted(Path curr) {
    return !root.equals(curr);
  }

  private static int SB_BUF_CAP = 128;
}
