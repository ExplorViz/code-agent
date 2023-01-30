package net.explorviz.code.analysis.git;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.explorviz.code.analysis.exceptions.MalformedPathException;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryFinder {
  private static Logger LOGGER = LoggerFactory.getLogger(DirectoryFinder.class);
  private static Map<String, String> paths = new HashMap<>();

  public static void resetDirectory(String path) {
    paths.remove(path);
  }

  public static String getDirectory(String path) throws MalformedPathException, NotFoundException {
    // checks if a path exists in the map and is still valid
    if (paths.get(path) != null && new File(paths.get(path)).isDirectory()) {
      return paths.get(path);
    }
    String sourceDir = path;
    // handle the wildcard
    if (sourceDir.contains("*")) {
      if (sourceDir.matches("\\*[/\\\\]?$")) {
        throw new MalformedPathException(
            "Wildcard character can not be the last, search would not terminate! Given -> "
                + sourceDir);
      }
      if (sourceDir.matches("\\\\\\\\|//")) {
        sourceDir = sourceDir.replaceAll("\\\\", "\\").replaceAll("//", "/");
        LOGGER.warn("found double file separator, replaced input with -> {}", sourceDir);
      }
      final String[] arr = sourceDir.split("[*\\\\/]");
      final List<String> traverseFolders = new ArrayList<>(Arrays.asList(arr));
      final String dir = findFolder(GitRepositoryHandler.getCurrentRepositoryPath(),
          traverseFolders);
      if (dir.isEmpty()) {
        throw new NotFoundException("directory was not found");
      }
      paths.put(path, new File(dir).getAbsolutePath());

    } else {
      String p = Path.of(GitRepositoryHandler.getCurrentRepositoryPath(), sourceDir)
          .toString();
      paths.put(path, p);
    }
    return paths.get(path);
  }

  private static String findFolder(String currentPath, List<String> traverseFolders) {

    // the current path is the folder we searched for, as the traverse folders are empty
    if (traverseFolders.isEmpty()) {
      return currentPath;
    }
    // get all directories in the current directory, so we can search for the right one
    String[] directories = new File(currentPath).list(
        (current, name) -> new File(current, name).isDirectory());
    // if this folder is empty, throw an exception. we only get here if the traverse folder
    // hierarchy is not right, or we got here through a wildcard operator
    if (directories == null) {
      return "";
    }
    // if the next traverse folder is found in the list, search there
    if (Arrays.stream(directories)
        .anyMatch(Predicate.isEqual(traverseFolders.get(0)))) {
      String folderName = traverseFolders.get(0);
      traverseFolders.remove(0);
      return findFolder(currentPath + File.separator + folderName, traverseFolders);
    }
    // this is a wildcard, perform depth-first search
    if (traverseFolders.get(0).isEmpty()) {
      // maybe the wildcard is there, but we are already in the right directory
      if (Arrays.stream(directories)
          .anyMatch(Predicate.isEqual(traverseFolders.get(1)))) {
        traverseFolders.remove(0);
        String folderName = traverseFolders.get(0);
        traverseFolders.remove(0);
        return findFolder(currentPath + File.separator + folderName, traverseFolders);
      }
      for (String directory : directories) {
        List<String> folders = traverseFolders.stream().skip(1).collect(Collectors.toList());
        // search in the next level as the folder is there
        String path = findFolder(currentPath + File.separator + directory, folders);
        if (!path.isEmpty()) {
          return path;
        }
        // search the next level with wildcard
        path = findFolder(currentPath + File.separator + directory, traverseFolders);
        if (!path.isEmpty()) {
          return path;
        }
      }
    }
    // folder was not found
    return "";
  }
}
