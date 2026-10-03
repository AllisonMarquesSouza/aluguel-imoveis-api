package com.br.meuimovel.repository;

import com.br.meuimovel.model.Proprietario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProprietarioRepository extends JpaRepository<Proprietario, Integer> {
    boolean existsByEmpresaIdAndDocumento(Integer empresaId, String documento);
    Optional<Proprietario> findByEmpresaIdAndDocumento(Integer empresaId, String documento);
    List<Proprietario> findByEmpresaIdAndNomeContainingIgnoreCase(
            Integer empresaId,
            String nome
    );

}
