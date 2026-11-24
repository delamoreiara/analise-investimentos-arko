import type { AnaliseDetalhada } from '../types/analysis'

const configuredApiUrl = import.meta.env.VITE_API_URL?.trim().replace(/\/+$/, '')
const API_URL = configuredApiUrl || (import.meta.env.DEV ? 'http://localhost:8080' : '')

export async function analyzeAsset(texto: string): Promise<AnaliseDetalhada> {
  if (!API_URL) {
    throw new Error('API não configurada. Defina VITE_API_URL com a URL HTTPS do backend e publique o frontend novamente.')
  }

  const params = new URLSearchParams({ texto })
  const response = await fetch(`${API_URL}/api/analyze/detalhado?${params}`, {
    method: 'POST',
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.mensagem ?? 'Não foi possível analisar o ativo.')
  }

  return response.json()
}
