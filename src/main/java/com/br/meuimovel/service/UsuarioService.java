package com.br.meuimovel.service;

import com.br.meuimovel.exception.UsuarioNotFoundException;
import com.br.meuimovel.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = usuarioRepository.findByEmail(username);
        if(user == null){
            throw new UsuarioNotFoundException("Usuario nao encontrado");
        }
        return user;
    }
}
