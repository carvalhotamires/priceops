package com.uniesp.priceops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.dto.AvaliacaoPrecoRequest;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.AlertaPrecoRepository;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlertaPrecoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertaPrecoRepository alertaPrecoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void prepararBanco() {
        limparDados();
    }

    @AfterEach
    void limparBancoAposTeste() {
        limparDados();
    }

    private void limparDados() {
        alertaPrecoRepository.deleteAll();
        produtoRepository.deleteAll();
        fornecedorRepository.deleteAll();
    }

    private Produto salvarProdutoInformatica(
            String codigo,
            String nome,
            String categoria,
            String unidade,
            String precoCompra,
            String precoVenda) {
        Produto p = new Produto();
        p.setCodigo(codigo);
        p.setNome(nome);
        p.setCategoria(categoria);
        p.setUnidadeMedida(unidade);
        p.setPrecoCompraAtual(new BigDecimal(precoCompra));
        p.setPrecoVendaAtual(new BigDecimal(precoVenda));
        p.setEstoqueAtual(30);
        p.setEstoqueMinimo(8);
        return produtoRepository.save(p);
    }

    private Fornecedor salvarDistribuidorPadrao() {
        Fornecedor f = new Fornecedor();
        f.setRazaoSocial("All Nations Comércio Exterior S.A.");
        f.setNomeFantasia("All Nations Informática");
        f.setCnpj("68552215000180");
        f.setEmail("vendas@allnations.com.br");
        f.setTelefone("(21) 3523-9000");
        f.setCidade("Rio de Janeiro");
        f.setUf("RJ");
        f.setPrazoEntregaDias(5);
        return fornecedorRepository.save(f);
    }

    @Test
    void deveGerarAlertaDeVariacaoAbusivaAltaEmMouseOpticoInf01() throws Exception {
        Produto mouse = salvarProdutoInformatica("INF-01", "Mouse Óptico USB", "Periféricos", "UN", "18.50", "34.90");
        Fornecedor distribuidor = salvarDistribuidorPadrao();

        // Aumento de +20% (de 18.50 para 22.20), acima do padrão de 15% -> severidade ALTA
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(mouse.getId());
        req.setFornecedorId(distribuidor.getId());
        req.setPrecoOfertado(new BigDecimal("22.20"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$.percentualVariacaoCompra", is(20.00)))
                .andExpect(jsonPath("$.alertasGerados", hasSize(1)))
                .andExpect(jsonPath("$.alertasGerados[0].tipoAlerta", is("VARIACAO_ABUSIVA_ALTA")))
                .andExpect(jsonPath("$.alertasGerados[0].severidade", is("ALTA")));
    }

    @Test
    void deveEscalonarSeveridadeParaCriticaEmAumentoExtremoDeTecladoInf03() throws Exception {
        Produto teclado = salvarProdutoInformatica("INF-03", "Teclado ABNT2", "Periféricos", "UN", "32.00", "59.90");

        // Aumento de +50% (de 32.00 para 48.00), dobrando a margem tolerada -> severidade CRITICA
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(teclado.getId());
        req.setPrecoOfertado(new BigDecimal("48.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$.alertasGerados[0].tipoAlerta", is("VARIACAO_ABUSIVA_ALTA")))
                .andExpect(jsonPath("$.alertasGerados[0].severidade", is("CRITICA")));
    }

    @Test
    void deveDispararAlertaCriticoDeMargemComprometidaEmHeadsetInf05() throws Exception {
        Produto headset = salvarProdutoInformatica("INF-05", "Headset Corporativo P2", "Áudio e Periféricos", "UN", "48.00", "89.90");
        Fornecedor distribuidor = salvarDistribuidorPadrao();

        // Custo cotado (R$ 92,00) supera o preço de venda praticado na loja (R$ 89,90)
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(headset.getId());
        req.setFornecedorId(distribuidor.getId());
        req.setPrecoOfertado(new BigDecimal("92.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$.alertasGerados[*].tipoAlerta",
                        containsInAnyOrder("VARIACAO_ABUSIVA_ALTA", "MARGEM_COMPROMETIDA")));
    }

    @Test
    void deveDetectarDiscrepanciaDeQuedaAtipicaEmPendriveCaixaInf04() throws Exception {
        Produto pendriveCx = salvarProdutoInformatica("INF-04", "Pendrive 64GB", "Armazenamento", "CX", "210.00", "349.00");

        // Redução abrupta de -90% (R$ 21,00 na caixa fechada -> provável erro de digitação de UN vs CX)
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(pendriveCx.getId());
        req.setPrecoOfertado(new BigDecimal("21.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$.alertasGerados[0].tipoAlerta", is("DISCREPANCIA_QUEDA_ATIPICA")))
                .andExpect(jsonPath("$.alertasGerados[0].severidade", is("ALTA")));
    }

    @Test
    void naoDeveGerarAlertaQuandoVariacaoDeCaboHdmiInf02EstiverDentroDaMargem() throws Exception {
        Produto caboHdmi = salvarProdutoInformatica("INF-02", "Cabo HDMI 2.0", "Cabos e Adaptadores", "UN", "14.00", "29.90");

        // Aumento leve de +5% (R$ 14,70), abaixo do limite tolerado de 15%
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(caboHdmi.getId());
        req.setPrecoOfertado(new BigDecimal("14.70"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomaliaDetectada", is(false)))
                .andExpect(jsonPath("$.alertasGerados", hasSize(0)));
    }

    @Test
    void deveRespeitarLimitePercentualCustomizadoNaAvaliacao() throws Exception {
        Produto caboHdmi = salvarProdutoInformatica("INF-02", "Cabo HDMI 2.0", "Cabos e Adaptadores", "UN", "14.00", "29.90");

        // Aumento de +10% (R$ 15,40), mas com limite estrito customizado de 8%
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(caboHdmi.getId());
        req.setPrecoOfertado(new BigDecimal("15.40"));
        req.setLimitePercentualCustomizado(new BigDecimal("8.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$.alertasGerados[0].limiteToleradoPercentual", is(8.00)));
    }

    @Test
    void deveProcessarLoteDeCotacoesDoCatalogoDeInformatica() throws Exception {
        Produto mouse = salvarProdutoInformatica("INF-01", "Mouse Óptico USB", "Periféricos", "UN", "18.50", "34.90");
        Produto teclado = salvarProdutoInformatica("INF-03", "Teclado ABNT2", "Periféricos", "UN", "32.00", "59.90");

        AvaliacaoPrecoRequest item1 = new AvaliacaoPrecoRequest();
        item1.setProdutoId(mouse.getId());
        item1.setPrecoOfertado(new BigDecimal("19.00")); // Normal (+2,70%)

        AvaliacaoPrecoRequest item2 = new AvaliacaoPrecoRequest();
        item2.setProdutoId(teclado.getId());
        item2.setPrecoOfertado(new BigDecimal("40.00")); // Abusivo (+25,00%)

        mockMvc.perform(post("/api/alertas/avaliar-lote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(item1, item2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].anomaliaDetectada", is(false)))
                .andExpect(jsonPath("$[1].anomaliaDetectada", is(true)));
    }

    @Test
    void deveGerenciarCicloDeVidaDaNotificacaoVisualizarEResolverComJustificativa() throws Exception {
        Produto mouse = salvarProdutoInformatica("INF-01", "Mouse Óptico USB", "Periféricos", "UN", "18.50", "34.90");

        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(mouse.getId());
        req.setPrecoOfertado(new BigDecimal("25.90"));

        MvcResult result = mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long alertaId = json.get("alertasGerados").get(0).get("id").asLong();

        mockMvc.perform(get("/api/alertas/notificacoes/pendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendentes", is(1)));

        mockMvc.perform(patch("/api/alertas/" + alertaId + "/visualizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusNotificacao", is("VISUALIZADO")));

        mockMvc.perform(patch("/api/alertas/" + alertaId + "/status")
                        .param("status", "RESOLVIDO")
                        .param("justificativa", "Negociado desconto de volume com representante do distribuidor."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusNotificacao", is("RESOLVIDO")))
                .andExpect(jsonPath("$.justificativaResolucao", containsString("Negociado desconto")));
    }

    @Test
    void deveRejeitarRequisicoesComPrecoNegativoOuProdutoInexistente() throws Exception {
        Produto mouse = salvarProdutoInformatica("INF-01", "Mouse Óptico USB", "Periféricos", "UN", "18.50", "34.90");

        AvaliacaoPrecoRequest precoNegativo = new AvaliacaoPrecoRequest();
        precoNegativo.setProdutoId(mouse.getId());
        precoNegativo.setPrecoOfertado(new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(precoNegativo)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("maior que zero")));

        AvaliacaoPrecoRequest produtoInexistente = new AvaliacaoPrecoRequest();
        produtoInexistente.setProdutoId(99999L);
        produtoInexistente.setPrecoOfertado(new BigDecimal("20.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(produtoInexistente)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Produto não encontrado")));
    }
}
