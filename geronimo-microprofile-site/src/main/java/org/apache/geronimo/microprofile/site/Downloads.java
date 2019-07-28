/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.microprofile.site;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.text.WordUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// regenerate when needed only, useless to do it for any site update
public class Downloads {

    private static final SAXParserFactory FACTORY = SAXParserFactory.newInstance();

    // always available once the release passed compared to central
    // note: if you have some stability issues (connection reset) then set that to MVN_BASE
    private static final String ASF_BASE = "https://repository.apache.org/content/repositories/releases/";

    // the entry point we want on the download page
    private static final String MVN_BASE = "http://repo.maven.apache.org/maven2/";

    private static final long KILO_RATION = 1024;

    static {
        FACTORY.setNamespaceAware(false);
        FACTORY.setValidating(false);
    }

    private Downloads() {
        // no-op
    }

    public static void main(final String[] args) {
        Locale.setDefault(Locale.ENGLISH);

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "256");

        final List<Download> downloads = Stream.of("org/apache/geronimo/config/geronimo-config-impl", "org/apache/geronimo/safeguard/safeguard-impl",
                "org/apache/geronimo/geronimo-jwt-auth", "org/apache/geronimo/geronimo-opentracing",
                "org/apache/geronimo/geronimo-health", "org/apache/geronimo/geronimo-metrics",
                "org/apache/geronimo/geronimo-openapi-impl", "org/apache/geronimo/geronimo-microprofile-aggregator",
                "org/apache/geronimo/utilda", "org/apache/geronimo/geronimo-microprofile-reporter")
                .flatMap(Downloads::toVersions)
                .flatMap(v -> v.base.endsWith("utilda") ?
                        Stream.of(v.classifiers("all").extensions("zip"), new Version(v.base, v.version).extensions("pom")) :
                        Stream.of(v))
                .map(v -> v.base.endsWith("geronimo-microprofile-aggregator") ? v.extensions("pom") : v)
                .map(v -> v.extensions.isEmpty() ? v.extensions("jar") : v)
                .flatMap(Downloads::toDownloadable).map(Downloads::fillDownloadable).filter(Objects::nonNull)
                .sorted(Downloads::versionComparator)
                .map(Downloads::toCentral)
                .collect(toList());

        final Collection<Download> lastReleases = downloads.stream()
                .collect(toMap(Download::getName, identity(), (a, b) -> a))
                .values().stream()
                .sorted(Downloads::versionComparator)
                .collect(toList());

