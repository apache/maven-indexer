/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.index.cli;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.inject.Guice;
import com.google.inject.Module;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.packer.IndexPackingRequest.IndexFormat;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.eclipse.sisu.launch.Main;
import org.eclipse.sisu.space.BeanScanning;

import static java.util.Objects.requireNonNull;

/**
 * A command line tool that can be used to index local Maven repository.
 * <p/>
 * The following command line options are supported:
 * <ul>
 * <li>-repository <path> : required path to repository to be indexed</li>
 * <li>-index <path> : required index folder used to store created index or where previously created index is
 * stored</li>
 * <li>-name <path> : required repository name/id</li>
 * <li>-target <path> : optional folder name where to save produced index files</li>
 * <li>-type <path> : optional indexer types</li>
 * <li>-format <path> : optional indexer formats</li>
 * </ul>
 * When index folder contains previously created index, the tool will use it as a base line and will generate chunks for
 * the incremental updates.
 * <p/>
 * The indexer types could be one of default, min or full. You can also specify list of comma-separated custom index
 * creators. An index creator should be a regular Plexus component, see
 * {@link org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator} and
 * {@link org.apache.maven.index.creator.JarFileContentsIndexCreator}.
 */
public class NexusIndexerCli {

    // Generic command line options

    public static final String QUIET = "q";

    public static final String DEBUG = "X";

    public static final String HELP = "h";

    public static final String VERSION = "v";

    // Command line options

    public static final String REPO = "r";

    public static final String INDEX = "i";

    public static final String NAME = "n";

    public static final String TYPE = "t";

    public static final String TARGET_DIR = "d";

    public static final String CREATE_INCREMENTAL_CHUNKS = "c";

    public static final String CREATE_FILE_CHECKSUMS = "s";

    public static final String INCREMENTAL_CHUNK_KEEP_COUNT = "k";

    public static final String UNPACK = "u";

    private static final long MB = 1024 * 1024;

    private Options options;

    public static void main(String[] args) {
        System.exit(new NexusIndexerCli().execute(args));
    }

    /**
     * Visible for testing.
     */
    int execute(String[] args) {
        CommandLine cli;

        try {
            cli = new DefaultParser().parse(buildCliOptions(), cleanArgs(args));
        } catch (ParseException e) {
            System.err.println("Unable to parse command line options: " + e.getMessage());

            displayHelp();

            return 1;
        }

        boolean debug = cli.hasOption(DEBUG);

        if (cli.hasOption(HELP)) {
            displayHelp();

            return 0;
        }

        if (cli.hasOption(VERSION)) {
            showVersion();

            return 0;
        } else if (debug) {
            showVersion();
        }

        final Module app = Main.wire(BeanScanning.INDEX);

        Components components = Guice.createInjector(app).getInstance(Components.class);

        if (cli.hasOption(UNPACK)) {
            try {
                return unpack(cli, components);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return 1;
            }
        } else if (cli.hasOption(INDEX) && cli.hasOption(REPO)) {
            try {
                return index(cli, components);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return 1;
            }
        } else {
            System.out.println();
            System.out.println("Use either unpack (\"" + UNPACK + "\") or index (\"" + INDEX + "\" and \"" + REPO
                    + "\") options, but none has been found!");
            System.out.println();
            displayHelp();
            return 1;
        }
    }

