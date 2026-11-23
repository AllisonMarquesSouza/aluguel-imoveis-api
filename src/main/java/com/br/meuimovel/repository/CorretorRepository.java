package com.br.meuimovel.repository;

import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.model.Corretor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorretorRepository extends JpaRepository<Corretor, Integer> {
    boolean existsByCreci(String creci);
    Optional<Corretor> findByIdAndFotoUrl(Integer id, String fotoUrl);
    Optional<Corretor> findByIdAndUsuario_Empresa_Id(Integer id, Integer usuarioEmpresaId);
    List<Corretor> findByUsuario_StatusOrderByUsuario_NomeAsc(UsuarioStatus status);
    List<Corretor> findByUsuario_Empresa_IdAndUsuario_StatusOrderByUsuario_NomeAsc(
            Integer empresaId, UsuarioStatus usuarioStatus);
    Optional<Corretor> findByIdAndUsuario_Status(Integer id, UsuarioStatus status);
}
