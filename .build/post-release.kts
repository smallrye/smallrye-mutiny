#!/usr/bin/env kscript
@file:MavenRepository("jcenter","https://jcenter.bintray.com/")
@file:MavenRepository("maven-central","https://repo1.maven.org/maven2/")
@file:DependsOn("org.kohsuke:github-api:1.101")

/**
 * Script run after the release.
 * The script does the following action in this order:
 * - checks that the previous milestone is closed, close it if not
 * - checks that the next milestone exists, or create it if not
 * - creates the Github release and compute the release notes
 *
 * Run with `./post-release.kts ${GIHUB_TOKEN}
 *
 * 1. The github token is mandatory.
 *
 * The version is taken from the last tag.
 */

import org.kohsuke.github.*
import java.io.File

val token = args[0]; // Fail if the first argument is not there

val github = GitHubBuilder().withOAuthToken(token).build()
val repository = github.getRepository("smallrye/smallrye-mutiny")

println("Listing tags of ${repository.getName()}")
val tags = repository.listTags().asList()
failIfPredicate({ tags.isNullOrEmpty() }, "No tags in repository ${repository.getName()}")
val tag = tags.first()
println("Last tag is " + tag.getName())

val version = tag.getName()
val nextVersion = nextVersion(tag)
println("Next version would be ${nextVersion}")

val milestones = repository.listMilestones(GHIssueState.ALL).asList()
failIfPredicate({ milestones.isNullOrEmpty() }, "No milestones in repository ${repository.getName()}")

println("Retrieving milestone ${version}")
val milestone = milestones.find { it.getTitle() == version}
val nextMilestone = milestones.find { it.getTitle() == nextVersion}

failIfPredicate({ milestone == null }, "Unable to find milestone ${version}")

println("Closing milestone ${version}")
milestone!!.close();
println("Milestone ${version} closed - ${milestone?.getHtmlUrl()}")

if (nextMilestone != null) {
    println("Milestone ${nextVersion} already created - ${nextMilestone?.getHtmlUrl()}")
} else {
    val newMilestone = repository.createMilestone(nextVersion, null)
    println("Milestone ${nextVersion} created - ${newMilestone.getHtmlUrl()}")
}

val issues = repository.getIssues(GHIssueState.CLOSED, milestone)

val release = repository.createRelease(version)
        .name(version)
        .body(createReleaseDescription(issues))
        .create()
println("Release ${version} created - ${release.getHtmlUrl()}")

fun GHIssue.line() : String {
    var title = this.getTitle();
    return "[${this.getNumber()}] ${title}"
}

fun GHIssue.markdown() : String {
    var title = this.getTitle();
    return "[#${this.getNumber()}](${this.getHtmlUrl()}) - ${title}"
}

fun createReleaseDescription(issues : List<GHIssue>) : String {
    val file = File("target/differences.md")
    var compatibility = ""
    if (file.isFile) {
        compatibility = file.readText(Charsets.UTF_8)
    }
    var desc = """### Changelog
        |
        |${issues.joinToString(separator="\n", transform={ "  * ${it.markdown()}" })}
        |
        |
        """.trimMargin("|")

    desc = desc.plus(compatibility).plus("\n")

    return desc
}

fun nextVersion(tag: GHTag) : String {
    return computeNewVersion(tag.getName())
}

fun fail(message: String, code: Int = 2) {
    println(message)
    kotlin.system.exitProcess(code)
}

fun failIfPredicate(predicate: () -> Boolean, message: String, code: Int = 2) {
    val success = predicate.invoke()
    if (success) {
        fail(message, code)
    }
}

fun computeNewVersion(last : String) : String {
    val segments = last.split(".")
    if (segments.size < 3) {
        fail("Invalid version ${last}, number of segments must be at least 3, found: ${segments.size}")
    }
    var newVersion = "${segments[0]}.${segments[1].toInt() + 1}.0"
    return newVersion
}


