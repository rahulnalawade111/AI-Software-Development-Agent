package dev.aidev.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JGit-backed git operations on project workspaces. */
@Service
public class GitService {

    public Git open(Path workspaceDir) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(workspaceDir.resolve(".git").toFile())
                .readEnvironment()
                .findGitDir()
                .build();
        return new Git(repo);
    }

    public Git init(Path workspaceDir) throws IOException {
        if (Files_gitDir(workspaceDir)) {
            return open(workspaceDir);
        }
        try {
            Git git = Git.init().setDirectory(workspaceDir.toFile()).call();
            return git;
        } catch (org.eclipse.jgit.api.errors.GitAPIException e) {
            throw new IOException("Git init failed", e);
        }
    }

    private boolean Files_gitDir(Path dir) {
        return dir.resolve(".git").toFile().isDirectory();
    }

    public void commitAll(Git git, String message, String authorName, String authorEmail) throws Exception {
        // stage everything
        git.add().addFilepattern(".").call();
        var status = git.status().call();
        if (status.getUncommittedChanges().isEmpty() && status.getUntracked().isEmpty()) {
            return; // nothing to commit
        }
        git.commit()
           .setAll(true)
           .setAuthor(authorName, authorEmail)
           .setCommitter(authorName, authorEmail)
           .setMessage(message)
           .call();
    }

    public Map<String, Object> status(Git git) throws Exception {
        var st = git.status().call();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("modified", new ArrayList<>(st.getModified()));
        result.put("added", new ArrayList<>(st.getAdded()));
        result.put("deleted", new ArrayList<>(st.getMissing()));
        result.put("untracked", new ArrayList<>(st.getUntracked()));
        result.put("changed", new ArrayList<>(st.getChanged()));
        return result;
    }

    public List<Map<String, Object>> log(Git git, int limit) throws Exception {
        List<Map<String, Object>> commits = new ArrayList<>();
        var iterable = git.log().setMaxCount(limit).call();
        for (var commit : iterable) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("hash", commit.getId().getName());
            c.put("shortHash", commit.getId().abbreviate(7).name());
            c.put("message", commit.getShortMessage());
            c.put("author", commit.getAuthorIdent().getName());
            c.put("time", commit.getCommitTime() * 1000L);
            commits.add(c);
        }
        return commits;
    }

    public String diff(Git git) throws Exception {
        var diffCommand = git.diff();
        var diffs = diffCommand.call();
        StringBuilder sb = new StringBuilder();
        for (var entry : diffs) {
            sb.append("--- ").append(entry.getOldPath()).append("\n+++ ").append(entry.getNewPath()).append("\n");
        }
        return sb.toString();
    }
}
