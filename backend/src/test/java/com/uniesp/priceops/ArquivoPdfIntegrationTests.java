package com.uniesp.priceops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.repository.ArquivoPdfRepository;
import com.uniesp.priceops.repository.FornecedorRepository;
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
class ArquivoPdfIntegrationTests {

    private static final Path PASTA_TESTE = Paths.get("target/test-uploads-pdf");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArquivoPdfRepository arquivoPdfRepository;

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
        arquivoPdfRepository.deleteAll();
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

    private Fornecedor salvarFornecedorPadrao() {
        Fornecedor f = new Fornecedor();
        f.setRazaoSocial("Kabum Distribuidora Tech LTDA");
        f.setNomeFantasia("Kabum B2B");
        f.setCnpj("12345678000190");
        f.setEmail("b2b@kabum.com.br");
        f.setTelefone("(19) 3333-4444");
        f.setCidade("Limeira");
        f.setUf("SP");
        f.setPrazoEntregaDias(4);
        return fornecedorRepository.save(f);
    }

    private byte[] gerarConteudoPdfValido(String textoInterno) {
        String pdfMinimo = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "% Conteudo: " + textoInterno + "\n"
                + "%%EOF\n";
        return pdfMinimo.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void deveRealizarUploadPdfValidoERegistrarAuditoria() throws Exception {
        Fornecedor fornecedor = salvarFornecedorPadrao();
        byte[] pdfBytes = gerarConteudoPdfValido("Tabela Precos Placas de Video Outubro 2026");

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "tabela_gpu_outubro.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                pdfBytes
        );

        mockMvc.perform(multipart("/api/arquivos-pdf/upload")
                        .file(arquivo)
                        .param("fornecedorId", fornecedor.getId().toString())
                        .param("usuarioResponsavel", "Tamires")
                        .param("observacao", "Tabela oficial de GPUs"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nomeOriginal", is("tabela_gpu_outubro.pdf")))
                .andExpect(jsonPath("$.hashSha256", matchesPattern("^[a-f0-9]{64}$")))
                .andExpect(jsonPath("$.statusProcessamento", is("RECEBIDO")))
                .andExpect(jsonPath("$.usuarioResponsavel", is("Tamires")))
                .andExpect(jsonPath("$.fornecedor.nomeFantasia", is("Kabum B2B")));

        // Confirma auditoria de sucesso
        mockMvc.perform(get("/api/arquivos-pdf/auditoria")
                        .param("evento", "UPLOAD_SUCESSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomeArquivo", is("tabela_gpu_outubro.pdf")))
                .andExpect(jsonPath("$[0].usuarioResponsavel", is("Tamires")));
    }

    @Test
    void deveBloquearUploadDuplicadoMesmoComNomeDiferenteEAuditarTentativa() throws Exception {
        byte[] mesmoConteudoPdf = gerarConteudoPdfValido("Cotacao Processadores Ryzen Série 9000");

        MockMultipartFile primeiroArquivo = new MockMultipartFile(
                "arquivo",
                "cotacao_amd_original.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                mesmoConteudoPdf
        );

        MvcResult resultadoOriginal = mockMvc.perform(multipart("/api/arquivos-pdf/upload")
                        .file(primeiroArquivo)
                        .param("usuarioResponsavel", "Operador 1"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonOriginal = objectMapper.readTree(resultadoOriginal.getResponse().getContentAsString());
        Long idOriginal = jsonOriginal.get("id").asLong();
        String hashOriginal = jsonOriginal.get("hashSha256").asText();

        // Tenta enviar exatamente o mesmo binário com outro nome de arquivo
        MockMultipartFile arquivoRenomeadoDuplicado = new MockMultipartFile(
                "arquivo",
                "copia_renomeada_amd.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                mesmoConteudoPdf
        );

        mockMvc.perform(multipart("/api/arquivos-pdf/upload")
                        .file(arquivoRenomeadoDuplicado)
                        .param("usuarioResponsavel", "Operador 2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro", is("DUPLICIDADE_ARQUIVO_PDF")))
                .andExpect(jsonPath("$.arquivoExistenteId", is(idOriginal.intValue())))
                .andExpect(jsonPath("$.arquivoExistenteNome", is("cotacao_amd_original.pdf")))
                .andExpect(jsonPath("$.hashSha256", is(hashOriginal)));

        // Garante que apenas 1 arquivo ficou salvo na tabela principal
        mockMvc.perform(get("/api/arquivos-pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Garante que a tentativa de duplicidade ficou registrada na auditoria
        mockMvc.perform(get("/api/arquivos-pdf/auditoria")
                        .param("evento", "DUPLICIDADE_BLOQUEADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomeArquivo", is("copia_renomeada_amd.pdf")))
                .andExpect(jsonPath("$[0].hashSha256", is(hashOriginal)))
                .andExpect(jsonPath("$[0].usuarioResponsavel", is("Operador 2")));
    }

    @Test
    void deveRejeitarArquivosVaziosOuFalsosPdfEAuditarFalha() throws Exception {
        // 1. Arquivo vazio
        MockMultipartFile vazio = new MockMultipartFile(
                "arquivo", "vazio.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/api/arquivos-pdf/upload").file(vazio))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("não pode estar vazio")));

        // 2. Arquivo texto disfarçado com extensão .pdf (sem assinatura binária %PDF-)
        MockMultipartFile falsoPdf = new MockMultipartFile(
                "arquivo",
                "planilha_disfarcada.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "codigo;produto;preco\n001;Mouse;50.00".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/arquivos-pdf/upload").file(falsoPdf))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Apenas arquivos PDF autênticos")));

        // Ambos os bloqueios devem constar na auditoria como ARQUIVO_INVALIDO
        mockMvc.perform(get("/api/arquivos-pdf/auditoria")
                        .param("evento", "ARQUIVO_INVALIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void devePermitirVerificarHashBaixarAtualizarStatusEDeletarMantendoAuditoria() throws Exception {
        byte[] conteudo = gerarConteudoPdfValido("Catalogo Monitores Gamer 240Hz");
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "catalogo_monitores.pdf", MediaType.APPLICATION_PDF_VALUE, conteudo
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/arquivos-pdf/upload")
                        .file(arquivo)
                        .param("usuarioResponsavel", "Tamires"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long id = json.get("id").asLong();
        String hash = json.get("hashSha256").asText();

        // Verifica hash existente
        mockMvc.perform(get("/api/arquivos-pdf/verificar-hash/" + hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duplicado", is(true)))
                .andExpect(jsonPath("$.arquivoExistenteId", is((int) id)));

        // Download do arquivo binário
        mockMvc.perform(get("/api/arquivos-pdf/" + id + "/download")
                        .param("usuarioResponsavel", "Tamires"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(conteudo));

        // Atualiza status de processamento
        mockMvc.perform(patch("/api/arquivos-pdf/" + id + "/status")
                        .param("status", "PROCESSADO")
                        .param("usuarioResponsavel", "Tamires"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusProcessamento", is("PROCESSADO")));

        // Deleta o arquivo
        mockMvc.perform(delete("/api/arquivos-pdf/" + id)
                        .param("usuarioResponsavel", "Tamires"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/arquivos-pdf/" + id))
                .andExpect(status().isNotFound());

        // Histórico de auditoria permanece intacto (UPLOAD + DOWNLOAD + STATUS + REMOCAO = 4 registros)
        mockMvc.perform(get("/api/arquivos-pdf/auditoria")
                        .param("hashSha256", hash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
    }
}
