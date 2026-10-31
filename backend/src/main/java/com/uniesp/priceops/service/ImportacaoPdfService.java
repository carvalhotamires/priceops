package com.uniesp.priceops.service;

import com.uniesp.priceops.exception.DuplicidadeImportacaoException;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.ImportacaoPdf;
import com.uniesp.priceops.model.LogAuditoriaPdf;
import com.uniesp.priceops.repository.FornecedorRepository;
import com.uniesp.priceops.repository.ImportacaoPdfRepository;
import com.uniesp.priceops.repository.LogAuditoriaPdfRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
public class ImportacaoPdfService {

    private static final Set<String> STATUS_PERMITIDOS = Set.of(
            "RECEBIDO", "EM_PROCESSAMENTO", "PROCESSADO", "ERRO"
    );

    @Autowired
    private ImportacaoPdfRepository importacaoPdfRepository;

    @Autowired
    private LogAuditoriaPdfRepository logAuditoriaPdfRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Value("${priceops.upload.dir:uploads/pdfs}")
    private String diretorioUpload;

    public ImportacaoPdf realizarUpload(
            MultipartFile arquivo,
            Long fornecedorId,
            String usuarioResponsavel,
            String observacao) {

        String usuario = normalizarUsuario(usuarioResponsavel);

        if (arquivo == null || arquivo.isEmpty()) {
            registrarAuditoria("ARQUIVO_INVALIDO", "SEM_NOME", null, 0L, null, fornecedorId, usuario,
                    "Tentativa de upload rejeitada: arquivo PDF ausente ou vazio.");
            throw new IllegalArgumentException("O arquivo PDF é obrigatório e não pode estar vazio.");
        }

        String nomeOriginal = extrairNomeSeguro(arquivo.getOriginalFilename());
        long tamanhoBytes = arquivo.getSize();

        // 1. Vínculo obrigatório com distribuidor homologado com status ATIVO
        if (fornecedorId == null) {
            registrarAuditoria("FORNECEDOR_INVALIDO", nomeOriginal, null, tamanhoBytes, null, null, usuario,
                    "Upload rejeitado: nenhum distribuidor homologado foi vinculado à importação.");
            throw new IllegalArgumentException("O vínculo com um distribuidor homologado (fornecedorId) é obrigatório.");
        }

        Fornecedor fornecedor = fornecedorRepository.findById(fornecedorId)
                .orElseThrow(() -> {
                    registrarAuditoria("FORNECEDOR_INVALIDO", nomeOriginal, null, tamanhoBytes,
                            null, fornecedorId, usuario,
                            "Upload rejeitado: distribuidor informado não encontrado (ID " + fornecedorId + ").");
                    return new IllegalArgumentException("Distribuidor não encontrado com id: " + fornecedorId);
                });

        if (!"ATIVO".equalsIgnoreCase(fornecedor.getStatus())) {
            registrarAuditoria("FORNECEDOR_INATIVO", nomeOriginal, null, tamanhoBytes,
                    null, fornecedorId, usuario,
                    "Upload bloqueado: o distribuidor '" + fornecedor.getNomeFantasia() + "' está com status INATIVO.");
            throw new IllegalArgumentException(
                    "Não é permitido importar tabelas de preços de um distribuidor com status INATIVO."
            );
        }

        // 2. Extração binária e validação de extensão (.pdf) + cabeçalho Magic Bytes (%PDF-)
        byte[] conteudoBinario;
        try {
            conteudoBinario = arquivo.getBytes();
        } catch (IOException e) {
            registrarAuditoria("ARQUIVO_INVALIDO", nomeOriginal, null, tamanhoBytes, null, fornecedorId, usuario,
                    "Falha na extração binária do arquivo enviado.");
            throw new IllegalArgumentException("Não foi possível ler o conteúdo binário do arquivo enviado.");
        }

        if (!possuiExtensaoPdf(nomeOriginal) || !possuiAssinaturaPdfValida(conteudoBinario)) {
            registrarAuditoria("ARQUIVO_INVALIDO", nomeOriginal, null, tamanhoBytes, null, fornecedorId, usuario,
                    "Tentativa de upload rejeitada: extensão (.pdf) ou assinatura binária inválida.");
            throw new IllegalArgumentException("Formato inválido. Apenas arquivos PDF autênticos (.pdf) são permitidos.");
        }

        // 3. Cálculo automático do hash criptográfico SHA-256
        String hashSha256 = calcularSha256(conteudoBinario);

        // 4. Controle de duplicidade para o mesmo fornecedor (HTTP 409 Conflict + Trilha Imutável)
        Optional<ImportacaoPdf> existenteOpt = importacaoPdfRepository.findByFornecedorIdAndHashSha256(
                fornecedorId, hashSha256
        );
        if (existenteOpt.isPresent()) {
            ImportacaoPdf existente = existenteOpt.get();
            String mensagemDuplicidade = String.format(
                    "Reimportação bloqueada por duplicidade: a tabela '%s' possui o mesmo hash SHA-256 do arquivo '%s' (ID %d), já importado para o distribuidor '%s' em %s.",
                    nomeOriginal, existente.getNomeOriginal(), existente.getId(),
                    fornecedor.getNomeFantasia(), existente.getImportadoEm()
            );
            registrarAuditoria("DUPLICIDADE_BLOQUEADA", nomeOriginal, hashSha256, tamanhoBytes,
                    existente.getId(), fornecedorId, usuario, mensagemDuplicidade);
            throw new DuplicidadeImportacaoException(mensagemDuplicidade, existente);
        }

        String nomeArmazenado = fornecedorId + "_" + hashSha256 + ".pdf";
        Path pastaDestino = Paths.get(diretorioUpload).toAbsolutePath().normalize();
        Path caminhoCompleto = pastaDestino.resolve(nomeArmazenado).normalize();

        if (!caminhoCompleto.startsWith(pastaDestino)) {
            throw new IllegalArgumentException("Caminho de armazenamento inválido.");
        }

        try {
            Files.createDirectories(pastaDestino);
            Files.write(caminhoCompleto, conteudoBinario);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao armazenar a tabela PDF no servidor.", e);
        }

        ImportacaoPdf novaImportacao = new ImportacaoPdf();
        novaImportacao.setNomeOriginal(nomeOriginal);
        novaImportacao.setNomeArmazenado(nomeArmazenado);
        novaImportacao.setCaminhoArmazenamento(caminhoCompleto.toString());
        novaImportacao.setContentType("application/pdf");
        novaImportacao.setTamanhoBytes(tamanhoBytes);
        novaImportacao.setHashSha256(hashSha256);
        novaImportacao.setFornecedor(fornecedor);
        novaImportacao.setUsuarioResponsavel(usuario);
        novaImportacao.setObservacao(observacao != null && !observacao.trim().isEmpty() ? observacao.trim() : null);
        novaImportacao.setStatusImportacao("RECEBIDO");

        ImportacaoPdf salva = importacaoPdfRepository.save(novaImportacao);

        registrarAuditoria("UPLOAD_SUCESSO", nomeOriginal, hashSha256, tamanhoBytes,
                salva.getId(), fornecedor.getId(), usuario,
                "Tabela de preços PDF ingerida e auditada com sucesso para o distribuidor '" + fornecedor.getNomeFantasia() + "'.");

        return salva;
    }

