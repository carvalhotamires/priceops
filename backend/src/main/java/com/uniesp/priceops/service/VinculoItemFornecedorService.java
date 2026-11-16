package com.uniesp.priceops.service;

import com.uniesp.priceops.dto.VinculoItemFornecedorRequestDto;
import com.uniesp.priceops.dto.VinculoItemFornecedorResponseDto;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.Produto;
import com.uniesp.priceops.model.VinculoItemFornecedor;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ProdutoRepository;
import com.uniesp.priceops.repository.VinculoItemFornecedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class VinculoItemFornecedorService {

    @Autowired
    private VinculoItemFornecedorRepository vinculoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Transactional
    public VinculoItemFornecedorResponseDto criarOuAtualizar(VinculoItemFornecedorRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dados de requisição não podem ser nulos.");
        }
        if (dto.getFornecedorId() == null) {
            throw new IllegalArgumentException("O ID do fornecedor (fornecedorId) é obrigatório.");
        }
        if (dto.getProdutoId() == null) {
            throw new IllegalArgumentException("O ID do produto interno (produtoId) é obrigatório.");
        }
        if (dto.getCodigoFornecedor() == null || dto.getCodigoFornecedor().trim().isEmpty()) {
            throw new IllegalArgumentException("O código/part number do item no fornecedor é obrigatório.");
        }

        Fornecedor fornecedor = fornecedorRepository.findById(dto.getFornecedorId())
                .orElseThrow(() -> new NoSuchElementException("Fornecedor não encontrado com ID: " + dto.getFornecedorId()));

        if (!"ATIVO".equalsIgnoreCase(fornecedor.getStatus())) {
            throw new IllegalArgumentException("Não é permitido vincular itens a um fornecedor INATIVO.");
        }

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado no catálogo com ID: " + dto.getProdutoId()));

        String codigoLimpo = dto.getCodigoFornecedor().trim();
        Optional<VinculoItemFornecedor> existenteOpt = vinculoRepository
                .findByFornecedorIdAndCodigoFornecedorIgnoreCase(fornecedor.getId(), codigoLimpo);

        VinculoItemFornecedor vinculo;
        if (existenteOpt.isPresent()) {
            vinculo = existenteOpt.get();
            vinculo.setProduto(produto);
            if (dto.getDescricaoNoFornecedor() != null) {
                vinculo.setDescricaoNoFornecedor(dto.getDescricaoNoFornecedor().trim());
            }
            if (dto.getUltimoPrecoRegistrado() != null) {
                vinculo.setUltimoPrecoRegistrado(dto.getUltimoPrecoRegistrado());
            }
            if (dto.getAtivo() != null) {
                vinculo.setAtivo(dto.getAtivo());
            }
        } else {
            vinculo = new VinculoItemFornecedor(
                    fornecedor,
                    produto,
                    codigoLimpo,
                    dto.getDescricaoNoFornecedor() != null ? dto.getDescricaoNoFornecedor().trim() : null,
                    dto.getUltimoPrecoRegistrado()
            );
            if (dto.getAtivo() != null) {
                vinculo.setAtivo(dto.getAtivo());
            }
        }

        VinculoItemFornecedor salvo = vinculoRepository.save(vinculo);
        return VinculoItemFornecedorResponseDto.fromEntity(salvo);
    }

    /**
     * Aprendizado automático do De-Para: memoriza o vínculo a partir de uma resolução de pendência
     */
    @Transactional
    public VinculoItemFornecedor salvarOuAtualizarVinculoAutomatico(
            Fornecedor fornecedor,
            Produto produto,
            String codigoFornecedor,
            String descricaoNoFornecedor,
            BigDecimal preco) {

        if (fornecedor == null || produto == null || codigoFornecedor == null || codigoFornecedor.isBlank()) {
            return null;
        }

        String codigoLimpo = codigoFornecedor.trim();
        Optional<VinculoItemFornecedor> existenteOpt = vinculoRepository
                .findByFornecedorIdAndCodigoFornecedorIgnoreCase(fornecedor.getId(), codigoLimpo);

        VinculoItemFornecedor vinculo;
        if (existenteOpt.isPresent()) {
            vinculo = existenteOpt.get();
            vinculo.setProduto(produto);
            vinculo.setAtivo(true);
            if (descricaoNoFornecedor != null && !descricaoNoFornecedor.isBlank()) {
                vinculo.setDescricaoNoFornecedor(descricaoNoFornecedor.trim());
            }
            if (preco != null) {
                vinculo.setUltimoPrecoRegistrado(preco);
            }
        } else {
            vinculo = new VinculoItemFornecedor(
                    fornecedor,
                    produto,
                    codigoLimpo,
                    descricaoNoFornecedor != null ? descricaoNoFornecedor.trim() : null,
                    preco
            );
        }

        return vinculoRepository.save(vinculo);
    }

    public List<VinculoItemFornecedorResponseDto> listar(Long fornecedorId, Long produtoId, String codigoFornecedor, Boolean apenasAtivos) {
        List<VinculoItemFornecedor> vinculos;

        if (fornecedorId != null && codigoFornecedor != null && !codigoFornecedor.isBlank()) {
            vinculos = vinculoRepository.findByFornecedorIdAndCodigoFornecedorIgnoreCase(fornecedorId, codigoFornecedor.trim())
                    .map(List::of)
                    .orElse(List.of());
        } else if (fornecedorId != null && Boolean.TRUE.equals(apenasAtivos)) {
            vinculos = vinculoRepository.findByFornecedorIdAndAtivoTrue(fornecedorId);
        } else if (fornecedorId != null) {
            vinculos = vinculoRepository.findByFornecedorId(fornecedorId);
        } else if (produtoId != null) {
            vinculos = vinculoRepository.findByProdutoId(produtoId);
        } else {
            vinculos = vinculoRepository.findAll();
        }

        return vinculos.stream()
                .map(VinculoItemFornecedorResponseDto::fromEntity)
                .toList();
    }

    public VinculoItemFornecedorResponseDto buscarPorId(Long id) {
        VinculoItemFornecedor vinculo = vinculoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vínculo comercial não encontrado com ID: " + id));
        return VinculoItemFornecedorResponseDto.fromEntity(vinculo);
    }

    @Transactional
    public VinculoItemFornecedorResponseDto alternarStatus(Long id, boolean ativo) {
        VinculoItemFornecedor vinculo = vinculoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vínculo comercial não encontrado com ID: " + id));
        vinculo.setAtivo(ativo);
        VinculoItemFornecedor salvo = vinculoRepository.save(vinculo);
        return VinculoItemFornecedorResponseDto.fromEntity(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        VinculoItemFornecedor vinculo = vinculoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vínculo comercial não encontrado com ID: " + id));
        vinculoRepository.delete(vinculo);
    }
}
