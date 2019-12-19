#!/usr/bin/env kscript
@file:DependsOn("org.kohsuke:github-api:1.101")

/**
 * Script checking that the repository can be released.
 * It checks that:
 * - the new version target an existing milestone
 * - the milestone has no opened issues
 * - the tag does not exist
 *
 * Run with `./pre-release.kts ${GIHUB_TOKEN} [--micro]
 *
 * 1. The github token is mandatory.
 * 2. The `--micro` is used to compute the increment. If omitted, the minor digit is incremented.
 */

import org.kohsuke.github.*
import java.io.File

val token = args[0]; // Fail if the first argument is not there
var micro = false
for (arg in args) {
    if (arg == "--micro") {
        micro = true;
    }
}

if (micro) {
    println("Micro enabled")
}

val github = GitHubBuilder().withOAuthToken(token).build()
val repository = github.getRepository("smallrye/smallrye-mutiny")
println("Listing tags of ${repository.getName()}")
val tags = repository.listTags().asList()
failIfPredicate({ tags.isNullOrEmpty() }, "No tags in repository ${repository.getName()}")
val tag = tags.first()
println("Last tag is " + tag.getName())

val newVersion = nextVersion(tag);
println("New version would be ${newVersion}")
val milestones = repository.listMilestones(GHIssueState.ALL).asList()
failIfPredicate({ milestones.isNullOrEmpty() }, "No milestones in repository ${repository.getName()}")

println("Checking the absence of the ${newVersion} tag")
val existingTag = tags.find { it.getName() == newVersion}
failIfPredicate( { existingTag != null }, "There is a tag with name ${newVersion}, invalid increment")

println("Checking the existence of the ${newVersion} milestone")
val existingMilestone = milestones.find { it.getTitle() == newVersion}
failIfPredicate( { existingMilestone == null }, "There is no milestone with name ${newVersion}, invalid increment")

println("Checking the number of open issues in the milestone ${newVersion}")
failIfPredicate( { existingMilestone?.getOpenIssues() != 0 }, "The milestone ${newVersion} has ${existingMilestone?.getOpenIssues()} issues opened, check check ${existingMilestone?.getHtmlUrl()}")

fun nextVersion(tag: GHTag) : String {
    // Retrieve the associated release
    val release = repository.getReleaseByTagName(tag.getName());
    if (release == null) {
        println("[WARNING] No release associated with tag ${tag.getName()}")
    }
    // All good, compute new version.
    return computeNewVersion(tag.getName(), micro)
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

fun computeNewVersion(last : String, micro: Boolean) : String {
    val segments = last.split(".")
    if (segments.size < 3) {
        fail("Invalid version ${last}, number of segments must be at least 3, found: ${segments.size}")
    }
    var newVersion = "${segments[0]}.${segments[1].toInt() + 1}.0"
    if (micro) {
        newVersion = "${segments[0]}.${segments[1]}.${segments[2].toInt() + 1}"
    }
    return newVersion
}