    public List<ImportacaoPdf> listarImportacoes(Long fornecedorId, String status, String nome) {
        if (fornecedorId != null) {
            return importacaoPdfRepository.findByFornecedorIdOrderByImportadoEmDesc(fornecedorId);
        }
        if (status != null && !status.trim().isEmpty()) {
            return importacaoPdfRepository.findByStatusImportacaoIgnoreCaseOrderByImportadoEmDesc(status.trim());
        }
        if (nome != null && !nome.trim().isEmpty()) {
            return importacaoPdfRepository.findByNomeOriginalContainingIgnoreCaseOrderByImportadoEmDesc(nome.trim());
        }
        return importacaoPdfRepository.findAllByOrderByImportadoEmDesc();
    }

    public ImportacaoPdf buscarPorId(Long id) {
        return importacaoPdfRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Importação PDF não encontrada com id: " + id));
    }

    public byte[] carregarConteudoArquivo(Long id, String usuarioResponsavel) {
        ImportacaoPdf importacao = buscarPorId(id);
        Path caminho = Paths.get(importacao.getCaminhoArmazenamento());

        if (!Files.exists(caminho)) {
            throw new NoSuchElementException("O arquivo físico não foi encontrado no armazenamento para o id: " + id);
        }

        try {
            byte[] bytes = Files.readAllBytes(caminho);
            registrarAuditoria("DOWNLOAD_ARQUIVO", importacao.getNomeOriginal(), importacao.getHashSha256(),
                    importacao.getTamanhoBytes(), importacao.getId(), importacao.getFornecedor().getId(),
                    normalizarUsuario(usuarioResponsavel),
                    "Download/consulta da tabela PDF realizado.");
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo PDF armazenado.", e);
        }
    }