        System.out.println("= Downloads\n:jbake-date: 2018-07-24\n:icons: font\n\n");
        System.out.println("== Last releases\n\n");
        System.out.println(tableHeader());
        lastReleases.forEach(Downloads::printRow);
        System.out.println(tableFooter());
        System.out.println();
        System.out.println("== Previous releases\n\n");
        downloads.stream()
                .filter(it -> !lastReleases.contains(it))
                .collect(groupingBy(Download::getName))
                .forEach((name, dls) -> {
                    System.out.println("=== " + name + "\n\n");
                    System.out.println(tableHeader());
                    dls.forEach(Downloads::printRow);
                    System.out.println(tableFooter());
                    System.out.println();
                });
        System.out.println();
    }

    private static String tableFooter() {
        return "|===";
    }

    private static String tableHeader() {
        return "[.table.table-bordered,options=\"header\"]\n|===\n|Name|Version|Date|Size|Type|Links";
    }

    private static int versionComparator(final Download o1, final Download o2) {
        final int formatComp = o2.format.compareTo(o1.format); // pom before jar
        if (formatComp != 0) {
            return formatComp;
        }

        final int nameComp = o1.name.compareTo(o2.name);
        if (nameComp != 0) {
            return nameComp;
        }

        final int versionComp = -compareVersions(o1.version, o2.version);
        if (versionComp != 0) {
            return versionComp;
        }

        final long dateComp = LocalDateTime.parse(o2.date, RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC)
                .toEpochMilli()
                - LocalDateTime.parse(o1.date, RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli();
        if (dateComp != 0) {
            return (int) dateComp;
        }

        return o1.url.compareTo(o2.url);
    }

    private static int compareVersions(final String version1, final String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        final int[] segments1 = parseVersion(version1);
        final int[] segments2 = parseVersion(version2);
        for (int i = 0; i < Math.min(segments1.length, segments2.length); i++) {
            if (segments1[i] < segments2[i]) {
                return -1;
            } else if (segments1[i] > segments2[i]) {
                return 1;
            }
        }
        return segments1.length - segments2.length; // can't be equal otherwise it would have exited at the first test
    }

    private static int[] parseVersion(final String version) {
        return Stream.of(version.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static Download toCentral(final Download download) {
        final Download dl = new Download(
                normalizeName(download.name),
                download.classifier,
                download.version,
                download.format,
                download.url.replace(ASF_BASE, MVN_BASE),
                download.sha1.replace(ASF_BASE, MVN_BASE),
                download.asc.replace(ASF_BASE, MVN_BASE));
        dl.date = download.date;
        dl.size = download.size;
        return dl;
    }

    private static String normalizeName(final String name) {
        String out = name;
        if (out.startsWith("Apache ")) {
            out = out.substring("Apache ".length());
        }
        if (!out.startsWith("Geronimo")) {
            out = "Geronimo " + out;
        }
        if (out.endsWith(" Impl")) {
            out = out.substring(0, out.length() - " Impl".length());
        }
        return out;
    }

    private static void printRow(final Download d) {
        System.out.println("|" + d.name + (d.classifier.isEmpty() ? "" : (" " + d.classifier)) + "|"
                + d.version + "|"
                + new SimpleDateFormat("d MMM yyyy")
                        .format(Date.from(LocalDateTime.parse(d.date, RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC)))
                + "|" + d.size + " kB " + "|" + d.format.toUpperCase() + "| " + d.url + "[icon:download[] "
                + d.format.toUpperCase() + "] " + d.sha1 + "[icon:download[] SHA1] ");
    }

    private static Download fillDownloadable(final Download download) {
        final int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                final URL url = new URL(download.url);
                final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection());
                connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(30));
                final int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    if (HttpURLConnection.HTTP_NOT_FOUND != responseCode) {
                        System.err.println("Got " + responseCode + " for " + download.url);
                    }
                    return null;
                }

                download.setDate(connection.getHeaderField("Last-Modified").replaceAll(" +", " "));
                download.setSize(toMega(ofNullable(connection.getHeaderField("Content-Length")).map(Long::parseLong).orElse(0L),
                        ofNullable(connection.getHeaderField("Accept-Ranges")).orElse("bytes")));

                connection.getInputStream().close();
                break;
            } catch (final IOException e) {
                if (i < maxRetries - 1) {
                    continue;
                }
                if (Boolean.getBoolean("debug")) {
                    e.printStackTrace();
                } else {
                    System.err.println("[ERROR] @fillDownload: " + e.getMessage() + " for " + download);
                }
                return null;
            }
        }
        return download;
    }

    private static long toMega(final long length, final String bytes) {
        if (!"bytes".equalsIgnoreCase(bytes)) {
            throw new IllegalArgumentException("Not handled unit: " + bytes);
        }
        return length / KILO_RATION;
    }

    private static Stream<Download> toDownloadable(final Version version) {
        final String base = version.base;
        final String artifactId = base.substring(base.lastIndexOf('/') + 1);
        final String artifactBase = version.base + "/" + version.version + "/" + artifactId + "-" + version.version;
        return version.extensions.stream()
                .flatMap(e -> (version.classifiers.isEmpty() ? Stream.of(new ArtifactDescription("", e))
                        : version.classifiers.stream().map(c -> new ArtifactDescription(c, e))))
                .map(a -> toDownload(artifactId, a.classifier, version.version, a.extension,
                        artifactBase + (a.classifier.isEmpty() ? '.' + a.extension : ('-' + a.classifier + '.' + a.extension))));
    }

    private static Download toDownload(final String artifactId, final String classifier, final String version,
            final String format, final String url) {
        return new Download(
                WordUtils.capitalize(artifactId.replace('-', ' ')),
                classifier, version, format, url, url + ".sha1", url + ".asc");
    }

    private static Stream<Version> toVersions(final String baseUrl) {
        final QuickMvnMetadataParser handler = new QuickMvnMetadataParser();
        final String base = ASF_BASE + baseUrl;
        try (final InputStream stream = new URL(base + "/maven-metadata.xml").openStream()) {
            final SAXParser parser = FACTORY.newSAXParser();
            parser.parse(stream, handler);
            return handler.foundVersions.stream().map(v -> new Version(base, v)).parallel();
        } catch (final Exception e) {
            if (Boolean.getBoolean("debug")) {
                e.printStackTrace();
            } else {
                System.err.println("[ERROR] @toVersions: " + e.getMessage() + " for " + baseUrl);
            }
            return Stream.empty();
        }
    }

    public static class Version {

        private final String base;

        private final String version;

        private final Collection<String> classifiers = new ArrayList<>();

        private final Collection<String> extensions = new ArrayList<>();

        public Version(final String base, final String version) {
            this.base = base;
            this.version = version;
        }

        private Version extensions(final String... values) {
            extensions.addAll(asList(values));
            return this;
        }

        private Version classifiers(final String... values) {
            classifiers.addAll(asList(values));
            return this;
        }

        public String getBase() {
            return base;
        }

        public String getVersion() {
            return version;
        }

        public Collection<String> getClassifiers() {
            return classifiers;
        }

        public Collection<String> getExtensions() {
            return extensions;
        }
    }

    public static class ArtifactDescription {

        private final String classifier;

        private final String extension;

        public ArtifactDescription(final String classifier, final String extension) {
            this.classifier = classifier;
            this.extension = extension;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getExtension() {
            return extension;
        }
    }

    public static class Download {

        private final String name;

        private final String classifier;

        private final String version;

        private final String format;

        private final String url;

        private final String sha1;

        private final String asc;

        private String date;

        private long size;

        public Download(final String name, final String classifier, final String version, final String format, final String url,
                final String sha1, final String asc) {
            this.name = name;
            this.classifier = classifier;
            this.version = version;
            this.format = format;
            this.url = url;
            this.sha1 = sha1;
            this.asc = asc;
        }

        public String getName() {
            return name;
        }

        public String getClassifier() {
            return classifier;
        }

        public String getVersion() {
            return version;
        }

        public String getFormat() {
            return format;
        }

        public String getUrl() {
            return url;
        }

        public String getSha1() {
            return sha1;
        }

        public String getAsc() {
            return asc;
        }

        public String getDate() {
            return date;
        }

        public void setDate(final String date) {
            this.date = date;
        }

        public long getSize() {
            return size;
        }

        public void setSize(final long size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "Download{" +
                    "name='" + name + '\'' +
                    ", classifier='" + classifier + '\'' +
                    ", version='" + version + '\'' +
                    ", format='" + format + '\'' +
                    ", url='" + url + '\'' +
                    ", sha1='" + sha1 + '\'' +
                    ", asc='" + asc + '\'' +
                    ", date='" + date + '\'' +
                    ", size=" + size +
                    '}';
        }
    }

    private static class QuickMvnMetadataParser extends DefaultHandler {

        private boolean versioning = false;

        private boolean versions = false;

        private StringBuilder version;

        private final Collection<String> foundVersions = new ArrayList<>();

        @Override
        public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
                throws SAXException {
            if ("versioning".equalsIgnoreCase(name)) {
                versioning = true;
            } else if ("versions".equalsIgnoreCase(name)) {
                versions = true;
            } else if (versioning && versions && "version".equalsIgnoreCase(name)) {
                version = new StringBuilder();
            }
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) {
            if (version != null) {
                version.append(new String(ch, start, length));
            }
        }

        public void endElement(final String uri, final String localName, final String name) {
            if ("versioning".equalsIgnoreCase(name)) {
                versioning = false;
            } else if ("versions".equalsIgnoreCase(name)) {
                versions = false;
            } else if ("version".equalsIgnoreCase(name)) {
                foundVersions.add(version.toString());
            }
        }
    }
}
