package net.explorviz.code.analysis.git; // NOPMD


import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.explorviz.code.analysis.FileIO;
import net.explorviz.code.analysis.exceptions.NotFoundException;
import net.explorviz.code.analysis.exceptions.PropertyNotDefinedException;
import net.explorviz.code.analysis.types.FileDescriptor;
import net.explorviz.code.analysis.types.RemoteRepositoryObject;
import net.explorviz.code.analysis.types.Triple;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injectable helper class for jGit concerns.
 */
@ApplicationScoped
public class GitRepositoryHandler { // NOPMD

  private static final Logger LOGGER = LoggerFactory.getLogger(GitRepositoryHandler.class);
  private static final String JAVA_PATH_SUFFIX = ".java";
  private static String repositoryPath;

  @ConfigProperty(name = "explorviz.gitanalysis.local.storage-path")
  /* default */ Optional<String> repoPathProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.url")
  /* default */ Optional<String> repoUrlProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.storage-path")
  /* default */ Optional<String> repoLocalStoragePathProperty; // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.username")
  /* default */ Optional<String> usernameProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.remote.password")
  /* default */ Optional<String> passwordProperty;  // NOCS

  @ConfigProperty(name = "explorviz.gitanalysis.branch")
  /* default */ Optional<String> repositoryBranchProperty;  // NOCS

  private Git git;

  public static String getCurrentRepositoryPath() {
    return repositoryPath;
  }

