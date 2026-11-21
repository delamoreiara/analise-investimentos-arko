import type { FormEvent } from 'react'

interface Props {
  loading: boolean
  onAnalyze: (text: string) => void
}

export function AssetSearch({ loading, onAnalyze }: Props) {
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const text = String(form.get('asset') ?? '').trim()
    if (text) onAnalyze(text)
  }

  return (
    <form className="search" onSubmit={submit}>
      <label htmlFor="asset">Qual ativo você quer analisar?</label>
      <div className="search-row">
        <input
          id="asset"
          name="asset"
          type="text"
          placeholder="Ex.: CDB Itaú 100% CDI liquidez diária"
          disabled={loading}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Analisando...' : 'Analisar ativo'}
        </button>
      </div>
    </form>
  )
}
