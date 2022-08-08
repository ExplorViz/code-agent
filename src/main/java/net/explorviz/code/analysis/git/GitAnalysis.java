package net.explorviz.code.analysis.git;

import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.util.Date;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Entrypoint for this service. Expects a local path to a Git repository folder
 * ("explorviz.repo.folder.path"). Sends the analysis's results to ExplorViz code service.
 */
@ApplicationScoped
public class GitAnalysis {

  @ConfigProperty(name = "explorviz.repo.folder.path")
  /* package */ String repoPath; // NOCS

  @Inject
  /* package */ GitHelper gitHelper; // NOCS

  private void analyzeAndSendGitRepoHistory() throws IOException, NoHeadException, GitAPIException {

    try (Repository repo = this.gitHelper.openGitRepository(this.repoPath)) {
      final Ref head = repo.exactRef("refs/heads/master");
      System.out.println("Found head: " + head);

      try (Git git = new Git(repo)) {
        // use the following instead to list commits on a specific branch
        // ObjectId branchId = repository.resolve("HEAD");
        final Iterable<RevCommit> commits = git.log().add(head.getObjectId()).call();

        // final Iterable<RevCommit> commits = git.log().all().call();
        int count = 0;
        for (final RevCommit commit : commits) {

          final PersonIdent authorIdent = commit.getAuthorIdent();
          final Date authorDate = authorIdent.getWhen();

          System.out.println("LogCommit: " + authorDate);
          count++;
        }
        System.out.println(count);
      }
    }
  }

  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, NoHeadException, GitAPIException {
    this.analyzeAndSendGitRepoHistory();
  }



}
