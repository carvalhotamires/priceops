package com.uniesp.priceops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.repository.FornecedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FornecedorIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparBanco() {
        fornecedorRepository.deleteAll();
    }

    private Fornecedor criarFornecedorExemplo(String cnpj, String nomeFantasia) {
        Fornecedor f = new Fornecedor();
        f.setRazaoSocial(nomeFantasia + " Distribuidora de Informática LTDA");
        f.setNomeFantasia(nomeFantasia);
        f.setCnpj(cnpj);
        f.setEmail("comercial@" + nomeFantasia.toLowerCase().replace(" ", "") + ".com.br");
        f.setTelefone("(11) 4002-8922");
        f.setContatoNome("Carlos Silva");
        f.setCidade("São Paulo");
        f.setUf("sp");
        f.setPrazoEntregaDias(5);
        return f;
    }

    @Test
    void deveCadastrarEConsultarFornecedorComSucesso() throws Exception {
        Fornecedor novo = criarFornecedorExemplo("12.345.678/0001-90", "All Nations Tech");

        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(novo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.cnpj", is("12345678000190")))
                .andExpect(jsonPath("$.uf", is("SP")))
                .andExpect(jsonPath("$.status", is("ATIVO")));

        mockMvc.perform(get("/api/fornecedores")
                        .param("nome", "all nations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomeFantasia", is("All Nations Tech")));
    }

    @Test
    void deveImpedirCadastroDeCnpjDuplicado() throws Exception {
        Fornecedor primeiro = criarFornecedorExemplo("12.345.678/0001-90", "Pauta Distribuidora");
        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(primeiro)))
                .andExpect(status().isCreated());

        // Tenta cadastrar com o mesmo CNPJ sem pontuação
        Fornecedor duplicado = criarFornecedorExemplo("12345678000190", "Outra Distribuidora");
        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicado)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Já existe um fornecedor cadastrado com o CNPJ")));
    }

    @Test
    void deveValidarCnpjInvalidoEPrazoNegativo() throws Exception {
        Fornecedor cnpjCurto = criarFornecedorExemplo("12345", "Aldo Componentes");
        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cnpjCurto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("14 dígitos")));

        Fornecedor prazoNegativo = criarFornecedorExemplo("98.765.432/0001-10", "Mazer Distribuidora");
        prazoNegativo.setPrazoEntregaDias(-2);
        mockMvc.perform(post("/api/fornecedores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prazoNegativo)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("não pode ser negativo")));
    }

    @Test
    void deveAtualizarEDeletarFornecedor() throws Exception {
        Fornecedor salvo = fornecedorRepository.save(criarFornecedorExemplo("11222333000181", "Hayamax"));

        salvo.setPrazoEntregaDias(3);
        salvo.setStatus("INATIVO");

        mockMvc.perform(put("/api/fornecedores/" + salvo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salvo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prazoEntregaDias", is(3)))
                .andExpect(jsonPath("$.status", is("INATIVO")));

        mockMvc.perform(get("/api/fornecedores")
                        .param("status", "INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/fornecedores/" + salvo.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/fornecedores/" + salvo.getId()))
                .andExpect(status().isNotFound());
    }
}
