interface Props {
  data: Array<Record<string, string | number | boolean | null>>
  dividendYield: number | null
}

export function RentabilidadeChart({ data, dividendYield }: Props) {
  if (data.length === 0) return null

  const values = data
    .map((item) => Number(item.valor ?? item.rentabilidade ?? item.cota))
    .filter((value) => Number.isFinite(value))

  if (values.length < 2) return null

  const width = 720
  const height = 220
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  const points = values.map((value, index) => {
    const x = (index / (values.length - 1)) * width
    const y = height - ((value - min) / range) * (height - 30) - 15
    return `${x},${y}`
  }).join(' ')

  return (
    <section className="panel">
      <h2>Rentabilidade histórica</h2>
      {dividendYield !== null && (
        <p className="yield">Dividend yield 12 meses: <strong>{dividendYield.toFixed(2)}%</strong></p>
      )}
      <svg className="chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Gráfico de rentabilidade histórica">
        <polyline points={points} fill="none" stroke="currentColor" strokeWidth="4" />
      </svg>
    </section>
  )
}
