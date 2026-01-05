package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.FileData;

/**
 * FileData object holds data from a analyzed .java file.
 * 
 * @deprecated Use {@link JavaFileDataHandler} directly.
 */
@Deprecated
public class FileDataHandler extends JavaFileDataHandler implements ProtoBufConvertable<FileData> {

  /**
   * Creates a blank FileData object for Java files.
   * 
   * @deprecated Use {@link JavaFileDataHandler#JavaFileDataHandler(String)} instead.
   */
  @Deprecated
  public FileDataHandler(final String fileName) {
    super(fileName);
  }
}
