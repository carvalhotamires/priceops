package com.uniesp.priceops.service;

import com.uniesp.priceops.dto.ComparativoPrecoProdutoResponseDto;
import com.uniesp.priceops.dto.CotacaoFornecedorDto;
import com.uniesp.priceops.dto.VariacaoHistoricaProdutoDto;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.HistoricoPreco;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.HistoricoPrecoRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalisePrecoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private HistoricoPrecoRepository historicoPrecoRepository;

    @Mock
    private FornecedorRepository fornecedorRepository;

    @InjectMocks
    private AnalisePrecoService analisePrecoService;

    private Produto produtoSsd;
    private Fornecedor fornecedorAlpha;
    private Fornecedor fornecedorBeta;
    private Fornecedor fornecedorGamma;

    @BeforeEach
    void setUp() {
        produtoSsd = new Produto();
        produtoSsd.setId(1L);
        produtoSsd.setCodigo("INF-01");
        produtoSsd.setNome("SSD Kingston NV2 1TB M.2 NVMe");
        produtoSsd.setPrecoCompraAtual(new BigDecimal("280.00"));
        produtoSsd.setPrecoVendaAtual(new BigDecimal("399.00"));

        fornecedorAlpha = new Fornecedor();
        fornecedorAlpha.setId(10L);
        fornecedorAlpha.setNomeFantasia("Distribuidora Alpha Tech");

        fornecedorBeta = new Fornecedor();
        fornecedorBeta.setId(20L);
        fornecedorBeta.setNomeFantasia("Distribuidora Beta Informática");

        fornecedorGamma = new Fornecedor();
        fornecedorGamma.setId(30L);
        fornecedorGamma.setNomeFantasia("Distribuidora Gamma Hardware");
    }

    private HistoricoPreco criarRegistro(Long id, Produto produto, Fornecedor fornecedor, BigDecimal preco, LocalDateTime data) {
        HistoricoPreco h = new HistoricoPreco();
        h.setId(id);
        h.setProduto(produto);
        h.setFornecedor(fornecedor);
        h.setPrecoRegistrado(preco);
        h.setRegistradoEm(data);
        h.setOrigem("IMPORTACAO_PDF");
        return h;
    }

    @Test
    @DisplayName("Deve calcular variação histórica com alta recente (+25.00%)")
    void deveCalcularVariacaoHistoricaComAltaRecente() {
        LocalDateTime agora = LocalDateTime.now();
        List<HistoricoPreco> historico = List.of(
                criarRegistro(1L, produtoSsd, fornecedorAlpha, new BigDecimal("100.00"), agora.minusDays(5)),
                criarRegistro(2L, produtoSsd, fornecedorAlpha, new BigDecimal("125.00"), agora)
        );

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoSsd));
        when(historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(1L)).thenReturn(historico);

        VariacaoHistoricaProdutoDto resultado = analisePrecoService.calcularVariacaoHistorica(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getProdutoId()).isEqualTo(1L);
        assertThat(resultado.getPrecoAnterior()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getUltimoPrecoRegistrado()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(resultado.getPercentualVariacaoRecente()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(resultado.getTendencia()).isEqualTo("ALTA");
        assertThat(resultado.getMenorPrecoHistorico()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(resultado.getMaiorPrecoHistorico()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(resultado.getPrecoMedioHistorico()).isEqualByComparingTo(new BigDecimal("112.50"));
        assertThat(resultado.getTotalRegistrosHistorico()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve calcular variação histórica com queda recente (-25.00%) e sinalizar oportunidade de compra")
    void deveCalcularVariacaoHistoricaComQuedaRecenteEOportunidadeCompra() {
        LocalDateTime agora = LocalDateTime.now();
        List<HistoricoPreco> historico = List.of(
                criarRegistro(1L, produtoSsd, fornecedorBeta, new BigDecimal("200.00"), agora.minusDays(10)),
                criarRegistro(2L, produtoSsd, fornecedorBeta, new BigDecimal("150.00"), agora)
        );

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoSsd));
        when(historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(1L)).thenReturn(historico);

        VariacaoHistoricaProdutoDto resultado = analisePrecoService.calcularVariacaoHistorica(1L);

        assertThat(resultado.getPrecoAnterior()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(resultado.getUltimoPrecoRegistrado()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(resultado.getPercentualVariacaoRecente()).isEqualByComparingTo(new BigDecimal("-25.00"));
        assertThat(resultado.getTendencia()).isEqualTo("QUEDA");
        assertThat(resultado.getClassificacaoOportunidade()).isEqualTo("OPORTUNIDADE_COMPRA");
    }

    @Test
    @DisplayName("Deve tratar produto com apenas 1 registro histórico (variação zero e status adequado)")
    void deveTratarProdutoComApenasUmRegistro() {
        LocalDateTime agora = LocalDateTime.now();
        List<HistoricoPreco> historico = List.of(
                criarRegistro(1L, produtoSsd, fornecedorAlpha, new BigDecimal("280.00"), agora)
        );

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoSsd));
        when(historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(1L)).thenReturn(historico);

        VariacaoHistoricaProdutoDto resultado = analisePrecoService.calcularVariacaoHistorica(1L);

        assertThat(resultado.getTotalRegistrosHistorico()).isEqualTo(1);
        assertThat(resultado.getPercentualVariacaoRecente()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.getTendencia()).isEqualTo("SEM_HISTORICO_SUFICIENTE");
        assertThat(resultado.getMenorPrecoHistorico()).isEqualByComparingTo(new BigDecimal("280.00"));
        assertThat(resultado.getMaiorPrecoHistorico()).isEqualByComparingTo(new BigDecimal("280.00"));
        assertThat(resultado.getPrecoMedioHistorico()).isEqualByComparingTo(new BigDecimal("280.00"));
    }

    @Test
    @DisplayName("Deve tratar produto sem nenhum histórico de cotações")
    void deveTratarProdutoSemHistorico() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoSsd));
        when(historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(1L)).thenReturn(new ArrayList<>());

        VariacaoHistoricaProdutoDto resultado = analisePrecoService.calcularVariacaoHistorica(1L);

        assertThat(resultado.getTotalRegistrosHistorico()).isEqualTo(0);
        assertThat(resultado.getTendencia()).isEqualTo("SEM_HISTORICO_SUFICIENTE");
        assertThat(resultado.getPrecoCompraBase()).isEqualByComparingTo(new BigDecimal("280.00"));
        assertThat(resultado.getUltimoPrecoRegistrado()).isNull();
    }

    @Test
    @DisplayName("Deve comparar múltiplos fornecedores e ranquear menor preço com economia máxima")
    void deveCompararMultiplosFornecedoresERanquearMelhorOferta() {
        LocalDateTime agora = LocalDateTime.now();
        List<HistoricoPreco> historico = List.of(
                criarRegistro(1L, produtoSsd, fornecedorAlpha, new BigDecimal("80.00"), agora.minusDays(2)),
                criarRegistro(2L, produtoSsd, fornecedorBeta, new BigDecimal("100.00"), agora.minusDays(1)),
                criarRegistro(3L, produtoSsd, fornecedorGamma, new BigDecimal("120.00"), agora)
        );

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoSsd));
        when(historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(1L)).thenReturn(historico);

        ComparativoPrecoProdutoResponseDto comparativo = analisePrecoService.compararPrecosFornecedores(1L);

        assertThat(comparativo).isNotNull();
        assertThat(comparativo.getProdutoId()).isEqualTo(1L);
        assertThat(comparativo.getMelhorFornecedorNome()).isEqualTo("Distribuidora Alpha Tech");
        assertThat(comparativo.getMenorPreco()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(comparativo.getMaiorPreco()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(comparativo.getEconomiaMaximaAbsoluta()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(comparativo.getEconomiaMaximaPercentual()).isEqualByComparingTo(new BigDecimal("33.33"));
        assertThat(comparativo.getTotalCotacoes()).isEqualTo(3);

        // A primeira cotação da lista ordenada deve ser a melhor oferta
        CotacaoFornecedorDto melhorCotacao = comparativo.getCotacoes().get(0);
        assertThat(melhorCotacao.isMelhorOferta()).isTrue();
        assertThat(melhorCotacao.getFornecedorNome()).isEqualTo("Distribuidora Alpha Tech");
        assertThat(melhorCotacao.getDiferencaMenorPreco()).isEqualByComparingTo(BigDecimal.ZERO);

        // A cotação do meio (Beta R$ 100,00) tem R$ 20,00 de diferença (+25.00% em relação aos R$ 80,00)
        CotacaoFornecedorDto cotacaoBeta = comparativo.getCotacoes().get(1);
        assertThat(cotacaoBeta.isMelhorOferta()).isFalse();
        assertThat(cotacaoBeta.getDiferencaMenorPreco()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(cotacaoBeta.getPercentualDiferencaMelhor()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Deve considerar a cotação mais recente de cada fornecedor no comparativo")
    void deveConsiderarCotacaoMaisRecenteDeCadaFornecedor() {
        LocalDateTime agora = LocalDateTime.now();
        List<HistoricoPreco> historico = List.of(
                // Alpha cotou R$ 90,00 há 3 dias e R$ 75,00 hoje
                criarRegistro(1L, produtoSsd, fornecedorAlpha, new BigDecimal("90.00"), agora.minusDays(3)),
                criarRegistro(2L, produtoSsd, fornecedorBeta, new BigDecimal("100.00"), agora.minusDays(2)),
                criarRegistro(3L, produtoSsd, fornecedorAlpha, new BigDecimal("75.00"), agora)
        );

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoSsd));
        when(historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(1L)).thenReturn(historico);

        ComparativoPrecoProdutoResponseDto comparativo = analisePrecoService.compararPrecosFornecedores(1L);

        assertThat(comparativo.getTotalCotacoes()).isEqualTo(2); // Apenas Alpha e Beta
        assertThat(comparativo.getMenorPreco()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(comparativo.getMelhorFornecedorNome()).isEqualTo("Distribuidora Alpha Tech");
    }

    @Test
    @DisplayName("Deve lançar NoSuchElementException quando produto não existir")
    void deveLancarExcecaoQuandoProdutoNaoExistir() {
        when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analisePrecoService.calcularVariacaoHistorica(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Produto não encontrado");
    }
}