    /**
     * Visible for testing.
     */
    Options buildCliOptions() {
        this.options = new Options();

        options.addOption(Option.builder(QUIET)
                .longOpt("quiet")
                .desc("Quiet output - only show errors")
                .build());

        options.addOption(Option.builder(DEBUG)
                .longOpt("debug")
                .desc("Produce execution debug output")
                .build());

        options.addOption(Option.builder(VERSION)
                .longOpt("version")
                .desc("Display version information")
                .build());

        options.addOption(Option.builder(HELP)
                .longOpt("help")
                .desc("Display help information")
                .build());

        options.addOption(Option.builder(INDEX)
                .longOpt("index")
                .argName("path")
                .hasArg()
                .desc("Path to the index folder")
                .build());

        options.addOption(Option.builder(TARGET_DIR)
                .longOpt("destination")
                .argName("path")
                .hasArg()
                .desc("Target folder")
                .build());

        options.addOption(Option.builder(REPO)
                .longOpt("repository")
                .argName("path")
                .hasArg()
                .desc("Path to the Maven repository")
                .build());

        options.addOption(Option.builder(NAME)
                .longOpt("name")
                .argName("string")
                .hasArg()
                .desc("Repository name")
                .build());

        options.addOption(Option.builder(CREATE_INCREMENTAL_CHUNKS)
                .longOpt("chunks")
                .desc("Create incremental chunks")
                .build());

        options.addOption(Option.builder(INCREMENTAL_CHUNK_KEEP_COUNT)
                .longOpt("keep")
                .argName("num")
                .hasArg()
                .desc("Number of incremental chunks to keep")
                .build());

        options.addOption(Option.builder(CREATE_FILE_CHECKSUMS)
                .longOpt("checksums")
                .desc("Create checksums for all files (sha1, md5)")
                .build());

        options.addOption(Option.builder(TYPE)
                .longOpt("type")
                .argName("type")
                .hasArg()
                .desc("Indexer type (default, min, full or comma separated list of custom types)")
                .build());

        options.addOption(Option.builder(UNPACK)
                .longOpt("unpack")
                .desc("Unpack an index file")
                .build());

        return options;
    }

    private String[] cleanArgs(String[] args) {
        List<String> cleaned = new ArrayList<>();

        StringBuilder currentArg = null;

        for (String arg : args) {
            boolean addedToBuffer = false;

            if (arg.startsWith("\"")) {
                // if we're in the process of building up another arg, push it and start over.
                // this is for the case: "-Dfoo=bar "-Dfoo2=bar two" (note the first unterminated quote)
                if (currentArg != null) {
                    cleaned.add(currentArg.toString());
                }

                // start building an argument here.
                currentArg = new StringBuilder(arg.substring(1));

                addedToBuffer = true;
            }

            // this has to be a separate "if" statement, to capture the case of: "-Dfoo=bar"
            if (arg.endsWith("\"")) {
                String cleanArgPart = arg.substring(0, arg.length() - 1);

                // if we're building an argument, keep doing so.
                if (currentArg != null) {
                    // if this is the case of "-Dfoo=bar", then we need to adjust the buffer.
                    if (addedToBuffer) {
                        currentArg.setLength(currentArg.length() - 1);
                    }
                    // otherwise, we trim the trailing " and append to the buffer.
                    else {
                        // TODO: introducing a space here...not sure what else to do but collapse whitespace
                        currentArg.append(' ').append(cleanArgPart);
                    }

                    // we're done with this argument, so add it.
                    cleaned.add(currentArg.toString());
                } else {
                    // this is a simple argument...just add it.
                    cleaned.add(cleanArgPart);
                }

                // the currentArg MUST be finished when this completes.
                currentArg = null;

                continue;
            }

            // if we haven't added this arg to the buffer, and we ARE building an argument
            // buffer, then append it with a preceding space...again, not sure what else to
            // do other than collapse whitespace.
            // NOTE: The case of a trailing quote is handled by nullifying the arg buffer.
            if (!addedToBuffer) {
                // append to the argument we're building, collapsing whitespace to a single space.
                if (currentArg != null) {
                    currentArg.append(' ').append(arg);
                }
                // this is a loner, just add it directly.
                else {
                    cleaned.add(arg);
                }
            }
        }

        // clean up.
        if (currentArg != null) {
            cleaned.add(currentArg.toString());
        }

        int cleanedSz = cleaned.size();
        String[] cleanArgs;

        if (cleanedSz == 0) {
            // if we didn't have any arguments to clean, simply pass the original array through
            cleanArgs = args;
        } else {
            cleanArgs = cleaned.toArray(new String[cleanedSz]);
        }

        return cleanArgs;
    }

    private void displayHelp() {
        System.out.println();

        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp("nexus-indexer [options]", "\nOptions:", options, "\n");
    }

    private void showVersion() {
        InputStream is;

        try {
            Properties properties = new Properties();

            is = getClass()
                    .getClassLoader()
                    .getResourceAsStream("META-INF/maven/org.apache.maven.indexer/indexer-core/pom.properties");

            if (is == null) {
                System.err.println("Unable determine version from JAR file.");

                return;
            }

            properties.load(is);

            if (properties.getProperty("builtOn") != null) {
                System.out.println("Version: " + properties.getProperty("version", "unknown") + " built on "
                        + properties.getProperty("builtOn"));
            } else {
                System.out.println("Version: " + properties.getProperty("version", "unknown"));
            }
        } catch (IOException e) {
            System.err.println("Unable determine version from JAR file: " + e.getMessage());
        }
    }

