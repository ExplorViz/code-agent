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

  public FileDescriptor(final ObjectId objectId, final String fileName) {
    this.objectId = objectId;
    this.fileName = fileName;
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
    return Objects.equals(fileName, descr.fileName);
  }

  @Override
  public int hashCode() {
    final int result = objectId == null ? 0 : objectId.hashCode();
    return 31 * result + (fileName == null ? 0 : fileName.hashCode());  // NOCS
  }

  @Override
  public String toString() {
    return "<" + objectId.toString() + ", " + fileName + ">";
  }
}