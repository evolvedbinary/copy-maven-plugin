/*
 * Copyright © 2015 Martin Frey (mfrey.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.mfrey.maven.plugin.copy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "copy", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CopyMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private String defaultDir;

    @Component
    private MavenProject project;

    @Parameter
    private Resource[] resources;

    @Parameter(defaultValue = "false")
    private boolean showfiles;

    @Parameter(defaultValue = "false", required = false)
    private boolean skip;

    private void cleanupEmptyDirs(final File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    cleanupEmptyDirs(file);
                }
            }
            if (directory.list().length == 0) {
                FileUtils.deleteDirectory(directory);
            }
        }
    }

    private void copyFile(final Resource resource, final File srcFile, final File destFile) throws IOException {
        List<Replace> replaces = resource.getReplaces();
        if (replaces.isEmpty()) {
            if (srcFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
                return;
            } else {
                if (destFile.exists()) {
                    if (resource.isReplaceExisting()) {
                        destFile.delete();
                    } else {
                        throw new FileExistsException(destFile);
                    }
                }
                if (resource.isMove()) {
                    FileUtils.moveFile(srcFile, destFile);
                } else {
                    FileUtils.copyFile(srcFile, destFile);
                }
            }
        } else {
            // Read the file and replace its content
            String content = FileUtils.readFileToString(srcFile, resource.getCharset());
            for (Replace replace : replaces) {
                if (replace.isRegex()) {
                    content = content.replaceAll(replace.getFrom(), replace.getTo());
                } else {
                    content = content.replace(replace.getFrom(), replace.getTo());
                }
            }

            // Write the new file
            if (destFile.exists()) {
                if (resource.isReplaceExisting()) {
                    destFile.delete();
                } else {
                    throw new FileExistsException(destFile);
                }
            }
            FileUtils.writeStringToFile(destFile, content, resource.getCharset());
            if (resource.isMove() && !srcFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
                FileUtils.deleteQuietly(srcFile);
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping copy maven plugin execution");
            return;
        }
        try {
            if (getResources() == null || getResources().length == 0) {
                getLog().warn("No resources were defined to move or copy");
                return;
            }
            for (Resource resource : getResources()) {

                File workingDir = new File(resource.getDirectory() == null ? defaultDir : resource.getDirectory());
                if (!workingDir.isAbsolute()) {
                    workingDir = new File(project.getBasedir(), resource.getDirectory() == null ? defaultDir
                            : resource.getDirectory());
                }
                if (getLog().isInfoEnabled()) {
                    logResource(resource, workingDir);
                }
                final Collection<File> srcFiles = getFiles(workingDir, resource);
                for (final File srcFile : srcFiles) {
                    String destPath = getNewPath(resource, workingDir, srcFile);
                    File destFile = new File(destPath);
                    if (isShowfiles() && getLog().isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(resource.isMove() ? " mv " : " cp ").append(srcFile.getAbsolutePath())
                                .append(System.lineSeparator());
                        sb.append("        -> ").append(destFile.getAbsolutePath());
                        getLog().info(sb);
                    }
                    copyFile(resource, srcFile, destFile);
                }
                try {
                    cleanupEmptyDirs(workingDir);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Could not cleanup empty directories", ex);
                }
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Error during CopyMojo", ex);
        }

    }

    @SuppressWarnings("unchecked")
    public Collection<File> getFiles(final File workingDir, final Resource resource) throws MojoExecutionException {
        try {
            final IOFileFilter includesFilter = toIOFileFilter(workingDir, resource.getIncludes());
            final IOFileFilter excludesFilter = toIOFileFilter(workingDir, resource.getExcludes());

            final IOFileFilter listFilter;
            if (includesFilter == null) {
                if (excludesFilter == null) {
                    // no filter
                    listFilter = FalseFileFilter.INSTANCE;
                } else {
                    // excludes only
                    listFilter = new NotFileFilter(excludesFilter);
                }
            } else {
                if (excludesFilter == null) {
                    // includes only
                    listFilter = includesFilter;
                } else {
                    // includes and excludes
                    listFilter = includesFilter.and(new NotFileFilter(excludesFilter));
                }
            }

            return FileUtils.listFiles(workingDir, listFilter, TrueFileFilter.INSTANCE);

        } catch (final UncheckedIOException e) {
            throw new MojoExecutionException("Unable to get paths to copy: " + e.getMessage(), e);
        }
    }

    private IOFileFilter toIOFileFilter(final File workingDir, final List<String> pathGlobs) {
        IOFileFilter ioFileFilter = null;
        if (pathGlobs != null && !pathGlobs.isEmpty()) {
            for (final String pathGlob : pathGlobs) {
                if (ioFileFilter == null) {
                    ioFileFilter = new RelativeWildcardPathFilter(workingDir, pathGlob);
                } else {
                    ioFileFilter = ioFileFilter.or(new RelativeWildcardPathFilter(workingDir, pathGlob));
                }
            }
        }
        return ioFileFilter;
    }

    private String getNewPath(final Resource resource, final File workingDir, final File file)
            throws MojoExecutionException {
        List<Replace> renames = resource.getPaths();
        if (renames.isEmpty()) {
            return file.getAbsolutePath();
        }
        String path = resource.isWorkOnFullPath() ? file.getAbsolutePath() : file.getAbsolutePath().substring(
                workingDir.getAbsolutePath().length());
        if (resource.isNormalizePath()) {
            path = FilenameUtils.normalize(path, true);
        }
        if (isShowfiles()) {
            getLog().info("---");
            getLog().info(path);
        }
        for (Replace rename : renames) {
            if (rename.getFrom() == null || rename.getTo() == null) {
                throw new MojoExecutionException("From and To cannot be NULL: " + rename);
            } else if (!rename.getFrom().equals(rename.getTo())) {
                if (resource.isNormalizePath()) {
                    if (rename.isRegex()) {
                        path = path.replaceAll(FilenameUtils.normalize(rename.getFrom(), true),
                                FilenameUtils.normalize(rename.getTo(), true));
                    } else {
                        path = path.replace(FilenameUtils.normalize(rename.getFrom(), true),
                                FilenameUtils.normalize(rename.getTo(), true));
                    }
                } else {
                    if (rename.isRegex()) {
                        path = path.replaceAll(rename.getFrom(), rename.getTo());
                    } else {
                        path = path.replace(rename.getFrom(), rename.getTo());
                    }
                }
            }
        }
        if (isShowfiles()) {
            getLog().info(path);
        }
        return resource.isWorkOnFullPath() ? path : workingDir.getAbsolutePath() + path;
    }

    public Resource[] getResources() {
        return resources;
    }

    public boolean isShowfiles() {
        return showfiles;
    }

    private void logResource(final Resource resource, final File workingDir) {
        StringBuilder sb = new StringBuilder();
        sb.append("----------").append(System.lineSeparator());
        sb.append("        Resource: ").append(resource.getId()).append(System.lineSeparator());
        sb.append("      WorkingDir: ").append(workingDir.getAbsolutePath()).append(System.lineSeparator());
        sb.append("         Charset: ").append(resource.getCharset()).append(System.lineSeparator());
        sb.append("            Move: ").append(resource.isMove()).append(System.lineSeparator());
        sb.append("  WorkOnFullPath: ").append(resource.isWorkOnFullPath()).append(System.lineSeparator());
        sb.append("   NormalizePath: ").append(resource.isNormalizePath()).append(System.lineSeparator());
        sb.append("        Includes:").append(System.lineSeparator());
        for (String include : resource.getIncludes()) {
            sb.append("                  ").append(include).append(System.lineSeparator());
        }
        sb.append("        Excludes:").append(System.lineSeparator());
        for (String exclude : resource.getExcludes()) {
            sb.append("                  ").append(exclude).append(System.lineSeparator());
        }
        sb.append("          Paths:").append(System.lineSeparator());
        for (Replace r : resource.getPaths()) {
            sb.append("                  ").append(r.getFrom()).append(" -> ").append(r.getTo())
                    .append(r.isRegex() ? " (regex)" : " (replace)").append(System.lineSeparator());
        }
        sb.append("        Replaces:").append(System.lineSeparator());
        for (Replace r : resource.getReplaces()) {
            sb.append("                  ").append(r.getFrom()).append(" -> ").append(r.getTo())
                    .append(r.isRegex() ? " (regex)" : " (replace)").append(System.lineSeparator());
        }
        getLog().info(sb);
    }

    public void setResources(final Resource[] resources) {
        this.resources = resources;
    }

    public void setShowfiles(final boolean showfiles) {
        this.showfiles = showfiles;
    }

    private static class RelativeWildcardPathFilter extends AbstractFileFilter {

        private final Path basePath;
        private final String[] wildcards;

        public RelativeWildcardPathFilter(final File basePath, final String... wildcards) {
            this.basePath = basePath.toPath();
            this.wildcards = wildcards;
        }

        @Override
        public boolean accept(File file) {
            return accept(basePath.relativize(file.toPath()));
        }

        @Override
        public boolean accept(File dir, String name) {
            final Path file = dir.toPath().resolve(name);
            return accept(basePath.relativize(file));
        }

        @Override
        public FileVisitResult accept(Path file, BasicFileAttributes attributes) {
            return accept(basePath.relativize(file)) ? FileVisitResult.CONTINUE : FileVisitResult.TERMINATE;
        }

        private boolean accept(final Path relativePath) {
            for (final String wildcard : wildcards) {
                if (FilenameUtils.wildcardMatch(relativePath.toString(), wildcard, IOCase.SENSITIVE)) {
                    return true;
                }
            }
            return false;
        }
    }
}
