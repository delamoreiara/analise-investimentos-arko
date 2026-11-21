import type { AnaliseDetalhada } from '../types/analysis'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export async function analyzeAsset(texto: string): Promise<AnaliseDetalhada> {
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
