package com.pucpr.tcc.monolith.testsupport;

import com.pucpr.tcc.monolith.security.JwtService;
import com.pucpr.tcc.monolith.users.entity.User;
import com.pucpr.tcc.monolith.users.entity.UserRole;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

/**
 * Helper reutilizado pelos testes de slice ({@code @WebMvcTest}) de Controller.
 *
 * Gera um usuário de teste com id definido via reflexão (a entidade User não
 * expõe setId — o id só existe depois de persistido) e emite um token JWT
 * <b>real</b>, assinado pelo {@link JwtService} de produção injetado no
 * contexto do slice. O {@link com.pucpr.tcc.monolith.security.JwtAuthenticationFilter}
 * processa esse token exatamente como em produção; só o
 * {@link UserDetailsService} é mockado, para devolver esse mesmo usuário sem
 * precisar de um banco de dados — é esse objeto que chega como
 * {@code @AuthenticationPrincipal} no Controller.
 */
public final class SecurityTestSupport {

    private SecurityTestSupport() {}

    public static User adminUser(long id) {
        return withId(new User("Admin de Teste", "admin.slice@test.com", "hash", UserRole.ADMIN), id);
    }

    public static User customerUser(long id) {
        return withId(new User("Cliente de Teste", "cliente.slice@test.com", "hash", UserRole.CUSTOMER), id);
    }

    public static User withId(User user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /**
     * Registra o usuário no {@link UserDetailsService} mockado e devolve o
     * header {@code Authorization: Bearer <token real>} pronto para uso em
     * {@code mockMvc.perform(...).header("Authorization", bearerTokenFor(...))}.
     */
    public static String bearerTokenFor(JwtService jwtService, UserDetailsService userDetailsService, User user) {
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
        return "Bearer " + jwtService.generateToken(user);
    }
}
