package com.uniesp.priceops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.dto.ResolucaoPendenciaRequestDto;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ImportacaoPdfRepository;
import com.uniesp.priceops.repository.ItemImportadoPdfRepository;
import com.uniesp.priceops.repository.LogAuditoriaPdfRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
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

@SpringBootTest(properties = "priceops.upload.dir=target/test-uploads-extracao")
@AutoConfigureMockMvc
class ExtracaoPdfIntegrationTests {

    private static final Path PASTA_TESTE = Paths.get("target/test-uploads-extracao");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportacaoPdfRepository importacaoPdfRepository;

    @Autowired
    private ItemImportadoPdfRepository itemImportadoPdfRepository;

    @Autowired
    private LogAuditoriaPdfRepository logAuditoriaPdfRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Fornecedor fornecedorAtivo;
    private Produto produtoInf01;
    private Produto produtoInf02;

    @BeforeEach
    void prepararAmbiente() throws IOException {
        limparDados();

        fornecedorAtivo = new Fornecedor();
        fornecedorAtivo.setRazaoSocial("Distribuidora Tech Informática Ltda");
        fornecedorAtivo.setNomeFantasia("Distribuidora Tech");
        fornecedorAtivo.setCnpj("12.345.678/0001-90");
        fornecedorAtivo.setEmail("contato@techdist.com.br");
        fornecedorAtivo.setTelefone("(11) 3300-1122");
        fornecedorAtivo.setCidade("São Paulo");
        fornecedorAtivo.setUf("SP");
        fornecedorAtivo.setPrazoEntregaDias(3);
        fornecedorAtivo.setStatus("ATIVO");
        fornecedorAtivo = fornecedorRepository.save(fornecedorAtivo);

        produtoInf01 = new Produto();
        produtoInf01.setCodigo("INF-01");
        produtoInf01.setNome("SSD Kingston 480GB A400 SATA 2.5");
        produtoInf01.setCategoria("Armazenamento");
        produtoInf01.setUnidadeMedida("UN");
        produtoInf01.setPrecoVendaAtual(new BigDecimal("230.00"));
        produtoInf01.setPrecoCompraAtual(new BigDecimal("180.00"));
        produtoInf01.setEstoqueAtual(20);
        produtoInf01.setEstoqueMinimo(5);
        produtoInf01.setStatus("ATIVO");
        produtoInf01 = produtoRepository.save(produtoInf01);

        produtoInf02 = new Produto();
        produtoInf02.setCodigo("INF-02");
        produtoInf02.setNome("Memoria RAM Kingston Fury 16GB DDR4 3200MHz");
        produtoInf02.setCategoria("Memórias");
        produtoInf02.setUnidadeMedida("UN");
        produtoInf02.setPrecoVendaAtual(new BigDecimal("310.00"));
        produtoInf02.setPrecoCompraAtual(new BigDecimal("240.00"));
        produtoInf02.setEstoqueAtual(15);
        produtoInf02.setEstoqueMinimo(4);
        produtoInf02.setStatus("ATIVO");
        produtoInf02 = produtoRepository.save(produtoInf02);
    }

    @AfterEach
    void limparAposTeste() throws IOException {
        limparDados();
    }

    private void limparDados() throws IOException {
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

    private Long fazerUploadPdf(byte[] conteudoPdf, String nomeArquivo) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "arquivo",
                nomeArquivo,
                "application/pdf",
                conteudoPdf
        );

