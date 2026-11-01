package com.uniesp.priceops.service;

import com.uniesp.priceops.dto.ResolucaoPendenciaRequestDto;
import com.uniesp.priceops.model.ItemImportadoPdf;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.ItemImportadoPdfRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ReconciliacaoItemService {

    private static final Pattern DIACRITICS_AND_SPECIAL = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{Punct}]");

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ItemImportadoPdfRepository itemImportadoPdfRepository;

    /**
     * Reconcilia um item extraído com o catálogo de produtos interno.
     */
    public void reconciliarItem(ItemImportadoPdf item) {
        if (item == null) {
            return;
        }

        // 1. Tentar correspondência exata por código de fabricante / SKU
        if (item.getCodigoFornecedor() != null && !item.getCodigoFornecedor().isBlank()) {
            Optional<Produto> produtoPorCodigo = produtoRepository.findByCodigoIgnoreCase(item.getCodigoFornecedor().trim());
            if (produtoPorCodigo.isPresent()) {
                item.setProduto(produtoPorCodigo.get());
                item.setStatusItem("VINCULADO");
                item.setConfiancaMatching(100.0);
                item.setMotivoPendencia(null);
                return;
            }
        }

        // 2. Tentar correspondência textual por similaridade na descrição
        List<Produto> todosProdutos = produtoRepository.findAll();
        Produto melhorCandidato = null;
        double maiorSimilaridade = 0.0;

        for (Produto produto : todosProdutos) {
            double similaridade = calcularSimilaridadeDescricao(item.getDescricao(), produto.getNome());
            if (similaridade > maiorSimilaridade) {
                maiorSimilaridade = similaridade;
                melhorCandidato = produto;
            }
        }

        item.setConfiancaMatching(Math.round(maiorSimilaridade * 100.0) / 100.0);

        if (maiorSimilaridade >= 85.0 && melhorCandidato != null) {
            item.setProduto(melhorCandidato);
            item.setStatusItem("VINCULADO");
            item.setMotivoPendencia(null);
        } else if (maiorSimilaridade >= 50.0 && melhorCandidato != null) {
            item.setProduto(melhorCandidato);
            item.setStatusItem("PENDENTE_VINCULO");
            item.setMotivoPendencia("Correspondência provável (" + item.getConfiancaMatching() + "%). Requer confirmação.");
        } else {
            item.setProduto(null);
            item.setStatusItem("PENDENTE_VINCULO");
            item.setMotivoPendencia("Item não identificado no catálogo de produtos.");
        }
    }

    /**
     * Resolve manualmente a pendência de um item (vincular ou ignorar).
     */
    @Transactional
    public ItemImportadoPdf resolverPendencia(Long itemId, ResolucaoPendenciaRequestDto dto) {
        ItemImportadoPdf item = itemImportadoPdfRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Item da importação não encontrado com ID: " + itemId));

        if (dto == null || dto.getAcao() == null || dto.getAcao().isBlank()) {
            throw new IllegalArgumentException("A ação de resolução ('VINCULAR' ou 'IGNORAR') é obrigatória.");
        }

        String acao = dto.getAcao().trim().toUpperCase();

        if ("VINCULAR".equals(acao)) {
            if (dto.getProdutoId() == null) {
                throw new IllegalArgumentException("O ID do produto (produtoId) é obrigatório para vincular o item.");
            }
            Produto produto = produtoRepository.findById(dto.getProdutoId())
                    .orElseThrow(() -> new NoSuchElementException("Produto não encontrado no catálogo com ID: " + dto.getProdutoId()));

            item.setProduto(produto);
            item.setStatusItem("VINCULADO");
            item.setConfiancaMatching(100.0);
            item.setMotivoPendencia(dto.getObservacao() != null && !dto.getObservacao().isBlank()
                    ? dto.getObservacao()
                    : "Vinculado manualmente pelo operador");
        } else if ("IGNORAR".equals(acao)) {
            item.setProduto(null);
            item.setStatusItem("IGNORADO");
            item.setMotivoPendencia(dto.getObservacao() != null && !dto.getObservacao().isBlank()
                    ? dto.getObservacao()
                    : "Ignorado manualmente pelo operador");
        } else {
            throw new IllegalArgumentException("Ação inválida. Utilize 'VINCULAR' ou 'IGNORAR'.");
        }

        return itemImportadoPdfRepository.save(item);
    }

    /**
     * Calcula o coeficiente de similaridade Jaccard ponderado entre duas strings.
     */
    double calcularSimilaridadeDescricao(String texto1, String texto2) {
        if (texto1 == null || texto2 == null || texto1.isBlank() || texto2.isBlank()) {
            return 0.0;
        }

        Set<String> tokens1 = extrairTokens(texto1);
        Set<String> tokens2 = extrairTokens(texto2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersecao = new HashSet<>(tokens1);
        intersecao.retainAll(tokens2);

        Set<String> uniao = new HashSet<>(tokens1);
        uniao.addAll(tokens2);

        double jaccard = (double) intersecao.size() / uniao.size();

        // Se uma string contém quase todas as palavras chave da outra, bonifica
        double inclusao = Math.max((double) intersecao.size() / tokens1.size(),
                                   (double) intersecao.size() / tokens2.size());

        double pontuacaoFinal = ((jaccard * 0.4) + (inclusao * 0.6)) * 100.0;
        return Math.min(100.0, pontuacaoFinal);
    }

    private Set<String> extrairTokens(String texto) {
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        String semAcentos = DIACRITICS_AND_SPECIAL.matcher(normalizado).replaceAll(" ");
        String[] palavras = semAcentos.toLowerCase().split("\\s+");

        Set<String> tokens = new HashSet<>();
        for (String palavra : palavras) {
            String p = palavra.trim();
            if (p.length() >= 2) {
                tokens.add(p);
            }
        }
        return tokens;
    }
}
