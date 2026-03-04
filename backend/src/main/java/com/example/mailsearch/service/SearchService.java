package com.example.mailsearch.service;

import com.example.mailsearch.config.AppProperties;
import com.example.mailsearch.dto.DocumentDetailResponse;
import com.example.mailsearch.dto.SearchResponse;
import com.example.mailsearch.dto.SearchResultItem;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {
    private final AppProperties appProperties;

    public SearchService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public SearchResponse search(String q, String kind, int page, int size) {
        String queryText = (q == null || q.isBlank()) ? "*:*" : q;
        int start = Math.max(page, 0) * Math.max(size, 1);
        int limit = Math.max(size, 1);

        try (Directory directory = FSDirectory.open(Paths.get(appProperties.getIndexDir()))) {
            if (!DirectoryReader.indexExists(directory)) {
                return new SearchResponse(0, List.of());
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                Query textQuery;
                Query titleRawQuery = null;

                if ("*:*".equals(queryText)) {
                    textQuery = new org.apache.lucene.search.MatchAllDocsQuery();
                } else {
                    MultiFieldQueryParser parser = new MultiFieldQueryParser(
                            new String[]{
                                    IndexService.FIELD_TITLE,
                                    IndexService.FIELD_SUBJECT,
                                    IndexService.FIELD_FROM,
                                    IndexService.FIELD_TO,
                                    IndexService.FIELD_CONTENT
                            },
                            new StandardAnalyzer()
                    );
                    textQuery = parser.parse(QueryParser.escape(queryText));
                    String normalized = queryText.toLowerCase().replace("*", "").replace("?", "").trim();
                    if (!normalized.isEmpty()) {
                        titleRawQuery = new WildcardQuery(new Term(IndexService.FIELD_TITLE_RAW, "*" + normalized + "*"));
                    }
                }

                BooleanQuery.Builder builder = new BooleanQuery.Builder();
                if (titleRawQuery != null) {
                    builder.add(textQuery, BooleanClause.Occur.SHOULD);
                    builder.add(titleRawQuery, BooleanClause.Occur.SHOULD);
                    builder.setMinimumNumberShouldMatch(1);
                } else {
                    builder.add(textQuery, BooleanClause.Occur.MUST);
                }

                if (kind != null && !kind.isBlank() && !"all".equalsIgnoreCase(kind)) {
                    builder.add(new TermQuery(new Term(IndexService.FIELD_KIND, kind.toLowerCase())), BooleanClause.Occur.FILTER);
                }

                Query finalQuery = builder.build();
                TopDocs topDocs = searcher.search(finalQuery, start + limit);

                List<SearchResultItem> items = new ArrayList<>();
                ScoreDoc[] hits = topDocs.scoreDocs;
                for (int i = start; i < Math.min(hits.length, start + limit); i++) {
                    Document doc = searcher.doc(hits[i].doc);
                    String content = value(doc, IndexService.FIELD_CONTENT);
                    items.add(new SearchResultItem(
                            value(doc, IndexService.FIELD_ID),
                            value(doc, IndexService.FIELD_KIND),
                            value(doc, IndexService.FIELD_TITLE),
                            value(doc, IndexService.FIELD_SUBJECT),
                            value(doc, IndexService.FIELD_EML_PATH),
                            value(doc, IndexService.FIELD_ATTACHMENT_NAME),
                            Integer.parseInt(value(doc, IndexService.FIELD_ATTACHMENT_INDEX, "-1")),
                            snippet(content),
                            Boolean.parseBoolean(value(doc, IndexService.FIELD_IS_DRM, "false"))
                    ));
                }

                return new SearchResponse(topDocs.totalHits.value, items);
            }
        } catch (Exception e) {
            throw new RuntimeException("검색 중 오류가 발생했습니다.", e);
        }
    }

    public DocumentDetailResponse findById(String id) {
        try (Directory directory = FSDirectory.open(Paths.get(appProperties.getIndexDir()))) {
            if (!DirectoryReader.indexExists(directory)) {
                return null;
            }

            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs topDocs = searcher.search(new TermQuery(new Term(IndexService.FIELD_ID, id)), 1);

                if (topDocs.scoreDocs.length == 0) {
                    return null;
                }

                Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
                return new DocumentDetailResponse(
                        value(doc, IndexService.FIELD_ID),
                        value(doc, IndexService.FIELD_KIND),
                        value(doc, IndexService.FIELD_TITLE),
                        value(doc, IndexService.FIELD_SUBJECT),
                        value(doc, IndexService.FIELD_FROM),
                        value(doc, IndexService.FIELD_TO),
                        value(doc, IndexService.FIELD_EML_PATH),
                        value(doc, IndexService.FIELD_ATTACHMENT_NAME),
                        Integer.parseInt(value(doc, IndexService.FIELD_ATTACHMENT_INDEX, "-1")),
                        value(doc, IndexService.FIELD_CONTENT),
                        Boolean.parseBoolean(value(doc, IndexService.FIELD_IS_DRM, "false"))
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("문서 조회 중 오류가 발생했습니다.", e);
        }
    }

    private String snippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 260 ? normalized.substring(0, 260) + "..." : normalized;
    }

    private String value(Document document, String field) {
        return value(document, field, "");
    }

    private String value(Document document, String field, String fallback) {
        String value = document.get(field);
        return value == null ? fallback : value;
    }
}
