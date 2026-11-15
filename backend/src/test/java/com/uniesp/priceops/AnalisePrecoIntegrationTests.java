package com.uniesp.priceops;

import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.HistoricoPreco;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AnalisePrecoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HistoricoPrecoRepository historicoPrecoRepository;

    @Autowired(required = false)
    private AlertaPrecoRepository alertaPrecoRepository;

    @Autowired(required = false)
    private VinculoItemFornecedorRepository vinculoItemFornecedorRepository;

    @Autowired(required = false)
    private ItemImportadoPdfRepository itemImportadoPdfRepository;

    @Autowired(required = false)
    private ImportacaoPdfRepository importacaoPdfRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    private Produto produto;
    private Fornecedor fornecedorA;
    private Fornecedor fornecedorB;

    @BeforeEach
    void prepararBanco() {
        historicoPrecoRepository.deleteAll();
        if (alertaPrecoRepository != null) alertaPrecoRepository.deleteAll();
        if (vinculoItemFornecedorRepository != null) vinculoItemFornecedorRepository.deleteAll();
        if (itemImportadoPdfRepository != null) itemImportadoPdfRepository.deleteAll();
        if (importacaoPdfRepository != null) importacaoPdfRepository.deleteAll();
        produtoRepository.deleteAll();
        fornecedorRepository.deleteAll();

        produto = new Produto();
        produto.setCodigo("INF-01");
        produto.setNome("SSD Kingston 480GB");
        produto.setCategoria("Armazenamento");
        produto.setUnidadeMedida("UN");
        produto.setPrecoCompraAtual(new BigDecimal("180.00"));
        produto.setPrecoVendaAtual(new BigDecimal("250.00"));
        produto.setEstoqueAtual(15);
        produto.setEstoqueMinimo(5);
        produto = produtoRepository.save(produto);

        fornecedorA = new Fornecedor();
        fornecedorA.setRazaoSocial("Distribuidora Alpha Ltda");
        fornecedorA.setNomeFantasia("Alpha Distribuidora");
        fornecedorA.setCnpj("11111111000111");
        fornecedorA.setEmail("alpha@distribuidora.com");
        fornecedorA.setTelefone("(11) 1111-1111");
        fornecedorA.setCidade("São Paulo");
        fornecedorA.setUf("SP");
        fornecedorA.setPrazoEntregaDias(3);
        fornecedorA = fornecedorRepository.save(fornecedorA);

        fornecedorB = new Fornecedor();
        fornecedorB.setRazaoSocial("Distribuidora Beta Ltda");
        fornecedorB.setNomeFantasia("Beta Distribuidora");
        fornecedorB.setCnpj("22222222000122");
        fornecedorB.setEmail("beta@distribuidora.com");
        fornecedorB.setTelefone("(21) 2222-2222");
        fornecedorB.setCidade("Rio de Janeiro");
        fornecedorB.setUf("RJ");
        fornecedorB.setPrazoEntregaDias(5);
        fornecedorB = fornecedorRepository.save(fornecedorB);
    }

    @AfterEach
    void limparBanco() {
        historicoPrecoRepository.deleteAll();
        produtoRepository.deleteAll();
        fornecedorRepository.deleteAll();
    }

    private void registrarHistorico(Produto p, Fornecedor f, BigDecimal preco, LocalDateTime data) {
        HistoricoPreco h = new HistoricoPreco();
        h.setProduto(p);
        h.setFornecedor(f);
        h.setPrecoRegistrado(preco);
        h.setOrigem("IMPORTACAO_PDF");
        h.setRegistradoEm(data);
        historicoPrecoRepository.save(h);
    }

    @Test
    @DisplayName("GET /api/analise-precos/produtos/{id}/variacao - deve retornar variação histórica calculada")
    void deveRetornarVariacaoHistorica() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        registrarHistorico(produto, fornecedorA, new BigDecimal("160.00"), agora.minusDays(5));
        registrarHistorico(produto, fornecedorA, new BigDecimal("200.00"), agora);

        mockMvc.perform(get("/api/analise-precos/produtos/" + produto.getId() + "/variacao")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoId").value(produto.getId()))
                .andExpect(jsonPath("$.codigo").value("INF-01"))
                .andExpect(jsonPath("$.precoAnterior").value(160.00))
                .andExpect(jsonPath("$.ultimoPrecoRegistrado").value(200.00))
                .andExpect(jsonPath("$.percentualVariacaoRecente").value(25.00))
                .andExpect(jsonPath("$.tendencia").value("ALTA"))
                .andExpect(jsonPath("$.menorPrecoHistorico").value(160.00))
                .andExpect(jsonPath("$.maiorPrecoHistorico").value(200.00))
                .andExpect(jsonPath("$.precoMedioHistorico").value(180.00));
    }

    @Test
    @DisplayName("GET /api/analise-precos/produtos/{id}/comparativo - deve ranquear melhor fornecedor e economia")
    void deveRetornarComparativoFornecedores() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        // Alpha vende a R$ 150,00 e Beta a R$ 200,00
        registrarHistorico(produto, fornecedorA, new BigDecimal("150.00"), agora);
        registrarHistorico(produto, fornecedorB, new BigDecimal("200.00"), agora);

        mockMvc.perform(get("/api/analise-precos/produtos/" + produto.getId() + "/comparativo")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoId").value(produto.getId()))
                .andExpect(jsonPath("$.melhorFornecedorNome").value("Alpha Distribuidora"))
                .andExpect(jsonPath("$.menorPreco").value(150.00))
                .andExpect(jsonPath("$.maiorPreco").value(200.00))
                .andExpect(jsonPath("$.economiaMaximaAbsoluta").value(50.00))
                .andExpect(jsonPath("$.economiaMaximaPercentual").value(25.00))
                .andExpect(jsonPath("$.totalCotacoes").value(2))
                .andExpect(jsonPath("$.cotacoes", hasSize(2)))
                .andExpect(jsonPath("$.cotacoes[0].fornecedorNome").value("Alpha Distribuidora"))
                .andExpect(jsonPath("$.cotacoes[0].melhorOferta").value(true))
                .andExpect(jsonPath("$.cotacoes[1].fornecedorNome").value("Beta Distribuidora"))
                .andExpect(jsonPath("$.cotacoes[1].melhorOferta").value(false));
    }

    @Test
    @DisplayName("GET /api/analise-precos/produtos/9999/variacao - deve retornar 404 quando produto não existir")
    void deveRetornar404ParaProdutoInexistente() throws Exception {
        mockMvc.perform(get("/api/analise-precos/produtos/9999/variacao"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/analise-precos/comparativo-geral - deve listar comparativo de catálogo ativo")
    void deveListarComparativoGeral() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        registrarHistorico(produto, fornecedorA, new BigDecimal("170.00"), agora);

        mockMvc.perform(get("/api/analise-precos/comparativo-geral"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].produtoId").value(produto.getId()))
                .andExpect(jsonPath("$[0].menorPreco").value(170.00));
    }
}
