package com.uniesp.priceops.service;

import com.uniesp.priceops.dto.VinculoItemFornecedorRequestDto;
import com.uniesp.priceops.dto.VinculoItemFornecedorResponseDto;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.model.VinculoItemFornecedor;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import com.uniesp.priceops.repository.VinculoItemFornecedorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VinculoItemFornecedorServiceTest {

    @Mock
    private VinculoItemFornecedorRepository vinculoRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private VinculoItemFornecedorService vinculoService;

    private Fornecedor fornecedorAtivo;
    private Fornecedor fornecedorInativo;
    private Produto produto;

    @BeforeEach
    void setUp() {
        fornecedorAtivo = new Fornecedor();
        fornecedorAtivo.setId(1L);
        fornecedorAtivo.setNomeFantasia("Distribuidora Tech");
        fornecedorAtivo.setStatus("ATIVO");

        fornecedorInativo = new Fornecedor();
        fornecedorInativo.setId(2L);
        fornecedorInativo.setNomeFantasia("Distribuidora Inativa");
        fornecedorInativo.setStatus("INATIVO");

        produto = new Produto();
        produto.setId(10L);
        produto.setCodigo("INF-01");
        produto.setNome("SSD Kingston 480GB A400");
    }

    @Test
    @DisplayName("Deve criar novo vínculo comercial com sucesso quando fornecedor e produto existirem")
    void deveCriarNovoVinculoComSucesso() {
        VinculoItemFornecedorRequestDto request = new VinculoItemFornecedorRequestDto(
                1L, 10L, "SA400S37/480G", "SSD 480GB KINGSTON", new BigDecimal("185.00")
        );

        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedorAtivo));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(vinculoRepository.findByFornecedorIdAndCodigoFornecedorIgnoreCase(1L, "SA400S37/480G"))
                .thenReturn(Optional.empty());
        when(vinculoRepository.save(any(VinculoItemFornecedor.class))).thenAnswer(i -> {
            VinculoItemFornecedor v = i.getArgument(0);
            v.setId(99L);
            return v;
        });

        VinculoItemFornecedorResponseDto response = vinculoService.criarOuAtualizar(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getCodigoFornecedor()).isEqualTo("SA400S37/480G");
        assertThat(response.getProdutoCodigo()).isEqualTo("INF-01");
        assertThat(response.getFornecedorNome()).isEqualTo("Distribuidora Tech");
        assertThat(response.getAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve atualizar vínculo existente quando mesmo fornecedor já possuir o código cadastrado")
    void deveAtualizarVinculoExistente() {
        VinculoItemFornecedor vinculoExistente = new VinculoItemFornecedor(
                fornecedorAtivo, produto, "SA400S37/480G", "Descricao antiga", new BigDecimal("170.00")
        );
        vinculoExistente.setId(50L);

        VinculoItemFornecedorRequestDto request = new VinculoItemFornecedorRequestDto(
                1L, 10L, "SA400S37/480G", "Descricao atualizada", new BigDecimal("189.90")
        );

        when(fornecedorRepository.findById(1L)).thenReturn(Optional.of(fornecedorAtivo));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(vinculoRepository.findByFornecedorIdAndCodigoFornecedorIgnoreCase(1L, "SA400S37/480G"))
                .thenReturn(Optional.of(vinculoExistente));
        when(vinculoRepository.save(any(VinculoItemFornecedor.class))).thenAnswer(i -> i.getArgument(0));

        VinculoItemFornecedorResponseDto response = vinculoService.criarOuAtualizar(request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getDescricaoNoFornecedor()).isEqualTo("Descricao atualizada");
        assertThat(response.getUltimoPrecoRegistrado()).isEqualByComparingTo(new BigDecimal("189.90"));
    }

    @Test
    @DisplayName("Deve rejeitar criação de vínculo para fornecedor com status INATIVO")
    void deveRejeitarVinculoParaFornecedorInativo() {
        VinculoItemFornecedorRequestDto request = new VinculoItemFornecedorRequestDto(
                2L, 10L, "COD-X", "Peca", new BigDecimal("100.00")
        );

        when(fornecedorRepository.findById(2L)).thenReturn(Optional.of(fornecedorInativo));

        assertThatThrownBy(() -> vinculoService.criarOuAtualizar(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é permitido vincular itens a um fornecedor INATIVO");
    }

    @Test
    @DisplayName("Deve alternar status de ativação do vínculo comercial")
    void deveAlternarStatusAtivacao() {
        VinculoItemFornecedor vinculo = new VinculoItemFornecedor(
                fornecedorAtivo, produto, "COD-Y", "Produto Y", new BigDecimal("50.00")
        );
        vinculo.setId(123L);
        vinculo.setAtivo(true);

        when(vinculoRepository.findById(123L)).thenReturn(Optional.of(vinculo));
        when(vinculoRepository.save(any(VinculoItemFornecedor.class))).thenAnswer(i -> i.getArgument(0));

        VinculoItemFornecedorResponseDto response = vinculoService.alternarStatus(123L, false);

        assertThat(response.getAtivo()).isFalse();
    }
}
