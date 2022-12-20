package net.explorviz.code.analysis.types;

import org.eclipse.jgit.transport.CredentialsProvider;

/**
 * Storage Object to abstract the remote repository's url and storage path.
 */
public class RemoteRepositoryObject {
  private String url;
  private String storagePath;
  private CredentialsProvider credentialsProvider;

  /**
   * Create new RemoteRepositoryObject holding data for cloning remote repository.
   *
   * @param url the repository's url
   * @param storagePath where to clone the repository to
   * @param credentialsProvider the credential provider for private repositories
   */
  public RemoteRepositoryObject(final String url, final String storagePath,
                                final CredentialsProvider credentialsProvider) {
    this.url = url;
    this.storagePath = storagePath;
    this.credentialsProvider = credentialsProvider;
  }

  public RemoteRepositoryObject(final String url, final String storagePath) {
    this(url, storagePath, CredentialsProvider.getDefault());
  }

  public RemoteRepositoryObject() {
    this("", "");
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getStoragePath() {
    return storagePath;
  }

  public void setStoragePath(final String storagePath) {
    this.storagePath = storagePath;
  }

  public CredentialsProvider getCredentialsProvider() {
    return credentialsProvider;
  }

  public void setCredentialsProvider(final CredentialsProvider credentialsProvider) {
    this.credentialsProvider = credentialsProvider;
  }
}