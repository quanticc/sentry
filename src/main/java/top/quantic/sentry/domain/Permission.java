package top.quantic.sentry.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import top.quantic.sentry.domain.enumeration.PermissionType;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Permission.
 */

@Document(collection = "permission")
public class Permission extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("type")
    private PermissionType type;

    @NotNull
    @Field("role")
    private String role;

    @NotNull
    @Field("operation")
    private String operation;

    @Field("resource")
    private String resource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PermissionType getType() {
        return type;
    }

    public Permission type(PermissionType type) {
        this.type = type;
        return this;
    }

    public void setType(PermissionType type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public Permission role(String role) {
        this.role = role;
        return this;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOperation() {
        return operation;
    }

    public Permission operation(String operation) {
        this.operation = operation;
        return this;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getResource() {
        return resource;
    }

    public Permission resource(String resource) {
        this.resource = resource;
        return this;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Permission permission = (Permission) o;
        if (permission.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, permission.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Permission{" +
            "id=" + id +
            ", type='" + type + "'" +
            ", role='" + role + "'" +
            ", operation='" + operation + "'" +
            ", resource='" + resource + "'" +
            '}';
    }
}
