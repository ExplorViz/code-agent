package net.explorviz.code.analysis.git;

import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
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

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.util.Objects;
import java.util.Optional;

/**
 * Injectable helper class for jGit concerns.
 */
@ApplicationScoped
public class GitRepositoryLoader {

    @ConfigProperty(name = "explorviz.gitanalysis.local.folder.path")
    String repoPathProperty;

    @ConfigProperty(name = "explorviz.gitanalysis.remote.url")
    Optional<String> repoURLProperty;

    @ConfigProperty(name = "explorviz.gitanalysis.remote.username")
    Optional<String> usernameProperty;

    @ConfigProperty(name = "explorviz.gitanalysis.remote.password")
    Optional<String> passwordProperty;

    @ConfigProperty(name = "explorviz.gitanalysis.fullanalysis", defaultValue = "true")
    boolean fullAnalysis;

    @ConfigProperty(name = "explorviz.gitanalysis.branch", defaultValue = "master")
    String repositoryBranch;

    private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryLoader.class);

    /**
     * Tries to download the Git {@link Repository} based on a given url to the given.
     *
     * @param repositoryPath      the system path of the local Repository
     * @param repositoryURL the remote repository URL
     * @param credentialsProvider the credential provider to access the repository
     * @return returns an opened git repository
     * @throws GitAPIException gets thrown if the git api encounters an error
     */
    public Repository downloadGitRepository(String repositoryPath, String repositoryURL, CredentialsProvider credentialsProvider) throws GitAPIException {

        Git git;
        try {
            LOGGER.info("Cloning repository from " + repositoryURL);
            git = Git.cloneRepository().setURI(repositoryURL).setCredentialsProvider(credentialsProvider).setDirectory(new File(repositoryPath)).call();
        } catch (TransportException te) {
            LOGGER.error("The repository is private, username and password are required.");
            throw te;
        } catch (InvalidRemoteException e) {
            LOGGER.error("The repository's URL seems not right, no git repository was found there.");
            throw e;
        }
        return git.getRepository();
    }

    /**
     * Tries to open the Git {@link Repository} based on a given folderPath.
     *
     * @param repositoryPath the system path of the local Repository
     * @return returns an opened git {@link Repository}
     * @throws IOException gets thrown if JGit cannot open the Git repository.
     */
    public Repository openGitRepository(String repositoryPath) throws IOException {

        final File localRepositoryDirectory = new File(repositoryPath);

        if (!localRepositoryDirectory.isDirectory()) {
            LOGGER.error("Given path is not a directory");
            throw new NotDirectoryException(repositoryPath);
        } else if (Objects.requireNonNull(localRepositoryDirectory.listFiles()).length == 0) {
            return null;
        }
        return Git.open(localRepositoryDirectory).getRepository();
    }

    /** Returns a Git {@link Repository} as an {@link InMemoryRepository}
     * @param remoteURL the remote url of the repository
     * @param credentialsProvider the credential provider to access the repository
     * @return returns an opened git repository
     * @throws GitAPIException gets thrown if the git api encounters an error
     */
    public Repository getInMemoryRepository(String remoteURL, CredentialsProvider credentialsProvider) throws GitAPIException {
        DfsRepositoryDescription repositoryDescription = new DfsRepositoryDescription();
        InMemoryRepository repository = new InMemoryRepository(repositoryDescription);
        Git git = new Git(repository);
        git.fetch().setRemote(remoteURL).setCredentialsProvider(credentialsProvider).call();
        return git.getRepository();
    }

    /**
     * Returns a Git {@link Repository} object by opening the repository found at {@code repositoryPath}.
     * If {@code repositoryURL} is the same (or empty) as the local repository's remote url, the repository will be updated.
     * If {@code repositoryURL} is specified and differs from the local repository's remote url, the local repository gets
     * deleted and the remote repository will be cloned to the given {@code repositoryPath}.
     * If {@code repositoryPath} is empty, {@code repositoryURL} must be present and the {@link Repository} is created as a pure
     * {@link InMemoryRepository}.
     *
     * @param repositoryPath the system path of the local Repository
     * @param repositoryURL the remote repository URL
     * @param username username to clone private repositories
     * @param password password to clone private repositories
     * @return returns an opened Git {@link Repository}
     * @throws IOException gets thrown if the path is not accessible or does not point to a folder
     * @throws GitAPIException gets thrown if the git api encounters an error
     */
    public Repository getGitRepository(String repositoryPath, String repositoryURL, String username, String password) throws IOException, GitAPIException {

        CredentialsProvider credentialsProvider;
        if (!username.isBlank() && !password.isBlank()) {
            credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        } else {
            credentialsProvider = CredentialsProvider.getDefault();
        }

        if (repositoryPath.isBlank()) {
            return getInMemoryRepository(repositoryURL, credentialsProvider);
        }

        Repository localRepository = this.openGitRepository(repositoryPath);
        if (localRepository == null) {
            localRepository = this.downloadGitRepository(repositoryPath, repositoryURL, credentialsProvider);
        } else if(Objects.equals(getRemoteOriginURL(localRepository), repositoryURL)) {
            new Git(localRepository).pull().call();
        } else {
            localRepository.close();
            Files.delete(new File(repositoryPath).toPath());
            localRepository = this.downloadGitRepository(repositoryPath, repositoryURL, credentialsProvider);
        }
        return localRepository;
    }

    /**
     * Returns a Git {@link Repository} object by using the parameters set in the application.properties
     * @return an opened Git {@link Repository}
     * @throws PropertyNotDefinedException gets thrown if a needed property is not present
     * @throws GitAPIException gets thrown if the git api encounters an error
     * @throws IOException gets thrown if JGit cannot open the Git repository.
     */
    public Repository getGitRepository() throws PropertyNotDefinedException, GitAPIException, IOException {
        if (repoURLProperty.isEmpty()) {
            throw new PropertyNotDefinedException("explorviz.repo.remote.url");
        }

        return getGitRepository(this.repoPathProperty, this.repoURLProperty.get(), this.usernameProperty.orElse(""), this.passwordProperty.orElse(""));
    }

    /**Returns the remote origin url from the given repository
     * @param repository the repository object
     * @return the remote origin url
     */
    public static String getRemoteOriginURL(Repository repository) {
        return repository.getConfig().getString("remote", "origin", "url");
    }

    /**
     * Returns the string content for a file path that was modified in a commit for a given repo.
     *
     * @param blobId The {@link ObjectId}.
     * @param repo   The {@link Repository}.
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
