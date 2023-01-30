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
import net.explorviz.code.analysis.types.RemoteRepositoryObject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
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
      this.gitRepositoryLoader.getGitRepository("",
          new RemoteRepositoryObject(url, tempGitLocation.getAbsolutePath(), "master"));
    });
  }

  @Test
  void testInvalidParameters() {
    Assertions.assertThrows(InvalidRemoteException.class, () -> {
      this.gitRepositoryLoader.getGitRepository("", new RemoteRepositoryObject("", "", "master"));
    });
  }

  @Test()
  void testMalformedRemote() {
    String url = "https://gitlab.com/0xhexdec/";
    Assertions.assertThrows(MalformedURLException.class, () -> {
      this.gitRepositoryLoader.getGitRepository("",
          new RemoteRepositoryObject(url, tempGitLocation.getAbsolutePath(), "master"));
    });
  }

  @Test
  void testFileInsteadDirectory() throws IOException {
    File file = new File(tempGitLocation.getAbsolutePath() + "/file");
    Assertions.assertTrue(file.createNewFile());
    Assertions.assertThrows(NotDirectoryException.class, () -> {
      this.gitRepositoryLoader.getGitRepository(file.getAbsolutePath(),
          new RemoteRepositoryObject("", "", "master"));
    });
  }

  @Test
  void openRepository() throws GitAPIException, IOException {
    // downloading the repository first
    try (Repository repository = this.gitRepositoryLoader.getGitRepository("",
        new RemoteRepositoryObject(httpsUrl, tempGitLocation.getAbsolutePath(), "master"))) {
      // call is here to satisfy checkstyle by not having empty try block
      System.out.println(GitRepositoryLoader.getRemoteOriginUrl(repository));
    } catch (Exception e) {
      Assertions.fail();
    }
    // checking the same folder and reopen the repository
    try (Repository repository = this.gitRepositoryLoader.getGitRepository(
        tempGitLocation.getAbsolutePath(), new RemoteRepositoryObject())) {
      Assertions.assertEquals(GitRepositoryLoader.getRemoteOriginUrl(repository), httpsUrl);
    }

  }


  @Test()
  void testPrivateRemote() {
    final String url = "https://gitlab.com/0xhexdec/privaterepotest.git";

    // try cloning without permission
    Assertions.assertThrows(TransportException.class, () -> {
      this.gitRepositoryLoader.getGitRepository("",
          new RemoteRepositoryObject(url, tempGitLocation.getAbsolutePath(), "main"));
    });

    Assertions.assertThrows(TransportException.class, () -> {
      this.gitRepositoryLoader.getGitRepository("",
          new RemoteRepositoryObject(url, tempGitLocation.getAbsolutePath(),
              new UsernamePasswordCredentialsProvider(
                  gitlabUserName, gitlabUserPassword), "master"));
    });
    Assertions.assertThrows(TransportException.class, () -> {
      this.gitRepositoryLoader.getGitRepository("",
          new RemoteRepositoryObject(url, tempGitLocation.getAbsolutePath(),
              new UsernamePasswordCredentialsProvider(
                  "username", "password"), "main"));
    });

    try (Repository repository = this.gitRepositoryLoader.getGitRepository("",
        new RemoteRepositoryObject(url, tempGitLocation.getAbsolutePath(),
            new UsernamePasswordCredentialsProvider(
                gitlabUserName, gitlabUserPassword), "main"))) {
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }

  }

  @Test()
  void testSsh() {
    try (Repository repository = this.gitRepositoryLoader.getGitRepository("",
        new RemoteRepositoryObject(sshUrl,
            tempGitLocation.getAbsolutePath(), "master"))) {
      // call is here to satisfy checkstyle by not having empty try block
      repository.getBranch();
    } catch (Exception e) {
      Assertions.fail();
    }
  }

  @Test()
  void testHttps() {
    try (Repository repository = this.gitRepositoryLoader.getGitRepository("",
        new RemoteRepositoryObject(
            httpsUrl, tempGitLocation.getAbsolutePath(), "master"))) {
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
    try (Repository repository = this.gitRepositoryLoader.getGitRepository("",
        new RemoteRepositoryObject(httpsUrl,
            tempGitLocation.getAbsolutePath(), "master"))) {
      Assertions.assertEquals(GitRepositoryLoader.getRemoteOriginUrl(repository), httpsUrl);
    }
  }

  @Test()
  void testGetStringifiedFileInCommit()
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {

    try (final Repository repository = this.gitRepositoryLoader.getGitRepository("",
        new RemoteRepositoryObject(
            "https://github.com/Alexander-Krause-Glau/Test-JGit-Code.git",
            tempGitLocation.getAbsolutePath(), "master"))) {

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
