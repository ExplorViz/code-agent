package net.explorviz.code.analysis.service;

import java.util.Optional;

/**
 * Configuration object for Git analysis operations.
 */
public record AnalysisConfig(Optional<String> repoPath, Optional<String> repoRemoteUrl, Optional<String> gitUsername,
                             Optional<String> gitPassword, Optional<String> branch, Optional<String> sourceDirectory,
                             Optional<String> restrictAnalysisToFolders, boolean calculateMetrics,
                             Optional<String> startCommit, Optional<String> endCommit, Optional<Integer> cloneDepth,
                             String landscapeToken, String applicationName) {

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
    private Optional<Integer> cloneDepth = Optional.empty();
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

    public Builder cloneDepth(final Optional<Integer> cloneDepth) {
      this.cloneDepth = cloneDepth;
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
          cloneDepth,
          landscapeToken,
          applicationName);
    }
  }
}
