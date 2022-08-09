package net.explorviz.code.analysis.git;

import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint for this service. Expects a local path to a Git repository folder
 * ("explorviz.repo.folder.path"). Sends the analysis's results to ExplorViz code service.
 */
@ApplicationScoped
public class GitAnalysis {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitAnalysis.class);

  @ConfigProperty(name = "explorviz.repo.folder.path")
  /* package */ String repoPath; // NOCS

  @Inject
  /* package */ GitHelper gitHelper; // NOCS

  private void analyzeAndSendGitRepoHistory() throws IOException, NoHeadException, GitAPIException {

    try (Repository repository = this.gitHelper.openGitRepository(this.repoPath)) {

      // get a list of all known heads, tags, remotes, ...
      final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();

      // a RevWalk allows to walk over commits based on some filtering that is defined
      try (RevWalk revWalk = new RevWalk(repository)) {

        revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
        revWalk.sort(RevSort.REVERSE, true);

        for (final Ref ref : allRefs) {
          revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Walking all commits starting with {}, refs: {}", allRefs.size(), allRefs);
        }
        int count = 0;
        for (final RevCommit commit : revWalk) {
          // System.out.println("Commit: " + commit);
          final PersonIdent authorIdent = commit.getAuthorIdent();
          final Date authorDate = authorIdent.getWhen();

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("LogCommitDate: {}", authorDate);
          }
          count++;
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Analyzed {} commits", count);
        }
      }
    }
  }


  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, NoHeadException, GitAPIException {
    this.analyzeAndSendGitRepoHistory();
  }

}
