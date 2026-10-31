package com.uniesp.priceops.service;

import com.uniesp.priceops.exception.DuplicidadeArquivoException;
import com.uniesp.priceops.model.ArquivoPdf;
import com.uniesp.priceops.model.Fornecedor;
import com.uniesp.priceops.model.LogAuditoriaPdf;
import com.uniesp.priceops.repository.ArquivoPdfRepository;
import com.uniesp.priceops.repository.FornecedorRepository;
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
public class ArquivoPdfService {

    private static final Set<String> STATUS_PERMITIDOS = Set.of(
            "RECEBIDO", "EM_PROCESSAMENTO", "PROCESSADO", "ERRO"
    );

    @Autowired
    private ArquivoPdfRepository arquivoPdfRepository;

    @Autowired
    private LogAuditoriaPdfRepository logAuditoriaPdfRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Value("${priceops.upload.dir:uploads/pdfs}")
    private String diretorioUpload;

    public ArquivoPdf realizarUpload(
            MultipartFile arquivo,
            Long fornecedorId,
            String usuarioResponsavel,
            String observacao) {

        String usuario = normalizarUsuario(usuarioResponsavel);

        if (arquivo == null || arquivo.isEmpty()) {
            registrarAuditoria("ARQUIVO_INVALIDO", "SEM_NOME", null, 0L, null, fornecedorId, usuario,
                    "Tentativa de upload rejeitada: arquivo ausente ou vazio.");
            throw new IllegalArgumentException("O arquivo PDF é obrigatório e não pode estar vazio.");
        }

        String nomeOriginal = extrairNomeSeguro(arquivo.getOriginalFilename());
        long tamanhoBytes = arquivo.getSize();

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            registrarAuditoria("ARQUIVO_INVALIDO", nomeOriginal, null, tamanhoBytes, null, fornecedorId, usuario,
                    "Falha na leitura do conteúdo do arquivo enviado.");
            throw new IllegalArgumentException("Não foi possível ler o conteúdo do arquivo enviado.");
        }

        if (!possuiExtensaoPdf(nomeOriginal) || !possuiAssinaturaPdfValida(conteudo)) {
            registrarAuditoria("ARQUIVO_INVALIDO", nomeOriginal, null, tamanhoBytes, null, fornecedorId, usuario,
                    "Tentativa de upload rejeitada: extensão ou cabeçalho binário não corresponde a um PDF válido.");
            throw new IllegalArgumentException("Formato de arquivo inválido. Apenas arquivos PDF autênticos (.pdf) são permitidos.");
        }

        String hashSha256 = calcularSha256(conteudo);

        // Prevenção de duplicidade por conteúdo binário (SHA-256)
        Optional<ArquivoPdf> existenteOpt = arquivoPdfRepository.findByHashSha256(hashSha256);
        if (existenteOpt.isPresent()) {
            ArquivoPdf existente = existenteOpt.get();
            String mensagemDuplicidade = String.format(
                    "Upload bloqueado por duplicidade: este arquivo PDF possui exatamente o mesmo conteúdo do arquivo '%s' (ID %d), enviado em %s.",
                    existente.getNomeOriginal(), existente.getId(), existente.getEnviadoEm()
            );
            registrarAuditoria("DUPLICIDADE_BLOQUEADA", nomeOriginal, hashSha256, tamanhoBytes,
                    existente.getId(), fornecedorId, usuario, mensagemDuplicidade);
            throw new DuplicidadeArquivoException(mensagemDuplicidade, existente);
        }

        Fornecedor fornecedor = null;
        if (fornecedorId != null) {
            fornecedor = fornecedorRepository.findById(fornecedorId)
                    .orElseThrow(() -> {
                        registrarAuditoria("ARQUIVO_INVALIDO", nomeOriginal, hashSha256, tamanhoBytes,
                                null, fornecedorId, usuario,
                                "Upload rejeitado: fornecedor informado não existe (ID " + fornecedorId + ").");
                        return new IllegalArgumentException("Fornecedor não encontrado com id: " + fornecedorId);
                    });
        }

