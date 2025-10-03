package dk.northtech.dassco_specify_adapter.domain;

import java.util.Objects;

public record Role(
        String name) {
        @Override
        public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Role role = (Role) o;
                return Objects.equals(name, role.name);
        }


}
