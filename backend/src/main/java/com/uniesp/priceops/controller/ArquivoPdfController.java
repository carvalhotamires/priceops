package com.uniesp.priceops.controller;

import com.uniesp.priceops.exception.DuplicidadeArquivoException;
import com.uniesp.priceops.model.ArquivoPdf;
import com.uniesp.priceops.model.LogAuditoriaPdf;
import com.uniesp.priceops.service.ArquivoPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api/arquivos-pdf")
@CrossOrigin(origins = "*") // Permite integração futura com o frontend React
public class ArquivoPdfController {

    @Autowired
    private ArquivoPdfService arquivoPdfService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> realizarUpload(
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            @RequestParam(value = "fornecedorId", required = false) Long fornecedorId,
            @RequestParam(value = "usuarioResponsavel", required = false) String usuarioResponsavel,
            @RequestParam(value = "observacao", required = false) String observacao) {
        try {
            ArquivoPdf salvo = arquivoPdfService.realizarUpload(arquivo, fornecedorId, usuarioResponsavel, observacao);
            return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
        } catch (DuplicidadeArquivoException e) {
            ArquivoPdf existente = e.getArquivoExistente();
            Map<String, Object> corpoConflito = new LinkedHashMap<>();
            corpoConflito.put("erro", "DUPLICIDADE_ARQUIVO_PDF");
            corpoConflito.put("mensagem", e.getMessage());
            corpoConflito.put("hashSha256", existente.getHashSha256());
            corpoConflito.put("arquivoExistenteId", existente.getId());
            corpoConflito.put("arquivoExistenteNome", existente.getNomeOriginal());
            corpoConflito.put("enviadoEm", existente.getEnviadoEm());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(corpoConflito);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<ArquivoPdf>> listar(
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nome) {
        return ResponseEntity.ok(arquivoPdfService.listarArquivos(fornecedorId, status, nome));
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<LogAuditoriaPdf>> listarAuditoria(
            @RequestParam(required = false) String evento,
            @RequestParam(required = false) String hashSha256) {
        return ResponseEntity.ok(arquivoPdfService.listarAuditoria(evento, hashSha256));
    }

    @GetMapping("/verificar-hash/{hashSha256}")
    public ResponseEntity<?> verificarHash(@PathVariable String hashSha256) {
        try {
            Optional<ArquivoPdf> existente = arquivoPdfService.verificarDuplicidadePorHash(hashSha256);
            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("duplicado", existente.isPresent());
            existente.ifPresent(arq -> {
                resposta.put("arquivoExistenteId", arq.getId());
                resposta.put("arquivoExistenteNome", arq.getNomeOriginal());
                resposta.put("enviadoEm", arq.getEnviadoEm());
            });
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(arquivoPdfService.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> baixarArquivo(
            @PathVariable Long id,
            @RequestParam(required = false) String usuarioResponsavel) {
        try {
            ArquivoPdf metadados = arquivoPdfService.buscarPorId(id);
            byte[] conteudo = arquivoPdfService.carregarConteudoArquivo(id, usuarioResponsavel);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + metadados.getNomeOriginal().replace("\"", "") + "\"")
                    .body(conteudo);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String usuarioResponsavel) {
        try {
            ArquivoPdf atualizado = arquivoPdfService.atualizarStatusProcessamento(id, status, usuarioResponsavel);
            return ResponseEntity.ok(atualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(
            @PathVariable Long id,
            @RequestParam(required = false) String usuarioResponsavel) {
        try {
            arquivoPdfService.deletar(id, usuarioResponsavel);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
