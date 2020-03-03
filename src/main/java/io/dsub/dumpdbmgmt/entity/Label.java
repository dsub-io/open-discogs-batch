package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.entity.intermed.LabelRelease;
import io.dsub.dumpdbmgmt.entity.intermed.WorkRelease;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Getter
@With
@Document(collection = "labels")
public final class Label extends BaseEntity {
    @Id
    private final Long id;
    @Field(name = "name")
    @Indexed
    private final String name;
    @Field(name = "contact_info")
    private final String contactInfo;
    @Field(name = "data_quality")
    private final String dataQuality;
    @Field(name = "profile")
    private final String profile;
    @Field(name = "urls")
    private Set<String> urls = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<Label> subLabels = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<Label> parentLabels = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<LabelRelease> labelReleases = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<WorkRelease> workReleases = Collections.synchronizedSet(new HashSet<>());

    protected Label() {
        this.id = null;
        this.name = null;
        this.contactInfo = null;
        this.dataQuality = null;
        this.profile = null;
    }

    public Label(Long id) {
        this.id = id;
        this.name = null;
        this.contactInfo = null;
        this.dataQuality = null;
        this.profile = null;
    }

    public Label withAddUrls(String... urls) {
        Set<String> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.urls);
        modifiedSet.addAll(Arrays.asList(urls));
        return this.withUrls(modifiedSet);
    }

    public Label withRemoveUrls(String url) {
        Set<String> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.urls);
        modifiedSet.removeIf(candidate -> candidate.equals(url));
        return this.withUrls(modifiedSet);
    }

    public Label withAddSubLabel(Label... labels) {
        Set<Label> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.subLabels);
        modifiedSet.addAll(Arrays.asList(labels));
        return this.withSubLabels(modifiedSet);
    }

    public Label withRemoveSubLabel(Label label) {
        Set<Label> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.subLabels);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(label.getId()));
        return this.withSubLabels(modifiedSet);
    }

    public Label withAddLabelRelease(LabelRelease... labelReleases) {
        Set<LabelRelease> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.labelReleases);
        modifiedSet.addAll(Arrays.asList(labelReleases));
        return this.withLabelReleases(modifiedSet);
    }

    public Label withRemoveLabelRelease(LabelRelease labelRelease) {
        Set<LabelRelease> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.labelReleases);
        modifiedSet.removeIf(candidate -> candidate.equals(labelRelease));
        return this.withLabelReleases(modifiedSet);
    }

    public Label withAddWorkReleases(WorkRelease... workReleases) {
        Set<WorkRelease> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.workReleases);
        modifiedSet.addAll(Arrays.asList(workReleases));
        return this.withWorkReleases(modifiedSet);
    }

    public Label withRemoveWorkRelease(WorkRelease workRelease) {
        Set<WorkRelease> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.workReleases);
        modifiedSet.removeIf(candidate -> candidate.equals(workRelease));
        return this.withWorkReleases(modifiedSet);
    }

    public Label withAddParentLabel(Label... labels) {
        Set<Label> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.parentLabels);
        modifiedSet.addAll(Arrays.asList(labels));
        return this.withParentLabels(modifiedSet);
    }

    public Label withRemoveParentLabel(Label label) {
        Set<Label> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.parentLabels);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(label.getId()));
        return this.withParentLabels(modifiedSet);
    }


    @Override
    public String toString() {
        return "Label{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", contactInfo='" + contactInfo + '\'' +
                ", dataQuality='" + dataQuality + '\'' +
                '}';
    }
}
