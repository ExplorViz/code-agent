package net.explorviz.code.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GitHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);

  public Repository openGitRepository(final String folderPath) throws IOException {

    final File repoDir = new File(folderPath);

    // now open the resulting repository with a FileRepositoryBuilder
    final FileRepositoryBuilder builder = new FileRepositoryBuilder();
    try (
        Repository repository = builder.setGitDir(repoDir).readEnvironment().findGitDir().build()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Having repository: {}", repository.getDirectory());
      }

      return repository;

      // the Ref holds an ObjectId for any type of object (tree, commit, blob, tree)
      // return repository.exactRef("refs/heads/master");
    }

  }

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
