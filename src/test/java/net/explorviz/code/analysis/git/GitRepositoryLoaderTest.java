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
  void testInvalidRemote() throws GitAPIException, IOException {
    CredentialsProvider provider = CredentialsProvider.getDefault();
    String url = "%%%%";

    Assertions.assertThrows(InvalidRemoteException.class, () -> {
      this.gitRepositoryLoader.downloadGitRepository(tempGitLocation.getAbsolutePath(), url,
          provider);
    });
  }

  @Test()
  void testMalformedRemote() throws GitAPIException, IOException {
    CredentialsProvider provider = CredentialsProvider.getDefault();
    String url = "https://gitlab.com/0xhexdec/";
    Assertions.assertThrows(MalformedURLException.class, () -> {
      this.gitRepositoryLoader.downloadGitRepository(tempGitLocation.getAbsolutePath(), url,
          provider);
    });
  }

  @Test
  void testFileInsteadDirectory() throws IOException {
    File file = new File(tempGitLocation.getAbsolutePath() + "/file");
    Assertions.assertTrue(file.createNewFile());
    Assertions.assertThrows(NotDirectoryException.class, () -> {
      this.gitRepositoryLoader.openGitRepository(file.getAbsolutePath());
    });
  }

  @Test
  void testInMemoryRepository() throws GitAPIException, IOException {
    final CredentialsProvider provider = CredentialsProvider.getDefault();
    try (Repository repository = this.gitRepositoryLoader.getInMemoryRepository(httpsUrl,
        provider)) {
      repository.getFullBranch();
    }
  }

  @Test
  void openRepository() throws IOException {
    final CredentialsProvider provider = CredentialsProvider.getDefault();
    try (Repository repository = this.gitRepositoryLoader.downloadGitRepository(
        tempGitLocation.getAbsolutePath(), httpsUrl, provider)) {
      // call is here to satisfy checkstyle by not having empty try block
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }

    try (Repository repository = this.gitRepositoryLoader.openGitRepository(
        tempGitLocation.getAbsolutePath())) {
      // call is here to satisfy checkstyle by not having empty try block
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }

  }

  @Test()
  void testPrivateRemote() throws GitAPIException, IOException {
    final String url = "https://gitlab.com/0xhexdec/privaterepotest.git";

    // try cloning without permission
    Assertions.assertThrows(TransportException.class, () -> {
      this.gitRepositoryLoader.getGitRepository(tempGitLocation.getAbsolutePath(), url,
          "", "");
    });

    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), url, gitlabUserName, gitlabUserPassword)) {
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }

  }

  @Test()
  void testSsh() throws GitAPIException, IOException {
    final CredentialsProvider provider = CredentialsProvider.getDefault();
    try (Repository repository = this.gitRepositoryLoader.downloadGitRepository(
        tempGitLocation.getAbsolutePath(), sshUrl, provider)) {
      // call is here to satisfy checkstyle by not having empty try block
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test()
  void testHttps() throws GitAPIException, IOException {
    final CredentialsProvider provider = CredentialsProvider.getDefault();
    try (Repository repository = this.gitRepositoryLoader.downloadGitRepository(
        tempGitLocation.getAbsolutePath(), httpsUrl, provider)) {
      // call is here to satisfy checkstyle by not having empty try block
      // repository.getBranch();
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
  void testRemoteLookup() throws GitAPIException, MalformedURLException {
    final CredentialsProvider provider = CredentialsProvider.getDefault();
    try (Repository repository = this.gitRepositoryLoader.downloadGitRepository(
        tempGitLocation.getAbsolutePath(), httpsUrl, provider)) {
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
            final String actual = this.gitRepositoryLoader.getContent(treeWalk.getObjectId(0),
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
