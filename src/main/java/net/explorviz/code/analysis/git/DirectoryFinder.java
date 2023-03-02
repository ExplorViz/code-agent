package net.explorviz.code.analysis.git;

import java.io.File;
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

/**
 * Class to ease the finding of directories inside a local git Repository.
 */
public final class DirectoryFinder {
  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryFinder.class);
  private static final Map<String, String> PATHS = new HashMap<>();

  private DirectoryFinder() {

  }

  /**
   * Resets the given path entry, the saved value will be removed.
   *
   * @param path the search string for the path used to create it
   */
  public static void resetDirectory(final String path) {
    PATHS.remove(path);
  }

  /**
   * Resets the internal path storage.
   */
  public static void reset() {
    PATHS.clear();
  }

  /**
   * Searches and return the relative path to the directory matching the search string.
   *
   * @param paths a list of search strings for the paths.
   * @param root the path the return should be relative to
   * @return the directories matching the search strings
   * @throws NotFoundException thrown if no directory matches the given search string
   */
  public static List<String> getRelativeDirectory(final List<String> paths,
                                                  final String root)
      throws NotFoundException {
    final List<String> relativePaths = new ArrayList<>();
    for (final String path : getDirectory(paths, root)) {
      relativePaths.add(path.replace(root, ""));
    }
    return relativePaths;
  }

  /**
   * Searches and return the absolute path to the directory matching the search string.
   *
   * @param paths a list of search strings for the paths.
   * @param root the root path to start the search from
   * @return the directories matching the search strings
   * @throws MalformedPathException thrown if the search string is malformed and can not be used
   * @throws NotFoundException      thrown if no directory matches the given search string
   */
  public static List<String> getDirectory(final List<String> paths, final String root) // NOPMD
      throws NotFoundException {
    final List<String> pathList = new ArrayList<>();
    for (final String path : paths) {
      // checks if a path exists in the map and is still valid
      if (PATHS.get(path) != null && new File(PATHS.get(path)).isDirectory()) { // NOPMD
        pathList.add(PATHS.get(path));
        continue;
      }
      String sourceDir = path;  // NOPMD
      boolean isOptionalPath = false;
      // check if path is enclosed in brackets and therefore optional
      if (sourceDir.matches("^\\[.*\\]$")) {
        sourceDir = sourceDir.replaceAll("\\[|\\]", "");
        isOptionalPath = true;
      }
      if (sourceDir.matches("\\*[/\\\\]?$")) {
        throw new MalformedPathException(
            "Wildcard character can not be the last, search would not terminate! Given -> "
                + sourceDir);
      }
      if (sourceDir.matches("\\\\\\\\|//")) {
        sourceDir = sourceDir.replaceAll("\\\\", "\\").replaceAll("//", "/");
        LOGGER.warn("found double file separator, replaced input with -> {}", sourceDir);
      }
      // Strip leading slashes
      sourceDir = sourceDir.replaceAll("^\\\\+|^/+", "");
      final String[] arr = sourceDir.split("[\\\\/]");
      final List<String> traverseFolders = new ArrayList<>(Arrays.asList(arr)); // NOPMD
      final String dir = findFolder(root, traverseFolders);
      if (dir.isEmpty()) {
        // skip this path if not found as it was declared as optional
        if (isOptionalPath) {
          continue;
        }
        throw new NotFoundException(
            "The search string " + path + " was not found anywhere inside " + root);
      }
      PATHS.put(path, new File(dir).getAbsolutePath()); // NOPMD

      pathList.add(PATHS.get(path));
    }
    return pathList;
  }

  private static String findFolder(final String currentPath, // NOPMD
                                   final List<String> traverseFolders) {

    // the current path is the folder we searched for, as the traverse folders are empty
    if (traverseFolders.isEmpty()) {
      return currentPath;
    }
    // get all directories in the current directory, so we can search for the right one
    final String[] directories = new File(currentPath).list(
        (current, name) -> new File(current, name).isDirectory());
    // if this folder is empty, throw an exception. we only get here if the traverse folder
    // hierarchy is not right, or we got here through a wildcard operator
    if (directories == null) {
      return "";
    }
    // if the next traverse folder is found in the list, search there
    if (Arrays.stream(directories)
        .anyMatch(Predicate.isEqual(traverseFolders.get(0)))) {
      final String folderName = traverseFolders.get(0);
      traverseFolders.remove(0);
      return findFolder(currentPath + File.separator + folderName, traverseFolders);
    }
    // this is a wildcard, perform depth-first search
    if ("*".equals(traverseFolders.get(0))) {
      // maybe the wildcard is there, but we are already in the right directory
      if (Arrays.stream(directories)
          .anyMatch(Predicate.isEqual(traverseFolders.get(1)))) {
        traverseFolders.remove(0);
        final String folderName = traverseFolders.get(0);
        traverseFolders.remove(0);
        return findFolder(currentPath + File.separator + folderName, traverseFolders);
      }
      for (final String directory : directories) {
        final List<String> folders = traverseFolders.stream().skip(1).collect(Collectors.toList());
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
