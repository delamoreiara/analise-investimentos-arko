interface Props {
  dados: Record<string, string | number | boolean | null>
  fundamentos: Array<Record<string, string | number | boolean | null>>
}

const labels: Record<string, string> = {
  classe: 'Classe',
  tipo_ativo: 'Tipo',
  ticker: 'Ticker',
  emissor_nome: 'Emissor',
  emissor_porte: 'Porte',
  percentual_cdi: 'Taxa do CDI',
  indexador: 'Indexador',
  liquidez_dias: 'Liquidez (dias)',
  vencimento_ano: 'Vencimento',
}

export function FundamentosTable({ dados, fundamentos }: Props) {
  const rows = fundamentos.length > 0
    ? fundamentos.flatMap((item) => Object.entries(item))
    : Object.entries(dados)

  if (rows.length === 0) return null

  return (
    <section className="panel">
      <h2>Fundamentos</h2>
      <div className="table-wrap">
        <table>
          <thead><tr><th>Indicador</th><th>Valor</th></tr></thead>
          <tbody>
            {rows.map(([key, value]) => (
              <tr key={key}>
                <td>{labels[key] ?? key}</td>
                <td>{String(value ?? 'Não informado')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
