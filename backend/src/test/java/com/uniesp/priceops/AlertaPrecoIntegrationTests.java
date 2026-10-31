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

    private Produto criarProdutoPadrao(String codigo, String nome, String precoCompra, String precoVenda) {
        Produto p = new Produto();
        p.setCodigo(codigo);
        p.setNome(nome);
        p.setCategoria("Hardware");
        p.setUnidadeMedida("UN");
        p.setPrecoCompraAtual(new BigDecimal(precoCompra));
        p.setPrecoVendaAtual(new BigDecimal(precoVenda));
        p.setEstoqueAtual(10);
        p.setEstoqueMinimo(3);
        return produtoRepository.save(p);
    }

    private Fornecedor criarFornecedorPadrao() {
        Fornecedor f = new Fornecedor();
        f.setRazaoSocial("All Nations Comércio Exterior LTDA");
        f.setNomeFantasia("All Nations");
        f.setCnpj("12345678000190");
        f.setEmail("vendas@allnations.com.br");
        f.setTelefone("(21) 3333-9000");
        f.setCidade("Rio de Janeiro");
        f.setUf("RJ");
        f.setPrazoEntregaDias(5);
        return fornecedorRepository.save(f);
    }

    @Test
    void deveGerarAlertasDeVariacaoAbusivaEMargemComprometida() throws Exception {
        Produto placaVideo = criarProdutoPadrao("GPU-RTX4060", "Placa de Vídeo RTX 4060 8GB", "1800.00", "2300.00");
        Fornecedor fornecedor = criarFornecedorPadrao();

        // Preço ofertado de R$ 2.430,00: +35% sobre compra (abusiva crítica) e acima da venda (margem comprometida)
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(placaVideo.getId());
        req.setFornecedorId(fornecedor.getId());
        req.setPrecoOfertado(new BigDecimal("2430.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$.percentualVariacaoCompra", is(35.00)))
                .andExpect(jsonPath("$.alertasGerados", hasSize(2)))
                .andExpect(jsonPath("$.alertasGerados[*].tipoAlerta",
                        containsInAnyOrder("VARIACAO_ABUSIVA_ALTA", "MARGEM_COMPROMETIDA")));

        // Confirma painel de notificações pendentes
        mockMvc.perform(get("/api/alertas/notificacoes/pendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendentes", is(2)))
                .andExpect(jsonPath("$.criticos", is(2)));
    }

    @Test
    void naoDeveGerarAlertaQuandoVariacaoEstiverDentroDoLimiteTolerado() throws Exception {
        Produto ssd = criarProdutoPadrao("SSD-NVME-1TB", "SSD Kingston NV2 1TB", "350.00", "499.90");

        // Aumento de +5,71% (abaixo do limite padrão de 15%)
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(ssd.getId());
        req.setPrecoOfertado(new BigDecimal("370.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomaliaDetectada", is(false)))
                .andExpect(jsonPath("$.alertasGerados", hasSize(0)));

        mockMvc.perform(get("/api/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deveDetectarDiscrepanciaDeQuedaAtipicaEAtualizarStatusDaNotificacao() throws Exception {
        Produto processador = criarProdutoPadrao("CPU-R7-7800X3D", "Processador Ryzen 7 7800X3D", "2500.00", "3299.00");

        // Preço cotado de R$ 250,00 (-90%, provável erro de digitação na tabela do fornecedor)
        AvaliacaoPrecoRequest req = new AvaliacaoPrecoRequest();
        req.setProdutoId(processador.getId());
        req.setPrecoOfertado(new BigDecimal("250.00"));

        MvcResult result = mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$.alertasGerados[0].tipoAlerta", is("DISCREPANCIA_QUEDA_ATIPICA")))
                .andExpect(jsonPath("$.alertasGerados[0].severidade", is("ALTA")))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long alertaId = json.get("alertasGerados").get(0).get("id").asLong();

        // Marca notificação como visualizada
        mockMvc.perform(patch("/api/alertas/" + alertaId + "/visualizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusNotificacao", is("VISUALIZADO")));

        // Resolve o alerta com justificativa
        mockMvc.perform(patch("/api/alertas/" + alertaId + "/status")
                        .param("status", "RESOLVIDO")
                        .param("justificativa", "Confirmado erro de digitação na planilha do distribuidor."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusNotificacao", is("RESOLVIDO")))
                .andExpect(jsonPath("$.justificativaResolucao", containsString("erro de digitação")));

        // Painel de pendentes agora deve estar zerado
        mockMvc.perform(get("/api/alertas/notificacoes/pendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPendentes", is(0)));
    }

    @Test
    void deveAvaliarLoteDeCotacoesEValidarDadosInvalidos() throws Exception {
        Produto memoria = criarProdutoPadrao("RAM-DDR5-16G", "Memória DDR5 16GB 5600MHz", "400.00", "590.00");

        AvaliacaoPrecoRequest itemNormal = new AvaliacaoPrecoRequest();
        itemNormal.setProdutoId(memoria.getId());
        itemNormal.setPrecoOfertado(new BigDecimal("410.00"));

        AvaliacaoPrecoRequest itemAbusivo = new AvaliacaoPrecoRequest();
        itemAbusivo.setProdutoId(memoria.getId());
        itemAbusivo.setPrecoOfertado(new BigDecimal("500.00")); // +25% (acima de 15%)

        mockMvc.perform(post("/api/alertas/avaliar-lote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(itemNormal, itemAbusivo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].anomaliaDetectada", is(false)))
                .andExpect(jsonPath("$[1].anomaliaDetectada", is(true)))
                .andExpect(jsonPath("$[1].alertasGerados[0].severidade", is("ALTA")));

        // Valida rejeição de preço negativo
        AvaliacaoPrecoRequest invalido = new AvaliacaoPrecoRequest();
        invalido.setProdutoId(memoria.getId());
        invalido.setPrecoOfertado(new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/alertas/avaliar-preco")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("maior que zero")));
    }
}
