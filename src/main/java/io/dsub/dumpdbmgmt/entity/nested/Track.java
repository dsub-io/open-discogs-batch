package io.dsub.dumpdbmgmt.entity.nested;

import io.dsub.dumpdbmgmt.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Objects;

@With
@Getter
@AllArgsConstructor
public final class Track extends BaseEntity {
    @Field(name = "title")
    @Indexed
    private final String title;
    @Field(name = "duration")
    private final String duration;
    @Field(name = "position")
    private final String position;

    public Track() {
        this.title = null;
        this.duration = null;
        this.position = null;
    }

    @Override
    public String toString() {
        return "Track{" +
                "title='" + title + '\'' +
                ", duration='" + duration + '\'' +
                ", position='" + position + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return Objects.equals(title, track.title) &&
                Objects.equals(duration, track.duration) &&
                Objects.equals(position, track.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, duration, position);
    }
}
