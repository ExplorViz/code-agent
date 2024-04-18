package net.explorviz.code.analysis.git;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.explorviz.code.analysis.exceptions.MalformedPathException;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to ease the finding of directories inside a local git Repository.
 */
public final class DirectoryFinder {

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryFinder.class);
  private static final Map<String, List<String>> PATHS = new HashMap<>();

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
   * @param root  the path the return should be relative to
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
   * @param root  the root path to start the search from
   * @return the directories matching the search strings
   * @throws MalformedPathException thrown if the search string is malformed and can not be used
   * @throws NotFoundException      thrown if no directory matches the given search string
   */
  public static List<String> getDirectory(final List<String> paths, final String root) // NOPMD
      throws NotFoundException {
    final List<String> pathList = new ArrayList<>();
    for (final String path : paths) {
      if (path.isEmpty()) {
        continue;
      }
      // Handle existing valid paths
      if (PATHS.containsKey(path) && PATHS.get(path).stream()
          .allMatch(p -> new File(p).isDirectory())) {

        pathList.addAll(PATHS.get(path));
        continue;
      }

      String sourceDir = path;
      boolean isOptionalPath = false;

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
        sourceDir = sourceDir.replaceAll("\\\\", "/").replaceAll("//", "/");
        LOGGER.warn("found double file separator, replaced input with -> {}", sourceDir);
      }
      sourceDir = sourceDir.replaceAll("^\\\\+|^/+", "");
      final String[] arr = sourceDir.split("[\\\\/]");
      final List<String> traverseFolders = new ArrayList<>(Arrays.asList(arr));

      final List<String> dirs = findFolder(root, traverseFolders);

      if (dirs.isEmpty() && !isOptionalPath) {
        throw new NotFoundException(
            "The search string " + path + " was not found anywhere inside " + root);
      }
      // Store all found directories
      PATHS.put(path, dirs);
      dirs.forEach(dir -> {
        File dirFile = new File(dir);
        if (dirFile.isDirectory()) {
          pathList.add(dirFile.getAbsolutePath());
        }
      });
    }
    return pathList;
  }


  private static List<String> findFolder(final String currentPath,
      final List<String> traverseFolders) {
    List<String> foundFolders = new ArrayList<>();

    // the current path is the folder we searched for, as the traverse folders are empty
    if (traverseFolders.isEmpty()) {
      foundFolders.add(currentPath);
      return foundFolders;
    }

    // get all directories in the current directory, so we can search for the right one
    final String[] directories = new File(currentPath).list(
        (current, name) -> new File(current, name).isDirectory());

    // if this folder is empty, return empty list
    if (directories == null) {
      return foundFolders;
    }

    // Check if the next traverse folder is found in the list, search there
    if (Arrays.stream(directories).anyMatch(Predicate.isEqual(traverseFolders.get(0)))) {
      final String folderName = traverseFolders.get(0);
      traverseFolders.remove(0);
      return findFolder(currentPath + File.separator + folderName,
          new ArrayList<>(traverseFolders));
    }

    // Handle wildcard searches
    if ("*".equals(traverseFolders.get(0))) {

      traverseFolders.remove(0); // Remove the wildcard for deeper searches

      for (final String directory : directories) {
        List<String> folders = new ArrayList<>(
            traverseFolders); // Copy remaining paths for recursive search

        // Search recursively without the wildcard
        List<String> pathResults = findFolder(currentPath + File.separator + directory, folders);
        foundFolders.addAll(pathResults);

        // Continue to search in remaining directories using wildcard if needed
        if (!traverseFolders.isEmpty()) {
          folders = new ArrayList<>(traverseFolders); // Restore traverse folders for next iteration
          folders.add(0, "*"); // Add wildcard back for the next directory
          pathResults = findFolder(currentPath + File.separator + directory, folders);
          foundFolders.addAll(pathResults);
        }
      }
    }

    return foundFolders;
  }

}
