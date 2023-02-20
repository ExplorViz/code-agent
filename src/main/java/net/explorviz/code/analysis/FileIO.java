package net.explorviz.code.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileIO {
  public static void deleteDirectory(String dir) throws IOException {
    Path path = Paths.get(dir);

    // read java doc, Files.walk need close the resources.
    // try-with-resources to ensure that the stream's open directories are closed
    try (Stream<Path> walk = Files.walk(path)) {
      walk
          .sorted(Comparator.reverseOrder())
          .forEach(FileIO::deleteDirectoryExtract);
    } catch (NoSuchFileException e) {
      // ignore exception, all done
    }
  }

  // extract method to handle exception in lambda
  private static void deleteDirectoryExtract(Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      boolean deleted = new File(path.toString()).delete();
      if (!deleted) {
        System.err.printf("Unable to delete this path : %s%n%s", path, e);
      }
    }
  }
}
