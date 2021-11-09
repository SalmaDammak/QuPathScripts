/**
 * Update paths within a QuPath project to handle images that have been moved.
 *
 * This works by first finding image paths that don't point to any existing files,
 * and then prompting the user to select a base directory below which the script
 * will search for image files with the same names.
 *
 * The process is a bit cumbersome, partly because there is no easy way to set
 * the path for an existing ProjectImageEntry.
 *
 * The API might change to allow this in the future.
 *
 * WARNING! This implementation doesn't handle sub-images within the same file
 * (e.g. some images supported by Bio-Formats).
 *
 * @author Pete Bankhead
 */

import qupath.lib.gui.QuPathGUI
import qupath.lib.gui.helpers.DisplayHelpers
import qupath.lib.projects.Project
import qupath.lib.projects.ProjectIO
import qupath.lib.projects.ProjectImageEntry
import qupath.lib.scripting.QPEx

import java.awt.image.BufferedImage

// Search directories recursively or not
def doRecursive = true

// Get the current project
def project = QPEx.getProject()
if (project == null) {
    DisplayHelpers.showErrorMessage("Update project paths", "No project found!")
    return
}

// Find entries with no corresponding file found
// Store in a set (by insertion order) for faster 'contains' checks later
def nonExistent = new LinkedHashSet<ProjectImageEntry>()
def mapped = new HashMap<ProjectImageEntry, ProjectImageEntry>()
for (entry in project.getImageList()) {
    if (new File(entry.getServerPath()).exists())
        mapped.put(entry, entry)
    else
        nonExistent.add(entry)
}
if (nonExistent.isEmpty()) {
    DisplayHelpers.showMessageDialog("Update project paths", "All image paths seem ok!")
    return
}

// Find out what we're missing
int nMissing = nonExistent.size()
println "Paths missing for " + nonExistent.size() + " image(s)"
nonExistent.each {
    println '  ' + it.getServerPath()
}    

// Prompt for a directory in which to search for images with the same name
def dir = QuPathGUI.getSharedDialogHelper().promptForDirectory(null)
// Check for cancel
if (dir == null)
    return

// Maintain a list of directories to check
def dirsToCheck = [dir]
while (!dirsToCheck.isEmpty() && !nonExistent.isEmpty()) {
    // Get the next directory
    dir = dirsToCheck.remove(0)
    println 'Searching ' + dir + ' for ' + nonExistent.size() + ' image(s)'
    // Get the names
    def allNames = dir.listFiles().findAll({it.isFile() && !it.isHidden()}).collect {it.getName()}
    // Loop through the non-existent entries
    def iter = nonExistent.iterator()
    int count = 0
    while (iter.hasNext()) {
        // If we find a file with the same name, create a new entry & add it to the map
        def entry = iter.next()
        def searchName = new File(entry.getServerPath()).getName()
        if (allNames.contains(searchName)) {
            def newEntry = new ProjectImageEntry(project, new File(dir, searchName).getAbsolutePath(), entry.getImageName(), entry.getMetadataMap())
            mapped.put(entry, newEntry)
            iter.remove()
            count++
        }
    }
    // Show update
    if (count > 0)
        println 'Found ' + count + ' image(s)'
    // If we still have images to match, optionally search recursively
    if (doRecursive && !nonExistent.isEmpty()) {
        def subDirectories = dir.listFiles().findAll {it.isDirectory() && !it.isHidden()}
        dirsToCheck.addAll(subDirectories)
    }
}

// Check if we achieved anything
if (nMissing == nonExistent.size()) {
    println 'Unable to find relevant images - please specify another directory!'
    return
}

// Print results
if (nonExistent.isEmpty())
    println 'Found all images!'
else
    println 'Still missing ' + nonExistent.size() + ' image(s)'

// If we achieved anything, update the project with a unique name
def newName = null
int n = 0
while (newName == null || new File(project.getBaseDirectory(), newName + '.qpproj').exists()) {
    n++
    newName = 'project-' + n
}
def projectNew = new Project(new File(project.getBaseDirectory(), newName), BufferedImage.class)
def newEntries = []
for (entry in project.getImageList()) {
    newEntries << mapped.getOrDefault(entry, entry)
}
projectNew.addAllImages(newEntries)
ProjectIO.writeProject(projectNew, newName)
println 'Project written to ' + projectNew.getFile()