        String nomeArmazenado = hashSha256 + ".pdf";
        Path pastaDestino = Paths.get(diretorioUpload).toAbsolutePath().normalize();
        Path caminhoCompleto = pastaDestino.resolve(nomeArmazenado).normalize();

        if (!caminhoCompleto.startsWith(pastaDestino)) {
            throw new IllegalArgumentException("Caminho de armazenamento inválido.");
        }

        try {
            Files.createDirectories(pastaDestino);
            Files.write(caminhoCompleto, conteudo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao armazenar o arquivo PDF no servidor.", e);
        }

        ArquivoPdf novoArquivo = new ArquivoPdf();
        novoArquivo.setNomeOriginal(nomeOriginal);
        novoArquivo.setNomeArmazenado(nomeArmazenado);
        novoArquivo.setCaminhoArmazenamento(caminhoCompleto.toString());
        novoArquivo.setContentType("application/pdf");
        novoArquivo.setTamanhoBytes(tamanhoBytes);
        novoArquivo.setHashSha256(hashSha256);
        novoArquivo.setFornecedor(fornecedor);
        novoArquivo.setUsuarioResponsavel(usuario);
        novoArquivo.setObservacao(observacao != null && !observacao.trim().isEmpty() ? observacao.trim() : null);
        novoArquivo.setStatusProcessamento("RECEBIDO");

        ArquivoPdf salvo = arquivoPdfRepository.save(novoArquivo);

        registrarAuditoria("UPLOAD_SUCESSO", nomeOriginal, hashSha256, tamanhoBytes,
                salvo.getId(), fornecedor != null ? fornecedor.getId() : null, usuario,
                "Upload realizado e auditado com sucesso.");

        return salvo;
    }

    public List<ArquivoPdf> listarArquivos(Long fornecedorId, String status, String nome) {
        if (fornecedorId != null) {
            return arquivoPdfRepository.findByFornecedorIdOrderByEnviadoEmDesc(fornecedorId);
        }
        if (status != null && !status.trim().isEmpty()) {
            return arquivoPdfRepository.findByStatusProcessamentoIgnoreCaseOrderByEnviadoEmDesc(status.trim());
        }
        if (nome != null && !nome.trim().isEmpty()) {
            return arquivoPdfRepository.findByNomeOriginalContainingIgnoreCaseOrderByEnviadoEmDesc(nome.trim());
        }
        return arquivoPdfRepository.findAllByOrderByEnviadoEmDesc();
    }

    public ArquivoPdf buscarPorId(Long id) {
        return arquivoPdfRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Arquivo PDF não encontrado com id: " + id));
    }

    public Optional<ArquivoPdf> verificarDuplicidadePorHash(String hashSha256) {
        if (hashSha256 == null || !hashSha256.trim().matches("^[a-fA-F0-9]{64}$")) {
            throw new IllegalArgumentException("O hash SHA-256 informado deve conter exatamente 64 caracteres hexadecimais.");
        }
        return arquivoPdfRepository.findByHashSha256(hashSha256.trim().toLowerCase());
    }

    public byte[] carregarConteudoArquivo(Long id, String usuarioResponsavel) {
        ArquivoPdf arquivo = buscarPorId(id);
        Path caminho = Paths.get(arquivo.getCaminhoArmazenamento());

        if (!Files.exists(caminho)) {
            throw new NoSuchElementException("O arquivo físico não foi encontrado no armazenamento para o id: " + id);
        }

        try {
            byte[] bytes = Files.readAllBytes(caminho);
            registrarAuditoria("DOWNLOAD_ARQUIVO", arquivo.getNomeOriginal(), arquivo.getHashSha256(),
                    arquivo.getTamanhoBytes(), arquivo.getId(),
                    arquivo.getFornecedor() != null ? arquivo.getFornecedor().getId() : null,
                    normalizarUsuario(usuarioResponsavel),
                    "Download/consulta do arquivo PDF realizado.");
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo PDF armazenado.", e);
        }
    }

