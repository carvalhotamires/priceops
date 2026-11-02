package com.uniesp.priceops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.dto.ResolucaoPendenciaRequestDto;
import com.uniesp.priceops.dto.VinculoItemFornecedorRequestDto;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "priceops.upload.dir=target/test-uploads-vinculos")
@AutoConfigureMockMvc
class VinculoItemFornecedorIntegrationTests {

    private static final Path PASTA_TESTE = Paths.get("target/test-uploads-vinculos");

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private HistoricoPrecoRepository historicoPrecoRepository;

    @Autowired
    private VinculoItemFornecedorRepository vinculoRepository;

    @Autowired
    private ItemImportadoPdfRepository itemImportadoPdfRepository;

    @Autowired
    private ImportacaoPdfRepository importacaoPdfRepository;

    @Autowired
    private LogAuditoriaPdfRepository logAuditoriaPdfRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Fornecedor fornecedor;
    private Produto produtoInf01;

    @BeforeEach
    void setup() throws IOException {
        limparDados();

        fornecedor = new Fornecedor();
        fornecedor.setRazaoSocial("All Nations Comércio S.A.");
        fornecedor.setNomeFantasia("All Nations");
        fornecedor.setCnpj("68.552.215/0001-80");
        fornecedor.setEmail("pedidos@allnations.com.br");
        fornecedor.setTelefone("(21) 3500-1100");
        fornecedor.setCidade("Rio de Janeiro");
        fornecedor.setUf("RJ");
        fornecedor.setPrazoEntregaDias(5);
        fornecedor.setStatus("ATIVO");
        fornecedor = fornecedorRepository.save(fornecedor);

        produtoInf01 = new Produto();
        produtoInf01.setCodigo("INF-01");
        produtoInf01.setNome("SSD Kingston 480GB A400 SATA 2.5");
        produtoInf01.setCategoria("Armazenamento");
        produtoInf01.setUnidadeMedida("UN");
        produtoInf01.setPrecoVendaAtual(new BigDecimal("230.00"));
        produtoInf01.setPrecoCompraAtual(new BigDecimal("180.00"));
        produtoInf01.setEstoqueAtual(10);
        produtoInf01.setEstoqueMinimo(2);
        produtoInf01.setStatus("ATIVO");
        produtoInf01 = produtoRepository.save(produtoInf01);
    }

    @AfterEach
    void tearDown() throws IOException {
        limparDados();
    }

