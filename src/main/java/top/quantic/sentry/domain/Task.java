package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.*;

/**
 * A Task.
 */

@Document(collection = "task")
public class Task extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("name")
    private String name;

    @NotNull
    @Field("group")
    private String group;

    @Field("description")
    private String description;

    @NotNull
    @Field("job_class")
    private String jobClass;

    @Field("data_map")
    private Map<String, Object> dataMap = new HashMap<>();

    @Field("triggers")
    private List<String> triggers = new ArrayList<>();

    @NotNull
    @Field("enabled")
    private Boolean enabled = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Task name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public Task group(String group) {
        this.group = group;
        return this;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public Task description(String description) {
        this.description = description;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobClass() {
        return jobClass;
    }

    public Task jobClass(String jobClass) {
        this.jobClass = jobClass;
        return this;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    public Task dataMap(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
        return this;
    }

    public void setDataMap(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
    }

    public List<String> getTriggers() {
        return triggers;
    }

    public Task triggers(List<String> triggers) {
        this.triggers = triggers;
        return this;
    }

    public void setTriggers(List<String> triggers) {
        this.triggers = triggers;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Task enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task task = (Task) o;
        if (task.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Task{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", group='" + group + "'" +
            ", description='" + description + "'" +
            ", jobClass='" + jobClass + "'" +
            ", dataMap=" + dataMap +
            ", triggers=" + triggers +
            ", enabled='" + enabled + "'" +
            '}';
    }
}
