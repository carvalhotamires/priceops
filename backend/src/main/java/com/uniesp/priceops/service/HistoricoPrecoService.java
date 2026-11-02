package com.uniesp.priceops.service;

import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.HistoricoPreco;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.HistoricoPrecoRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class HistoricoPrecoService {

    @Autowired
    private HistoricoPrecoRepository historicoPrecoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    // Sempre cria um novo registro no banco (nunca sobrescreve os preços anteriores)
    public HistoricoPreco registrarPreco(Long produtoId, Long fornecedorId, BigDecimal preco, String origem, Long importacaoPdfId) {
        if (produtoId == null) {
            throw new IllegalArgumentException("O ID do produto é obrigatório.");
        }
        if (fornecedorId == null) {
            throw new IllegalArgumentException("O ID do fornecedor é obrigatório.");
        }
        if (preco == null || preco.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com id: " + produtoId));

        Fornecedor fornecedor = fornecedorRepository.findById(fornecedorId)
                .orElseThrow(() -> new NoSuchElementException("Fornecedor não encontrado com id: " + fornecedorId));

        HistoricoPreco novoHistorico = new HistoricoPreco();
        novoHistorico.setProduto(produto);
        novoHistorico.setFornecedor(fornecedor);
        novoHistorico.setPrecoRegistrado(preco);
        novoHistorico.setOrigem(origem != null && !origem.isBlank() ? origem.trim().toUpperCase() : "MANUAL");
        novoHistorico.setImportacaoPdfId(importacaoPdfId);
        novoHistorico.setRegistradoEm(LocalDateTime.now());

        return historicoPrecoRepository.save(novoHistorico);
    }

    public List<HistoricoPreco> listar(Long produtoId, Long fornecedorId) {
        if (produtoId != null && fornecedorId != null) {
            return historicoPrecoRepository.findByProdutoIdAndFornecedorIdOrderByRegistradoEmAscIdAsc(produtoId, fornecedorId);
        }
        if (produtoId != null) {
            return historicoPrecoRepository.findByProdutoIdOrderByRegistradoEmAscIdAsc(produtoId);
        }
        if (fornecedorId != null) {
            return historicoPrecoRepository.findByFornecedorIdOrderByRegistradoEmDescIdDesc(fornecedorId);
        }
        return historicoPrecoRepository.findAllByOrderByRegistradoEmDescIdDesc();
    }

    public HistoricoPreco buscarPorId(Long id) {
        return historicoPrecoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Histórico não encontrado com id: " + id));
    }
}
