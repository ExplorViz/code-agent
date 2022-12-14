package net.explorviz.code.analysis.git;

import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing the repository loader.
 */
@QuarkusTest
public class GitRepositoryLoaderTest {

  @Inject
  GitRepositoryLoader gitRepositoryLoader;  // NOCS

  private File tempGitLocation;

  private final String sshUrl = "git@gitlab.com:0xhexdec/busydoingnothing.git";
  private final String httpsUrl = "https://gitlab.com/0xhexdec/busydoingnothing.git";

  private final String gitlabUserName = "privateTestConnector";
  private final String gitlabUserPassword = "_QGDgx@2!sD/y!Y";


  @BeforeEach
  void setup() throws IOException {
    tempGitLocation = Files.createTempDirectory("explorviz-test").toFile();
  }

  @AfterEach
  void tearDown() throws IOException {
    try (Stream<Path> walk = Files.walk(tempGitLocation.toPath())) {
      walk.sorted(Comparator.reverseOrder()).map(Path::toFile)
          .forEach(File::delete);
    }
  }


  @Test()
  void testInvalidRemote() {
    String url = "%%%%";

    Assertions.assertThrows(InvalidRemoteException.class, () -> {
      this.gitRepositoryLoader.getGitRepository(tempGitLocation.getAbsolutePath(), url);
    });
  }

  @Test
  void testInvalidParameters() {
    Assertions.assertThrows(IOException.class, () -> {
      this.gitRepositoryLoader.getGitRepository("");
    });
  }

  @Test()
  void testMalformedRemote() {
    String url = "https://gitlab.com/0xhexdec/";
    Assertions.assertThrows(MalformedURLException.class, () -> {
      this.gitRepositoryLoader.getGitRepository(tempGitLocation.getAbsolutePath(), url);
    });
  }

  @Test
  void testFileInsteadDirectory() throws IOException {
    File file = new File(tempGitLocation.getAbsolutePath() + "/file");
    Assertions.assertTrue(file.createNewFile());
    Assertions.assertThrows(NotDirectoryException.class, () -> {
      this.gitRepositoryLoader.getGitRepository(file.getAbsolutePath());
    });
  }

  @Test
  void testOverridingRepository() throws GitAPIException, IOException {
    // create mocked local git repository
    try (Git git = Git.init().setDirectory(tempGitLocation).call()) {
      // for (int i = 0; i < 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1; i++) {
      for (int i = 0; i < 10; i++) {  // NOCS  10 is arbitrary here
        final String filename = String.format("testfile%d", i);
        File f = new File(git.getRepository().getDirectory().getParent(),
            filename);
        f.createNewFile();
        if (i % 2 == 0) {
          git.add().addFilepattern(filename).call();
        }
      }
    }

    try (final Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), httpsUrl)) {
      repository.getBranch();
    }
  }

  @Test
  void openRepository() throws GitAPIException, IOException {
    // downloading the repository first
    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), httpsUrl)) {
      // call is here to satisfy checkstyle by not having empty try block
      System.out.println(GitRepositoryLoader.getRemoteOriginUrl(repository));
    } catch (Exception e) {
      Assertions.fail();
    }
    // checking the same folder and reopen the repository
    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath())) {
      // call is here to satisfy checkstyle by not having empty try block
      System.out.println(GitRepositoryLoader.getRemoteOriginUrl(repository));
      Assertions.assertEquals(GitRepositoryLoader.getRemoteOriginUrl(repository), httpsUrl);
    }

  }

  @Test()
  void testPrivateRemote() {
    final String url = "https://gitlab.com/0xhexdec/privaterepotest.git";

    // try cloning without permission
    Assertions.assertThrows(TransportException.class, () -> {
      this.gitRepositoryLoader.getGitRepository(tempGitLocation.getAbsolutePath(), url);
    });

    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), url, gitlabUserName, gitlabUserPassword)) {
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }

  }

  @Test()
  void testSsh() {
    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), sshUrl)) {
      // call is here to satisfy checkstyle by not having empty try block
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test()
  void testHttps() {
    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), httpsUrl)) {
      // call is here to satisfy checkstyle by not having empty try block
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test()
  void testSshConversion() {
    Assertions.assertEquals(Map.entry(true, httpsUrl),
        GitRepositoryLoader.convertSshToHttps(httpsUrl));


    Assertions.assertEquals(Map.entry(true, httpsUrl),
        GitRepositoryLoader.convertSshToHttps(sshUrl));

    // if the url looks off, assume the user wants it that way
    final String urlUnderTest2 = "abc.xyz";
    Assertions.assertEquals(Map.entry(false, urlUnderTest2),
        GitRepositoryLoader.convertSshToHttps(urlUnderTest2));

  }


  @Test
  void testRemoteLookup() throws GitAPIException, IOException {
    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), httpsUrl)) {
      Assertions.assertEquals(GitRepositoryLoader.getRemoteOriginUrl(repository), httpsUrl);
    }
  }

  @Test()
  void testGetStringifiedFileInCommit()
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {

    try (final Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(),
        "https://github.com/Alexander-Krause-Glau/Test-JGit-Code.git", "", "")) {

      try (RevWalk walk = new RevWalk(repository)) {
        final ObjectId id = repository.resolve("8ee1f25");

        final RevCommit commit = walk.parseCommit(id);

        final RevTree tree = commit.getTree();

        try (TreeWalk treeWalk = new TreeWalk(repository)) {
          treeWalk.addTree(tree);
          treeWalk.setRecursive(true);
          while (treeWalk.next()) {
            final String actual = GitRepositoryLoader.getContent(treeWalk.getObjectId(0),
                repository);
            final String expected =
                "package testgit.my.test.pckg;\n" + "\n" + "public class TestGitClass {\n" + "\n"
                    + "  private final String testVariable;\n" + "\n"
                    + "  public TestGitClass(final String testVariable) {\n"
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
