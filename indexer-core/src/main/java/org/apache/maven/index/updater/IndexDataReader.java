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
package org.apache.maven.index.updater;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.DocumentFilter;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.NexusAnalyzer;
import org.apache.maven.index.context.NexusIndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An index data reader used to parse transfer index format.
 *
 * @author Eugene Kuleshov
 */
public class IndexDataReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexDataReader.class);

    private final DataInputStream dis;
    private final Path tempStorage;
    private final DocumentFilter filter;
    private final FSDirectoryFactory factory;
    private final int threads;

    public IndexDataReader(final InputStream is) throws IOException {
        this(is, 1);
    }

    public IndexDataReader(final InputStream is, final int threads) throws IOException {
        this(is, null, null, null, threads);
    }

    public IndexDataReader(final InputStream is, final IndexUpdateRequest request) throws IOException {
        this(
                is,
                request.getIndexTempDir() != null ? request.getIndexTempDir().toPath() : null,
                request.getDocumentFilter(),
                request.getFSDirectoryFactory(),
                request.getThreads());
    }

    public IndexDataReader(
            final InputStream is,
            final Path tempStorage,
            final DocumentFilter filter,
            final FSDirectoryFactory factory,
            final int threads)
            throws IOException {
        if (threads < 1) {
            throw new IllegalArgumentException("Reader threads must be greater than zero: " + threads);
        }
        this.tempStorage = Objects.requireNonNullElse(tempStorage, Path.of(System.getProperty("java.io.tmpdir")));
        this.factory = Objects.requireNonNullElse(factory, FSDirectoryFactory.DEFAULT);
        this.filter = filter;
        this.threads = threads;

        // MINDEXER-13
        // LightweightHttpWagon may have performed automatic decompression
        // Handle it transparently
        is.mark(2);
        InputStream data;
        if (is.read() == 0x1f && is.read() == 0x8b) // GZIPInputStream.GZIP_MAGIC
        {
            is.reset();
            data = new BufferedInputStream(new GZIPInputStream(is, 1024 * 8), 1024 * 8);
        } else {
            is.reset();
            data = new BufferedInputStream(is, 1024 * 8);
        }

        this.dis = new DataInputStream(data);
    }

    public IndexDataReadResult readIndex(IndexWriter w, IndexingContext context) throws IOException {
        if (threads == 1) {
            return readIndexST(w, context);
        } else {
            return readIndexMT(w, context);
        }
    }

    private IndexDataReadResult readIndexST(IndexWriter w, IndexingContext context) throws IOException {
        LOGGER.debug("Reading ST index...");
        Instant start = Instant.now();
        long timestamp = readHeader();

        Date date = null;

        if (timestamp != -1) {
            date = new Date(timestamp);

            IndexUtils.updateTimestamp(w.getDirectory(), date);
        }

        int n = 0;

        Document doc;
        Set<String> rootGroups = new HashSet<>();
        Set<String> allGroups = new HashSet<>();

        while ((doc = readDocument()) != null) {
            addToIndex(doc, context, w, rootGroups, allGroups);
            n++;
        }

        w.commit();

        IndexDataReadResult result = new IndexDataReadResult();
        result.setDocumentCount(n);
        result.setTimestamp(date);
        result.setRootGroups(rootGroups);
        result.setAllGroups(allGroups);

        LOGGER.debug(
                "Reading ST index done in {} sec",
                Duration.between(start, Instant.now()).getSeconds());
        return result;
    }

    private IndexDataReadResult readIndexMT(IndexWriter w, IndexingContext context) throws IOException {
        LOGGER.debug("Reading MT index...");
        Instant start = Instant.now();
        long timestamp = readHeader();

        int n = 0;

        final Document theEnd = new Document();

        Set<String> rootGroups = ConcurrentHashMap.newKeySet();
        Set<String> allGroups = ConcurrentHashMap.newKeySet();
        ArrayBlockingQueue<Document> queue = new ArrayBlockingQueue<>(10000);

        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        ArrayList<Exception> errors = new ArrayList<>();
        ArrayList<FSDirectory> siloDirectories = new ArrayList<>(threads);
        ArrayList<IndexWriter> siloWriters = new ArrayList<>(threads);
        LOGGER.debug("Creating {} silo writer threads...", threads);
        for (int i = 0; i < threads; i++) {
            final int silo = i;
            FSDirectory siloDirectory = tempDirectory("silo" + i);
            siloDirectories.add(siloDirectory);
            siloWriters.add(tempWriter(siloDirectory));
            executorService.execute(() -> {
                LOGGER.debug("Starting thread {}", Thread.currentThread().getName());
                try {
                    while (true) {
                        try {
                            Document doc = queue.take();
                            if (doc == theEnd) {
                                break;
                            }
                            addToIndex(doc, context, siloWriters.get(silo), rootGroups, allGroups);
                        } catch (InterruptedException | IOException e) {
                            errors.add(e);
                            break;
                        }
                    }
                } finally {
                    LOGGER.debug("Done thread {}", Thread.currentThread().getName());
                }
            });
        }

        LOGGER.debug("Loading up documents into silos");
        try {
            Document doc;
            while ((doc = readDocument()) != null) {
                queue.put(doc);
                n++;
            }
            LOGGER.debug("Signalling END");
            for (int i = 0; i < threads; i++) {
                queue.put(theEnd);
            }

            LOGGER.debug("Shutting down threads");
            executorService.shutdown();
            executorService.awaitTermination(5L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IOException("Interrupted", e);
        }

        if (!errors.isEmpty()) {
            IOException exception = new IOException("Error during load of index");
            errors.forEach(exception::addSuppressed);
            throw exception;
        }

        LOGGER.debug("Silos loaded...");
        Date date = null;
        if (timestamp != -1) {
            date = new Date(timestamp);
            IndexUtils.updateTimestamp(w.getDirectory(), date);
        }

        LOGGER.debug("Closing silo writers...");
        for (IndexWriter siloWriter : siloWriters) {
            siloWriter.commit();
            siloWriter.close();
        }

        LOGGER.debug("Merging silo directories...");
        w.addIndexes(siloDirectories.toArray(new Directory[0]));

        LOGGER.debug("Cleanup of silo directories...");
        for (FSDirectory siloDirectory : siloDirectories) {
            File dir = siloDirectory.getDirectory().toFile();
            siloDirectory.close();
            IndexUtils.delete(dir);
        }

        LOGGER.debug("Finalizing...");
        w.commit();

        IndexDataReadResult result = new IndexDataReadResult();
        result.setDocumentCount(n);
        result.setTimestamp(date);
        result.setRootGroups(rootGroups);
        result.setAllGroups(allGroups);

        LOGGER.debug(
                "Reading MT index done in {} sec",
                Duration.between(start, Instant.now()).getSeconds());
        return result;
    }

    private FSDirectory tempDirectory(final String name) throws IOException {
        return factory.open(
                Files.createTempDirectory(tempStorage, name + ".dir").toFile());
    }

    private IndexWriter tempWriter(final FSDirectory directory) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(new NexusAnalyzer());
        config.setUseCompoundFile(false);
        return new NexusIndexWriter(directory, config);
    }

    private void addToIndex(
            final Document doc,
            final IndexingContext context,
            final IndexWriter indexWriter,
            final Set<String> rootGroups,
            final Set<String> allGroups)
            throws IOException {
        ArtifactInfo ai = IndexUtils.constructArtifactInfo(doc, context);
        if (ai != null) {
            if (filter == null || filter.accept(doc)) {
                indexWriter.addDocument(IndexUtils.updateDocument(doc, context, false, ai));
                rootGroups.add(ai.getRootGroup());
                allGroups.add(ai.getGroupId());
            }
        } else {
            // these two fields are automatically handled in code above
            if (doc.getField(ArtifactInfo.ALL_GROUPS) == null && doc.getField(ArtifactInfo.ROOT_GROUPS) == null) {
                indexWriter.addDocument(doc);
            }
        }
    }

    public long readHeader() throws IOException {
        final byte hdrbyte = (byte) ((IndexDataWriter.VERSION << 24) >> 24);

        if (hdrbyte != dis.readByte()) {
            // data format version mismatch
            throw new IOException("Provided input contains unexpected data (0x01 expected as 1st byte)!");
        }

        return dis.readLong();
    }

    public Document readDocument() throws IOException {
        int fieldCount;
        try {
            fieldCount = dis.readInt();
        } catch (EOFException ex) {
            return null; // no more documents
        }

        Document doc = new Document();

        for (int i = 0; i < fieldCount; i++) {
            doc.add(readField());
        }

        // Fix up UINFO field wrt MINDEXER-41
        final Field uinfoField = (Field) doc.getField(ArtifactInfo.UINFO);
        final String info = doc.get(ArtifactInfo.INFO);
        if (uinfoField != null && info != null && !info.isEmpty()) {
            final String[] splitInfo = ArtifactInfo.FS_PATTERN.split(info);
            if (splitInfo.length > 6) {
                final String extension = splitInfo[6];
                final String uinfoString = uinfoField.stringValue();
                if (uinfoString.endsWith(ArtifactInfo.FS + ArtifactInfo.NA)) {
                    uinfoField.setStringValue(uinfoString + ArtifactInfo.FS + ArtifactInfo.nvl(extension));
                }
            }
        }

        return doc;
    }

    private Field readField() throws IOException {
        int flags = dis.read();

        FieldType fieldType = new FieldType();
        if ((flags & IndexDataWriter.F_INDEXED) > 0) {
            boolean tokenized = (flags & IndexDataWriter.F_TOKENIZED) > 0;
            fieldType.setTokenized(tokenized);
        }
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        fieldType.setStored((flags & IndexDataWriter.F_STORED) > 0);

        String name = dis.readUTF();
        String value = readUTF(dis);

        return new Field(name, value, fieldType);
    }

    private static String readUTF(DataInput in) throws IOException {
        int utflen = in.readInt();

        byte[] bytearr;
        char[] chararr;

        try {
            bytearr = new byte[utflen];
            chararr = new char[utflen];
        } catch (OutOfMemoryError e) {
            throw new IOException(
                    "Index data content is inappropriate (is junk?), leads to OutOfMemoryError!"
                            + " See MINDEXER-28 for more information!",
                    e);
        }

        int c, char2, char3;
        int count = 0;
        int chararrCount = 0;

        in.readFully(bytearr, 0, utflen);

        while (count < utflen) {
            c = bytearr[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            chararr[chararrCount++] = (char) c;
        }

        while (count < utflen) {
            c = bytearr[count] & 0xff;
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    /* 0xxxxxxx */
                    count++;
                    chararr[chararrCount++] = (char) c;
                    break;

                case 12:
                case 13:
                    /* 110x xxxx 10xx xxxx */
                    count += 2;
                    if (count > utflen) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    char2 = bytearr[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("malformed input around byte " + count);
                    }
                    chararr[chararrCount++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
                    break;

                case 14:
                    /* 1110 xxxx 10xx xxxx 10xx xxxx */
                    count += 3;
                    if (count > utflen) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    char2 = bytearr[count - 2];
                    char3 = bytearr[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new UTFDataFormatException("malformed input around byte " + (count - 1));
                    }
                    chararr[chararrCount++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F)));
                    break;

                default:
                    /* 10xx xxxx, 1111 xxxx */
                    throw new UTFDataFormatException("malformed input around byte " + count);
            }
        }

        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararrCount);
    }

    /**
     * An index data read result holder
     */
    public static class IndexDataReadResult {
        private Date timestamp;

        private int documentCount;

        private Set<String> rootGroups;

        private Set<String> allGroups;

        public void setDocumentCount(int documentCount) {
            this.documentCount = documentCount;
        }

        public int getDocumentCount() {
            return documentCount;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setRootGroups(Set<String> rootGroups) {
            this.rootGroups = rootGroups;
        }

        public Set<String> getRootGroups() {
            return rootGroups;
        }

        public void setAllGroups(Set<String> allGroups) {
            this.allGroups = allGroups;
        }

        public Set<String> getAllGroups() {
            return allGroups;
        }
    }

    /**
     * Reads index content by using a visitor. <br>
     * The visitor is called for each read documents after it has been populated with Lucene fields.
     *
     * @param visitor an index data visitor
     * @param context indexing context
     * @return statistics about read data
     * @throws IOException in case of an IO exception during index file access
     */
    public IndexDataReadResult readIndex(final IndexDataReadVisitor visitor, final IndexingContext context)
            throws IOException {
        dis.readByte(); // data format version

        long timestamp = dis.readLong();

        Date date = null;

        if (timestamp != -1) {
            date = new Date(timestamp);
        }

        int n = 0;

        Document doc;
        while ((doc = readDocument()) != null) {
            visitor.visitDocument(IndexUtils.updateDocument(doc, context, false));

            n++;
        }

        IndexDataReadResult result = new IndexDataReadResult();
        result.setDocumentCount(n);
        result.setTimestamp(date);
        return result;
    }

    /**
     * Visitor of indexed Lucene documents.
     */
    public interface IndexDataReadVisitor {

        /**
         * Called on each read document. The document is already populated with fields.
         *
         * @param document read document
         */
        void visitDocument(Document document);
    }
}
