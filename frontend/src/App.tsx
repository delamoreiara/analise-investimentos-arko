import './App.css'
import { AssetSearch } from './components/AssetSearch'
import { FundamentosTable } from './components/FundamentosTable'
import { RentabilidadeChart } from './components/RentabilidadeChart'
import { useAssetAnalysis } from './hooks/useAssetAnalysis'

function App() {
  const { analysis, loading, error, analyze } = useAssetAnalysis()

  const decision = analysis?.resultado.decisao
  const decisionLabel =
    decision === 'REPROVA' || decision === 'NAORECOMENDA'
      ? 'Não recomenda'
      : decision === 'CONDICIONA' || decision === 'ELEGIVEL'
        ? 'Recomenda com condição'
        : decision === 'APROVA' || decision === 'RECOMENDA'
          ? 'Recomenda'
          : 'Resultado indeterminado'

  return (
    <main className="app-shell">
      <header className="brand">
        <span className="brand-mark">A</span>
        <span>Arko Investimentos</span>
      </header>

      <section className="hero">
        <p className="eyebrow">Recomendador de ativos</p>
        <h1>Vale a pena investir?</h1>
        <p className="subtitle">Descreva o ativo como você falaria com um assessor.</p>
        <AssetSearch loading={loading} onAnalyze={analyze} />
      </section>

      {error && <div className="alert error">{error}</div>}

      {analysis && (
        <div className="results">
          <section className={`verdict ${String(decision).toLowerCase()}`}>
            <p>Veredito</p>
            <h2>{decisionLabel}</h2>
            {analysis.resultado.objetivo && <span>Objetivo: {analysis.resultado.objetivo}</span>}
            <p className="message">{analysis.mensagem}</p>
          </section>

          {analysis.resultado.regrasAplicadas.length > 0 && (
            <section className="panel">
              <h2>Regras aplicadas</h2>
              <ul className="rules">
                {analysis.resultado.regrasAplicadas.map((rule, index) => (
                  <li key={rule}>
                    <strong>{rule}</strong>
                    {analysis.resultado.justificativas[index] && (
                      <span>{analysis.resultado.justificativas[index]}</span>
                    )}
                  </li>
                ))}
              </ul>
            </section>
          )}

          <FundamentosTable dados={analysis.dadosExtraidos} fundamentos={analysis.fundamentos} />
          <RentabilidadeChart data={analysis.rentabilidade} dividendYield={analysis.dividendYield12m} />

          {analysis.alertas.length > 0 && (
            <section className="panel alerts">
              <h2>Observações</h2>
              {analysis.alertas.map((alert) => <p key={alert}>{alert}</p>)}
            </section>
          )}
        </div>
      )}
    </main>
  )
}

export default App
