package backend.chessmate.global.auth.config;

import backend.chessmate.global.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final User user;
    private final String id;
    private final String roleKey;

    public UserPrincipal(User u) {
        this.user = u;
        this.id = u.getLichessId();
        this.roleKey = u.getRole().getKey();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(roleKey));
    }

    @Override
    public String getPassword() {
        return null; // JWT 인증 방식이라면 패스워드 필요 없음
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // 🔥 추가: User 엔티티 직접 반환
    public User getUser() {
        return this.user;
    }
}
