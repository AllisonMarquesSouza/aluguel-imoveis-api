package com.br.meuimovel.repository;

import com.br.meuimovel.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Usuario findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Integer id);
    List<Usuario> findAllByOrderByNomeAsc();
    List<Usuario> findAllByEmpresa_IdOrderByNomeAsc(Integer empresaId);
    Optional<Usuario> findByIdAndEmpresa_Id(Integer id, Integer empresaId);
}
