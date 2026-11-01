package com.uniesp.priceops.service;

import com.uniesp.priceops.dto.ResolucaoPendenciaRequestDto;
import com.uniesp.priceops.model.ImportacaoPdf;
import com.uniesp.priceops.model.ItemImportadoPdf;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.ItemImportadoPdfRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliacaoItemServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ItemImportadoPdfRepository itemImportadoPdfRepository;

    @InjectMocks
    private ReconciliacaoItemService reconciliacaoItemService;

    private Produto produtoInf01;
    private Produto produtoInf02;
    private ImportacaoPdf importacao;

    @BeforeEach
    void setUp() {
        importacao = new ImportacaoPdf();
        importacao.setId(1L);

        produtoInf01 = new Produto();
        produtoInf01.setId(101L);
        produtoInf01.setCodigo("INF-01");
        produtoInf01.setNome("SSD Kingston 480GB A400 SATA 2.5");
        produtoInf01.setPrecoVendaAtual(new BigDecimal("220.00"));
        produtoInf01.setPrecoCompraAtual(new BigDecimal("180.00"));
        produtoInf01.setEstoqueAtual(20);
        produtoInf01.setEstoqueMinimo(5);
        produtoInf01.setCategoria("Armazenamento");
        produtoInf01.setUnidadeMedida("UN");

        produtoInf02 = new Produto();
        produtoInf02.setId(102L);
        produtoInf02.setCodigo("INF-02");
        produtoInf02.setNome("Memoria RAM Kingston Fury 16GB DDR4 3200MHz");
        produtoInf02.setPrecoVendaAtual(new BigDecimal("300.00"));
        produtoInf02.setPrecoCompraAtual(new BigDecimal("240.00"));
        produtoInf02.setEstoqueAtual(15);
        produtoInf02.setEstoqueMinimo(3);
        produtoInf02.setCategoria("Memórias");
        produtoInf02.setUnidadeMedida("UN");
    }

    @Test
    @DisplayName("Deve vincular item automaticamente quando houver match exato por código")
    void deveVincularItemPorCodigoExato() {
        when(produtoRepository.findByCodigoIgnoreCase("INF-01")).thenReturn(Optional.of(produtoInf01));

        ItemImportadoPdf item = new ItemImportadoPdf(importacao, "INF-01", "SSD Kingston 480GB",
                new BigDecimal("185.00"), 30, 1);

        reconciliacaoItemService.reconciliarItem(item);

        assertThat(item.getStatusItem()).isEqualTo("VINCULADO");
        assertThat(item.getProduto()).isEqualTo(produtoInf01);
        assertThat(item.getConfiancaMatching()).isEqualTo(100.0);
        assertThat(item.getMotivoPendencia()).isNull();
    }

    @Test
    @DisplayName("Deve sugerir vínculo ou vincular quando a similaridade de descrição for alta")
    void deveVincularItemPorSimilaridadeDescricao() {
        when(produtoRepository.findByCodigoIgnoreCase("SEM-COD")).thenReturn(Optional.empty());
        when(produtoRepository.findAll()).thenReturn(List.of(produtoInf01, produtoInf02));

        ItemImportadoPdf item = new ItemImportadoPdf(importacao, "SEM-COD", "SSD Kingston 480GB A400 SATA 2.5",
                new BigDecimal("185.00"), 30, 1);

        reconciliacaoItemService.reconciliarItem(item);

        assertThat(item.getStatusItem()).isEqualTo("VINCULADO");
        assertThat(item.getProduto()).isEqualTo(produtoInf01);
        assertThat(item.getConfiancaMatching()).isGreaterThanOrEqualTo(85.0);
    }

    @Test
    @DisplayName("Deve marcar como PENDENTE_VINCULO quando o item não for encontrado no catálogo")
    void deveMarcarComoPendenteQuandoNaoEncontrarNoCatalogo() {
        when(produtoRepository.findByCodigoIgnoreCase("DESCONHECIDO")).thenReturn(Optional.empty());
        when(produtoRepository.findAll()).thenReturn(List.of(produtoInf01, produtoInf02));

        ItemImportadoPdf item = new ItemImportadoPdf(importacao, "DESCONHECIDO", "Gabinete Gamer RGB Lateral Vidro",
                new BigDecimal("350.00"), 5, 1);

        reconciliacaoItemService.reconciliarItem(item);

        assertThat(item.getStatusItem()).isEqualTo("PENDENTE_VINCULO");
        assertThat(item.getProduto()).isNull();
        assertThat(item.getMotivoPendencia()).contains("Item não identificado no catálogo");
    }

    @Test
    @DisplayName("Deve resolver pendência vinculando manualmente ao produto selecionado pelo operador")
    void deveResolverPendenciaVinculandoManualmente() {
        ItemImportadoPdf item = new ItemImportadoPdf(importacao, "GEN-01", "Item sem de-para",
                new BigDecimal("190.00"), 10, 1);
        item.setId(55L);

        when(itemImportadoPdfRepository.findById(55L)).thenReturn(Optional.of(item));
        when(produtoRepository.findById(101L)).thenReturn(Optional.of(produtoInf01));
        when(itemImportadoPdfRepository.save(any(ItemImportadoPdf.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolucaoPendenciaRequestDto dto = new ResolucaoPendenciaRequestDto(101L, "VINCULAR");
        ItemImportadoPdf resolvido = reconciliacaoItemService.resolverPendencia(55L, dto);

        assertThat(resolvido.getStatusItem()).isEqualTo("VINCULADO");
        assertThat(resolvido.getProduto()).isEqualTo(produtoInf01);
        assertThat(resolvido.getMotivoPendencia()).contains("Vinculado manualmente");
    }

    @Test
    @DisplayName("Deve resolver pendência marcando o item como IGNORADO")
    void deveResolverPendenciaIgnorandoItem() {
        ItemImportadoPdf item = new ItemImportadoPdf(importacao, "GEN-02", "Cabo de força avulso",
                new BigDecimal("15.00"), 100, 2);
        item.setId(56L);

        when(itemImportadoPdfRepository.findById(56L)).thenReturn(Optional.of(item));
        when(itemImportadoPdfRepository.save(any(ItemImportadoPdf.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolucaoPendenciaRequestDto dto = new ResolucaoPendenciaRequestDto(null, "IGNORAR");
        dto.setObservacao("Item de baixo valor não monitorado");
        ItemImportadoPdf ignorado = reconciliacaoItemService.resolverPendencia(56L, dto);

        assertThat(ignorado.getStatusItem()).isEqualTo("IGNORADO");
        assertThat(ignorado.getProduto()).isNull();
        assertThat(ignorado.getMotivoPendencia()).isEqualTo("Item de baixo valor não monitorado");
    }

    @Test
    @DisplayName("Deve falhar ao tentar resolver pendência de item inexistente")
    void deveFalharParaItemInexistente() {
        when(itemImportadoPdfRepository.findById(999L)).thenReturn(Optional.empty());

        ResolucaoPendenciaRequestDto dto = new ResolucaoPendenciaRequestDto(101L, "VINCULAR");

        assertThatThrownBy(() -> reconciliacaoItemService.resolverPendencia(999L, dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Item da importação não encontrado com ID: 999");
    }
}