    private int index(final CommandLine cli, Components components)
            throws IOException, UnsupportedExistingLuceneIndexException {
        String indexDirectoryName = cli.getOptionValue(INDEX);

        File indexFolder = new File(indexDirectoryName);

        String outputDirectoryName = cli.getOptionValue(TARGET_DIR, ".");

        File outputFolder = new File(outputDirectoryName);

        File repositoryFolder = new File(cli.getOptionValue(REPO));

        String repositoryName = cli.getOptionValue(NAME, indexFolder.getName());

        List<IndexCreator> indexers = getIndexers(cli, components);

        boolean createChecksums = cli.hasOption(CREATE_FILE_CHECKSUMS);

        boolean createIncrementalChunks = cli.hasOption(CREATE_INCREMENTAL_CHUNKS);

        boolean debug = cli.hasOption(DEBUG);

        boolean quiet = cli.hasOption(QUIET);

        Integer chunkCount = cli.hasOption(INCREMENTAL_CHUNK_KEEP_COUNT)
                ? Integer.parseInt(cli.getOptionValue(INCREMENTAL_CHUNK_KEEP_COUNT))
                : null;

        if (!quiet) {
            System.err.printf("Repository Folder: %s\n", repositoryFolder.getAbsolutePath());
            System.err.printf("Index Folder:      %s\n", indexFolder.getAbsolutePath());
            System.err.printf("Output Folder:     %s\n", outputFolder.getAbsolutePath());
            System.err.printf("Repository name:   %s\n", repositoryName);
            System.err.printf("Indexers: %s\n", indexers);

            if (createChecksums) {
                System.err.print("Will create checksum files for all published files (sha1, md5).\n");
            } else {
                System.err.print("Will not create checksum files.\n");
            }

            if (createIncrementalChunks) {
                System.err.print("Will create incremental chunks for changes, along with baseline file.\n");
            } else {
                System.err.print("Will create baseline file.\n");
            }
        }

        long tstart = System.currentTimeMillis();

        IndexingContext context = components.indexer.addIndexingContext( //
                repositoryName, // context id
                repositoryName, // repository id
                repositoryFolder, // repository folder
                indexFolder, // index folder
                null, // repositoryUrl
                null, // index update url
                indexers);

        try {
            ArtifactScanningListener listener = new IndexerListener(context, debug, quiet);

            components.indexer.scan(context, listener, true);

            IndexSearcher indexSearcher = context.acquireIndexSearcher();

            try {
                IndexPackingRequest request =
                        new IndexPackingRequest(context, indexSearcher.getIndexReader(), outputFolder);

                request.setCreateChecksumFiles(createChecksums);

                request.setCreateIncrementalChunks(createIncrementalChunks);

                request.setFormats(List.of(IndexFormat.FORMAT_V1));

                if (chunkCount != null) {
                    request.setMaxIndexChunks(chunkCount);
                }

                packIndex(components.packer, request, debug, quiet);
            } finally {
                context.releaseIndexSearcher(indexSearcher);
            }

            if (!quiet) {
                printStats(tstart);
            }
        } finally {
            components.indexer.removeIndexingContext(context, false);
        }
        return 0;
    }

