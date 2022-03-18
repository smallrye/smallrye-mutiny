///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.2
//DEPS org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r
//DEPS com.google.guava:guava:31.0-jre
//DEPS org.slf4j:slf4j-simple:1.7.32

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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(name = "BlameAPI", mixinStandardHelpOptions = true, version = "BlameAPI 0.2",
        description = "Find the Java deprecated or experimental methods in a local Git repository.")
class BlameAPI implements Callable<Integer> {

	enum Target {
		deprecated,
		experimental
	}

    @Option(names = "--root", description = "The root of the git directory.", defaultValue = ".")
	private File directory;

	@Option(names = "--target", description = "The search target in [deprecated, experimental].", defaultValue = "deprecated")
	private Target target;

	private Map<File, BlameResult> cache = new HashMap<>();

	public static void main(String... args) {
        int exitCode = new CommandLine(new BlameAPI()).execute(args);
        System.exit(exitCode);
    }

    @Override
	public Integer call() throws Exception {
		var repository = openRepository();

		List<BlameTarget> list = new ArrayList<>();
		// Collect blameTargets
		var blameTargets = getBlameTargets();

		// Retrieve the commit related to these blameTargets.
		for (var entry : blameTargets.asMap().entrySet()) {
			for (var d : entry.getValue()) {
				extractBlame(repository, d);
				list.add(d);
			}
		}

		// Sort blameTarget by dates (oldest first)
		Collections.sort(list);

		// Print report
		for(BlameTarget blameTarget : list) {
			System.out.println(blameTarget);
		}

        return 0;
	}


	private void extractBlame(Repository repository, BlameTarget blameTarget) throws GitAPIException, NoWorkTreeException, IOException {
		BlameResult blame = cache.computeIfAbsent(blameTarget.file, f -> {
			BlameCommand blamer = new BlameCommand(repository);
			blamer.setFilePath(blameTarget.file.getAbsolutePath()
				.substring(directory.getAbsolutePath().length() +1));
			try {
				return blamer.call();
			} catch (GitAPIException e) {
				// ignore.
				return null;
			}
		});  

		if (blame == null) {
			blameTarget.author = "unknown";
			blameTarget.commit = "commit not found";
			blameTarget.date = Integer.MAX_VALUE;
			return;
		}

		RevCommit commit = blame.getSourceCommit(blameTarget.line);
		blameTarget.commit = commit.getShortMessage();
		blameTarget.date = commit.getCommitTime();
		blameTarget.author = commit.getAuthorIdent().getName();
	}

	static class BlameTarget implements Comparable<BlameTarget> {
		String signature;
		int line;
		File file;
		String commit;
		int date;
		String author;

		@Override
		public int compareTo(BlameTarget o) {
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
	
	public Multimap<File, BlameTarget> getBlameTargets() throws IOException {
		Iterable<Path> path = MoreFiles.fileTraverser().breadthFirst(directory.toPath());
		Multimap<File, BlameTarget> map = ArrayListMultimap.create();

		for(Path p : path) {
			File file = p.toFile();
			if (file.getName().endsWith(".java") && !file.getName().equals("BlameAPI.java")) {
				List<String> result = Files.readLines(file, Charsets.UTF_8);
				for (int i = 0; i < result.size(); i++) {
					if (isDeprecated(result.get(i))) {
						BlameTarget blameTarget = new BlameTarget();
						blameTarget.line = i;
						blameTarget.signature = result.get(i + 1).trim();
						blameTarget.file = file;
						map.put(file, blameTarget);
					}
				}
			}
		}
		return map;
	}

	private boolean isDeprecated(String l) {
		var annotation = (target == Target.deprecated) ? "@Deprecated" : "@Experimental";
		return l.trim().contains(annotation);
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
