package com.uniesp.priceops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.controller.HistoricoPrecoController.RegistroPrecoRequest;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HistoricoPrecoIntegrationTests {

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

    @Autowired
    private ObjectMapper objectMapper;

    private Produto produto;
    private Fornecedor fornecedor;

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
        produto.setPrecoVendaAtual(new BigDecimal("240.00"));
        produto.setEstoqueAtual(20);
        produto.setEstoqueMinimo(5);
        produto = produtoRepository.save(produto);

        fornecedor = new Fornecedor();
        fornecedor.setRazaoSocial("All Nations Informática Ltda");
        fornecedor.setNomeFantasia("All Nations");
        fornecedor.setCnpj("68552215000180");
        fornecedor.setEmail("vendas@allnations.com.br");
        fornecedor.setTelefone("(21) 3523-9000");
        fornecedor.setCidade("Rio de Janeiro");
        fornecedor.setUf("RJ");
        fornecedor.setPrazoEntregaDias(5);
        fornecedor = fornecedorRepository.save(fornecedor);
    }

    @AfterEach
    void limparBancoAposTeste() {
        historicoPrecoRepository.deleteAll();
        produtoRepository.deleteAll();
        fornecedorRepository.deleteAll();
    }

    @Test
    void deveSalvarMultiplosPrecosNoHistoricoSemSobrescreverAnteriores() throws Exception {
        RegistroPrecoRequest preco1 = new RegistroPrecoRequest(produto.getId(), fornecedor.getId(), new BigDecimal("180.00"), "MANUAL");
        RegistroPrecoRequest preco2 = new RegistroPrecoRequest(produto.getId(), fornecedor.getId(), new BigDecimal("195.00"), "MANUAL");
        RegistroPrecoRequest preco3 = new RegistroPrecoRequest(produto.getId(), fornecedor.getId(), new BigDecimal("189.00"), "MANUAL");

        mockMvc.perform(post("/api/historico-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preco1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/historico-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preco2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/historico-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preco3)))
                .andExpect(status().isCreated());

        // Verifica que os 3 preços continuam salvos em ordem temporal (sem sobrescrita)
        mockMvc.perform(get("/api/historico-precos")
                        .param("produtoId", produto.getId().toString())
                        .param("fornecedorId", fornecedor.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].precoRegistrado", is(180.00)))
                .andExpect(jsonPath("$[1].precoRegistrado", is(195.00)))
                .andExpect(jsonPath("$[2].precoRegistrado", is(189.00)));
    }

    @Test
    void deveRejeitarPrecoInvalido() throws Exception {
        RegistroPrecoRequest invalido = new RegistroPrecoRequest(produto.getId(), fornecedor.getId(), BigDecimal.ZERO, "MANUAL");

        mockMvc.perform(post("/api/historico-precos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());
    }
}