    public ImportacaoPdf atualizarStatusImportacao(Long id, String novoStatus, String usuarioResponsavel) {
        ImportacaoPdf importacao = buscarPorId(id);

        if (novoStatus == null || novoStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("O novo status de importação é obrigatório.");
        }

        String statusNormalizado = novoStatus.trim().toUpperCase();
        if (!STATUS_PERMITIDOS.contains(statusNormalizado)) {
            throw new IllegalArgumentException("Status inválido. Valores permitidos: " + STATUS_PERMITIDOS);
        }

        String anterior = importacao.getStatusImportacao();
        importacao.setStatusImportacao(statusNormalizado);
        ImportacaoPdf atualizada = importacaoPdfRepository.save(importacao);

        registrarAuditoria("STATUS_ATUALIZADO", atualizada.getNomeOriginal(), atualizada.getHashSha256(),
                atualizada.getTamanhoBytes(), atualizada.getId(), atualizada.getFornecedor().getId(),
                normalizarUsuario(usuarioResponsavel),
                String.format("Status da importação alterado de '%s' para '%s'.", anterior, statusNormalizado));

        return atualizada;
    }

    public void deletar(Long id, String usuarioResponsavel) {
        ImportacaoPdf importacao = buscarPorId(id);
        Path caminho = Paths.get(importacao.getCaminhoArmazenamento());

        try {
            Files.deleteIfExists(caminho);
        } catch (IOException ignored) {
        }

        importacaoPdfRepository.delete(importacao);

        registrarAuditoria("ARQUIVO_REMOVIDO", importacao.getNomeOriginal(), importacao.getHashSha256(),
                importacao.getTamanhoBytes(), importacao.getId(), importacao.getFornecedor().getId(),
                normalizarUsuario(usuarioResponsavel),
                "Registro de importação PDF removido mantendo a trilha imutável de auditoria.");
    }

    public List<LogAuditoriaPdf> listarAuditoria(String status, String hashSha256, Long fornecedorId) {
        if (status != null && !status.trim().isEmpty()) {
            return logAuditoriaPdfRepository.findByStatusAuditoriaIgnoreCaseOrderByDataHoraDesc(status.trim());
        }
        if (hashSha256 != null && !hashSha256.trim().isEmpty()) {
            return logAuditoriaPdfRepository.findByHashSha256OrderByDataHoraDesc(hashSha256.trim().toLowerCase());
        }
        if (fornecedorId != null) {
            return logAuditoriaPdfRepository.findByFornecedorIdOrderByDataHoraDesc(fornecedorId);
        }
        return logAuditoriaPdfRepository.findAllByOrderByDataHoraDesc();
    }

    private void registrarAuditoria(
            String statusAuditoria,
            String nomeArquivo,
            String hashSha256,
            Long tamanhoBytes,
            Long importacaoPdfId,
            Long fornecedorId,
            String usuarioResponsavel,
            String detalhes) {

        LogAuditoriaPdf log = new LogAuditoriaPdf();
        log.setStatusAuditoria(statusAuditoria);
        log.setNomeArquivo(nomeArquivo != null ? nomeArquivo : "DESCONHECIDO");
        log.setHashSha256(hashSha256);
        log.setTamanhoBytes(tamanhoBytes);
        log.setImportacaoPdfId(importacaoPdfId);
        log.setFornecedorId(fornecedorId);
        log.setUsuarioResponsavel(usuarioResponsavel);
        log.setDetalhes(detalhes);
        logAuditoriaPdfRepository.save(log);
    }

    private String normalizarUsuario(String usuarioResponsavel) {
        return (usuarioResponsavel == null || usuarioResponsavel.trim().isEmpty())
                ? "SISTEMA"
                : usuarioResponsavel.trim();
    }

    private String extrairNomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.trim().isEmpty()) {
            return "tabela.pdf";
        }
        String limpo = nomeOriginal.replace("\\", "/");
        int ultimaBarra = limpo.lastIndexOf('/');
        String apenasNome = ultimaBarra >= 0 ? limpo.substring(ultimaBarra + 1) : limpo;
        return apenasNome.trim().isEmpty() ? "tabela.pdf" : apenasNome.trim();
    }

    private boolean possuiExtensaoPdf(String nomeArquivo) {
        return nomeArquivo != null && nomeArquivo.toLowerCase().endsWith(".pdf");
    }

    private boolean possuiAssinaturaPdfValida(byte[] conteudo) {
        return conteudo != null
                && conteudo.length >= 5
                && conteudo[0] == 0x25
                && conteudo[1] == 0x50
                && conteudo[2] == 0x44
                && conteudo[3] == 0x46
                && conteudo[4] == 0x2D;
    }

    private String calcularSha256(byte[] conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(conteudo);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível no ambiente Java.", e);
        }
    }
}
