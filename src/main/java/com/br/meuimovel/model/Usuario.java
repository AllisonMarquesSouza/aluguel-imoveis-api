package com.br.meuimovel.model;

import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JoinColumn(name = "empresa_id")
    @ManyToOne
    private Empresa empresa;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private UsuarioPerfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UsuarioStatus status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public Usuario(Empresa empresa, String nome,
                   String email, String senha,
                   UsuarioPerfil perfil, UsuarioStatus status,
                   LocalDateTime criadoEm) {
        this.empresa = empresa;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
        this.status = status;
        this.criadoEm = criadoEm;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(this.perfil == UsuarioPerfil.ADMINISTRADOR)
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"),
                    new SimpleGrantedAuthority("ROLE_GESTOR"),
                    new SimpleGrantedAuthority("ROLE_CORRETOR"),
                    new SimpleGrantedAuthority("ROLE_CLIENTE"));
        else if (this.perfil == UsuarioPerfil.CORRETOR){
            return List.of(new SimpleGrantedAuthority("ROLE_CORRETOR"));
        }
        else if (this.perfil == UsuarioPerfil.GESTOR){
            return List.of(new SimpleGrantedAuthority("ROLE_GESTOR"));
        }

        return List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    //apenas usuários ativos podem fazer login
    @Override
    public boolean isEnabled() {
        return this.status == UsuarioStatus.ATIVO;
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public  String getUsername() {
        return this.email;
    }
}
