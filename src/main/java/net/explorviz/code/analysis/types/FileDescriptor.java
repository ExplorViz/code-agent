package net.explorviz.code.analysis.types;

import java.util.Objects;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Basic data object to link the objectId of git files to the associated file names. Heavily based
 * on {@link com.github.javaparser.utils.Pair}
 */
public class FileDescriptor {
  public final ObjectId objectId; // NOCS
  public final String fileName;   // NOCS
  public final String relativePath; // NOCS
  public int modifiedLines; // NOCS
  public int addedLines; // NOCS
  public int removedLines; // NOCS

  /**
   * Create a new FileDescriptor.
   *
   * @param objectId the ObjectId of the File
   * @param fileName the name of the File
   * @param relativePath the relative path of the file starting from the repository's
   *     directory.
   */
  public FileDescriptor(final ObjectId objectId, final String fileName, final String relativePath) {
    this.objectId = objectId;
    this.fileName = fileName;
    this.relativePath = relativePath;
  }

  /**
   * Create a new FileDescriptor with modification data added.
   *
   * @param objectId the ObjectId of the File
   * @param fileName the name of the File
   * @param relativePath the relative path of the file starting from the repository's
   *     directory.
   * @param modificationData a {@link Triple} containing the modification data of the file. left
   *     -> amount of modified lines, middle -> amount of added lines, right -> amount of removed
   *     lines
   */
  public FileDescriptor(final ObjectId objectId, final String fileName, final String relativePath,
                        final Triple<Integer, Integer, Integer> modificationData) {
    this(objectId, fileName, relativePath);
    if (modificationData != null) {
      this.modifiedLines = modificationData.getLeft();
      this.addedLines = modificationData.getMiddle();
      this.removedLines = modificationData.getRight();
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final FileDescriptor descr = (FileDescriptor) o;

    if (!Objects.equals(objectId, descr.objectId)) {
      return false;
    }
    if (!Objects.equals(fileName, descr.fileName)) {
      return false;
    }
    return Objects.equals(relativePath, descr.relativePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectId, fileName, relativePath);
  }

  @Override
  public String toString() {
    return "<" + objectId.toString() + ", " + fileName + ", " + relativePath + ">";
  }
}