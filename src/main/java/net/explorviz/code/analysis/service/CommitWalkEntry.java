package net.explorviz.code.analysis.service;

/** Lightweight commit metadata collected during a branch walk. */
record CommitWalkEntry(String hash, int commitTime) {}
