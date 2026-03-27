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

  private boolean sendToRemote = true;
  private boolean calculateMetrics = true;
  private String startCommit;
  private String endCommit;
  private Integer cloneDepth;
  private String landscapeToken = "mytokenvalue";
  private String applicationName = "";
  private String applicationRoot;
  private String codeAnalysisExcludedFileExtensions;

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

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(final String branch) {
    this.branch = branch;
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

  public String getApplicationRoot() {
    return applicationRoot;
  }

  public void setApplicationRoot(final String applicationRoot) {
    this.applicationRoot = applicationRoot;
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

  public Integer getCloneDepth() {
    return cloneDepth;
  }

  public void setCloneDepth(final Integer cloneDepth) {
    this.cloneDepth = cloneDepth;
  }

  public String getCodeAnalysisExcludedFileExtensions() {
    return codeAnalysisExcludedFileExtensions;
  }

  public void setCodeAnalysisExcludedFileExtensions(final String codeAnalysisExcludedFileExtensions) {
    this.codeAnalysisExcludedFileExtensions = codeAnalysisExcludedFileExtensions;
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
        .gitUsername(Optional.ofNullable(username))
        .gitPassword(Optional.ofNullable(password))
        .branch(Optional.ofNullable(branch))
        .sourceDirectory(Optional.ofNullable(sourceDirectory))
        .restrictAnalysisToFolders(Optional.ofNullable(restrictAnalysisToFolders))
        .calculateMetrics(calculateMetrics)
        .startCommit(Optional.ofNullable(startCommit))
        .endCommit(Optional.ofNullable(endCommit))
        .cloneDepth(Optional.ofNullable(cloneDepth))
        .landscapeToken((landscapeToken != null && !landscapeToken.isBlank()) ? landscapeToken : "mytokenvalue")
        .applicationName(applicationName != null ? applicationName : "")
        .applicationRoot(Optional.ofNullable(applicationRoot))
        .codeAnalysisExcludedFileExtensions(codeAnalysisExcludedFileExtensions)
        .build();
  }
}
