package com.uniesp.priceops.service;

import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.repository.FornecedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FornecedorService {

    @Autowired
    private FornecedorRepository fornecedorRepository;

    public List<Fornecedor> listarTodos() {
        return fornecedorRepository.findAll();
    }

    public List<Fornecedor> buscarPorNome(String nome) {
        return fornecedorRepository.findByNomeFantasiaContainingIgnoreCaseOrRazaoSocialContainingIgnoreCase(nome, nome);
    }

    public List<Fornecedor> buscarPorStatus(String status) {
        return fornecedorRepository.findByStatusIgnoreCase(status);
    }

    public Fornecedor buscarPorId(Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fornecedor não encontrado com id: " + id));
    }

    public Fornecedor salvar(Fornecedor fornecedor) {
        validarCamposObrigatorios(fornecedor);

        String cnpjNormalizado = normalizarCnpj(fornecedor.getCnpj());
        fornecedor.setCnpj(cnpjNormalizado);

        if (fornecedor.getId() == null && fornecedorRepository.findByCnpj(cnpjNormalizado).isPresent()) {
            throw new IllegalArgumentException("Já existe um fornecedor cadastrado com o CNPJ: " + fornecedor.getCnpj());
        }

        if (fornecedor.getUf() != null) {
            fornecedor.setUf(fornecedor.getUf().trim().toUpperCase());
        }

        if (fornecedor.getStatus() == null || fornecedor.getStatus().trim().isEmpty()) {
            fornecedor.setStatus("ATIVO");
        } else {
            fornecedor.setStatus(fornecedor.getStatus().trim().toUpperCase());
        }

        return fornecedorRepository.save(fornecedor);
    }

    public Fornecedor atualizar(Long id, Fornecedor fornecedorAtualizado) {
        Fornecedor fornecedorExistente = buscarPorId(id);

        validarCamposObrigatorios(fornecedorAtualizado);

        String cnpjNormalizado = normalizarCnpj(fornecedorAtualizado.getCnpj());
        Optional<Fornecedor> existenteMesmoCnpj = fornecedorRepository.findByCnpj(cnpjNormalizado);
        if (existenteMesmoCnpj.isPresent() && !existenteMesmoCnpj.get().getId().equals(id)) {
            throw new IllegalArgumentException("Já existe outro fornecedor cadastrado com o CNPJ: " + cnpjNormalizado);
        }

        fornecedorExistente.setRazaoSocial(fornecedorAtualizado.getRazaoSocial());
        fornecedorExistente.setNomeFantasia(fornecedorAtualizado.getNomeFantasia());
        fornecedorExistente.setCnpj(cnpjNormalizado);
        fornecedorExistente.setEmail(fornecedorAtualizado.getEmail());
        fornecedorExistente.setTelefone(fornecedorAtualizado.getTelefone());
        fornecedorExistente.setContatoNome(fornecedorAtualizado.getContatoNome());
        fornecedorExistente.setCidade(fornecedorAtualizado.getCidade());
        fornecedorExistente.setUf(fornecedorAtualizado.getUf() != null ? fornecedorAtualizado.getUf().trim().toUpperCase() : null);
        fornecedorExistente.setPrazoEntregaDias(fornecedorAtualizado.getPrazoEntregaDias());

        if (fornecedorAtualizado.getStatus() != null && !fornecedorAtualizado.getStatus().trim().isEmpty()) {
            fornecedorExistente.setStatus(fornecedorAtualizado.getStatus().trim().toUpperCase());
        }

        return fornecedorRepository.save(fornecedorExistente);
    }

    /**
     * Realiza a desativação lógica (soft delete) do distribuidor homologado,
     * preservando a integridade histórica de compras, importações PDF e alertas.
     */
    public Fornecedor desativarLogicamente(Long id) {
        Fornecedor fornecedor = buscarPorId(id);
        fornecedor.setStatus("INATIVO");
        return fornecedorRepository.save(fornecedor);
    }

    public void deletar(Long id) {
        desativarLogicamente(id);
    }

    private void validarCamposObrigatorios(Fornecedor fornecedor) {
        if (fornecedor.getRazaoSocial() == null || fornecedor.getRazaoSocial().trim().isEmpty()) {
            throw new IllegalArgumentException("A razão social é obrigatória.");
        }
        if (fornecedor.getNomeFantasia() == null || fornecedor.getNomeFantasia().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome fantasia é obrigatório.");
        }
        if (fornecedor.getCnpj() == null || fornecedor.getCnpj().trim().isEmpty()) {
            throw new IllegalArgumentException("O CNPJ é obrigatório.");
        }
        if (fornecedor.getEmail() == null || fornecedor.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("O e-mail de contato é obrigatório.");
        }
        if (fornecedor.getTelefone() == null || fornecedor.getTelefone().trim().isEmpty()) {
            throw new IllegalArgumentException("O telefone de contato é obrigatório.");
        }
        if (fornecedor.getPrazoEntregaDias() != null && fornecedor.getPrazoEntregaDias() < 0) {
            throw new IllegalArgumentException("O prazo de entrega em dias não pode ser negativo.");
        }
    }

    private String normalizarCnpj(String cnpj) {
        String apenasDigitos = cnpj.replaceAll("\\D", "");
        if (apenasDigitos.length() != 14) {
            throw new IllegalArgumentException("O CNPJ deve conter exatamente 14 dígitos numéricos.");
        }
        return apenasDigitos;
    }
}
