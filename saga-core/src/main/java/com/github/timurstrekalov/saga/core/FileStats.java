package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;

public class FileStats {

    private final URI baseUri;
    private final URI fileUri;
    private final List<LineCoverageRecord> lineCoverageRecords;
    private final boolean separateFile;

    // for stringtemplate
    private final String parentName;

    private final String id;

    FileStats(final URI baseUri, final URI fileUri, final List<LineCoverageRecord> lineCoverageRecords, final boolean separateFile) {
        this.baseUri = baseUri;
        this.fileUri = fileUri;
        this.separateFile = separateFile;

        parentName = new File(getRelativeName()).getParent();

        this.id = generateId();
        this.lineCoverageRecords = lineCoverageRecords;
    }

    private String generateId() {
        return Hashing.md5().hashString(fileUri.toString()).toString();
    }

    public List<LineCoverageRecord> getLineCoverageRecords() {
        return lineCoverageRecords;
    }

    public Collection<LineCoverageRecord> getExecutableLineCoverageRecords() {
        return Collections2.filter(lineCoverageRecords, new Predicate<LineCoverageRecord>() {
            @Override
            public boolean apply(final LineCoverageRecord record) {
                return record.isExecutable();
            }
        });
    }

    public int getStatements() {
        return Collections2.filter(lineCoverageRecords, new Predicate<LineCoverageRecord>() {
            @Override
            public boolean apply(final LineCoverageRecord input) {
                return input.isExecutable();
            }
        }).size();
    }

    public int getExecuted() {
        return MiscUtil.sum(lineCoverageRecords, new Function<LineCoverageRecord, Integer>() {

            @Override
            public Integer apply(final LineCoverageRecord input) {
                return input.getTimesExecuted() > 0 ? 1 : 0;
            }
        });
    }

    public int getCoverage() {
        return MiscUtil.toCoverage(getStatements(), getExecuted());
    }

    public boolean getHasStatements() {
        return getStatements() > 0;
    }

    public String getBarColor() {
        return MiscUtil.getColor(getCoverage());
    }

    public int getBarColorAsArgb() {
        return MiscUtil.getColorAsArgb(getCoverage());
    }

    static FileStats merge(final FileStats s1, final FileStats s2) {
        final List<LineCoverageRecord> r1 = s1.getLineCoverageRecords();
        final List<LineCoverageRecord> r2 = s2.getLineCoverageRecords();

        Preconditions.checkArgument(s1.fileUri.equals(s2.fileUri), "Got different file names: %s and %s", s1, s2);
        Preconditions.checkArgument(r1.size() == r2.size(), "Got different numbers of line coverage records: %s and %s", s1, s2);

        final List<LineCoverageRecord> mergedRecords = Lists.newLinkedList();

        for (int i = 0; i < r1.size(); i++) {
            final LineCoverageRecord l1 = r1.get(i);
            final LineCoverageRecord l2 = r2.get(i);

            try {
                mergedRecords.add(LineCoverageRecord.merge(l1, l2));
            } catch (final Exception e) {
                throw new RuntimeException("Error merging " + s1.fileUri + " and " + s2.fileUri, e);
            }
        }

        return new FileStats(s1.baseUri, s1.fileUri, mergedRecords, s1.separateFile);
    }

    public URI getFileUri() {
        return fileUri;
    }

    public String getFileName() {
        return UriUtil.getLastSegmentOrHost(fileUri);
    }

    public String getFullName() {
        return UriUtil.getPath(fileUri);
    }

    public String getRelativeName() {
        return isSeparateFile()
                ? ResourceUtil.getRelativePath(fileUri.toString(), baseUri.toString(), "/")
                : fileUri.toString();
    }

    public String getParentName() {
        return parentName;
    }

    public String getId() {
        return id;
    }

    public boolean isSeparateFile() {
        return separateFile;
    }

    @Override
    public String toString() {
        return fileUri.toString();
    }

}
