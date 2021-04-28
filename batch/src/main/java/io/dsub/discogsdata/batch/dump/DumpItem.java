package io.dsub.discogsdata.batch.dump;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Getter
@Setter
@Slf4j
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DumpItem implements Comparable<DumpItem> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private DumpType dumpType;
    private String uri;
    private String eTag;
    private Long size;
    private LocalDateTime lastModified;

    @Transient
    @Builder.Default
    private InputStream inputStream = null;

    public String getResourceUrl() {
        return SimpleDumpFetcher.LAST_KNOWN_BUCKET_URL + "/" + this.uri;
    }

    public String getRootElementName() {
        return this.getDumpType().name().toLowerCase().replace("xml", "");
    }

    public synchronized InputStream getInputStream() throws IOException {
        if (inputStream == null) {
            setInputStream();
        }
        return inputStream;
    }

    public void setInputStream() throws IOException {
        Path path = Path.of(uri.split("/")[2]);
        inputStream = Files.newInputStream(path);
    }

    @Override
    public int compareTo(DumpItem that) {
        return this.lastModified
                .compareTo(that.getLastModified());
    }
}


