package net.explorviz.code.analysis.api;

import java.util.Optional;
import net.explorviz.code.analysis.service.AnalysisConfig;

/**
 * Request object for triggering a Git analysis.
 */
public class AnalysisRequest {

  private String repoPath;
  private String repoRemoteUrl;
  private String remoteStoragePath;
  private String username;
  private String password;
  private String branch;
  private String sourceDirectory;
  private String restrictAnalysisToFolders;
  private boolean fetchRemoteData = true;
  private boolean sendToRemote = true;
  private boolean calculateMetrics = true;
  private String startCommit;
  private String endCommit;
  private boolean saveCrashedFiles;
  private String landscapeToken = "";
  private String applicationName = "";

  public AnalysisRequest() {
  }

  public String getRepoPath() {
    return repoPath;
  }

  public void setRepoPath(final String repoPath) {
    this.repoPath = repoPath;
  }

  public String getRepoRemoteUrl() {
    return repoRemoteUrl;
  }

  public void setRepoRemoteUrl(final String repoRemoteUrl) {
    this.repoRemoteUrl = repoRemoteUrl;
  }

  public String getSourceDirectory() {
    return sourceDirectory;
  }

  public void setSourceDirectory(final String sourceDirectory) {
    this.sourceDirectory = sourceDirectory;
  }

  public String getRestrictAnalysisToFolders() {
    return restrictAnalysisToFolders;
  }

  public void setRestrictAnalysisToFolders(final String restrictAnalysisToFolders) {
    this.restrictAnalysisToFolders = restrictAnalysisToFolders;
  }

  public boolean isFetchRemoteData() {
    return fetchRemoteData;
  }

  public void setFetchRemoteData(final boolean fetchRemoteData) {
    this.fetchRemoteData = fetchRemoteData;
  }

  public boolean isSendToRemote() {
    return sendToRemote;
  }

  public void setSendToRemote(final boolean sendToRemote) {
    this.sendToRemote = sendToRemote;
  }

  public boolean isCalculateMetrics() {
    return calculateMetrics;
  }

  public void setCalculateMetrics(final boolean calculateMetrics) {
    this.calculateMetrics = calculateMetrics;
  }

  public String getStartCommit() {
    return startCommit;
  }

  public void setStartCommit(final String startCommit) {
    this.startCommit = startCommit;
  }

  public String getEndCommit() {
    return endCommit;
  }

  public void setEndCommit(final String endCommit) {
    this.endCommit = endCommit;
  }

  public boolean isSaveCrashedFiles() {
    return saveCrashedFiles;
  }

  public void setSaveCrashedFiles(final boolean saveCrashedFiles) {
    this.saveCrashedFiles = saveCrashedFiles;
  }

  public String getLandscapeToken() {
    return landscapeToken;
  }

  public void setLandscapeToken(final String landscapeToken) {
    this.landscapeToken = landscapeToken;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   * Converts this request to an AnalysisConfig.
   *
   * @return The analysis configuration
   */
  public AnalysisConfig toConfig() {
    return new AnalysisConfig.Builder()
        .repoPath(Optional.ofNullable(repoPath))
        .repoRemoteUrl(Optional.ofNullable(repoRemoteUrl))
        .remoteStoragePath(Optional.ofNullable(remoteStoragePath))
        .gitUsername(Optional.ofNullable(username))
        .gitPassword(Optional.ofNullable(password))
        .branch(Optional.ofNullable(branch))
        .sourceDirectory(Optional.ofNullable(sourceDirectory))
        .restrictAnalysisToFolders(Optional.ofNullable(restrictAnalysisToFolders))
        .fetchRemoteData(fetchRemoteData)
        .sendToRemote(sendToRemote)
        .calculateMetrics(calculateMetrics)
        .startCommit(Optional.ofNullable(startCommit))
        .endCommit(Optional.ofNullable(endCommit))
        .saveCrashedFiles(saveCrashedFiles)
        .landscapeToken(landscapeToken != null ? landscapeToken : "")
        .applicationName(applicationName != null ? applicationName : "")
        .build();
  }
}