  private static AbstractTreeIterator prepareTreeParser(final Repository repository,
      final RevTree tree) throws IOException {
    final CanonicalTreeParser treeParser = new CanonicalTreeParser();
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
    if (url.matches("^git@\\S+.\\S+:\\w+(/[\\S&&[^/]]+)+[.git]?$")) {
      final String convertedUrl = url.replace(":", "/").replace("git@", "https://");
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("The URL seems to be a SSH url, currently"
            + " only HTTPS is supported, converted url now is: " + convertedUrl);
      }
      return Map.entry(true, convertedUrl);
    } else if (url.matches("^https?://\\S+(/[\\S&&[^/]]+)+[.git]?")) {
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
   * @param repo   The {@link Repository}.
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

  /**
   * Tries to download the Git {@link Repository} based on a given Url to the given.
   *
   * @param remoteRepositoryObject the {@link RemoteRepositoryObject} object containing the path and
   *                               url
   * @return returns an opened git repository
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  private Repository downloadGitRepository( // NOCS NOPMD
      final RemoteRepositoryObject remoteRepositoryObject) throws GitAPIException, IOException {

    final Map.Entry<Boolean, String> checkedRepositoryUrl = convertSshToHttps(
        remoteRepositoryObject.getUrl());

    String repoPath = remoteRepositoryObject.getStoragePath();
    if (remoteRepositoryObject.getStoragePath().isBlank()) {
      repoPath = Files.createTempDirectory("TemporaryRepository").toAbsolutePath().toString();
      LOGGER.atInfo().addArgument(repoPath)
          .log("No path given, repository will be cloned to: {}");
    } else if (new File(repoPath).isAbsolute()) {
      LOGGER.atInfo().addArgument(repoPath)
          .log("Repository will be cloned to: {}");
    } else {
      LOGGER.atInfo().log("Found local path for remote repository.");
      repoPath = GitRepositoryHandler.convertRelativeToAbsolutePath(repoPath);
    }

    try {
      LOGGER.atInfo().addArgument(checkedRepositoryUrl.getValue())
          .log("Cloning repository from: {}");

      FileIO.cleanDirectory(repoPath);
      this.git = Git.cloneRepository().setURI(checkedRepositoryUrl.getValue())
          .setCredentialsProvider(remoteRepositoryObject.getCredentialsProvider())
          .setDirectory(new File(repoPath)).setBranch(remoteRepositoryObject.getBranchNameOrNull())
          .call();
      repositoryPath = new File(repoPath).getAbsolutePath();
      return this.git.getRepository();
    } catch (TransportException te) {
      if (!checkedRepositoryUrl.getKey()) {
        throw (MalformedURLException) new MalformedURLException(
            checkedRepositoryUrl.getValue()).initCause(te);
      }

      if (LOGGER.isErrorEnabled()) {
        if (te.getMessage().contains("not found in upstream")) {
          LOGGER.error("Transport Exception thrown, branch not found");
        } else if (te.getMessage().contains("no CredentialsProvider")) {
          LOGGER.error("Transport Exception thrown, repository is private, no credentials given");
        } else if (te.getMessage().contains("not authorized")) {
          LOGGER.error("Transport Exception thrown, credential are wrong");
        } else {
          LOGGER.error("Transport Exception thrown: " + te.getMessage()); // NOPMD
        }
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
      try {
        if ("true".equals(System.getenv("GITLAB_CI"))) {
          this.git.checkout().setName(branchName).setCreateBranch(true)
              .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
              .setStartPoint("origin/" + branchName).call();
        } else {
          this.git.checkout().setName(branchName).call();
        }
      } catch (RefNotFoundException e) {
        if (LOGGER.isErrorEnabled()) {
          LOGGER.error("The given branch name <{}> was not found", branchName);
        }
        throw e;
      }
    }
    GitRepositoryHandler.repositoryPath = new File(repositoryPath).getAbsolutePath();
    return this.git.getRepository();
  }

  /**
   * Returns a Git {@link Repository} object by opening the repository found at
   * {@code localRepositoryPath}. <br> If {@code localRepositoryPath} is empty, the repository gets
   * cloned based on data defined in {@code remoteRepositoryObject} and the opened repository gets
   * returned.
   *
   * @param localRepositoryPath    the system path of the local Repository
   * @param remoteRepositoryObject the {@link RemoteRepositoryObject} object containing the path and
   *                               url
   * @return returns an opened Git {@link Repository}
   * @throws IOException     gets thrown if the path is not accessible or does not point to a
   *                         folder
   * @throws GitAPIException gets thrown if the git api encounters an error
   */
  public Repository getGitRepository(final String localRepositoryPath,
      final RemoteRepositoryObject remoteRepositoryObject) throws IOException, GitAPIException {

    if (localRepositoryPath.isBlank()) {
      LOGGER.atInfo().log("No local repository given, using remote");
      return this.downloadGitRepository(remoteRepositoryObject);
    } else if (new File(localRepositoryPath).isAbsolute()) {
      return this.openGitRepository(localRepositoryPath, remoteRepositoryObject.getBranchName());
    } else {
      String absolutePath = GitRepositoryHandler.convertRelativeToAbsolutePath(localRepositoryPath);
      return this.openGitRepository(absolutePath, remoteRepositoryObject.getBranchName());
    }
  }

  /**
   * Returns a Git {@link Repository} object by using the parameters set in the
   * application.properties.<br> The local repository defined in
   * {@code  explorviz.gitanalysis.local.storage-path} will be used.
   * <br>
   * If {@code  explorviz.gitanalysis.local.storage-path} is empty, the repository defined in
   * {@code  explorviz.gitanalysis.remote.url} will be cloned to the location
   * {@code explorviz.gitanalysis.remote.storage-path}.<br> If no storage path is given, a temporary
   * directory will be created. <br> The branch given in {@code explorviz.gitanalysis.branch} will
   * be used if present, otherwise the default (remote) or current (local) will be used.
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

    final CredentialsProvider credentialsProvider;
    if (usernameProperty.isEmpty() || passwordProperty.isEmpty()) {
      credentialsProvider = CredentialsProvider.getDefault();
    } else {
      credentialsProvider = new UsernamePasswordCredentialsProvider(usernameProperty.get(),
          passwordProperty.get());
    }

    return getGitRepository(this.repoPathProperty.orElse(""),
        new RemoteRepositoryObject(this.repoUrlProperty.orElse(""),
            repoLocalStoragePathProperty.orElse(""), credentialsProvider,
            repositoryBranchProperty.orElse("")));
  }

  /**
   * Returns the changed filenames between two given commits.
   *
   * @param repository       the current repository
   * @param oldCommit        the old commit, as a baseline for the difference calculation
   * @param newCommit        the new commit, gets checked against the old commit
   * @param pathRestrictions comma sep. list of search strings specifying the folders to analyze
   * @return triple of FileDescriptor specifying modified, delete and added files
   * @throws GitAPIException   thrown if git encounters an exception
   * @throws IOException       thrown if files are not available
   * @throws NotFoundException thrown if the restrictionPath was not found
   */
  public Triple<List<FileDescriptor>, List<FileDescriptor>, List<FileDescriptor>> listDiff(
      final Repository repository, final Optional<RevCommit> oldCommit, final RevCommit newCommit,
      final String pathRestrictions) throws GitAPIException, IOException, NotFoundException {
    if (pathRestrictions == null || pathRestrictions.isEmpty()) {
      return listDiff(repository, oldCommit, newCommit, new ArrayList<>());
    }
    return listDiff(repository, oldCommit, newCommit, Arrays.asList(pathRestrictions.split(",")));
  }

  /**
   * Returns the changed filenames between two given commits.
   *
   * @param repository the current repository
   * @param oldCommit  the old commit, as a baseline for the difference calculation
   * @param newCommit  the new commit, gets checked against the old commit
   * @return triple of FileDescriptor specifying modified, delete and added files
   * @throws GitAPIException thrown if git encounters an exception
   * @throws IOException     thrown if files are not available
   */
  public Triple<List<FileDescriptor>, List<FileDescriptor>, List<FileDescriptor>> listDiff(
      final Repository repository, // NOPMD
      final Optional<RevCommit> oldCommit, final RevCommit newCommit,
      final List<String> pathRestrictions) throws GitAPIException, IOException, NotFoundException {
    final List<FileDescriptor> modifiedObjectIdList = new ArrayList<>();
    final List<FileDescriptor> deletedObjectIdList = new ArrayList<>();
    List<FileDescriptor> addedObjectIdList = new ArrayList<>();

    final TreeFilter filter = getJavaFileTreeFilter(pathRestrictions);

    if (oldCommit.isEmpty()) {
      addedObjectIdList = listFilesInCommit(repository, newCommit, filter);
    } else {
      final List<DiffEntry> diffs = this.git.diff()
          .setOldTree(prepareTreeParser(repository, oldCommit.get().getTree()))
          .setNewTree(prepareTreeParser(repository, newCommit.getTree())).setPathFilter(filter)
          .call();

      for (final DiffEntry diff : diffs) {
        if (diff.getChangeType().equals(DiffEntry.ChangeType.DELETE)) {
          //LOGGER.info("File Deleted");
          putInList2(repository, diff, deletedObjectIdList);
          continue;
        } else if (diff.getChangeType().equals(DiffEntry.ChangeType.RENAME)) {
          //LOGGER.info("File Renamed");
          putInList(repository, diff, addedObjectIdList);
        } else if (diff.getChangeType().equals(DiffEntry.ChangeType.COPY)) {
          //LOGGER.info("File Copied");
          putInList(repository, diff, addedObjectIdList);
        } else if (diff.getChangeType().equals(DiffEntry.ChangeType.MODIFY)) {
          //LOGGER.info("File Modified");
          putInList(repository, diff, modifiedObjectIdList);
        } else if (diff.getChangeType().equals(DiffEntry.ChangeType.ADD)) {
          //LOGGER.info("File Added");
          putInList(repository, diff, addedObjectIdList);
        }
      }
    }
    return new Triple<List<FileDescriptor>, List<FileDescriptor>, List<FileDescriptor>>(
        modifiedObjectIdList, deletedObjectIdList, addedObjectIdList);
  }

  private void putInList(final Repository repository, final DiffEntry diff,
      final List<FileDescriptor> objectIdList)
      throws CorruptObjectException, MissingObjectException, IOException {
    Triple<Integer, Integer, Integer> mods;
    try (DiffFormatter diffFormatter = new DiffFormatter(// NOPMD
        DisabledOutputStream.INSTANCE)) {
      diffFormatter.setRepository(repository);
      final FileHeader fileHeader = diffFormatter.toFileHeader(diff);
      mods = countModifications(fileHeader.toEditList()); //TODO: don't need to do that when deleted
    }
    final String[] parts = diff.getNewPath().split("/");
    objectIdList.add(
        new FileDescriptor(diff.getNewId().toObjectId(), parts[parts.length - 1], diff.getNewPath(),
            mods));
  }

  private void putInList2(final Repository repository, final DiffEntry diff,
      final List<FileDescriptor> objectIdList)
      throws CorruptObjectException, MissingObjectException, IOException {
    Triple<Integer, Integer, Integer> mods;
    try (DiffFormatter diffFormatter = new DiffFormatter(// NOPMD
        DisabledOutputStream.INSTANCE)) {
      diffFormatter.setRepository(repository);
      final FileHeader fileHeader = diffFormatter.toFileHeader(diff);
      mods = countModifications(fileHeader.toEditList()); //TODO: don't need to do that when deleted
    }
    final String[] parts = diff.getOldPath().split("/");
    objectIdList.add(
        new FileDescriptor(diff.getOldId().toObjectId(), parts[parts.length - 1], diff.getOldPath(),
            mods));
  }

  private Triple<Integer, Integer, Integer> countModifications(final EditList editList) {
    int modifiedLines = 0;
    int addedLines = 0;
    int deletedLines = 0;
    for (final Edit edit : editList) {
      if (edit.getBeginA() == edit.getEndA() && edit.getBeginB() < edit.getEndB()) {
        // insert edit
        addedLines += edit.getLengthB();
      } else if (edit.getBeginA() < edit.getEndA() && edit.getBeginB() == edit.getEndB()) {
        // delete edit
        deletedLines += edit.getLengthA();
      } else if (edit.getBeginA() < edit.getEndA() && edit.getBeginB() < edit.getEndB()) {
        modifiedLines += edit.getLengthB();
      }
    }
    return new Triple<>(modifiedLines, addedLines, deletedLines);
  }

  /**
   * Returns a list of all Java Files in the repository.
   *
   * @param repository       the current repository
   * @param commit           the commit to get the list of files for
   * @param pathRestrictions a list of search strings specifying the folders to analyze, if omitted,
   *                         the entire repository will be searched
   * @return returns a list of FileDescriptors of all java files within the specified folders
   * @throws IOException       thrown if files are not available
   * @throws NotFoundException thrown if the restrictionPath was not found
   */
  public List<FileDescriptor> listFilesInCommit(final Repository repository, // NOPMD
      final RevCommit commit, final List<String> pathRestrictions)
      throws IOException, NotFoundException {
    return listFilesInCommit(repository, commit, getJavaFileTreeFilter(pathRestrictions));
  }

  /**
   * Returns a list of all Java Files in the repository.
   *
   * @param repository       the current repository
   * @param commit           the commit to get the list of files for
   * @param pathRestrictions a comma separated list of search strings specifying the folders to
   *                         analyze, if omitted, the entire repository will be searched
   * @return returns a list of FileDescriptors of all java files within the specified folders
   * @throws IOException       thrown if files are not available
   * @throws NotFoundException thrown if the restrictionPath was not found
   */
  public List<FileDescriptor> listFilesInCommit(final Repository repository, // NOPMD
      final RevCommit commit, final String pathRestrictions) throws IOException, NotFoundException {
    return listFilesInCommit(repository, commit,
        getJavaFileTreeFilter(Arrays.asList(pathRestrictions.split(","))));
  }

  private List<FileDescriptor> listFilesInCommit(final Repository repository, // NOPMD
      final RevCommit commit, final TreeFilter filter) throws IOException {
    final List<FileDescriptor> objectIdList = new ArrayList<>();
    try (final TreeWalk treeWalk = new TreeWalk(repository)) { // NOPMD
      treeWalk.addTree(commit.getTree());
      treeWalk.setRecursive(true);
      treeWalk.setFilter(filter);
      while (treeWalk.next()) {
        objectIdList.add(new FileDescriptor(treeWalk.getObjectId(0), treeWalk.getNameString(),
            treeWalk.getPathString()));
      }
    }
    return objectIdList;

  }

  private TreeFilter getJavaFileTreeFilter(final List<String> pathRestrictions)
      throws NotFoundException {
    if (pathRestrictions.isEmpty() || pathRestrictions.size() == 1 && pathRestrictions.get(0)
        .isBlank()) {
      return PathSuffixFilter.create(JAVA_PATH_SUFFIX);
    } else {
      final List<String> pathList = DirectoryFinder.getRelativeDirectory(pathRestrictions,
          getCurrentRepositoryPath());
      final List<String> newPathList = new ArrayList<>();
      for (final String path : pathList) {
        newPathList.add(path.replaceFirst("^\\\\|/", "").replaceAll("\\\\", "/"));
      }
      final TreeFilter pathFilter = PathFilterGroup.createFromStrings(newPathList);
      final PathSuffixFilter suffixFilter = PathSuffixFilter.create(JAVA_PATH_SUFFIX);
      return AndTreeFilter.create(pathFilter, suffixFilter);
    }
  }

  public boolean isUnreachableCommit(final Optional<String> commitId, final String branch) {

    return commitId.isPresent() && this.isUnreachableCommit(commitId.get(), branch);
  }

  /**
   * Checks if the given commit is unreachable by the given branch (is not part of the branch).
   *
   * @param commitId the full SHA-1 id of the commit
   * @param branch   the branch name
   * @return if the given commit is unreachable by the given branch
   */
  public boolean isUnreachableCommit(final String commitId, final String branch) {
    return !this.isReachableCommit(commitId, branch);
  }

  /**
   * Checks if the given commit is reachable by the given branch (is part of the branch).
   *
   * @param commitId the full SHA-1 id of the commit
   * @param branch   the branch name
   * @return if the commit is reachable by the given branch
   */
  public boolean isReachableCommit(final String commitId, final String branch) {
    if (commitId == null || commitId.isEmpty()) {
      return true;
    }
    try {
      final Map<ObjectId, String> map = this.git.nameRev().addPrefix(branch)
          .add(ObjectId.fromString(commitId)).call();
      if (!map.isEmpty()) {
        return true;
      }
    } catch (GitAPIException | MissingObjectException e) {
      throw new RuntimeException(e);  // NOPMD
    }
    return false;
  }

  public static String convertRelativeToAbsolutePath(String relativePath) {
    String systemPath = System.getProperty("user.dir");
    systemPath = systemPath.replace("\\build\\classes\\java\\main", "");
    systemPath = systemPath.replace("/build/classes/java/main", "");
    String absolutePath = Paths.get(systemPath, relativePath).toString();
    LOGGER.atInfo().addArgument(relativePath).addArgument(absolutePath)
        .log("Converted relative path {} to absolute path {}");
    return absolutePath;
  }

}
