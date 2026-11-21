import { useState } from 'react'
import { analyzeAsset } from '../services/analysisService'
import type { AnaliseDetalhada } from '../types/analysis'

export function useAssetAnalysis() {
  const [analysis, setAnalysis] = useState<AnaliseDetalhada | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function analyze(texto: string) {
    setLoading(true)
    setError(null)
    try {
      setAnalysis(await analyzeAsset(texto))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Erro inesperado.')
    } finally {
      setLoading(false)
    }
  }

  return { analysis, loading, error, analyze }
}