    private void limparDados() throws IOException {
        if (historicoPrecoRepository != null) historicoPrecoRepository.deleteAll();
        vinculoRepository.deleteAll();
        itemImportadoPdfRepository.deleteAll();
        importacaoPdfRepository.deleteAll();
        logAuditoriaPdfRepository.deleteAll();
        produtoRepository.deleteAll();
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

    private byte[] gerarPdfTabela(List<String> linhas) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.setLeading(14.5f);
                stream.newLineAtOffset(50, 700);

                for (String l : linhas) {
                    stream.showText(l);
                    stream.newLine();
                }

                stream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("US-05 - Deve criar, listar e alternar status de vínculo comercial via REST API")
    void deveGerenciarVinculoComercialViaRest() throws Exception {
        VinculoItemFornecedorRequestDto request = new VinculoItemFornecedorRequestDto(
                fornecedor.getId(), produtoInf01.getId(), "SA400S37/480G", "HD SSD 480GB KINGSTON", new BigDecimal("179.90")
        );

        // 1. POST - Criar vínculo
        MvcResult postRes = mockMvc.perform(post("/api/vinculos-fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.codigoFornecedor", is("SA400S37/480G")))
                .andExpect(jsonPath("$.fornecedorNome", is("All Nations")))
                .andExpect(jsonPath("$.produtoCodigo", is("INF-01")))
                .andReturn();

        JsonNode postJson = objectMapper.readTree(postRes.getResponse().getContentAsString());
        Long vinculoId = postJson.get("id").asLong();

        // 2. GET - Listar com filtros
        mockMvc.perform(get("/api/vinculos-fornecedores")
                        .param("fornecedorId", fornecedor.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigoFornecedor", is("SA400S37/480G")));

        // 3. PATCH - Alternar status
        mockMvc.perform(patch("/api/vinculos-fornecedores/{id}/status", vinculoId)
                        .param("ativo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", is(false)));

        // 4. DELETE - Remover vínculo
        mockMvc.perform(delete("/api/vinculos-fornecedores/{id}", vinculoId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/vinculos-fornecedores/{id}", vinculoId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("US-05 - Aprendizado Contínuo: Resolução de pendência salva De-Para e automatiza importação futura")
    void deveAprenderDeParaEReconhecerAutomaticamenteEmProximaImportacao() throws Exception {
        String codigoDistribuidor = "DIST-SSD-480";

        // ==========================================
        // 1ª IMPORTAÇÃO: Item vem com SKU desconhecido do catálogo interno
        // ==========================================
        byte[] pdf1Bytes = gerarPdfTabela(List.of(
                "TABELA ALL NATIONS - OUTUBRO",
                "CODIGO | DESCRICAO | VALOR | ESTOQUE",
                codigoDistribuidor + " | Unidade Flash Solid State Kingston 480 | 182,00 | 25"
        ));

        MockMultipartFile file1 = new MockMultipartFile(
                "arquivo", "allnations_out.pdf", "application/pdf", pdf1Bytes
        );

        MvcResult upload1Res = mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(file1)
                        .param("fornecedorId", fornecedor.getId().toString())
                        .param("usuarioResponsavel", "comprador"))
                .andExpect(status().isCreated())
                .andReturn();

        Long imp1Id = objectMapper.readTree(upload1Res.getResponse().getContentAsString()).get("id").asLong();

        // Extrai dados da 1ª importação: como o código é desconhecido e descrição é diferente, fica pendente
        MvcResult ext1Res = mockMvc.perform(post("/api/importacoes-pdf/{id}/extrair", imp1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendentes", is(1)))
                .andReturn();

        Long itemId = objectMapper.readTree(ext1Res.getResponse().getContentAsString())
                .get("itens").get(0).get("id").asLong();

        // Operador resolve a pendência associando ao produto INF-01
        ResolucaoPendenciaRequestDto resolucao = new ResolucaoPendenciaRequestDto(produtoInf01.getId(), "VINCULAR");
        resolucao.setObservacao("Vínculo inicial aprendido");

        mockMvc.perform(put("/api/importacoes-pdf/{id}/itens/{itemId}/resolver-pendencia", imp1Id, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolucao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusItem", is("VINCULADO")))
                .andExpect(jsonPath("$.produtoVinculadoId", is(produtoInf01.getId().intValue())));

        // Valida que o De-Para comercial foi memorizado no banco (US-05)
        mockMvc.perform(get("/api/vinculos-fornecedores")
                        .param("fornecedorId", fornecedor.getId().toString())
                        .param("codigoFornecedor", codigoDistribuidor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].produtoCodigo", is("INF-01")));

        // ==========================================
        // 2ª IMPORTAÇÃO: Novo PDF do mesmo fornecedor contendo o mesmo SKU
        // ==========================================
        byte[] pdf2Bytes = gerarPdfTabela(List.of(
                "TABELA ALL NATIONS - NOVEMBRO",
                "CODIGO | DESCRICAO | VALOR | ESTOQUE",
                codigoDistribuidor + " | SSD Kingston 480GB A400 | 179,00 | 30"
        ));

        MockMultipartFile file2 = new MockMultipartFile(
                "arquivo", "allnations_nov.pdf", "application/pdf", pdf2Bytes
        );

        MvcResult upload2Res = mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(file2)
                        .param("fornecedorId", fornecedor.getId().toString())
                        .param("usuarioResponsavel", "comprador"))
                .andExpect(status().isCreated())
                .andReturn();

        Long imp2Id = objectMapper.readTree(upload2Res.getResponse().getContentAsString()).get("id").asLong();

        // Extrai a 2ª tabela: O SISTEMA DEVE VINCULAR AUTOMATICAMENTE VIA DE-PARA MEMORIZADO!
        mockMvc.perform(post("/api/importacoes-pdf/{id}/extrair", imp2Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens", is(1)))
                .andExpect(jsonPath("$.totalVinculados", is(1))) // 100% automático!
                .andExpect(jsonPath("$.totalPendentes", is(0)))  // Zero pendências!
                .andExpect(jsonPath("$.itens[0].codigoFornecedor", is(codigoDistribuidor)))
                .andExpect(jsonPath("$.itens[0].statusItem", is("VINCULADO")))
                .andExpect(jsonPath("$.itens[0].produtoVinculadoId", is(produtoInf01.getId().intValue())));
    }
}
