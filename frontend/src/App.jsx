import React, { useState, useEffect } from 'react';
import { api } from './services/api';
import {
  TrendingUp,
  TrendingDown,
  Upload,
  CheckCircle,
  AlertTriangle,
  FileText,
  Package,
  Truck,
  DollarSign,
  Layers,
  ArrowRight,
  RefreshCw,
  Search,
  ExternalLink,
  ShieldCheck,
  Percent
} from 'lucide-react';

export default function App() {
  const [abaAtiva, setAbaAtiva] = useState('analise');
  const [loading, setLoading] = useState(false);
  const [mensagem, setMensagem] = useState(null);

  // Dados globais
  const [produtos, setProdutos] = useState([]);
  const [fornecedores, setFornecedores] = useState([]);
  const [importacoes, setImportacoes] = useState([]);
  const [comparativoGeral, setComparativoGeral] = useState([]);

  // Estado para Reconciliação (US-04)
  const [importacaoSelecionada, setImportacaoSelecionada] = useState(null);
  const [itensPrevia, setItensPrevia] = useState([]);
  const [resumoPrevia, setResumoPrevia] = useState(null);
  const [itemEmResolucao, setItemEmResolucao] = useState(null);
  const [produtoVinculoId, setProdutoVinculoId] = useState('');

  // Estado para Upload (US-03)
  const [arquivoUpload, setArquivoUpload] = useState(null);
  const [fornecedorUploadId, setFornecedorUploadId] = useState('');
  const [observacaoUpload, setObservacaoUpload] = useState('');

  // Carregamento inicial
  useEffect(() => {
    carregarDadosIniciais();
  }, []);

  const carregarDadosIniciais = async () => {
    setLoading(true);
    try {
      const [prods, forns, imps, comps] = await Promise.all([
        api.getProdutos().catch(() => []),
        api.getFornecedores().catch(() => []),
        api.getImportacoes().catch(() => []),
        api.getComparativoGeral().catch(() => []),
      ]);
      setProdutos(prods);
      setFornecedores(forns);
      setImportacoes(imps);
      setComparativoGeral(comps);
      if (forns.length > 0 && !fornecedorUploadId) {
        setFornecedorUploadId(forns[0].id.toString());
      }
    } catch (err) {
      mostrarNotificacao('Erro ao carregar dados do servidor: ' + err.message, 'danger');
    } finally {
      setLoading(false);
    }
  };

  const mostrarNotificacao = (texto, tipo = 'success') => {
    setMensagem({ texto, tipo });
    setTimeout(() => setMensagem(null), 5000);
  };

  // Upload de PDF (US-03)
  const handleUploadPdf = async (e) => {
    e.preventDefault();
    if (!arquivoUpload) {
      mostrarNotificacao('Por favor, selecione um arquivo PDF.', 'warning');
      return;
    }

    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('arquivo', arquivoUpload);
      formData.append('fornecedorId', fornecedorUploadId);
      formData.append('usuarioResponsavel', 'Operador PriceOps');
      formData.append('observacao', observacaoUpload || 'Upload de cotação mensal');

      const novaImportacao = await api.uploadPdf(formData);
      mostrarNotificacao(`PDF '${novaImportacao.nomeOriginal}' importado e auditado com sucesso (SHA-256 gerado).`);
      setArquivoUpload(null);
      setObservacaoUpload('');
      const imps = await api.getImportacoes();
      setImportacoes(imps);
      setAbaAtiva('reconciliacao');
      selecionarImportacao(novaImportacao.id);
    } catch (err) {
      mostrarNotificacao(err.message, 'danger');
    } finally {
      setLoading(false);
    }
  };

  // Extração e Prévia de Itens (US-04)
  const selecionarImportacao = async (id) => {
    setLoading(true);
    try {
      setImportacaoSelecionada(id);
      // Tenta obter prévia se já extraída, ou aciona extração
      let previa;
      try {
        previa = await api.getPreviaImportacao(id);
      } catch {
        previa = await api.extrairItensPdf(id);
      }
      setResumoPrevia(previa);
      setItensPrevia(previa.itens || []);
    } catch (err) {
      mostrarNotificacao('Falha ao processar extração do PDF: ' + err.message, 'danger');
    } finally {
      setLoading(false);
    }
  };

  // Resolução de Pendência (US-04 / US-05)
  const handleResolverPendencia = async () => {
    if (!itemEmResolucao) return;

    setLoading(true);
    try {
      const payload = {
        acao: produtoVinculoId ? 'VINCULAR_PRODUTO_EXISTENTE' : 'CRIAR_NOVO_PRODUTO',
        produtoId: produtoVinculoId ? Number(produtoVinculoId) : null,
      };

      await api.resolverPendencia(importacaoSelecionada, itemEmResolucao.id, payload);
      mostrarNotificacao(`Item '${itemEmResolucao.descricao}' reconciliado com sucesso!`);
      setItemEmResolucao(null);
      setProdutoVinculoId('');

      // Recarrega itens e prévia
      const previa = await api.getPreviaImportacao(importacaoSelecionada);
      setResumoPrevia(previa);
      setItensPrevia(previa.itens || []);

      // Atualiza produtos e comparativos
      const [prods, comps] = await Promise.all([
        api.getProdutos().catch(() => []),
        api.getComparativoGeral().catch(() => []),
      ]);
      setProdutos(prods);
      setComparativoGeral(comps);
    } catch (err) {
      mostrarNotificacao('Erro ao resolver pendência: ' + err.message, 'danger');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="brand">
          <Layers size={26} />
          <span>PriceOps</span>
        </div>

        <ul className="nav-links">
          <li
            className={`nav-item ${abaAtiva === 'analise' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('analise')}
          >
            <TrendingUp size={18} />
            <span>Comparativo & Preços</span>
          </li>
          <li
            className={`nav-item ${abaAtiva === 'reconciliacao' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('reconciliacao')}
          >
            <AlertTriangle size={18} />
            <span>Reconciliação & PDFs</span>
          </li>
          <li
            className={`nav-item ${abaAtiva === 'importacoes' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('importacoes')}
          >
            <Upload size={18} />
            <span>Upload de Tabelas</span>
          </li>
          <li
            className={`nav-item ${abaAtiva === 'produtos' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('produtos')}
          >
            <Package size={18} />
            <span>Catálogo de Produtos</span>
          </li>
          <li
            className={`nav-item ${abaAtiva === 'fornecedores' ? 'active' : ''}`}
            onClick={() => setAbaAtiva('fornecedores')}
          >
            <Truck size={18} />
            <span>Fornecedores</span>
          </li>
        </ul>

        <div style={{ marginTop: 'auto', paddingTop: '1.5rem', borderTop: '1px solid #1e293b' }}>
          <div style={{ fontSize: '0.8rem', color: '#64748b' }}>
            PriceOps v1.0.0<br />
            Docker & CI Conectados
          </div>
        </div>
      </aside>

      {/* Conteúdo Principal */}
      <main className="main-content">
        <header className="topbar">
          <h1 className="topbar-title">
            {abaAtiva === 'analise' && 'Dashboard de Variações e Oportunidades de Compra (US-07)'}
            {abaAtiva === 'reconciliacao' && 'Extração Tabular & Resolução de Pendências (US-04/US-05)'}
            {abaAtiva === 'importacoes' && 'Upload e Auditoria de Tabelas em PDF (US-03)'}
            {abaAtiva === 'produtos' && 'Catálogo de Produtos Internos (US-01)'}
            {abaAtiva === 'fornecedores' && 'Fornecedores e Distribuidores (US-02)'}
          </h1>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <button className="btn btn-outline" onClick={carregarDadosIniciais} disabled={loading}>
              <RefreshCw size={15} className={loading ? 'spin' : ''} />
              <span>Atualizar</span>
            </button>
            <span className="badge badge-success">
              <ShieldCheck size={14} /> Backend Online
            </span>
          </div>
        </header>

        <div className="content-body">
          {/* Notificação / Alert */}
          {mensagem && (
            <div className={`card`} style={{ borderLeft: `4px solid var(--${mensagem.tipo})`, padding: '1rem' }}>
              <strong>{mensagem.tipo === 'success' ? 'Sucesso!' : 'Atenção:'}</strong> {mensagem.texto}
            </div>
          )}

          {/* ABA 1: DASHBOARD ANALÍTICO (US-07 / US-06) */}
          {abaAtiva === 'analise' && (
            <>
              <div className="stats-grid">
                <div className="stat-card">
                  <div className="stat-icon" style={{ backgroundColor: '#ecfdf5', color: '#10b981' }}>
                    <DollarSign size={24} />
                  </div>
                  <div>
                    <div className="stat-number">{comparativoGeral.length}</div>
                    <div className="stat-label">Itens com Cotações Ativas</div>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon" style={{ backgroundColor: '#eff6ff', color: '#2563eb' }}>
                    <Package size={24} />
                  </div>
                  <div>
                    <div className="stat-number">{produtos.length}</div>
                    <div className="stat-label">Produtos Cadastrados</div>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon" style={{ backgroundColor: '#fef3c7', color: '#d97706' }}>
                    <Truck size={24} />
                  </div>
                  <div>
                    <div className="stat-number">{fornecedores.length}</div>
                    <div className="stat-label">Fornecedores Monitorados</div>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-icon" style={{ backgroundColor: '#fdf2f8', color: '#db2777' }}>
                    <Percent size={24} />
                  </div>
                  <div>
                    <div className="stat-number">
                      {comparativoGeral.length > 0
                        ? `${Math.max(...comparativoGeral.map((c) => c.economiaMaximaPercentual || 0))}%`
                        : '0%'}
                    </div>
                    <div className="stat-label">Economia Máxima Detectada</div>
                  </div>
                </div>
              </div>

              <div className="card">
                <div className="card-header">
                  <h2 className="card-title">
                    <TrendingUp size={20} color="var(--primary)" />
                    Matriz Comparativa de Fornecedores & Recomendações de Compra (US-07)
                  </h2>
                </div>

                {comparativoGeral.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                    Nenhuma cotação encontrada no momento. Importe uma tabela em PDF para gerar comparativos.
                  </div>
                ) : (
                  <div className="table-responsive">
                    <table>
                      <thead>
                        <tr>
                          <th>Código</th>
                          <th>Produto</th>
                          <th>Melhor Fornecedor</th>
                          <th>Menor Preço</th>
                          <th>Maior Preço</th>
                          <th>Economia Potencial</th>
                          <th>Recomendação Inteligente</th>
                        </tr>
                      </thead>
                      <tbody>
                        {comparativoGeral.map((item) => (
                          <tr key={item.produtoId}>
                            <td><strong>{item.codigo}</strong></td>
                            <td>{item.nome}</td>
                            <td>
                              <span className="badge badge-success">
                                {item.melhorFornecedorNome || 'N/D'}
                              </span>
                            </td>
                            <td>
                              <strong style={{ color: 'var(--success)' }}>
                                R$ {Number(item.menorPreco || 0).toFixed(2)}
                              </strong>
                            </td>
                            <td>R$ {Number(item.maiorPreco || 0).toFixed(2)}</td>
                            <td>
                              {item.economiaMaximaAbsoluta > 0 ? (
                                <span className="badge badge-info">
                                  R$ {Number(item.economiaMaximaAbsoluta).toFixed(2)} ({item.economiaMaximaPercentual}%)
                                </span>
                              ) : (
                                <span className="badge badge-secondary">Preço Único</span>
                              )}
                            </td>
                            <td>
                              <span style={{ fontSize: '0.85rem', color: '#475569' }}>
                                {item.recomendacaoCompra}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          )}

          {/* ABA 2: RECONCILIAÇÃO & PENDÊNCIAS (US-04 / US-05) */}
          {abaAtiva === 'reconciliacao' && (
            <div className="card">
              <div className="card-header">
                <h2 className="card-title">
                  <AlertTriangle size={20} color="var(--warning)" />
                  Pré-visualização e Resolução de Pendências de Tabelas PDF
                </h2>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <select
                    className="form-control"
                    value={importacaoSelecionada || ''}
                    onChange={(e) => selecionarImportacao(e.target.value)}
                  >
                    <option value="">Selecione uma importação...</option>
                    {importacoes.map((imp) => (
                      <option key={imp.id} value={imp.id}>
                        #{imp.id} - {imp.nomeOriginal} ({imp.fornecedor?.nomeFantasia || 'Fornecedor'})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {resumoPrevia && (
                <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem' }}>
                  <span className="badge badge-info">Total Itens: {resumoPrevia.totalItensExtraidos}</span>
                  <span className="badge badge-success">Reconciliados: {resumoPrevia.itensVinculados}</span>
                  <span className="badge badge-warning">Pendências: {resumoPrevia.itensPendentes}</span>
                </div>
              )}

              {itensPrevia.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
                  {importacaoSelecionada
                    ? 'Nenhum item extraído nesta tabela.'
                    : 'Selecione uma tabela importada acima para visualizar os itens e resolver pendências.'}
                </div>
              ) : (
                <div className="table-responsive">
                  <table>
                    <thead>
                      <tr>
                        <th>Linha</th>
                        <th>Cód. Fornecedor</th>
                        <th>Descrição Extraída do PDF</th>
                        <th>Preço Unit.</th>
                        <th>Status Reconciliação</th>
                        <th>Produto Catálogo Vinculado</th>
                        <th>Ações</th>
                      </tr>
                    </thead>
                    <tbody>
                      {itensPrevia.map((item) => (
                        <tr key={item.id}>
                          <td>{item.numeroLinhaTabela}</td>
                          <td><code>{item.codigoFornecedor || 'S/C'}</code></td>
                          <td>{item.descricao}</td>
                          <td><strong>R$ {Number(item.precoCotado || 0).toFixed(2)}</strong></td>
                          <td>
                            {item.statusItem === 'VINCULADO' && (
                              <span className="badge badge-success">
                                <CheckCircle size={12} /> Vinculado ({item.confiancaMatching || 100}%)
                              </span>
                            )}
                            {item.statusItem === 'PENDENTE_VINCULO' && (
                              <span className="badge badge-warning">
                                <AlertTriangle size={12} /> Pendente ({item.confiancaMatching || 0}%)
                              </span>
                            )}
                            {item.statusItem === 'IGNORADO' && (
                              <span className="badge badge-secondary">Ignorado</span>
                            )}
                          </td>
                          <td>
                            {item.produtoNome ? (
                              <span><strong>{item.produtoCodigo}:</strong> {item.produtoNome}</span>
                            ) : (
                              <span style={{ color: '#94a3b8', fontStyle: 'italic' }}>Não vinculado</span>
                            )}
                          </td>
                          <td>
                            {item.statusItem === 'PENDENTE_VINCULO' && (
                              <button
                                className="btn btn-primary"
                                style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                                onClick={() => {
                                  setItemEmResolucao(item);
                                  setProdutoVinculoId('');
                                }}
                              >
                                Resolver
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* ABA 3: UPLOAD E AUDITORIA DE PDF (US-03) */}
          {abaAtiva === 'importacoes' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '1.5rem' }}>
              <div className="card">
                <h2 className="card-title" style={{ marginBottom: '1rem' }}>
                  <Upload size={18} color="var(--primary)" />
                  Novo Upload de Tabela PDF
                </h2>

                <form onSubmit={handleUploadPdf}>
                  <div className="form-group">
                    <label className="form-label">Fornecedor / Distribuidor</label>
                    <select
                      className="form-control"
                      value={fornecedorUploadId}
                      onChange={(e) => setFornecedorUploadId(e.target.value)}
                      required
                    >
                      {fornecedores.map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.nomeFantasia} ({f.cnpj})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Arquivo PDF da Tabela</label>
                    <input
                      type="file"
                      accept=".pdf"
                      className="form-control"
                      onChange={(e) => setArquivoUpload(e.target.files[0])}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Observações</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Ex: Tabela Promocional Novembro"
                      value={observacaoUpload}
                      onChange={(e) => setObservacaoUpload(e.target.value)}
                    />
                  </div>

                  <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '0.5rem' }} disabled={loading}>
                    <Upload size={16} /> Fazer Upload e Auditar
                  </button>
                </form>
              </div>

              <div className="card">
                <h2 className="card-title" style={{ marginBottom: '1rem' }}>
                  <FileText size={18} color="var(--primary)" />
                  Histórico de Tabelas Importadas & Auditoria (SHA-256)
                </h2>

                <div className="table-responsive">
                  <table>
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Arquivo</th>
                        <th>Fornecedor</th>
                        <th>Hash SHA-256 (Auditoria)</th>
                        <th>Data Importação</th>
                        <th>Ação</th>
                      </tr>
                    </thead>
                    <tbody>
                      {importacoes.map((imp) => (
                        <tr key={imp.id}>
                          <td>#{imp.id}</td>
                          <td><strong>{imp.nomeOriginal}</strong></td>
                          <td>{imp.fornecedor?.nomeFantasia}</td>
                          <td>
                            <code style={{ fontSize: '0.75rem' }} title={imp.hashSha256}>
                              {imp.hashSha256?.substring(0, 16)}...
                            </code>
                          </td>
                          <td>{new Date(imp.importadoEm).toLocaleDateString('pt-BR')}</td>
                          <td>
                            <button
                              className="btn btn-outline"
                              style={{ padding: '0.25rem 0.5rem', fontSize: '0.8rem' }}
                              onClick={() => {
                                setAbaAtiva('reconciliacao');
                                selecionarImportacao(imp.id);
                              }}
                            >
                              Ver Itens
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ABA 4: CATÁLOGO DE PRODUTOS (US-01) */}
          {abaAtiva === 'produtos' && (
            <div className="card">
              <div className="card-header">
                <h2 className="card-title">
                  <Package size={20} color="var(--primary)" />
                  Catálogo Interno de Produtos
                </h2>
              </div>
              <div className="table-responsive">
                <table>
                  <thead>
                    <tr>
                      <th>Código SKU</th>
                      <th>Descrição</th>
                      <th>Categoria</th>
                      <th>Custo Compra Atual</th>
                      <th>Preço Venda</th>
                      <th>Estoque Atual</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {produtos.map((p) => (
                      <tr key={p.id}>
                        <td><strong>{p.codigo}</strong></td>
                        <td>{p.nome}</td>
                        <td>{p.categoria}</td>
                        <td>R$ {Number(p.precoCompraAtual || 0).toFixed(2)}</td>
                        <td><strong>R$ {Number(p.precoVendaAtual || 0).toFixed(2)}</strong></td>
                        <td>{p.estoqueAtual} {p.unidadeMedida}</td>
                        <td>
                          <span className="badge badge-success">{p.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ABA 5: FORNECEDORES (US-02) */}
          {abaAtiva === 'fornecedores' && (
            <div className="card">
              <div className="card-header">
                <h2 className="card-title">
                  <Truck size={20} color="var(--primary)" />
                  Fornecedores e Distribuidores Homologados
                </h2>
              </div>
              <div className="table-responsive">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Nome Fantasia</th>
                      <th>Razão Social</th>
                      <th>CNPJ</th>
                      <th>Email Comercial</th>
                      <th>Prazo Entrega</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {fornecedores.map((f) => (
                      <tr key={f.id}>
                        <td>#{f.id}</td>
                        <td><strong>{f.nomeFantasia}</strong></td>
                        <td>{f.razaoSocial}</td>
                        <td>{f.cnpj}</td>
                        <td>{f.email}</td>
                        <td>{f.prazoEntregaDias} dias</td>
                        <td>
                          <span className="badge badge-success">{f.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Modal de Resolução de Pendência (US-04) */}
      {itemEmResolucao && (
        <div className="modal-backdrop">
          <div className="modal-content">
            <h3 style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <AlertTriangle color="var(--warning)" />
              Resolver Pendência de Reconciliação
            </h3>

            <p style={{ fontSize: '0.9rem', color: '#475569', marginBottom: '1rem' }}>
              <strong>Item do PDF:</strong> {itemEmResolucao.descricao} <br />
              <strong>Preço Cotado:</strong> R$ {Number(itemEmResolucao.precoCotado || 0).toFixed(2)}
            </p>

            <div className="form-group">
              <label className="form-label">Selecione o Produto correspondente no Catálogo Interno:</label>
              <select
                className="form-control"
                value={produtoVinculoId}
                onChange={(e) => setProdutoVinculoId(e.target.value)}
              >
                <option value="">-- Cadastrar como Novo Produto no Catálogo --</option>
                {produtos.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.codigo} - {p.nome} (Atual: R$ {Number(p.precoCompraAtual || 0).toFixed(2)})
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setItemEmResolucao(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleResolverPendencia}
                disabled={loading}
              >
                Confirmar Reconciliação
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
