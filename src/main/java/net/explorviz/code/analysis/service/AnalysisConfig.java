package net.explorviz.code.analysis.service;

import java.util.Optional;

/**
 * Configuration object for Git analysis operations.
 */
public class AnalysisConfig {

  private final Optional<String> repoPath;
  private final Optional<String> repoRemoteUrl;
  private final Optional<String> gitUsername;
  private final Optional<String> gitPassword;
  private final Optional<String> branch;
  private final Optional<String> sourceDirectory;
  private final Optional<String> restrictAnalysisToFolders;
  private final boolean calculateMetrics;
  private final Optional<String> startCommit;
  private final Optional<String> endCommit;
  private final String landscapeToken;
  private final String applicationName;


  public AnalysisConfig(
      final Optional<String> repoPath,
      final Optional<String> repoRemoteUrl,
      final Optional<String> gitUsername,
      final Optional<String> gitPassword,
      final Optional<String> branch,
      final Optional<String> sourceDirectory,
      final Optional<String> restrictAnalysisToFolders,
      final boolean calculateMetrics,
      final Optional<String> startCommit,
      final Optional<String> endCommit,
      final String landscapeToken,
      final String applicationName) {
    this.repoPath = repoPath;
    this.repoRemoteUrl = repoRemoteUrl;
    this.gitUsername = gitUsername;
    this.gitPassword = gitPassword;
    this.branch = branch;
    this.sourceDirectory = sourceDirectory;
    this.restrictAnalysisToFolders = restrictAnalysisToFolders;
    this.calculateMetrics = calculateMetrics;
    this.startCommit = startCommit;
    this.endCommit = endCommit;
    this.landscapeToken = landscapeToken;
    this.applicationName = applicationName;
  }

  public Optional<String> getRepoPath() {
    return repoPath;
  }

  public Optional<String> getRepoRemoteUrl() {
    return repoRemoteUrl;
  }

  public Optional<String> getGitUsername() {
    return gitUsername;
  }

  public Optional<String> getGitPassword() {
    return gitPassword;
  }

  public Optional<String> getBranch() {
    return branch;
  }

  public Optional<String> getSourceDirectory() {
    return sourceDirectory;
  }

  public Optional<String> getRestrictAnalysisToFolders() {
    return restrictAnalysisToFolders;
  }

  public boolean isCalculateMetrics() {
    return calculateMetrics;
  }

  public Optional<String> getStartCommit() {
    return startCommit;
  }

  public Optional<String> getEndCommit() {
    return endCommit;
  }

  public String getLandscapeToken() {
    return landscapeToken;
  }

  public String getApplicationName() {
    return applicationName;
  }

  /**
   * Builder for AnalysisConfig.
   */
  public static class Builder {
    private Optional<String> repoPath = Optional.empty();
    private Optional<String> repoRemoteUrl = Optional.empty();
    private Optional<String> gitUsername = Optional.empty();
    private Optional<String> gitPassword = Optional.empty();
    private Optional<String> branch = Optional.empty();
    private Optional<String> sourceDirectory = Optional.empty();
    private Optional<String> restrictAnalysisToFolders = Optional.empty();
    private boolean calculateMetrics = true;
    private Optional<String> startCommit = Optional.empty();
    private Optional<String> endCommit = Optional.empty();
    private String landscapeToken = "";
    private String applicationName = "";

    public Builder repoPath(final Optional<String> repoPath) {
      this.repoPath = repoPath;
      return this;
    }

    public Builder repoRemoteUrl(final Optional<String> repoRemoteUrl) {
      this.repoRemoteUrl = repoRemoteUrl;
      return this;
    }
    
    public Builder gitUsername(final Optional<String> gitUsername) {
      this.gitUsername = gitUsername;
      return this;
    }

    public Builder gitPassword(final Optional<String> gitPassword) {
      this.gitPassword = gitPassword;
      return this;
    }

    public Builder branch(final Optional<String> branch) {
      this.branch = branch;
      return this;
    }

    public Builder sourceDirectory(final Optional<String> sourceDirectory) {
      this.sourceDirectory = sourceDirectory;
      return this;
    }

    public Builder restrictAnalysisToFolders(final Optional<String> restrictAnalysisToFolders) {
      this.restrictAnalysisToFolders = restrictAnalysisToFolders;
      return this;
    }

    public Builder calculateMetrics(final boolean calculateMetrics) {
      this.calculateMetrics = calculateMetrics;
      return this;
    }

    public Builder startCommit(final Optional<String> startCommit) {
      this.startCommit = startCommit;
      return this;
    }

    public Builder endCommit(final Optional<String> endCommit) {
      this.endCommit = endCommit;
      return this;
    }

    public Builder landscapeToken(final String landscapeToken) {
      this.landscapeToken = landscapeToken;
      return this;
    }

    public Builder applicationName(final String applicationName) {
      this.applicationName = applicationName;
      return this;
    }

    public AnalysisConfig build() {
      return new AnalysisConfig(
          repoPath,
          repoRemoteUrl,
          gitUsername,
          gitPassword,
          branch,
          sourceDirectory,
          restrictAnalysisToFolders,
          calculateMetrics,
          startCommit,
          endCommit,
          landscapeToken,
          applicationName
      );
    }
  }
}

