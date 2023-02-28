package net.explorviz.code.analysis.git;

import net.explorviz.code.analysis.handler.FileDataHandler;
import net.explorviz.code.analysis.types.FileDescriptor;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * A simple collector for metrics based on git data.
 */
public final class GitMetricCollector {

  private static String author = "";

  private GitMetricCollector() {
  }


  /**
   * Resets the author, call once per commit before calling
   * {@link GitMetricCollector#addCommitGitMetrics(FileDataHandler, RevCommit)}.
   */
  public static void resetAuthor() {
    author = "";
  }

  /**
   * Adds git metrics that are valid for all files within a commit. For performance reasons, some
   * data gets cached. before calling this method for a commit, call
   * {@link GitMetricCollector#resetAuthor()} once for every new commit.
   *
   * @param fileDataHandler the fileDataHandler to add the metric to
   * @param commit the current commit
   */
  public static void addCommitGitMetrics(final FileDataHandler fileDataHandler,
                                         final RevCommit commit) {
    if (author.isBlank()) {
      author = commit.getAuthorIdent().getEmailAddress();
    }
    fileDataHandler.setAuthor(author);
  }

  /**
   * Adds git metrics that are valid for a specific file.
   *
   * @param fileDataHandler the fileDataHandler to add the metric to
   * @param fileDescriptor the fileDescriptor holding the file data
   */
  public static void addFileGitMetrics(final FileDataHandler fileDataHandler,
                                       final FileDescriptor fileDescriptor) {
    fileDataHandler.setModifications(fileDescriptor.modifiedLines, fileDescriptor.addedLines,
        fileDescriptor.removedLines);
  }
}
