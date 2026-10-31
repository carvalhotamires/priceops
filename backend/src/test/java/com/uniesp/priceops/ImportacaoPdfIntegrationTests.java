package com.uniesp.priceops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ImportacaoPdfRepository;
import com.uniesp.priceops.repository.LogAuditoriaPdfRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "priceops.upload.dir=target/test-uploads-pdf")
@AutoConfigureMockMvc
class ImportacaoPdfIntegrationTests {

    private static final Path PASTA_TESTE = Paths.get("target/test-uploads-pdf");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportacaoPdfRepository importacaoPdfRepository;

    @Autowired
    private LogAuditoriaPdfRepository logAuditoriaPdfRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void prepararAmbiente() throws IOException {
        limparDadosEPasta();
    }

    @AfterEach
    void limparAmbienteAposTeste() throws IOException {
        limparDadosEPasta();
    }

    private void limparDadosEPasta() throws IOException {
        importacaoPdfRepository.deleteAll();
        logAuditoriaPdfRepository.deleteAll();
        fornecedorRepository.deleteAll();

        if (Files.exists(PASTA_TESTE)) {
            try (Stream<Path> caminhos = Files.walk(PASTA_TESTE)) {
                caminhos.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    private Fornecedor salvarDistribuidor(String cnpj, String nomeFantasia, String status) {
        Fornecedor f = new Fornecedor();
        f.setRazaoSocial(nomeFantasia + " Distribuidora de Informática S.A.");
        f.setNomeFantasia(nomeFantasia);
        f.setCnpj(cnpj);
        f.setEmail("cotacoes@" + nomeFantasia.toLowerCase().replace(" ", "") + ".com.br");
        f.setTelefone("(11) 3500-9900");
        f.setCidade("São Paulo");
        f.setUf("SP");
        f.setPrazoEntregaDias(4);
        f.setStatus(status);
        return fornecedorRepository.save(f);
    }

    private byte[] gerarConteudoPdfValido(String textoInterno) {
        String pdfMinimo = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "% Tabela Informatica: " + textoInterno + "\n"
                + "%%EOF\n";
        return pdfMinimo.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void deveImportarTabelaPdfDeDistribuidorAtivoComSha256EAuditoria() throws Exception {
        Fornecedor distribuidorAtivo = salvarDistribuidor("68552215000180", "All Nations", "ATIVO");
        byte[] pdfBytes = gerarConteudoPdfValido("INF-01 Mouse Optico USB R$ 18,50 | INF-02 Cabo HDMI 2.0 R$ 14,00");

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "tabela_perifericos_allnations.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                pdfBytes
        );

        mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(arquivo)
                        .param("fornecedorId", distribuidorAtivo.getId().toString())
                        .param("usuarioResponsavel", "Tamires")
                        .param("observacao", "Tabela atacado periféricos Outubro"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nomeOriginal", is("tabela_perifericos_allnations.pdf")))
                .andExpect(jsonPath("$.hashSha256", matchesPattern("^[a-f0-9]{64}$")))
                .andExpect(jsonPath("$.statusImportacao", is("RECEBIDO")))
                .andExpect(jsonPath("$.fornecedor.nomeFantasia", is("All Nations")));

        mockMvc.perform(get("/api/importacoes-pdf/auditoria")
                        .param("status", "UPLOAD_SUCESSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].dataHora", notNullValue()));
    }

    @Test
    void deveBloquearReimportacaoIdenticaParaMesmoFornecedorCom409ConflictETrilhaDeAuditoria() throws Exception {
        Fornecedor pauta = salvarDistribuidor("83878959000105", "Pauta Distribuidora", "ATIVO");
        byte[] mesmoConteudoPdf = gerarConteudoPdfValido("INF-03 Teclado ABNT2 R$ 32,00 | INF-04 Pendrive 64GB CX R$ 210,00");

        MockMultipartFile primeiroArquivo = new MockMultipartFile(
                "arquivo",
                "tabela_pauta_v1.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                mesmoConteudoPdf
        );

        MvcResult original = mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(primeiroArquivo)
                        .param("fornecedorId", pauta.getId().toString()))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonOriginal = objectMapper.readTree(original.getResponse().getContentAsString());
        String hashOriginal = jsonOriginal.get("hashSha256").asText();

        // Tenta reimportar tabela idêntica (mesmo binário SHA-256) para o mesmo distribuidor
        MockMultipartFile duplicadoRenomeado = new MockMultipartFile(
                "arquivo",
                "copia_tabela_pauta.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                mesmoConteudoPdf
        );

        mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(duplicadoRenomeado)
                        .param("fornecedorId", pauta.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro", is("DUPLICIDADE_IMPORTACAO_PDF")))
                .andExpect(jsonPath("$.hashSha256", is(hashOriginal)));

        // Confirma trilha imutável de auditoria registrando data, hora e status DUPLICIDADE_BLOQUEADA
        mockMvc.perform(get("/api/importacoes-pdf/auditoria")
                        .param("status", "DUPLICIDADE_BLOQUEADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].hashSha256", is(hashOriginal)))
                .andExpect(jsonPath("$[0].dataHora", notNullValue()));
    }

    @Test
    void deveBloquearUploadSemFornecedorOuComDistribuidorInativoOuExtensaoInvalida() throws Exception {
        Fornecedor inativo = salvarDistribuidor("11222333000181", "Aldo Componentes", "INATIVO");
        byte[] pdfValido = gerarConteudoPdfValido("INF-05 Headset Corporativo P2 R$ 48,00");

        MockMultipartFile arquivoPdf = new MockMultipartFile(
                "arquivo", "tabela_headset.pdf", MediaType.APPLICATION_PDF_VALUE, pdfValido
        );

        // 1. Sem vínculo de fornecedor
        mockMvc.perform(multipart("/api/importacoes-pdf/upload").file(arquivoPdf))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("obrigatório")));

        // 2. Com fornecedor INATIVO
        mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(arquivoPdf)
                        .param("fornecedorId", inativo.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("INATIVO")));

        // 3. Extensão inválida (.csv disfarçado)
        Fornecedor ativo = salvarDistribuidor("98765432000110", "Mazer Informática", "ATIVO");
        MockMultipartFile arquivoInvalido = new MockMultipartFile(
                "arquivo", "tabela.csv", "text/csv", "INF-01;18.50".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(arquivoInvalido)
                        .param("fornecedorId", ativo.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Apenas arquivos PDF autênticos")));
    }
}
