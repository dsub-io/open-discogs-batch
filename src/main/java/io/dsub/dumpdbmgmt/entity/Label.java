package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.entity.nested.Catalog;
import io.dsub.dumpdbmgmt.entity.nested.Service;
import io.dsub.dumpdbmgmt.util.ArraysUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
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
    @Field(name = "catalogs")
    private Set<Catalog> catalogs = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "services")
    private Set<Service> services = Collections.synchronizedSet(new HashSet<>());

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

    public Label withAddCatalogs(Catalog... catalogs) {
        Set<Catalog> set =  Collections.synchronizedSet(new HashSet<>());
        set.addAll(this.catalogs);
        set.addAll(Arrays.asList(catalogs));
        return this.withCatalogs(set);
    }

    public Label withRemoveCatalog(Catalog catalog) {
        Set<Catalog> set =  Collections.synchronizedSet(new HashSet<>());
        set.addAll(catalogs);
        set.removeIf(entry -> entry.equals(catalog));
        return this.withCatalogs(set);
    }

    public Label withAddServices(Service... services) {
        Set<Service> set =  Collections.synchronizedSet(new HashSet<>());
        set.addAll(this.services);
        set.addAll(Arrays.asList(services));
        return this.withServices(set);
    }

    public Label withRemoveComapnyRelease(Service service) {
        Set<Service> set =  Collections.synchronizedSet(new HashSet<>());
        set.addAll(this.services);
        set.removeIf(entry -> entry.equals(service));
        return this.withServices(set);
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
