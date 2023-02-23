package net.explorviz.code.analysis.git;

import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.types.FileDescriptor;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitMetricCollector {

  private static String author = "";


  public static void resetAuthor() {
    author = "";
  }

  public static void addCommitGitMetrics(FileDataHandler fileDataHandler, RevCommit commit) {
    if (author.isBlank()) {
      author = commit.getAuthorIdent().getEmailAddress();
    }
    fileDataHandler.setAuthor(author);
  }

  public static void addFileGitMetrics(FileDataHandler fileDataHandler,
                                       final FileDescriptor fileDescriptor) {
    fileDataHandler.setModifications(fileDescriptor.modifiedLines, fileDescriptor.addedLines,
        fileDescriptor.removedLines);
  }
}
