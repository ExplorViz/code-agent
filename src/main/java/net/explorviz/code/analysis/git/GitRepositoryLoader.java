package net.explorviz.code.analysis.git;


import com.github.javaparser.utils.Pair;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.types.RemoteRepositoryObject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injectable helper class for jGit concerns.
 */
@ApplicationScoped
public class GitRepositoryLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryLoader.class);
  private Git git = null;

  @ConfigProperty(name = "explorviz.gitanalysis.local.folder.path")
  /* default */ Optional<String> repoPathProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.url")
  /* default */ Optional<String> repoUrlProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.localstoragepath")
  /* default */ Optional<String> repoLocalStoragePathProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.username")
  /* default */ Optional<String> usernameProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.password")
  /* default */ Optional<String> passwordProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.branch", defaultValue = "master")
  /* default */ String repositoryBranch;  // NOCS


  /**
   * Tries to download the Git {@link Repository} based on a given Url to the given.
   *
   * @param remoteRepositoryObject the {@link RemoteRepositoryObject} object containing the path
   *     and url
   * @return returns an opened git repository
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  private Repository downloadGitRepository(final RemoteRepositoryObject remoteRepositoryObject)
      throws GitAPIException, IOException {

    final Map.Entry<Boolean, String> checkedRepositoryUrl = convertSshToHttps(
        remoteRepositoryObject.getUrl());

    String repoPath = remoteRepositoryObject.getStoragePath();
    if (remoteRepositoryObject.getStoragePath().isBlank()) {
      repoPath = Files.createTempDirectory("TemporaryRepository").toAbsolutePath().toString();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("The repository will be cloned to " + repoPath);
      }
    }

    try {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Cloning repository from " + checkedRepositoryUrl.getValue());
      }
      this.git = Git.cloneRepository().setURI(checkedRepositoryUrl.getValue())
          .setCredentialsProvider(remoteRepositoryObject.getCredentialsProvider())
          .setDirectory(new File(repoPath))
          .setBranchesToClone(remoteRepositoryObject.getBranchNameAsListOrNull())
          .setBranch(remoteRepositoryObject.getBranchNameOrNull())
          .call();

      return this.git.getRepository();
    } catch (TransportException te) {
      if (!checkedRepositoryUrl.getKey()) {
        throw (MalformedURLException) new MalformedURLException(
            checkedRepositoryUrl.getValue()).initCause(te);
      }
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("The repository is private, username and password are required.");
      }
      throw te;
    } catch (InvalidRemoteException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("The repository's Url seems not right, no git repository was found there.");
      }
      throw e;
    }
  }

  /**
   * Tries to open the Git {@link Repository} based on a given folderPath.
   *
   * @param repositoryPath the system path of the local Repository
   * @return returns an opened git {@link Repository}
   * @throws IOException gets thrown if JGit cannot open the Git repository.
   */
  private Repository openGitRepository(final String repositoryPath, final String branchName)
      throws IOException, GitAPIException {

    final File localRepositoryDirectory = new File(repositoryPath);

    if (!localRepositoryDirectory.isDirectory()) {
      LOGGER.error("Given path is not a directory.");
      throw new NotDirectoryException(repositoryPath);
    }

    if (Objects.requireNonNull(localRepositoryDirectory.listFiles()).length == 0) {
      return null;
    }
    this.git = Git.open(localRepositoryDirectory);
    if (!branchName.isBlank()) {
      this.git.checkout().setName(branchName).call();
    }
    return this.git.getRepository();
  }

  /**
   * Returns a Git {@link Repository} object by opening the repository found at
   * {@code localRepositoryPath}. <br> If {@code localRepositoryPath} is empty, the repository gets
   * cloned based on data defined in {@code remoteRepositoryObject} and the opened repository gets
   * returned.
   *
   * @param localRepositoryPath the system path of the local Repository
   * @param remoteRepositoryObject the {@link RemoteRepositoryObject} object containing the path
   *     and url
   * @return returns an opened Git {@link Repository}
   * @throws IOException     gets thrown if the path is not accessible or does not point to a
   *                         folder
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  public Repository getGitRepository(final String localRepositoryPath,
                                     final RemoteRepositoryObject remoteRepositoryObject)
      throws IOException, GitAPIException {

    if (localRepositoryPath.isBlank()) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("No local repository given, using remote");
      }

      return this.downloadGitRepository(remoteRepositoryObject);
    } else {
      return this.openGitRepository(localRepositoryPath, remoteRepositoryObject.getBranchName());
    }
  }

  /**
   * Returns a Git {@link Repository} object by using the parameters set in the
   * application.properties.<br> The local repository defined in
   * {@code  explorviz.gitanalysis.local.folder.path} will be used.
   * <br>
   * If {@code  explorviz.gitanalysis.local.folder.path} is empty, the repository defined in
   * {@code  explorviz.gitanalysis.remote.url} will be cloned to the location
   * {@code explorviz.gitanalysis.remote.localstoragepath}.<br> If no storage path is given, a
   * temporary directory will be created. <br> The branch given in
   * {@code explorviz.gitanalysis.branch} will be used if present, otherwise the default (remote) or
   * current (local) will be used.
   *
   * @return an opened Git {@link Repository}
   * @throws PropertyNotDefinedException gets thrown if a needed property is not present
   * @throws GitAPIException             gets thrown if the git api encounters an error
   * @throws IOException                 gets thrown if JGit cannot open the Git repository.
   */
  public Repository getGitRepository()
      throws PropertyNotDefinedException, GitAPIException, IOException {
    if (repoUrlProperty.isEmpty() && repoPathProperty.isEmpty()) {
      throw new PropertyNotDefinedException("explorviz.gitanalysis.local.folder.path");
    }

    CredentialsProvider credentialsProvider;
    if (usernameProperty.isEmpty() || passwordProperty.isEmpty()) {
      credentialsProvider = CredentialsProvider.getDefault();
    } else {
      credentialsProvider = new UsernamePasswordCredentialsProvider(usernameProperty.get(),
          passwordProperty.get());
    }

    return getGitRepository(this.repoPathProperty.orElse(""),
        new RemoteRepositoryObject(this.repoUrlProperty.orElse(""),
            repoLocalStoragePathProperty.orElse(""),
            credentialsProvider));
  }

  public List<Pair<ObjectId, String>> listDiff(Repository repository, Optional<RevCommit> oldCommit,
                                               RevCommit newCommit)
      throws GitAPIException, IOException {
    List<Pair<ObjectId, String>> objectIdList = new ArrayList<>();

    if (oldCommit.isEmpty()) {
      try (final TreeWalk treeWalk = new TreeWalk(repository)) { // NOPMD
        treeWalk.addTree(newCommit.getTree());
        treeWalk.setRecursive(true);
        treeWalk.setFilter(PathSuffixFilter.create(".java"));
        while (treeWalk.next()) {
          objectIdList.add(new Pair<>(treeWalk.getObjectId(0), treeWalk.getNameString()));
        }
      }
    } else {
      final List<DiffEntry> diffs = this.git.diff()
          .setOldTree(prepareTreeParser(repository, oldCommit.get().getTree()))
          .setNewTree(prepareTreeParser(repository, newCommit.getTree()))
          .setPathFilter(PathSuffixFilter.create(".java"))
          .call();

      for (DiffEntry diff : diffs) {
        if (diff.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
          LOGGER.error("DELETE!!!");
        }
        String[] parts = diff.getNewPath().split("/");
        objectIdList.add(new Pair<>(diff.getNewId().toObjectId(), parts[parts.length - 1]));
      }
    }
    return objectIdList;
  }

  public static String getCurrentBranch(Repository repository) throws IOException {
    return repository.getFullBranch();
  }

  private static AbstractTreeIterator prepareTreeParser(Repository repository, RevTree tree)
      throws IOException {
    CanonicalTreeParser treeParser = new CanonicalTreeParser();
    try (ObjectReader reader = repository.newObjectReader()) {
      treeParser.reset(reader, tree.getId());
    }
    return treeParser;
  }

  /**
   * Converts a git ssh url to a https url and returns it as well as if the conversion is usable. If
   * the given url is already in https format, it will be returned as-is and the flag is set to
   * true. If the given url is in ssh format, it will be converted to https and returned and the
   * flag is set to true. If it is neither, a warning will be printed the url will get returned but
   * the flag is set to false.
   *
   * @param url the original git url
   * @return a Tuple containing a flag if the returned url should be used and the url itself
   */
  public static Map.Entry<Boolean, String> convertSshToHttps(final String url) {
    if (url.matches("^git@\\S+.\\S+:\\w+(/[\\S&&[^/]]+)+.git$")) {
      final String convertedUrl = url.replace(":", "/").replace("git@", "https://");
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "The URL seems to be a SSH url, currently"
                + " only HTTPS is supported, converted url now is: "
                + convertedUrl);
      }
      return Map.entry(true, convertedUrl);
    } else if (url.matches("^http[s]*://\\S+.\\S+(/[\\S&&[^/]]+)+.git$")) {
      // it should not matter if it is http or https here, the user should know
      return Map.entry(true, url);
    } else {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Could not convert the url to https url.");
      }
      return Map.entry(false, url);
    }
  }

  /**
   * Returns the remote origin Url from the given repository.
   *
   * @param repository the repository object
   * @return the remote origin Url
   */
  public static String getRemoteOriginUrl(final Repository repository) {
    return repository.getConfig().getString("remote", "origin", "url");
  }

  /**
   * Returns the string content for a file path that was modified in a commit for a given repo.
   *
   * @param blobId The {@link ObjectId}.
   * @param repo The {@link Repository}.
   * @return The stringified file content.
   * @throws IOException Thrown if JGit cannot open the Git repo.
   */
  public static String getContent(final ObjectId blobId, final Repository repo) throws
      IOException {
    try (ObjectReader objectReader = repo.newObjectReader()) {
      final ObjectLoader objectLoader = objectReader.open(blobId);
      final byte[] bytes = objectLoader.getBytes();
      return new String(bytes, StandardCharsets.UTF_8);
    }

  }

}