const BASE_URL = '/api';

async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const config = {
    ...options,
    headers: {
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, config);
    if (!response.ok) {
      let errorMessage = `Erro HTTP ${response.status}`;
      try {
        const errorData = await response.json();
        errorMessage = errorData.mensagem || errorData.erro || JSON.stringify(errorData);
      } catch {
        const text = await response.text();
        if (text) errorMessage = text;
      }
      throw new Error(errorMessage);
    }

    if (response.status === 204) {
      return null;
    }

    return await response.json();
  } catch (error) {
    console.error(`Falha na requisição para ${endpoint}:`, error);
    throw error;
  }
}

export const api = {
  // Produtos (US-01)
  getProdutos: () => request('/produtos'),
  getProdutoPorId: (id) => request(`/produtos/${id}`),

  // Fornecedores (US-02)
  getFornecedores: () => request('/fornecedores'),

  // Upload & Auditoria de PDFs (US-03)
  getImportacoes: () => request('/importacoes-pdf'),
  getAuditoria: () => request('/importacoes-pdf/auditoria'),
  uploadPdf: (formData) =>
    request('/importacoes-pdf/upload', {
      method: 'POST',
      body: formData,
    }),

  // Extração e Pendências (US-04, US-05)
  extrairItensPdf: (importacaoId, usuario = 'Operador PriceOps') =>
    request(`/importacoes-pdf/${importacaoId}/extrair?usuarioResponsavel=${encodeURIComponent(usuario)}`, {
      method: 'POST',
    }),
  getPreviaImportacao: (importacaoId) => request(`/importacoes-pdf/${importacaoId}/pre-visualizacao`),
  getItensImportacao: (importacaoId, status = '') => {
    const q = status ? `?status=${encodeURIComponent(status)}` : '';
    return request(`/importacoes-pdf/${importacaoId}/itens${q}`);
  },
  resolverPendencia: (importacaoId, itemId, payload, usuario = 'Operador PriceOps') =>
    request(`/importacoes-pdf/${importacaoId}/itens/${itemId}/resolver-pendencia?usuarioResponsavel=${encodeURIComponent(usuario)}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),

  // Histórico de Preços (US-06)
  getHistoricoPrecos: (produtoId = '', fornecedorId = '') => {
    const params = new URLSearchParams();
    if (produtoId) params.append('produtoId', produtoId);
    if (fornecedorId) params.append('fornecedorId', fornecedorId);
    const q = params.toString() ? `?${params.toString()}` : '';
    return request(`/historico-precos${q}`);
  },

  // Análise de Preços e Comparativo (US-07)
  getVariacaoHistorica: (produtoId) => request(`/analise-precos/produtos/${produtoId}/variacao`),
  getComparativoFornecedores: (produtoId) => request(`/analise-precos/produtos/${produtoId}/comparativo`),
  getComparativoGeral: () => request('/analise-precos/comparativo-geral'),
};
