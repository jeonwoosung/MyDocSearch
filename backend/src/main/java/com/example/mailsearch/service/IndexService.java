package com.example.mailsearch.service;

import com.example.mailsearch.config.AppProperties;
import com.example.mailsearch.dto.DrmFileItemResponse;
import com.example.mailsearch.dto.IndexOperationResponse;
import com.example.mailsearch.dto.IndexStatusResponse;
import com.example.mailsearch.model.AttachmentInfo;
import com.example.mailsearch.model.EmlParseResult;
import com.example.mailsearch.util.EmlParser;
import jakarta.annotation.PostConstruct;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.Tika;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class IndexService {
    public static final String FIELD_ID = "id";
    public static final String FIELD_KIND = "kind";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_TITLE_RAW = "titleRaw";
    public static final String FIELD_SUBJECT = "subject";
    public static final String FIELD_FROM = "from";
    public static final String FIELD_TO = "to";
    public static final String FIELD_EML_PATH = "emlPath";
    public static final String FIELD_ATTACHMENT_NAME = "attachmentName";
    public static final String FIELD_ATTACHMENT_INDEX = "attachmentIndex";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_LAST_MODIFIED = "lastModified";
    public static final String FIELD_IS_DRM = "isDrm";

    public static final String KIND_EML = "eml";
    public static final String KIND_ATTACHMENT = "attachment";
    public static final String KIND_FILE = "file";

    private static final Set<String> INDEXABLE_EXTENSIONS = Set.of(
            "eml", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
            "hwp", "hwpx", "csv", "rtf", "odt", "ods", "odp", "xml", "html",
            "htm", "msg"
    );

    private final AppProperties appProperties;
    private final EmlParser emlParser;
    private final Tika tika = new Tika();
    private final JdbcTemplate jdbcTemplate;
    private final Object lock = new Object();
    private volatile Instant lastIndexedAt;
    private volatile Instant lastRebuildAt;
    private volatile Instant lastIncrementalAt;

    public IndexService(AppProperties appProperties, EmlParser emlParser, JdbcTemplate jdbcTemplate) {
        this.appProperties = appProperties;
        this.emlParser = emlParser;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(appProperties.getIndexDir()));
        jdbcTemplate.execute("""
                create table if not exists app_metadata (
                    meta_key varchar(100) primary key,
                    meta_value varchar(200) not null
                )
                """);
        lastRebuildAt = loadInstant("lastRebuildAt");
        lastIncrementalAt = loadInstant("lastIncrementalAt");
        lastIndexedAt = loadLatestIndexInstant();
    }

    public IndexOperationResponse rebuild(String requestedRootPath) {
        synchronized (lock) {
            Path root = resolveRoot(requestedRootPath);
            List<Path> files = listIndexableFiles(root);
            int emlCount = 0;
            int attachmentCount = 0;
            int fileCount = 0;
            int skippedCount = 0;

            try (Directory directory = openDirectory();
                 IndexWriter writer = new IndexWriter(directory, writerConfig())) {
                writer.deleteAll();

                for (Path path : files) {
                    try {
                        IndexCounts counts = indexSinglePath(writer, path);
                        emlCount += counts.emlCount;
                        attachmentCount += counts.attachmentCount;
                        fileCount += counts.fileCount;
                    } catch (Exception singleFileError) {
                        skippedCount++;
                    }
                }
                writer.commit();
            } catch (Exception e) {
                throw new RuntimeException("색인 재구성 중 오류가 발생했습니다. 원인: " + rootCauseMessage(e), e);
            }

            lastRebuildAt = Instant.now();
            lastIndexedAt = lastRebuildAt;
            saveInstant("lastRebuildAt", lastRebuildAt);
            return new IndexOperationResponse(
                    "색인을 재구성했습니다. (건너뜀: " + skippedCount + ")",
                    emlCount,
                    attachmentCount,
                    fileCount
            );
        }
    }

    public IndexOperationResponse update(String requestedRootPath) {
        synchronized (lock) {
            Path root = resolveRoot(requestedRootPath);
            List<Path> files = listIndexableFiles(root);
            Set<String> currentPaths = new HashSet<>();
            files.forEach(p -> currentPaths.add(p.toString()));

            int emlCount = 0;
            int attachmentCount = 0;
            int fileCount = 0;
            int skippedCount = 0;

            try (Directory directory = openDirectory();
                 IndexWriter writer = new IndexWriter(directory, writerConfig())) {

                removeDeletedDocuments(writer, currentPaths);

                for (Path path : files) {
                    String kind = kindForPath(path);
                    long fileLastModified = Files.getLastModifiedTime(path).toMillis();
                    Long indexedLastModified = findIndexedLastModified(directory, path.toString(), kind);

                    if (indexedLastModified == null || indexedLastModified < fileLastModified) {
                        writer.deleteDocuments(new Term(FIELD_EML_PATH, path.toString()));
                        try {
                            IndexCounts counts = indexSinglePath(writer, path);
                            emlCount += counts.emlCount;
                            attachmentCount += counts.attachmentCount;
                            fileCount += counts.fileCount;
                        } catch (Exception singleFileError) {
                            skippedCount++;
                        }
                    }
                }

                writer.commit();
            } catch (Exception e) {
                throw new RuntimeException("색인 갱신 중 오류가 발생했습니다. 원인: " + rootCauseMessage(e), e);
            }

            lastIncrementalAt = Instant.now();
            lastIndexedAt = loadLatestIndexInstant();
            saveInstant("lastIncrementalAt", lastIncrementalAt);
            return new IndexOperationResponse(
                    "변경 파일 기준으로 색인을 갱신했습니다. (건너뜀: " + skippedCount + ")",
                    emlCount,
                    attachmentCount,
                    fileCount
            );
        }
    }

    public IndexOperationResponse deleteAll() {
        synchronized (lock) {
            try (Directory directory = openDirectory();
                 IndexWriter writer = new IndexWriter(directory, writerConfig())) {
                writer.deleteAll();
                writer.commit();
            } catch (Exception e) {
                throw new RuntimeException("색인 삭제 중 오류가 발생했습니다. 원인: " + rootCauseMessage(e), e);
            }
            lastIndexedAt = Instant.now();
            return new IndexOperationResponse("색인을 모두 삭제했습니다.", 0, 0, 0);
        }
    }

    public IndexStatusResponse status() {
        synchronized (lock) {
            int emlCount = 0;
            int attachmentCount = 0;
            int fileCount = 0;

            try (Directory directory = openDirectory()) {
                if (!DirectoryReader.indexExists(directory)) {
                    return new IndexStatusResponse(appProperties.getEmlRoot(), appProperties.getIndexDir(), 0, 0, 0, lastIndexedAt, lastRebuildAt, lastIncrementalAt);
                }

                try (IndexReader reader = DirectoryReader.open(directory)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    emlCount = Math.toIntExact(searcher.count(new TermQuery(new Term(FIELD_KIND, KIND_EML))));
                    attachmentCount = Math.toIntExact(searcher.count(new TermQuery(new Term(FIELD_KIND, KIND_ATTACHMENT))));
                    fileCount = Math.toIntExact(searcher.count(new TermQuery(new Term(FIELD_KIND, KIND_FILE))));
                }
            } catch (Exception e) {
                throw new RuntimeException("색인 상태 조회 중 오류가 발생했습니다. 원인: " + rootCauseMessage(e), e);
            }

            return new IndexStatusResponse(appProperties.getEmlRoot(), appProperties.getIndexDir(), emlCount, attachmentCount, fileCount, lastIndexedAt, lastRebuildAt, lastIncrementalAt);
        }
    }

    public List<DrmFileItemResponse> drmFiles() {
        synchronized (lock) {
            try (Directory directory = openDirectory()) {
                if (!DirectoryReader.indexExists(directory)) {
                    return List.of();
                }

                try (IndexReader reader = DirectoryReader.open(directory)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    BooleanQuery.Builder builder = new BooleanQuery.Builder();
                    builder.add(new TermQuery(new Term(FIELD_KIND, KIND_FILE)), BooleanClause.Occur.FILTER);
                    builder.add(new TermQuery(new Term(FIELD_IS_DRM, "true")), BooleanClause.Occur.FILTER);

                    TopDocs topDocs = searcher.search(builder.build(), reader.maxDoc());
                    List<DrmFileItemResponse> items = new java.util.ArrayList<>();
                    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                        Document doc = searcher.doc(scoreDoc.doc);
                        items.add(new DrmFileItemResponse(
                                doc.get(FIELD_ID),
                                doc.get(FIELD_TITLE),
                                doc.get(FIELD_EML_PATH),
                                Long.parseLong(doc.get(FIELD_LAST_MODIFIED))
                        ));
                    }
                    return items;
                }
            } catch (Exception e) {
                throw new RuntimeException("DRM 파일 목록 조회 중 오류가 발생했습니다. 원인: " + rootCauseMessage(e), e);
            }
        }
    }

    private void removeDeletedDocuments(IndexWriter writer, Set<String> existingPaths) throws Exception {
        try (Directory directory = openDirectory()) {
            if (!DirectoryReader.indexExists(directory)) {
                return;
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs docs = searcher.search(new MatchAllDocsQuery(), reader.maxDoc());

                for (ScoreDoc scoreDoc : docs.scoreDocs) {
                    Document doc = searcher.doc(scoreDoc.doc);
                    String sourcePath = doc.get(FIELD_EML_PATH);
                    if (sourcePath != null && !existingPaths.contains(sourcePath)) {
                        writer.deleteDocuments(new Term(FIELD_EML_PATH, sourcePath));
                    }
                }
            }
        }
    }

    private Long findIndexedLastModified(Directory directory, String sourcePath, String kind) throws Exception {
        if (!DirectoryReader.indexExists(directory)) {
            return null;
        }

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new TermQuery(new Term(FIELD_ID, idFor(sourcePath, kind, -1)));
            TopDocs docs = searcher.search(query, 1);
            if (docs.scoreDocs.length == 0) {
                return null;
            }
            Document doc = searcher.doc(docs.scoreDocs[0].doc);
            String value = doc.get(FIELD_LAST_MODIFIED);
            return value == null ? null : Long.parseLong(value);
        }
    }

    private IndexCounts indexSinglePath(IndexWriter writer, Path path) throws Exception {
        if ("eml".equals(extensionOf(path))) {
            return indexSingleEml(writer, path);
        }
        return indexSingleFile(writer, path);
    }

    private IndexCounts indexSingleEml(IndexWriter writer, Path emlPath) throws Exception {
        EmlParseResult parsed = emlParser.parse(emlPath);
        long modifiedMillis = Files.getLastModifiedTime(emlPath).toMillis();

        Document emlDoc = baseDoc(emlPath, KIND_EML, -1, modifiedMillis, false);
        String emlTitle = fileName(emlPath);
        emlDoc.add(new TextField(FIELD_TITLE, emlTitle, Field.Store.YES));
        emlDoc.add(new StringField(FIELD_TITLE_RAW, emlTitle.toLowerCase(), Field.Store.YES));
        emlDoc.add(new TextField(FIELD_SUBJECT, nullSafe(parsed.getSubject()), Field.Store.YES));
        emlDoc.add(new TextField(FIELD_FROM, nullSafe(parsed.getFrom()), Field.Store.YES));
        emlDoc.add(new TextField(FIELD_TO, nullSafe(parsed.getTo()), Field.Store.YES));
        emlDoc.add(new TextField(FIELD_CONTENT, nullSafe(parsed.getBodyText()), Field.Store.YES));
        emlDoc.add(new StoredField(FIELD_ATTACHMENT_NAME, ""));
        writer.addDocument(emlDoc);

        int attachmentCount = 0;
        for (AttachmentInfo attachment : parsed.getAttachments()) {
            Document attachmentDoc = baseDoc(emlPath, KIND_ATTACHMENT, attachment.getIndex(), modifiedMillis, false);
            String attachmentTitle = nullSafe(attachment.getName());
            attachmentDoc.add(new TextField(FIELD_TITLE, attachmentTitle, Field.Store.YES));
            attachmentDoc.add(new StringField(FIELD_TITLE_RAW, attachmentTitle.toLowerCase(), Field.Store.YES));
            attachmentDoc.add(new TextField(FIELD_SUBJECT, nullSafe(parsed.getSubject()), Field.Store.YES));
            attachmentDoc.add(new TextField(FIELD_FROM, nullSafe(parsed.getFrom()), Field.Store.YES));
            attachmentDoc.add(new TextField(FIELD_TO, nullSafe(parsed.getTo()), Field.Store.YES));
            attachmentDoc.add(new TextField(FIELD_CONTENT, nullSafe(attachment.getExtractedText()), Field.Store.YES));
            attachmentDoc.add(new StoredField(FIELD_ATTACHMENT_NAME, nullSafe(attachment.getName())));
            writer.addDocument(attachmentDoc);
            attachmentCount++;
        }

        return new IndexCounts(1, attachmentCount, 0);
    }

    private IndexCounts indexSingleFile(IndexWriter writer, Path filePath) throws Exception {
        long modifiedMillis = Files.getLastModifiedTime(filePath).toMillis();
        boolean isDrm = isDrmFile(filePath);

        String extractedText = "";
        if (!isDrm) {
            try {
                if (Files.size(filePath) <= appProperties.getMaxExtractBytes()) {
                    extractedText = nullSafe(extractTextSafely(filePath));
                }
            } catch (Exception ignored) {
                extractedText = "";
            }
        }

        Document fileDoc = baseDoc(filePath, KIND_FILE, -1, modifiedMillis, isDrm);
        String title = fileName(filePath);
        fileDoc.add(new TextField(FIELD_TITLE, title, Field.Store.YES));
        fileDoc.add(new StringField(FIELD_TITLE_RAW, title.toLowerCase(), Field.Store.YES));
        fileDoc.add(new TextField(FIELD_SUBJECT, "", Field.Store.YES));
        fileDoc.add(new TextField(FIELD_FROM, "", Field.Store.YES));
        fileDoc.add(new TextField(FIELD_TO, "", Field.Store.YES));
        fileDoc.add(new TextField(FIELD_CONTENT, extractedText, Field.Store.YES));
        fileDoc.add(new StoredField(FIELD_ATTACHMENT_NAME, ""));
        writer.addDocument(fileDoc);

        return new IndexCounts(0, 0, 1);
    }

    private Document baseDoc(Path path, String kind, int attachmentIndex, long modifiedMillis, boolean isDrm) {
        Document doc = new Document();
        doc.add(new StringField(FIELD_ID, idFor(path.toString(), kind, attachmentIndex), Field.Store.YES));
        doc.add(new StringField(FIELD_KIND, kind, Field.Store.YES));
        doc.add(new StringField(FIELD_EML_PATH, path.toString(), Field.Store.YES));
        doc.add(new StringField(FIELD_ATTACHMENT_INDEX, String.valueOf(attachmentIndex), Field.Store.YES));
        doc.add(new StringField(FIELD_LAST_MODIFIED, String.valueOf(modifiedMillis), Field.Store.YES));
        doc.add(new StringField(FIELD_IS_DRM, String.valueOf(isDrm), Field.Store.YES));
        return doc;
    }

    private boolean isDrmFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] head = in.readNBytes(5);
            return head.length == 5 && "SCDSA".equals(new String(head, StandardCharsets.US_ASCII));
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<Path> listIndexableFiles(Path root) {
        if (!Files.exists(root)) {
            throw new IllegalArgumentException("색인 루트 경로가 존재하지 않습니다: " + root);
        }

        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> INDEXABLE_EXTENSIONS.contains(extensionOf(path)))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("대상 파일 탐색 중 오류가 발생했습니다.", e);
        }
    }

    private String extensionOf(Path path) {
        String fileName = fileName(path);
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private String kindForPath(Path path) {
        return "eml".equals(extensionOf(path)) ? KIND_EML : KIND_FILE;
    }

    private Directory openDirectory() throws IOException {
        return FSDirectory.open(Paths.get(appProperties.getIndexDir()));
    }

    private IndexWriterConfig writerConfig() {
        return new IndexWriterConfig(new StandardAnalyzer());
    }

    private Path resolveRoot(String requestedRootPath) {
        if (requestedRootPath == null || requestedRootPath.isBlank()) {
            return Paths.get(appProperties.getEmlRoot());
        }
        return Paths.get(requestedRootPath);
    }

    private String fileName(Path path) {
        return path.getFileName() == null ? path.toString() : path.getFileName().toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String extractTextSafely(Path filePath) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(appProperties.getMaxExtractChars());
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        try (InputStream in = Files.newInputStream(filePath)) {
            parser.parse(in, handler, metadata, context);
        } catch (SAXException e) {
            // Partial extraction is acceptable for indexing and prevents runaway memory use.
        }

        return handler.toString();
    }

    private Instant loadInstant(String key) {
        try {
            String value = jdbcTemplate.queryForObject(
                    "select meta_value from app_metadata where meta_key = ?",
                    String.class,
                    key
            );
            return value == null ? null : Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void saveInstant(String key, Instant value) {
        jdbcTemplate.update(
                "merge into app_metadata (meta_key, meta_value) key(meta_key) values (?, ?)",
                key,
                value.toString()
        );
    }

    private Instant loadLatestIndexInstant() {
        if (lastRebuildAt == null) {
            return lastIncrementalAt;
        }
        if (lastIncrementalAt == null) {
            return lastRebuildAt;
        }
        return lastRebuildAt.isAfter(lastIncrementalAt) ? lastRebuildAt : lastIncrementalAt;
    }

    private String idFor(String sourcePath, String kind, int attachmentIndex) {
        return UUID.nameUUIDFromBytes((sourcePath + "|" + kind + "|" + attachmentIndex).getBytes()).toString();
    }

    private record IndexCounts(int emlCount, int attachmentCount, int fileCount) {
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
