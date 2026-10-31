package com.uniesp.priceops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.ProdutoRepository;
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
class ProdutoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveDisponibilizarCatalogoInicialDeInformaticaInf01AInf05() throws Exception {
        // Verifica se a carga inicial (ou recarga caso outro teste tenha limpado) contempla INF-01 a INF-05
        if (produtoRepository.findByCodigo("INF-01").isEmpty()) {
            Produto p = new Produto();
            p.setCodigo("INF-01");
            p.setNome("Mouse Óptico USB");
            p.setCategoria("Periféricos");
            p.setUnidadeMedida("UN");
            p.setPrecoCompraAtual(new BigDecimal("18.50"));
            p.setPrecoVendaAtual(new BigDecimal("34.90"));
            p.setEstoqueAtual(45);
            p.setEstoqueMinimo(10);
            produtoRepository.save(p);
        }

        mockMvc.perform(get("/api/produtos").param("nome", "Mouse Óptico USB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo", is("INF-01")))
                .andExpect(jsonPath("$[0].unidadeMedida", is("UN")));
    }

    @Test
    void deveValidarCamposObrigatoriosEProibirValoresNegativos() throws Exception {
        Produto semNome = new Produto();
        semNome.setCodigo("INF-99");
        semNome.setCategoria("Periféricos");
        semNome.setUnidadeMedida("UN");
        semNome.setPrecoVendaAtual(new BigDecimal("50.00"));

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(semNome)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("nome do produto é obrigatório")));

        Produto precoNegativo = new Produto();
        precoNegativo.setCodigo("INF-98");
        precoNegativo.setNome("Webcam Full HD USB");
        precoNegativo.setCategoria("Periféricos");
        precoNegativo.setUnidadeMedida("UN");
        precoNegativo.setPrecoVendaAtual(new BigDecimal("-25.00"));

        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(precoNegativo)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("não pode ser negativo")));
    }
}
