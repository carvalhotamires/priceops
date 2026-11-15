package com.uniesp.priceops.service;

import com.uniesp.priceops.dto.ComparativoPrecoProdutoResponseDto;
import com.uniesp.priceops.dto.CotacaoFornecedorDto;
import com.uniesp.priceops.dto.VariacaoHistoricaProdutoDto;
import com.uniesp.priceops.model.HistoricoPreco;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.HistoricoPrecoRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class AnalisePrecoService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal CINCO_NEGATIVO = new BigDecimal("-5.00");
    private static final BigDecimal QUINZE_POSITIVO = new BigDecimal("15.00");

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private HistoricoPrecoRepository historicoPrecoRepository;

    @Autowired(required = false)
    private FornecedorRepository fornecedorRepository;

    /**
     * Calcula a variação temporal de preços de um produto, oscilações recentes e menor histórico.
     */
    public VariacaoHistoricaProdutoDto calcularVariacaoHistorica(Long produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com id: " + produtoId));

        List<HistoricoPreco> historico = historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(produtoId);

        VariacaoHistoricaProdutoDto dto = new VariacaoHistoricaProdutoDto();
        dto.setProdutoId(produto.getId());
        dto.setCodigo(produto.getCodigo());
        dto.setNome(produto.getNome());
        dto.setPrecoCompraBase(produto.getPrecoCompraAtual());
        dto.setTotalRegistrosHistorico(historico.size());

        if (historico.isEmpty()) {
            dto.setTendencia("SEM_HISTORICO_SUFICIENTE");
            dto.setClassificacaoOportunidade("SEM_HISTORICO");
            dto.setMensagemAnalise("Nenhuma cotação histórica registrada para este item no banco de dados.");
            return dto;
        }

        BigDecimal soma = BigDecimal.ZERO;
        BigDecimal menorPreco = historico.get(0).getPrecoRegistrado();
        BigDecimal maiorPreco = historico.get(0).getPrecoRegistrado();

        for (HistoricoPreco h : historico) {
            BigDecimal preco = h.getPrecoRegistrado();
            soma = soma.add(preco);
            if (preco.compareTo(menorPreco) < 0) {
                menorPreco = preco;
            }
            if (preco.compareTo(maiorPreco) > 0) {
                maiorPreco = preco;
            }
        }

        BigDecimal precoMedio = soma.divide(BigDecimal.valueOf(historico.size()), 2, RoundingMode.HALF_UP);
        dto.setMenorPrecoHistorico(menorPreco.setScale(2, RoundingMode.HALF_UP));
        dto.setMaiorPrecoHistorico(maiorPreco.setScale(2, RoundingMode.HALF_UP));
        dto.setPrecoMedioHistorico(precoMedio);

        HistoricoPreco ultimoRegistro = historico.get(historico.size() - 1);
        dto.setUltimoPrecoRegistrado(ultimoRegistro.getPrecoRegistrado().setScale(2, RoundingMode.HALF_UP));

        if (historico.size() == 1) {
            dto.setPrecoAnterior(ultimoRegistro.getPrecoRegistrado().setScale(2, RoundingMode.HALF_UP));
            dto.setPercentualVariacaoRecente(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            dto.setPercentualVariacaoMenorHistorico(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            dto.setTendencia("SEM_HISTORICO_SUFICIENTE");
            dto.setClassificacaoOportunidade("PRECO_ESTAVEL");
            dto.setMensagemAnalise("Apenas 1 registro encontrado. Aguardando novas cotações para cálculo de tendência.");
            return dto;
        }

        HistoricoPreco registroAnterior = historico.get(historico.size() - 2);
        BigDecimal precoAnterior = registroAnterior.getPrecoRegistrado().setScale(2, RoundingMode.HALF_UP);
        dto.setPrecoAnterior(precoAnterior);

        // Variação Recente: ((Ultimo - Anterior) / Anterior) * 100
        BigDecimal variacaoRecente = calcularVariacaoPercentual(ultimoRegistro.getPrecoRegistrado(), precoAnterior);
        dto.setPercentualVariacaoRecente(variacaoRecente);

        // Variação contra o menor histórico: ((Ultimo - Menor) / Menor) * 100
        BigDecimal variacaoMenor = calcularVariacaoPercentual(ultimoRegistro.getPrecoRegistrado(), menorPreco);
        dto.setPercentualVariacaoMenorHistorico(variacaoMenor);

        // Definição da Tendência
        if (variacaoRecente.compareTo(BigDecimal.ZERO) > 0) {
            dto.setTendencia("ALTA");
        } else if (variacaoRecente.compareTo(BigDecimal.ZERO) < 0) {
            dto.setTendencia("QUEDA");
        } else {
            dto.setTendencia("ESTAVEL");
        }

        // Classificação da Oportunidade de Compra
        if (variacaoRecente.compareTo(CINCO_NEGATIVO) <= 0 || ultimoRegistro.getPrecoRegistrado().compareTo(precoMedio) < 0) {
            dto.setClassificacaoOportunidade("OPORTUNIDADE_COMPRA");
            dto.setMensagemAnalise("Preço favorável: recente queda de " + variacaoRecente.abs() + "% ou valor abaixo da média histórica.");
        } else if (variacaoRecente.compareTo(QUINZE_POSITIVO) >= 0) {
            dto.setClassificacaoOportunidade("PRECO_DESFAVORAVEL");
            dto.setMensagemAnalise("Atenção: aumento recente expressivo de " + variacaoRecente + "%.");
        } else {
            dto.setClassificacaoOportunidade("PRECO_ESTAVEL");
            dto.setMensagemAnalise("Preço estável dentro da margem de oscilação esperada.");
        }

        return dto;
    }

    /**
     * Compara cotações de fornecedores distintos para um mesmo produto e calcula economias.
     */
    public ComparativoPrecoProdutoResponseDto compararPrecosFornecedores(Long produtoId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com id: " + produtoId));

        List<HistoricoPreco> historico = historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(produtoId);

        ComparativoPrecoProdutoResponseDto resposta = new ComparativoPrecoProdutoResponseDto();
        resposta.setProdutoId(produto.getId());
        resposta.setCodigo(produto.getCodigo());
        resposta.setNome(produto.getNome());

        if (historico.isEmpty()) {
            resposta.setTotalCotacoes(0);
            resposta.setRecomendacaoCompra("Sem cotações registradas para comparação.");
            return resposta;
        }

        // Filtra para considerar apenas a cotação mais recente de cada fornecedor
        Map<Long, HistoricoPreco> cotacaoMaisRecentePorFornecedor = new LinkedHashMap<>();
        for (HistoricoPreco h : historico) {
            if (h.getFornecedor() != null && h.getFornecedor().getId() != null) {
                cotacaoMaisRecentePorFornecedor.put(h.getFornecedor().getId(), h);
            }
        }

        List<HistoricoPreco> cotacoesFiltradas = new ArrayList<>(cotacaoMaisRecentePorFornecedor.values());
        if (cotacoesFiltradas.isEmpty()) {
            resposta.setTotalCotacoes(0);
            resposta.setRecomendacaoCompra("Nenhum fornecedor válido associado às cotações.");
            return resposta;
        }

        // Determina Menor e Maior Preço
        BigDecimal menorPreco = cotacoesFiltradas.get(0).getPrecoRegistrado();
        BigDecimal maiorPreco = cotacoesFiltradas.get(0).getPrecoRegistrado();
        Long melhorFornecedorId = cotacoesFiltradas.get(0).getFornecedor().getId();
        String melhorFornecedorNome = cotacoesFiltradas.get(0).getFornecedor().getNomeFantasia();
        BigDecimal somaTotal = BigDecimal.ZERO;

        for (HistoricoPreco h : cotacoesFiltradas) {
            BigDecimal preco = h.getPrecoRegistrado();
            somaTotal = somaTotal.add(preco);

            if (preco.compareTo(menorPreco) < 0) {
                menorPreco = preco;
                melhorFornecedorId = h.getFornecedor().getId();
                melhorFornecedorNome = h.getFornecedor().getNomeFantasia();
            }
            if (preco.compareTo(maiorPreco) > 0) {
                maiorPreco = preco;
            }
        }

        BigDecimal precoMedio = somaTotal.divide(BigDecimal.valueOf(cotacoesFiltradas.size()), 2, RoundingMode.HALF_UP);
        BigDecimal economiaAbsoluta = maiorPreco.subtract(menorPreco).setScale(2, RoundingMode.HALF_UP);
        BigDecimal economiaPercentual = maiorPreco.compareTo(BigDecimal.ZERO) > 0
                ? economiaAbsoluta.multiply(CEM).divide(maiorPreco, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        resposta.setMelhorFornecedorId(melhorFornecedorId);
        resposta.setMelhorFornecedorNome(melhorFornecedorNome);
        resposta.setMenorPreco(menorPreco.setScale(2, RoundingMode.HALF_UP));
        resposta.setMaiorPreco(maiorPreco.setScale(2, RoundingMode.HALF_UP));
        resposta.setPrecoMedioCotado(precoMedio);
        resposta.setEconomiaMaximaAbsoluta(economiaAbsoluta);
        resposta.setEconomiaMaximaPercentual(economiaPercentual);
        resposta.setTotalCotacoes(cotacoesFiltradas.size());

        // Monta a lista detalhada de cotações com cálculo de gap para a melhor oferta
        List<CotacaoFornecedorDto> listaDto = new ArrayList<>();
        for (HistoricoPreco h : cotacoesFiltradas) {
            BigDecimal preco = h.getPrecoRegistrado().setScale(2, RoundingMode.HALF_UP);
            boolean isMelhor = preco.compareTo(menorPreco) == 0;
            BigDecimal diferenca = preco.subtract(menorPreco).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentualGap = menorPreco.compareTo(BigDecimal.ZERO) > 0
                    ? diferenca.multiply(CEM).divide(menorPreco, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            CotacaoFornecedorDto itemDto = new CotacaoFornecedorDto(
                    h.getFornecedor().getId(),
                    h.getFornecedor().getNomeFantasia(),
                    preco,
                    h.getRegistradoEm(),
                    h.getOrigem(),
                    isMelhor,
                    diferenca,
                    percentualGap
            );
            listaDto.add(itemDto);
        }

        // Ordena por preço cotado crescente (menor preço primeiro)
        listaDto.sort(Comparator.comparing(CotacaoFornecedorDto::getPrecoCotado));
        resposta.setCotacoes(listaDto);

        if (cotacoesFiltradas.size() > 1 && economiaAbsoluta.compareTo(BigDecimal.ZERO) > 0) {
            resposta.setRecomendacaoCompra("Recomendado comprar de '" + melhorFornecedorNome +
                    "'. Economia de até R$ " + economiaAbsoluta + " (" + economiaPercentual + "%) vs fornecedor mais caro.");
        } else {
            resposta.setRecomendacaoCompra("Cotação única ou valores idênticos entre fornecedores.");
        }

        return resposta;
    }

    /**
     * Lista o comparativo de preços para todos os produtos com cotações cadastradas.
     */
    public List<ComparativoPrecoProdutoResponseDto> listarComparativoGeral() {
        List<Produto> produtos = produtoRepository.findAll();
        List<ComparativoPrecoProdutoResponseDto> resultado = new ArrayList<>();

        for (Produto produto : produtos) {
            try {
                ComparativoPrecoProdutoResponseDto comp = compararPrecosFornecedores(produto.getId());
                if (comp.getTotalCotacoes() != null && comp.getTotalCotacoes() > 0) {
                    resultado.add(comp);
                }
            } catch (Exception ignored) {
            }
        }

        return resultado;
    }

    private BigDecimal calcularVariacaoPercentual(BigDecimal valorAtual, BigDecimal valorBase) {
        if (valorBase == null || valorBase.compareTo(BigDecimal.ZERO) <= 0 || valorAtual == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal diferenca = valorAtual.subtract(valorBase);
        return diferenca.multiply(CEM).divide(valorBase, 2, RoundingMode.HALF_UP);
    }
}
