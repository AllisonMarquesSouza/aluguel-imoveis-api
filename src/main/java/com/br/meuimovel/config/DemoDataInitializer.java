package com.br.meuimovel.config;

import com.br.meuimovel.enums.TipoDocumento;
import com.br.meuimovel.enums.TipoEmpresa;
import com.br.meuimovel.enums.Uf;
import com.br.meuimovel.enums.UsuarioPerfil;
import com.br.meuimovel.enums.UsuarioStatus;
import com.br.meuimovel.model.Cidade;
import com.br.meuimovel.model.Corretor;
import com.br.meuimovel.model.Empresa;
import com.br.meuimovel.model.Usuario;
import com.br.meuimovel.repository.CidadeRepository;
import com.br.meuimovel.repository.CorretorRepository;
import com.br.meuimovel.repository.EmpresaRepository;
import com.br.meuimovel.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private static final String CIDADE_NOME = "Recife";
    private static final Uf CIDADE_UF = Uf.PE;
    private static final String EMPRESA_DOCUMENTO = "04252011000110";
    private static final String EMPRESA_CRECI = "12345-J";
    private static final String CORRETOR_CRECI = "54321-F";
    private static final String FOTO_CORRETOR_URL = "/demo-assets/corretor.svg";

    private final CidadeRepository cidadeRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorretorRepository corretorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.seed.administrator.email}")
    private String administratorEmail;

    @Value("${demo.seed.administrator.password}")
    private String administratorPassword;

    @Value("${demo.seed.broker.email}")
    private String brokerEmail;

    @Value("${demo.seed.broker.password}")
    private String brokerPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Cidade cidade = cidadeRepository.findByNomeIgnoreCaseAndUf(CIDADE_NOME, CIDADE_UF)
                .orElseGet(() -> cidadeRepository.save(new Cidade(CIDADE_NOME, CIDADE_UF)));

        criarAdministradorSeAusente();

        var empresaExistente = empresaRepository.findByDocumento(EMPRESA_DOCUMENTO);
        if (empresaExistente.isEmpty() && empresaRepository.existsByCreci(EMPRESA_CRECI)) {
            log.warn("CRECI reservado para a empresa de demonstração já está em uso; empresa e corretor não foram criados.");
            return;
        }

        Empresa empresa = empresaExistente.orElseGet(() -> criarEmpresa(cidade));
        criarCorretorSeAusente(empresa);
    }

    private Empresa criarEmpresa(Cidade cidade) {
        Empresa empresa = Empresa.builder()
                .tipo(TipoEmpresa.IMOBILIARIA)
                .nome("Imobiliária Demonstração Recife")
                .razaoSocial("Imobiliária Demonstração Recife Ltda.")
                .tipoDocumento(TipoDocumento.CNPJ)
                .documento(EMPRESA_DOCUMENTO)
                .creci(EMPRESA_CRECI)
                .descricao("Empresa de demonstração para desenvolvimento local.")
                .telefone("8133334444")
                .whatsapp("81999998888")
                .email("contato@imobiliaria-demo.test")
                .cidadeBase(cidade)
                .cidadesAtuacao(Set.of(cidade))
                .ativa(true)
                .criadoEm(LocalDateTime.now())
                .build();

        return empresaRepository.save(empresa);
    }

    private void criarAdministradorSeAusente() {
        if (usuarioRepository.existsByEmail(administratorEmail)) {
            log.info("Usuário de demonstração {} já existe; mantendo os dados atuais.", administratorEmail);
            return;
        }

        usuarioRepository.save(new Usuario(
                null,
                "Administrador de Demonstração",
                administratorEmail,
                passwordEncoder.encode(administratorPassword),
                UsuarioPerfil.ADMINISTRADOR,
                UsuarioStatus.ATIVO,
                LocalDateTime.now()
        ));
    }

    private void criarCorretorSeAusente(Empresa empresa) {
        if (usuarioRepository.existsByEmail(brokerEmail)) {
            log.info("Usuário de demonstração {} já existe; mantendo os dados atuais.", brokerEmail);
            return;
        }
        if (corretorRepository.existsByCreci(CORRETOR_CRECI)) {
            log.warn("CRECI reservado para o corretor de demonstração já está em uso; usuário e registro não foram criados.");
            return;
        }

        Usuario usuario = usuarioRepository.save(new Usuario(
                empresa,
                "Corretor de Demonstração",
                brokerEmail,
                passwordEncoder.encode(brokerPassword),
                UsuarioPerfil.CORRETOR,
                UsuarioStatus.ATIVO,
                LocalDateTime.now()
        ));

        corretorRepository.save(new Corretor(
                usuario,
                FOTO_CORRETOR_URL,
                CORRETOR_CRECI,
                "81999990000",
                "81999990000",
                "Atendimento de demonstração em Recife e região."
        ));
    }
}
