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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
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

  @ConfigProperty(name = "explorviz.gitanalysis.remote.username")
  /* default */ Optional<String> usernameProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.password")
  /* default */ Optional<String> passwordProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.fullanalysis", defaultValue = "true")
  /* default */ boolean fullAnalysis; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.branch", defaultValue = "master")
  /* default */ String repositoryBranch;  // NOCS

  /**
   * Tries to download the Git {@link Repository} based on a given Url to the given.
   *
   * @param repositoryPath the system path of the local Repository
   * @param repositoryUrl the remote repository Url
   * @param credentialsProvider the credential provider to access the repository
   * @return returns an opened git repository
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  public Repository downloadGitRepository(final String repositoryPath, final String repositoryUrl,
                                          final CredentialsProvider credentialsProvider)
      throws GitAPIException, MalformedURLException {
    final Map.Entry<Boolean, String> checkedRepositoryUrl = convertSshToHttps(repositoryUrl);

    try {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Cloning repository from " + checkedRepositoryUrl.getValue());
      }
      return Git.cloneRepository().setURI(checkedRepositoryUrl.getValue()).setCredentialsProvider(
          credentialsProvider).setDirectory(new File(repositoryPath)).call().getRepository();
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
  public Repository openGitRepository(final String repositoryPath) throws IOException {

    final File localRepositoryDirectory = new File(repositoryPath);

    if (!localRepositoryDirectory.isDirectory()) {
      LOGGER.error("Given path is not a directory");
      throw new NotDirectoryException(repositoryPath);
    }

    if (Objects.requireNonNull(localRepositoryDirectory.listFiles()).length == 0) {
      return null;
    }
    return Git.open(localRepositoryDirectory).getRepository();
  }

  /**
   * Returns a Git {@link Repository} as an {@link InMemoryRepository}.
   *
   * @param remoteUrl the remote Url of the repository
   * @param credentialsProvider the credential provider to access the repository
   * @return returns an opened git repository
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  public Repository getInMemoryRepository(final String remoteUrl,
                                          final CredentialsProvider credentialsProvider)
      throws GitAPIException {
    final DfsRepositoryDescription repositoryDescription = new DfsRepositoryDescription();
    final Git git = new Git(new InMemoryRepository(repositoryDescription)); // NOPMD
    git.fetch().setRemote(remoteUrl).setCredentialsProvider(credentialsProvider).call();
    return git.getRepository();
  }

  /**
   * Returns a Git {@link Repository} object by opening the repository found at
   * {@code repositoryPath}. If {@code repositoryUrl} is the same (or empty) as the local
   * repository's remote Url, the repository will be updated. If {@code repositoryUrl} is specified
   * and differs from the local repository's remote Url, the local repository gets deleted and the
   * remote repository will be cloned to the given {@code repositoryPath}. If {@code repositoryPath}
   * is empty, {@code repositoryUrl} must be present and the {@link Repository} is created as a pure
   * {@link InMemoryRepository}.
   *
   * @param repositoryPath the system path of the local Repository
   * @param repositoryUrl the remote repository Url
   * @param username username to clone private repositories
   * @param password password to clone private repositories
   * @return returns an opened Git {@link Repository}
   * @throws IOException     gets thrown if the path is not accessible or does not point to a
   *                         folder
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  public Repository getGitRepository(final String repositoryPath, // NOPMD
                                     final String repositoryUrl,
                                     final String username, final String password)
      throws IOException, GitAPIException {

    CredentialsProvider credentialsProvider;
    if (username.isBlank() || password.isBlank()) {
      credentialsProvider = CredentialsProvider.getDefault();
    } else {
      credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
    }

    if (repositoryPath.isBlank()) {
      return getInMemoryRepository(repositoryUrl, credentialsProvider);
    }

    Repository localRepository = this.openGitRepository(repositoryPath);
    if (localRepository == null) {
      localRepository = this.downloadGitRepository(repositoryPath, repositoryUrl, // NOPMD
          credentialsProvider);
    } else if (Objects.equals(getRemoteOriginUrl(localRepository), repositoryUrl)) {
      // TODO: repoUrl need to begin with http
      // seems like, at least with gitlab, jgit does not like the ssh style git@... url
      // produces -remote hung up unexpectedly- even with cloned repo, pulls are not doable
      // with ssh, maybe check here if it is http and try, or throw exception if the url
      // is http style.

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Pulling latests commits...");
      }
      try (Git git = new Git(localRepository)) {
        final PullCommand pullCommand = git.pull();
        pullCommand.call();
      }
    } else {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Local repository does not match with remote, local will be overwritten.");
      }
      localRepository.close();
      Files.delete(new File(repositoryPath).toPath());
      localRepository = this.downloadGitRepository(repositoryPath, repositoryUrl,
          credentialsProvider);
    }
    return localRepository;
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
    if (repoUrlProperty.isEmpty()) {
      throw new PropertyNotDefinedException("explorviz.gitanalysis.remote.url");
    }

    if (repoPathProperty.isEmpty()) {
      throw new PropertyNotDefinedException("explorviz.gitanalysis.local.folder.path");
    }

    return getGitRepository(this.repoPathProperty.get(), this.repoUrlProperty.get(),
        this.usernameProperty.orElse(""), this.passwordProperty.orElse(""));
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
  public String getContent(final ObjectId blobId, final Repository repo) throws IOException {
    try (ObjectReader objectReader = repo.newObjectReader()) {
      final ObjectLoader objectLoader = objectReader.open(blobId);
      final byte[] bytes = objectLoader.getBytes();
      return new String(bytes, StandardCharsets.UTF_8);
    }

  }


}
