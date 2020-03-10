package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.util.ArraysUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
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
    @Field(name = "sub_labels")
    private Long[] subLabels = new Long[0];
    @Field(name = "parent_labels")
    private Long[] parentLabels = new Long[0];
    @Field(name = "urls")
    private Set<String> urls = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "releases_ids")
    private Set<ObjectId> labelReleases = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "company_releases")
    private Set<ObjectId> companyReleases = Collections.synchronizedSet(new HashSet<>());

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

    public Label withAddSubLabel(Long... labelIds) {
        Long[] arr = ArraysUtil.merge(this.subLabels, labelIds);
        return this.withSubLabels(arr);
    }

    public Label withRemoveSubLabel(Long labelId) {
        Long[] arr = ArraysUtil.remove(this.subLabels, labelId);
        return this.withSubLabels(arr);
    }

    public Label withAddParentLabel(Long... labelIds) {
        Long[] arr = ArraysUtil.merge(this.parentLabels, labelIds);
        return this.withParentLabels(arr);
    }

    public Label withRemoveParentLabel(Long labelId) {
        Long[] arr = ArraysUtil.remove(this.parentLabels, labelId);
        return this.withParentLabels(arr);
    }

    public Label withAddLabelRelease(ObjectId... labelReleaseIds) {
        Set<ObjectId> idSet =  Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(labelReleases);
        idSet.addAll(Arrays.asList(labelReleaseIds));
        return this.withLabelReleases(idSet);
    }

    public Label withRemoveLabelRelease(ObjectId labelReleaseId) {
        Set<ObjectId> idSet =  Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(labelReleases);
        idSet.removeIf(entry -> entry.equals(labelReleaseId));
        return this.withLabelReleases(idSet);
    }

    public Label withAddCompanyReleases(ObjectId... companyReleaseIds) {
        Set<ObjectId> idSet =  Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(companyReleases);
        idSet.addAll(Arrays.asList(companyReleaseIds));
        return this.withCompanyReleases(idSet);
    }

    public Label withRemoveComapnyRelease(ObjectId companyReleaseId) {
        Set<ObjectId> idSet =  Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(companyReleases);
        idSet.removeIf(entry -> entry.equals(companyReleaseId));
        return this.withCompanyReleases(idSet);
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
