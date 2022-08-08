package net.explorviz.code.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Injectable helper class for jGit concerns.
 */
@ApplicationScoped
public class GitHelper {

  // private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);

  /**
   * Tries to open the Git {@link Repository} based on a given folderPath.
   *
   * @param folderPath The absolute file path to the repo folder that contains the .git folder.
   * @return repository A {@link Repository}.
   * @throws IOException Thrown if JGit cannot open the Git repo.
   */
  public Repository openGitRepository(final String folderPath) throws IOException {

    final File repoDir = new File(folderPath);

    return Git.open(repoDir).getRepository();
  }

  /**
   * Returns the string content for a file path that was modified in a commit for a given repo.
   *
   * @param repo The {@link Repository}.
   * @param commit The encompassing Git commit.
   * @param path Path to the desired file.
   * @return The stringified file content.
   * @throws IOException Thrown if JGit cannot open the Git repo.
   */
  public String getContent(final Repository repo, final RevCommit commit, final String path)
      throws IOException {
    try (TreeWalk treeWalk = TreeWalk.forPath(repo, path, commit.getTree())) {
      final ObjectId blobId = treeWalk.getObjectId(0);
      try (ObjectReader objectReader = repo.newObjectReader()) {
        final ObjectLoader objectLoader = objectReader.open(blobId);
        final byte[] bytes = objectLoader.getBytes();
        return new String(bytes, StandardCharsets.UTF_8);
      }
    }
  }


}
