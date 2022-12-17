package net.explorviz.code.analysis.git;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
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
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injectable helper class for jGit concerns.
 */
@ApplicationScoped
public class GitRepositoryLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryLoader.class);

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
    }

    try {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Cloning repository from " + checkedRepositoryUrl.getValue());
      }
      return Git.cloneRepository().setURI(checkedRepositoryUrl.getValue()).setCredentialsProvider(
              remoteRepositoryObject.getCredentialsProvider()).setDirectory(new File(repoPath))
          .call()
          .getRepository();
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
  private Repository openGitRepository(final String repositoryPath) throws IOException {

    final File localRepositoryDirectory = new File(repositoryPath);

    if (!localRepositoryDirectory.isDirectory()) {
      LOGGER.error("Given path is not a directory.");
      throw new NotDirectoryException(repositoryPath);
    }

    if (Objects.requireNonNull(localRepositoryDirectory.listFiles()).length == 0) {
      return null;
    }
    return Git.open(localRepositoryDirectory).getRepository();
  }

  /**
   * Returns a Git {@link Repository} object by opening the repository found at
   * {@code localRepositoryPath}. If {@code localRepositoryPath} is empty, the repository gets
   * cloned from {@code remoteRepositoryUrl} to {@code remoteRepositoryPath} and the opened
   * repository gets returned.
   *
   * @param localRepositoryPath the system path of the local Repository
   * @param remoteRepositoryObject the {@link RemoteRepositoryObject} object containing the path
   *     and url
   * @return returns an opened Git {@link Repository}
   * @throws IOException     gets thrown if the path is not accessible or does not point to a
   *                         folder
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  public Repository getGitRepository(final String localRepositoryPath, // NOPMD
                                     final RemoteRepositoryObject remoteRepositoryObject)
      throws IOException, GitAPIException {

    if (localRepositoryPath.isBlank() && LOGGER.isInfoEnabled()) {
      LOGGER.info("No local repository given, using remote");

      return this.downloadGitRepository(remoteRepositoryObject);
    }

    if (!localRepositoryPath.isBlank()) {
      return this.openGitRepository(localRepositoryPath);
    }

    return null;
  }

  /**
   * Returns a Git {@link Repository} object by using the parameters set in the
   * application.properties.
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
  public static String getContent(final ObjectId blobId, final Repository repo) throws IOException {
    try (ObjectReader objectReader = repo.newObjectReader()) {
      final ObjectLoader objectLoader = objectReader.open(blobId);
      final byte[] bytes = objectLoader.getBytes();
      return new String(bytes, StandardCharsets.UTF_8);
    }

  }
}