        MvcResult result = mockMvc.perform(multipart("/api/importacoes-pdf/upload")
                        .file(file)
                        .param("fornecedorId", fornecedorAtivo.getId().toString())
                        .param("usuarioResponsavel", "comprador@priceops.com")
                        .param("observacao", "Tabela para teste US04"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    @Test
    @DisplayName("US04 - Deve extrair dados do PDF, vincular itens conhecidos e manter pendências para itens novos")
    void deveProcessarExtracaoComSucessoEVincularItens() throws Exception {
        List<String> tabelaLinhas = List.of(
                "TABELA OFICIAL DE PRECOS - TECH",
                "CODIGO | DESCRICAO | PRECO | QTD",
                "INF-01 | SSD Kingston 480GB A400 SATA 2.5 | R$ 185,00 | 40",
                "INF-02 | Memoria RAM Kingston Fury 16GB DDR4 3200MHz | 242,00 | 15",
                "NOVO-99 | Cooler Master Hyper 212 Black RGB | 199,90 | 10"
        );

        byte[] pdfBytes = gerarPdfTabela(tabelaLinhas);
        Long importacaoId = fazerUploadPdf(pdfBytes, "tabela_tech_nov.pdf");

        // Executar extração
        mockMvc.perform(post("/api/importacoes-pdf/{id}/extrair", importacaoId)
                        .param("usuarioResponsavel", "operador@priceops.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importacaoId", is(importacaoId.intValue())))
                .andExpect(jsonPath("$.statusImportacao", is("PROCESSADO")))
                .andExpect(jsonPath("$.totalItens", is(3)))
                .andExpect(jsonPath("$.totalVinculados", is(2)))
                .andExpect(jsonPath("$.totalPendentes", is(1)))
                .andExpect(jsonPath("$.itens", hasSize(3)))
                .andExpect(jsonPath("$.itens[0].codigoFornecedor", is("INF-01")))
                .andExpect(jsonPath("$.itens[0].statusItem", is("VINCULADO")))
                .andExpect(jsonPath("$.itens[0].produtoVinculadoId", is(produtoInf01.getId().intValue())))
                .andExpect(jsonPath("$.itens[2].codigoFornecedor", is("NOVO-99")))
                .andExpect(jsonPath("$.itens[2].statusItem", is("PENDENTE_VINCULO")))
                .andExpect(jsonPath("$.itens[2].produtoVinculadoId", nullValue()));

        // Verificar pré-visualização
        mockMvc.perform(get("/api/importacoes-pdf/{id}/pre-visualizacao", importacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens", is(3)))
                .andExpect(jsonPath("$.totalVinculados", is(2)))
                .andExpect(jsonPath("$.totalPendentes", is(1)));

        // Verificar log de auditoria
        mockMvc.perform(get("/api/importacoes-pdf/auditoria")
                        .param("status", "EXTRACAO_CONCLUIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].detalhes", containsString("Extração concluída com sucesso")));
    }

    @Test
    @DisplayName("US04 - Deve resolver pendência vinculando o item manualmente a um produto existente")
    void deveResolverPendenciaVinculandoManualmente() throws Exception {
        List<String> tabelaLinhas = List.of(
                "CODIGO | DESCRICAO | PRECO | QTD",
                "COD-PENDENTE | Memoria DDR4 16GB Kingston Fury | 240,00 | 20"
        );

        byte[] pdfBytes = gerarPdfTabela(tabelaLinhas);
        Long importacaoId = fazerUploadPdf(pdfBytes, "tabela_pendencia.pdf");

        // Executa extração
        MvcResult resExtracao = mockMvc.perform(post("/api/importacoes-pdf/{id}/extrair", importacaoId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonPrevia = objectMapper.readTree(resExtracao.getResponse().getContentAsString());
        Long itemId = jsonPrevia.get("itens").get(0).get("id").asLong();

        // Operador resolve a pendência associando ao produto INF-02
        ResolucaoPendenciaRequestDto request = new ResolucaoPendenciaRequestDto(produtoInf02.getId(), "VINCULAR");
        request.setObservacao("Vínculo confirmado pelo comprador");

        mockMvc.perform(put("/api/importacoes-pdf/{id}/itens/{itemId}/resolver-pendencia", importacaoId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .param("usuarioResponsavel", "gerente@priceops.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemId.intValue())))
                .andExpect(jsonPath("$.statusItem", is("VINCULADO")))
                .andExpect(jsonPath("$.produtoVinculadoId", is(produtoInf02.getId().intValue())))
                .andExpect(jsonPath("$.motivoPendencia", is("Vínculo confirmado pelo comprador")));

        // Pré-visualização agora deve acusar 1 vinculado e 0 pendentes
        mockMvc.perform(get("/api/importacoes-pdf/{id}/pre-visualizacao", importacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVinculados", is(1)))
                .andExpect(jsonPath("$.totalPendentes", is(0)));
    }

    @Test
    @DisplayName("US04 - Deve resolver pendência marcando o item como IGNORADO")
    void deveResolverPendenciaMarcandoComoIgnorado() throws Exception {
        List<String> tabelaLinhas = List.of(
                "CODIGO | DESCRICAO | PRECO | QTD",
                "DESCARTE-01 | Embalagem Plastica Bolha | 5,00 | 100"
        );

        byte[] pdfBytes = gerarPdfTabela(tabelaLinhas);
        Long importacaoId = fazerUploadPdf(pdfBytes, "tabela_descarte.pdf");

        MvcResult resExtracao = mockMvc.perform(post("/api/importacoes-pdf/{id}/extrair", importacaoId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonPrevia = objectMapper.readTree(resExtracao.getResponse().getContentAsString());
        Long itemId = jsonPrevia.get("itens").get(0).get("id").asLong();

        ResolucaoPendenciaRequestDto request = new ResolucaoPendenciaRequestDto(null, "IGNORAR");
        request.setObservacao("Insumo de embalagem, não é item de catálogo");

        mockMvc.perform(put("/api/importacoes-pdf/{id}/itens/{itemId}/resolver-pendencia", importacaoId, itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusItem", is("IGNORADO")))
                .andExpect(jsonPath("$.produtoVinculadoId", nullValue()))
                .andExpect(jsonPath("$.motivoPendencia", is("Insumo de embalagem, não é item de catálogo")));
    }

    @Test
    @DisplayName("US04 - Deve retornar 404 ao tentar extrair ou consultar importação inexistente")
    void deveRetornar404ParaImportacaoInexistente() throws Exception {
        mockMvc.perform(post("/api/importacoes-pdf/{id}/extrair", 9999L))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/importacoes-pdf/{id}/pre-visualizacao", 9999L))
                .andExpect(status().isNotFound());
    }
}