    public ArquivoPdf atualizarStatusProcessamento(Long id, String novoStatus, String usuarioResponsavel) {
        ArquivoPdf arquivo = buscarPorId(id);

        if (novoStatus == null || novoStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("O novo status de processamento é obrigatório.");
        }

        String statusNormalizado = novoStatus.trim().toUpperCase();
        if (!STATUS_PERMITIDOS.contains(statusNormalizado)) {
            throw new IllegalArgumentException("Status inválido. Valores permitidos: " + STATUS_PERMITIDOS);
        }

        String statusAnterior = arquivo.getStatusProcessamento();
        arquivo.setStatusProcessamento(statusNormalizado);
        ArquivoPdf atualizado = arquivoPdfRepository.save(arquivo);

        registrarAuditoria("STATUS_ATUALIZADO", atualizado.getNomeOriginal(), atualizado.getHashSha256(),
                atualizado.getTamanhoBytes(), atualizado.getId(),
                atualizado.getFornecedor() != null ? atualizado.getFornecedor().getId() : null,
                normalizarUsuario(usuarioResponsavel),
                String.format("Status de processamento alterado de '%s' para '%s'.", statusAnterior, statusNormalizado));

        return atualizado;
    }

    public void deletar(Long id, String usuarioResponsavel) {
        ArquivoPdf arquivo = buscarPorId(id);
        Path caminho = Paths.get(arquivo.getCaminhoArmazenamento());

        try {
            Files.deleteIfExists(caminho);
        } catch (IOException ignored) {
            // Mantém a remoção lógica do banco mesmo que o arquivo físico já tenha sido limpo
        }

        arquivoPdfRepository.delete(arquivo);

        registrarAuditoria("ARQUIVO_REMOVIDO", arquivo.getNomeOriginal(), arquivo.getHashSha256(),
                arquivo.getTamanhoBytes(), arquivo.getId(),
                arquivo.getFornecedor() != null ? arquivo.getFornecedor().getId() : null,
                normalizarUsuario(usuarioResponsavel),
                "Arquivo PDF removido do sistema.");
    }

    public List<LogAuditoriaPdf> listarAuditoria(String evento, String hashSha256) {
        if (evento != null && !evento.trim().isEmpty()) {
            return logAuditoriaPdfRepository.findByEventoIgnoreCaseOrderByDataHoraDesc(evento.trim());
        }
        if (hashSha256 != null && !hashSha256.trim().isEmpty()) {
            return logAuditoriaPdfRepository.findByHashSha256OrderByDataHoraDesc(hashSha256.trim().toLowerCase());
        }
        return logAuditoriaPdfRepository.findAllByOrderByDataHoraDesc();
    }

    private void registrarAuditoria(
            String evento,
            String nomeArquivo,
            String hashSha256,
            Long tamanhoBytes,
            Long arquivoPdfId,
            Long fornecedorId,
            String usuarioResponsavel,
            String detalhes) {

        LogAuditoriaPdf log = new LogAuditoriaPdf();
        log.setEvento(evento);
        log.setNomeArquivo(nomeArquivo != null ? nomeArquivo : "DESCONHECIDO");
        log.setHashSha256(hashSha256);
        log.setTamanhoBytes(tamanhoBytes);
        log.setArquivoPdfId(arquivoPdfId);
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
            return "documento.pdf";
        }
        String limpo = nomeOriginal.replace("\\", "/");
        int ultimaBarra = limpo.lastIndexOf('/');
        String apenasNome = ultimaBarra >= 0 ? limpo.substring(ultimaBarra + 1) : limpo;
        return apenasNome.trim().isEmpty() ? "documento.pdf" : apenasNome.trim();
    }

    private boolean possuiExtensaoPdf(String nomeArquivo) {
        return nomeArquivo != null && nomeArquivo.toLowerCase().endsWith(".pdf");
    }

    private boolean possuiAssinaturaPdfValida(byte[] conteudo) {
        // Assinatura oficial Magic Bytes de um PDF: "%PDF-" (0x25 0x50 0x44 0x46 0x2D)
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
