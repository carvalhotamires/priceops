package com.uniesp.priceops.controller;

import com.uniesp.priceops.exception.DuplicidadeImportacaoException;
import com.uniesp.priceops.model.ImportacaoPdf;
import com.uniesp.priceops.model.LogAuditoriaPdf;
import com.uniesp.priceops.service.ImportacaoPdfService;
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

@RestController
@RequestMapping({"/api/importacoes-pdf", "/api/arquivos-pdf"})
@CrossOrigin(origins = "*") // Permite integração futura com o frontend React
public class ImportacaoPdfController {

    @Autowired
    private ImportacaoPdfService importacaoPdfService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> realizarUpload(
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
            @RequestParam(value = "fornecedorId", required = false) Long fornecedorId,
            @RequestParam(value = "usuarioResponsavel", required = false) String usuarioResponsavel,
            @RequestParam(value = "observacao", required = false) String observacao) {
        try {
            ImportacaoPdf salva = importacaoPdfService.realizarUpload(arquivo, fornecedorId, usuarioResponsavel, observacao);
            return ResponseEntity.status(HttpStatus.CREATED).body(salva);
        } catch (DuplicidadeImportacaoException e) {
            ImportacaoPdf existente = e.getImportacaoExistente();
            Map<String, Object> corpoConflito = new LinkedHashMap<>();
            corpoConflito.put("erro", "DUPLICIDADE_IMPORTACAO_PDF");
            corpoConflito.put("mensagem", e.getMessage());
            corpoConflito.put("hashSha256", existente.getHashSha256());
            corpoConflito.put("fornecedorId", existente.getFornecedor().getId());
            corpoConflito.put("importacaoExistenteId", existente.getId());
            corpoConflito.put("importacaoExistenteNome", existente.getNomeOriginal());
            corpoConflito.put("importadoEm", existente.getImportadoEm());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(corpoConflito);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<ImportacaoPdf>> listar(
            @RequestParam(required = false) Long fornecedorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nome) {
        return ResponseEntity.ok(importacaoPdfService.listarImportacoes(fornecedorId, status, nome));
    }

    @GetMapping("/auditoria")
    public ResponseEntity<List<LogAuditoriaPdf>> listarAuditoria(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String hashSha256,
            @RequestParam(required = false) Long fornecedorId) {
        return ResponseEntity.ok(importacaoPdfService.listarAuditoria(status, hashSha256, fornecedorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(importacaoPdfService.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> baixarArquivo(
            @PathVariable Long id,
            @RequestParam(required = false) String usuarioResponsavel) {
        try {
            ImportacaoPdf metadados = importacaoPdfService.buscarPorId(id);
            byte[] conteudo = importacaoPdfService.carregarConteudoArquivo(id, usuarioResponsavel);

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
            ImportacaoPdf atualizada = importacaoPdfService.atualizarStatusImportacao(id, status, usuarioResponsavel);
            return ResponseEntity.ok(atualizada);
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
            importacaoPdfService.deletar(id, usuarioResponsavel);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
