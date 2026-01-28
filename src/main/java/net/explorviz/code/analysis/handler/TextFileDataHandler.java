package net.explorviz.code.analysis.handler;

import net.explorviz.code.proto.FileData;
import net.explorviz.code.proto.Language;

public class TextFileDataHandler extends AbstractFileDataHandler {

  public TextFileDataHandler(final String fileName, final Language language) {
    super(fileName);
    builder.setLanguage(language);
  }

  @Override
  public FileData getProtoBufObject() {
    return builder.build();
  }

  /**
   * Calculate and add basic metrics for the text file.
   *
   * @param content The file content
   */
  public void calculateMetrics(final String content) {
    if (content == null || content.isEmpty()) {
      addMetric("loc", "0");
      addMetric("size", "0");
      return;
    }

    final int loc = content.split("\r\n|\r|\n").length;
    addMetric("loc", String.valueOf(loc));

    // Add file size in bytes
    addMetric("size", String.valueOf(content.length()));
  }
}
