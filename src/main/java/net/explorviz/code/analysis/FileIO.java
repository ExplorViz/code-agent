package net.explorviz.code.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to bundle some file deletion methods.
 */
public final class FileIO { // NOCS
  private static final Logger LOGGER = LoggerFactory.getLogger(FileIO.class);

  private FileIO() {
  }

  /**
   * Deletes a directory with all its children files and directories.
   *
   * @param dir the path to the directory
   * @throws IOException gets thrown if the path to the directory is not valid
   */
  public static void cleanDirectory(final String dir) throws IOException {
    final Path path = Paths.get(dir);
    final File[] files = path.toFile().listFiles();
    // clean if directory is not empty
    if (files == null || files.length != 0) {

      try (Stream<Path> walk = Files.walk(path)) {
        walk
            .sorted(Comparator.reverseOrder())
            .forEach(FileIO::deleteDirectoryExtract);
      } catch (NoSuchFileException e) {
        assert true;
      }
    }
  }

  // extract method to handle exception in lambda
  private static void deleteDirectoryExtract(final Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      final boolean deleted = new File(path.toString()).delete();
      if (!deleted && LOGGER.isErrorEnabled()) {
        LOGGER.error("Unable to delete this path : {} , {}", path, e);
      }
    }
  }
}
