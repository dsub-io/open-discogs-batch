package io.dsub.discogs.batch.query;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.dsub.discogs.batch.query.JpaEntityExtractorTest.TestEntity;
import io.dsub.discogs.common.entity.artist.Artist;
import io.dsub.discogs.common.entity.artist.ArtistMember;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.entity.base.BaseTimeEntity;
import io.dsub.discogs.common.entity.release.ReleaseItemTrack;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;

class PostgresqlJpaEntityQueryBuilderTest {

  PostgresqlJpaEntityQueryBuilder builder = new PostgresqlJpaEntityQueryBuilder();


}
