package net.explorviz.code.analysis.git;

import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Testing the repository loader.
 */
@QuarkusTest
public class GitRepositoryLoaderTest {

  @Inject
  GitRepositoryLoader gitRepositoryLoader;  //NOCS

  //  @Test()
  //  void testOpenRepo()
  //      throws InvalidRemoteException, TransportException, GitAPIException, IOException {
  //
  //    final File tmpGitLocation = Files.createTempDirectory("explorviz-test").toFile();
  //
  //    OLD
  //    try (Git result = Git.cloneRepository()
  //    .setURI("https://github.com/Alexander-Krause-Glau/Test-JGit-Code.git")
  //    .setDirectory(tmpGitLocation).call()) {
  //
  //      final Repository repo = this.gitRepositoryLoader
  //      .openGitRepository(tmpGitLocation.getAbsolutePath());
  //
  //      Assertions.assertEquals(repo.getRemoteNames(), result.getRepository().getRemoteNames());
  //    }
  //
  //  }

  @Test()
  void testInvalidRemote() throws GitAPIException, IOException {
    CredentialsProvider provider = CredentialsProvider.getDefault();
    final File tmpGitLocation = Files.createTempDirectory("explorviz-test1").toFile();

    Assertions.assertThrows(InvalidRemoteException.class, () -> {
      try (Repository repository = this.gitRepositoryLoader.downloadGitRepository(
          tmpGitLocation.getAbsolutePath(), "", provider)) {
        System.out.println("Open");
      }
    });
  }

  @Test()
  void testPrivateRemote() throws GitAPIException, IOException {
    CredentialsProvider provider = CredentialsProvider.getDefault();
    String url = "https://gitlab.com/0xhexdec/interpreter";
    final File tmpGitLocation = Files.createTempDirectory("explorviz-test2").toFile();

    Assertions.assertThrows(TransportException.class, () -> {
      Repository repository = this.gitRepositoryLoader.downloadGitRepository(
          tmpGitLocation.getAbsolutePath(), url, provider);
    });
  }


  @Test()
  void testGetStringifiedFileInCommit()
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {

    final File tmpGitLocation = Files.createTempDirectory("explorviz-test3").toFile();

    try (final Repository repository = this.gitRepositoryLoader.getGitRepository(
        tmpGitLocation.getAbsolutePath(),
        "https://github.com/Alexander-Krause-Glau/Test-JGit-Code.git", "", "")) {

      try (RevWalk walk = new RevWalk(repository)) {
        final ObjectId id = repository.resolve("8ee1f25");

        final RevCommit commit = walk.parseCommit(id);

        final RevTree tree = commit.getTree();

        try (TreeWalk treeWalk = new TreeWalk(repository)) {
          treeWalk.addTree(tree);
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            final String actual = this.gitRepositoryLoader.getContent(treeWalk.getObjectId(0),
                repository);
            final String expected = "package testgit.my.test.pckg;\n" + "\n"
                + "public class TestGitClass {\n" + "\n" + "  private final String testVariable;\n"
                + "\n" + "  public TestGitClass(final String testVariable) {\n"
                + "    this.testVariable = testVariable;\n" + "  }\n" + "\n" + "}";

            Assertions.assertEquals(expected.replace(" ", "").replace("\n", "").replace("\r", ""),
                actual.replace(" ", "").replace("\n", "").replace("\r", ""));

            walk.dispose();

          }
        }
      }
    }

  }

}
