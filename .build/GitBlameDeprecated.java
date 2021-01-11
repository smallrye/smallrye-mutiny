///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.eclipse.jgit:org.eclipse.jgit:5.10.0.202012080955-r
//DEPS com.google.guava:guava:30.1-jre
//DEPS org.slf4j:slf4j-simple:1.7.30


import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;


@Command(name = "GitBlameDeprecated", mixinStandardHelpOptions = true, version = "GitBlameDeprecated 0.1",
        description = "Find the (Java) deprecated methods in a local Git repository. Usage: jbang GitBlameDeprecated path-to-repository")
class GitBlameDeprecated implements Callable<Integer> {

    @Parameters(index = "0", description = "The root of the git directory", defaultValue = ".")
	private File directory;

	private Map<File, BlameResult> cache = new HashMap<>();

	public static void main(String... args) {
        int exitCode = new CommandLine(new GitBlameDeprecated()).execute(args);
        System.exit(exitCode);
    }

    @Override
	public Integer call() throws Exception {
		var repository = openRepository();

		List<Deprecation> list = new ArrayList<>();
		// Collect deprecations
		var deprecations = getDeprecations();

		// Retrieve the commit related to these deprecations.
		for (var entry : deprecations.asMap().entrySet()) {
			for (var d : entry.getValue()) {
				extractBlame(repository, d);
				list.add(d);
			}
		}

		// Sort deprecation by dates (oldest first)
		Collections.sort(list);

		// Print report
		for(Deprecation deprecation : list) {
			System.out.println(deprecation);
		}

        return 0;
	}


	private void extractBlame(Repository repository, Deprecation deprecation) throws GitAPIException, NoWorkTreeException, IOException {
		BlameResult blame = cache.computeIfAbsent(deprecation.file, f -> {
			BlameCommand blamer = new BlameCommand(repository);
			blamer.setFilePath(deprecation.file.getAbsolutePath()
				.substring(directory.getAbsolutePath().length() +1));
			try {
				return blamer.call();
			} catch (GitAPIException e) {
				// ignore.
				return null;
			}
		});  

		if (blame == null) {
			deprecation.author = "unknown";
			deprecation.commit = "commit not found";
			deprecation.date = Integer.MAX_VALUE;
			return;
		}

		RevCommit commit = blame.getSourceCommit(deprecation.line);
		deprecation.commit = commit.getShortMessage();
		deprecation.date = commit.getCommitTime();
		deprecation.author = commit.getAuthorIdent().getName();
	}

	static class Deprecation implements Comparable<Deprecation> {
		String signature;
		int line;
		File file;
		String commit;
		int date;
		String author;

		@Override
		public int compareTo(Deprecation o) {
			return Integer.compare(date, o.date);
		}

		String getHumanDate() {
			Long temp = date * 1000L; 
			Date ms = new Date(temp);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			return sdf.format(ms);
		}

		String getSignature() {
			return signature
				.replace("{", "")
				.replace("public", "")
				.replace("default", "")
				.replace("static", "");
		}

		public String toString() {
			return getHumanDate() + " - " + getSignature()
					+ " (" + file.getName() + ":" + line +") [" + commit + ", by " + author + "]";
		}
	}
	
	public Multimap<File, Deprecation> getDeprecations() throws IOException {
		Iterable<Path> path = MoreFiles.fileTraverser().breadthFirst(directory.toPath());
		Multimap<File, Deprecation> map = ArrayListMultimap.create();

		for(Path p : path) {
			File file = p.toFile();
			if (file.getName().endsWith(".java")) {
				List<String> result = Files.readLines(file, Charsets.UTF_8);
				for (int i = 0; i < result.size(); i++) {
					if (isDeprecated(result.get(i))) {
						Deprecation deprecation = new Deprecation();
						deprecation.line = i;
						deprecation.signature = result.get(i + 1).trim();
						deprecation.file = file;
						map.put(file, deprecation);
					}
				}
			}
		}
		return map;
	}

	private boolean isDeprecated(String l) {
		return l.trim().equalsIgnoreCase("@Deprecated");
	}

	public Repository openRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(directory, ".git"))
                .readEnvironment() 
                .findGitDir() 
				.build();
        return repository;
    }
}
