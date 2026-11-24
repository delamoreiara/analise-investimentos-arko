interface Props {
  data: Array<Record<string, string | number | boolean | null>>
  dividendYield: number | null
}

export function RentabilidadeChart({ data, dividendYield }: Props) {
  const values = data
    .map((item) => Number(item.valor ?? item.rentabilidade ?? item.cota))
    .filter((value) => Number.isFinite(value))

  const width = 720
  const height = 220
  const min = values.length > 0 ? Math.min(...values) : 0
  const max = values.length > 0 ? Math.max(...values) : 0
  const range = max - min || 1
  const points = values.length >= 2 ? values.map((value, index) => {
    const x = (index / (values.length - 1)) * width
    const y = height - ((value - min) / range) * (height - 30) - 15
    return `${x},${y}`
  }).join(' ') : ''

  if (data.length === 0 && dividendYield === null) {
    return (
      <section className="panel">
        <h2>Rentabilidade e dividendos</h2>
        <p className="unavailable">Dados indisponíveis nesta consulta.</p>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>Rentabilidade histórica</h2>
      {dividendYield !== null ? (
        <p className="yield">Dividend yield 12 meses: <strong>{dividendYield.toFixed(2)}%</strong></p>
      ) : (
        <p className="unavailable">Dividend yield 12 meses indisponível.</p>
      )}
      {values.length >= 2 ? (
        <svg className="chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Gráfico de rentabilidade histórica">
          <polyline points={points} fill="none" stroke="currentColor" strokeWidth="4" />
        </svg>
      ) : (
        <p className="unavailable">{values.length === 1 ? 'Existe um dado, mas não há série suficiente para gráfico.' : 'Série histórica indisponível.'}</p>
      )}
    </section>
  )
}
