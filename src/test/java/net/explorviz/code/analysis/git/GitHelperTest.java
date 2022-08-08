package net.explorviz.code.analysis.git;

import io.quarkus.test.junit.QuarkusTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GitHelperTest {

  @Inject
  GitHelper gitHelper;

  @Test()
  void testOpenRepo()
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {

    final File tmpGitLocation = Files.createTempDirectory("explorviz-test").toFile();


    try (Git result =
        Git.cloneRepository().setURI("https://github.com/Alexander-Krause-Glau/Test-JGit-Code.git")
            .setDirectory(tmpGitLocation).call()) {

      final Repository repo = this.gitHelper.openGitRepository(tmpGitLocation.getAbsolutePath());

      Assertions.assertEquals(repo.getRemoteNames(), result.getRepository().getRemoteNames());
    }

  }

  @Test()
  void testGetStringifiedFileInCommit()
      throws InvalidRemoteException, TransportException, GitAPIException, IOException {

    final File tmpGitLocation = Files.createTempDirectory("explorviz-test").toFile();


    try (Git result =
        Git.cloneRepository().setURI("https://github.com/Alexander-Krause-Glau/Test-JGit-Code.git")
            .setDirectory(tmpGitLocation).call()) {

      final Repository repo = this.gitHelper.openGitRepository(tmpGitLocation.getAbsolutePath());

      try (RevWalk walk = new RevWalk(repo)) {
        final ObjectId id = repo.resolve("8ee1f25");
        final RevCommit commit = walk.parseCommit(id);
        final String content =
            this.gitHelper.getContent(repo, commit, "my/test/pckg/TestGitClass.java");

        final String expected = "package testgit.my.test.pckg;\n" + "\n"
            + "public class TestGitClass {\n" + "\n" + "  private final String testVariable;\n"
            + "\n" + "  public TestGitClass(final String testVariable) {\n"
            + "    this.testVariable = testVariable;\n" + "  }\n" + "\n" + "}";

        Assertions.assertEquals(expected.replace(" ", "").replace("\n", "").replace("\r", ""),
            content.replace(" ", "").replace("\n", "").replace("\r", ""));

        walk.dispose();

      }
    }

  }

}
