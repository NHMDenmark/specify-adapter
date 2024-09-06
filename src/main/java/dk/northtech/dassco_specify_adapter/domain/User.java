package dk.northtech.dassco_specify_adapter.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class User {
    public String username;
    public String token;
    public String keycloakId;
    public Set<String> roles = new HashSet<>();

    public User() {
    }

    public User(String username, Set<String> roles) {
        this.username = username;
        this.roles = roles;
    }

    public User(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username) && Objects.equals(roles, user.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, roles);
    }
}
