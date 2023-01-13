package net.explorviz.code.analysis.git;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import net.explorviz.code.analysis.JavaParserService;
import net.explorviz.code.analysis.Parser;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.proto.StructureEventServiceGrpc;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
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

  @ConfigProperty(name = "explorviz.gitanalysis.local.folder.path")
  /* default */ Optional<String> repoPathProperty;  // NOCS

  @Inject
  /* package */ GitRepositoryLoader gitRepositoryLoader; // NOCS

  @Inject
  /* package */ JavaParserService parserService; // NOCS

  @Inject
  /* package */ Parser parser; // NOCS

  @GrpcClient("structureevent")
  /* package */ StructureEventServiceGrpc.StructureEventServiceBlockingStub grpcClient; // NOCS

  private void analyzeAndSendRepo()
      throws IOException, GitAPIException, PropertyNotDefinedException { // NOPMD
    // steps:
    // open or download repository
    // get remote state of the analyzed data
    // loop for missing commits
    //  - find difference between last and "current" commit
    //  - analyze differences
    //  - send data chunk

    try (Repository repository = this.gitRepositoryLoader.getGitRepository()) {

      // get a list of all known heads, tags, remotes, ...
      final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();

      // a RevWalk allows to walk over commits based on some filtering that is defined
      try (RevWalk revWalk = new RevWalk(repository)) {

        // revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
        revWalk.sort(RevSort.COMMIT_TIME_DESC, true);
        // revWalk.sort(RevSort.REVERSE, true);

        for (final Ref ref : allRefs) {
          revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Walking all commits starting with {}, refs: {}", allRefs.size(), allRefs);
        }
        int count = 0;
        int files = 0;
        for (final RevCommit commit : revWalk) {

          final PersonIdent authorIdent = commit.getAuthorIdent();
          final Date commitDate = authorIdent.getWhen();

          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("LogCommitDate: {}", commitDate);
          }
          count++;

          final RevTree tree = commit.getTree();
          // System.out.println("Having tree: " + tree);

          // now use a TreeWalk to iterate over all files in the Tree recursively
          // you can set Filters to narrow down the results if needed
          try (final TreeWalk treeWalk = new TreeWalk(repository)) { // NOPMD
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathSuffixFilter.create(".java"));
            while (treeWalk.next()) {
              // System.out.println("found: " + treeWalk.getPathString());

              final String fileContent =
                  this.gitRepositoryLoader.getContent(treeWalk.getObjectId(0), repository);

              // TODO: original parser
              // final List<StructureFileEvent> classes =
              //     this.parserService.processStringifiedClass(fileContent);

              // TODO: new parser
              LOGGER.info("analyze: " + treeWalk.getPathString());
              this.parser.fullParse(fileContent);
              files++;
              // System.out.println(this.parser.fullParse(fileContent));
              // break;

              // TODO: enable GRPC again
              // for (int i = 0; i < classes.size(); i++) {
              //   final StructureFileEvent event = classes.get(i);
              //   final StructureFileEvent eventWithTiming = StructureFileEvent.newBuilder(event)
              //       .setEpochMilli(authorIdent.getWhen().getTime()).build();
              //   classes.set(i, eventWithTiming);
              //   // grpcClient.sendStructureFileEvent(event).await().indefinitely();
              //   grpcClient.sendStructureFileEvent(event);
              // }

              // if (LOGGER.isDebugEnabled()) {
              //   LOGGER.debug("Classes names: {}", classes);
              // }


            }
          }
          break;
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Analyzed {} commits", count);
          LOGGER.debug("Analyzed {} files", files);
        }
      }
    }
  }

  public void run() throws GitAPIException, IOException, PropertyNotDefinedException {
    this.analyzeAndSendRepo();
  }


  /* package */ void onStart(@Observes final StartupEvent ev)
      throws IOException, NoHeadException, GitAPIException, PropertyNotDefinedException {
    // TODO: delete, but currently needed for testing
    // if (repoPathProperty.isEmpty()) {
    //   return;
    // }
    this.analyzeAndSendRepo();
  }

}