    private int unpack(CommandLine cli, Components components) throws IOException {
        final String indexDirectoryName = cli.getOptionValue(INDEX, ".");
        final File indexFolder = new File(indexDirectoryName).getCanonicalFile();
        final File indexArchive = new File(indexFolder, IndexingContext.INDEX_FILE_PREFIX + ".gz");

        final String outputDirectoryName = cli.getOptionValue(TARGET_DIR, ".");
        final File outputFolder = new File(outputDirectoryName).getCanonicalFile();

        final boolean quiet = cli.hasOption(QUIET);
        if (!quiet) {
            System.err.printf("Index Folder:      %s\n", indexFolder.getAbsolutePath());
            System.err.printf("Output Folder:     %s\n", outputFolder.getAbsolutePath());
        }

        long tstart = System.currentTimeMillis();

        final List<IndexCreator> indexers = getIndexers(cli, components);

        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(indexArchive)); //
                FSDirectory directory = FSDirectory.open(outputFolder.toPath())) {
            DefaultIndexUpdater.unpackIndexData(is, 4, directory, (IndexingContext) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class[] {IndexingContext.class}, new PartialImplementation() {
                        public List<IndexCreator> getIndexCreators() {
                            return indexers;
                        }
                    }));
        }

        if (!quiet) {
            printStats(tstart);
        }
        return 0;
    }

    private List<IndexCreator> getIndexers(final CommandLine cli, Components components) {
        String type = "default";

        if (cli.hasOption(TYPE)) {
            type = cli.getOptionValue(TYPE);
        }

        List<IndexCreator> indexers = new ArrayList<>(); // NexusIndexer.DEFAULT_INDEX;

        if ("default".equals(type)) {
            indexers.add(requireNonNull(components.allIndexCreators.get("min")));
            indexers.add(requireNonNull(components.allIndexCreators.get("jarContent")));
        } else if ("full".equals(type)) {
            indexers.addAll(components.allIndexCreators.values());
        } else {
            for (String name : type.split(",")) {
                indexers.add(requireNonNull(components.allIndexCreators.get(name)));
            }
        }
        return indexers;
    }

    private void packIndex(IndexPacker packer, IndexPackingRequest request, boolean debug, boolean quiet) {
        try {
            packer.packIndex(request);
        } catch (IOException e) {
            if (!quiet) {
                System.err.printf("Cannot zip index: %s\n", e.getMessage());

                if (debug) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void printStats(final long startTimeInMillis) {
        long t = System.currentTimeMillis() - startTimeInMillis;

        long s = TimeUnit.MILLISECONDS.toSeconds(t);
        if (t > TimeUnit.MINUTES.toMillis(1)) {
            long m = TimeUnit.MILLISECONDS.toMinutes(t);

            System.err.printf("Total time:   %d min %d sec\n", m, s - (m * 60));
        } else {
            System.err.printf("Total time:   %d sec\n", s);
        }

        Runtime r = Runtime.getRuntime();

        System.err.printf(
                "Final memory: %dM/%dM\n", //
                (r.totalMemory() - r.freeMemory()) / MB, r.totalMemory() / MB);
    }

    /**
     * Scanner listener
     */
    private static final class IndexerListener implements ArtifactScanningListener {
        private final IndexingContext context;

        private final boolean debug;

        private final boolean quiet;

        private long ts = System.currentTimeMillis();

        private int count;

        IndexerListener(IndexingContext context, boolean debug, boolean quiet) {
            this.context = context;
            this.debug = debug;
            this.quiet = quiet;
        }

        @Override
        public void scanningStarted(IndexingContext context) {
            if (!quiet) {
                System.err.println("Scanning started");
            }
        }

        @Override
        public void artifactDiscovered(ArtifactContext ac) {
            count++;

            long t = System.currentTimeMillis();

            ArtifactInfo ai = ac.getArtifactInfo();

            if (!quiet && debug && "maven-plugin".equals(ai.getPackaging())) {
                System.err.printf(
                        "Plugin: %s:%s:%s - %s %s\n", //
                        ai.getGroupId(), ai.getArtifactId(), ai.getVersion(), ai.getPrefix(), "" + ai.getGoals());
            }

            if (!quiet && (debug || (t - ts) > 2000L)) {
                System.err.printf("  %6d %s\n", count, formatFile(ac.getPom()));
                ts = t;
            }
        }

        @Override
        public void artifactError(ArtifactContext ac, Exception e) {
            if (!quiet) {
                System.err.printf("! %6d %s - %s\n", count, formatFile(ac.getPom()), e.getMessage());

                System.err.printf("         %s\n", formatFile(ac.getArtifact()));

                if (debug) {
                    e.printStackTrace();
                }
            }

            ts = System.currentTimeMillis();
        }

        private String formatFile(File file) {
            return file.getAbsolutePath()
                    .substring(context.getRepository().getAbsolutePath().length() + 1);
        }

        @Override
        public void scanningFinished(IndexingContext context, ScanningResult result) {
            if (!quiet) {
                if (result.hasExceptions()) {
                    System.err.printf(
                            "Scanning errors:   %s\n", result.getExceptions().size());
                }

                System.err.printf("Artifacts added:   %s\n", result.getTotalFiles());
                System.err.printf("Artifacts deleted: %s\n", result.getDeletedFiles());
            }
        }
    }
}
